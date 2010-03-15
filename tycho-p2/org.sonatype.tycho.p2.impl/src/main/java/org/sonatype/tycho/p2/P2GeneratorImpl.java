package org.sonatype.tycho.p2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.actions.ICapabilityAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.sonatype.tycho.p2.facade.P2Generator;
import org.sonatype.tycho.p2.facade.internal.P2Resolver;
import org.sonatype.tycho.p2.maven.repository.xmlio.ArtifactsIO;
import org.sonatype.tycho.p2.maven.repository.xmlio.MetadataIO;

@SuppressWarnings( "restriction" )
public class P2GeneratorImpl
    implements P2Generator
{
    private static final String[] SUPPORTED_TYPES = { P2Resolver.TYPE_ECLIPSE_PLUGIN,
        P2Resolver.TYPE_ECLIPSE_TEST_PLUGIN, P2Resolver.TYPE_ECLIPSE_FEATURE, P2Resolver.TYPE_ECLIPSE_UPDATE_SITE,
        P2Resolver.TYPE_ECLIPSE_APPLICATION };

    /**
     * Whether we need full p2 metadata (false) or just required capabilities.
     */
    private boolean dependenciesOnly;

    private IProgressMonitor monitor = new NullProgressMonitor();

    public P2GeneratorImpl( boolean dependenciesOnly )
    {
        this.dependenciesOnly = dependenciesOnly;
    }

    public void generateMetadata( File location, String packaging, String groupId, String artifactId, String version,
                                  File content, File artifacts )
        throws IOException
    {
        LinkedHashSet<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();
        LinkedHashSet<IArtifactDescriptor> artifactDescriptors = new LinkedHashSet<IArtifactDescriptor>();

        generateMetadata( location, packaging, groupId, artifactId, version, null, units, artifactDescriptors );

        new MetadataIO().writeXML( units, content );
        new ArtifactsIO().writeXML( artifactDescriptors, artifacts );
    }

    private IRequiredCapability[] extractExtraEntriesAsIURequirement( File location )
    {
        Properties buildProperties = loadProperties( location );
        if ( buildProperties == null || buildProperties.size() == 0 )
            return null;
        ArrayList<IRequiredCapability> result = new ArrayList<IRequiredCapability>();
        Set<Entry<Object, Object>> pairs = buildProperties.entrySet();
        for ( Entry<Object, Object> pair : pairs )
        {
            if ( !( pair.getValue() instanceof String ) )
                continue;
            String buildPropertyKey = (String) pair.getKey();
            if ( buildPropertyKey.startsWith( "extra." ) )
            {
                createRequirementFromExtraClasspathProperty( result, ( (String) pair.getValue() ).split( "," ) );
            }
        }

        String extra = buildProperties.getProperty( "jars.extra.classpath" );
        if ( extra != null )
        {
            createRequirementFromExtraClasspathProperty( result, extra.split( "," ) );
        }
        if ( result.isEmpty() )
            return null;
        return result.toArray( new IRequiredCapability[result.size()] );
    }

    private void createRequirementFromExtraClasspathProperty( ArrayList<IRequiredCapability> result, String[] urls )
    {
        for ( int i = 0; i < urls.length; i++ )
        {
            createRequirementFromPlatformURL( result, urls[i].trim() );
        }
    }

    private void createRequirementFromPlatformURL( ArrayList<IRequiredCapability> result, String url )
    {
        Pattern platformURL = Pattern.compile( "platform:/(plugin|fragment)/([^/]*)(/)*.*" );
        Matcher m = platformURL.matcher( url );
        if ( m.matches() )
            result.add( MetadataFactory.createRequiredCapability( IInstallableUnit.NAMESPACE_IU_ID, m.group( 2 ),
                                                                  VersionRange.emptyRange, null, false, false ) );
    }

    private static Properties loadProperties( File project )
    {
        File file = new File( project, "build.properties" );

        Properties buildProperties = new Properties();
        if ( file.canRead() )
        {
            InputStream is = null;
            try
            {
                try
                {
                    is = new FileInputStream( file );
                    buildProperties.load( is );
                }
                finally
                {
                    if ( is != null )
                        is.close();
                }
            }
            catch ( Exception e )
            {
                // ignore
            }
        }

        return buildProperties;
    }

    @SuppressWarnings( "unchecked" )
    public void generateMetadata( File location, String packaging, String groupId, String artifactId, String version,
                                  List<Properties> environments, Set<IInstallableUnit> units,
                                  Set<IArtifactDescriptor> artifacts )
    {
        TransientArtifactRepository artifactsRepository = new TransientArtifactRepository();

        PublisherInfo request = new PublisherInfo();
        request.setArtifactRepository( artifactsRepository );
        request.setMetadataRepository( new TransientMetadataRepository() );

        request.addAdvice( new MavenPropertiesAdvice( groupId, artifactId, version ) );
        final IRequiredCapability[] extraRequirements = extractExtraEntriesAsIURequirement( location );
        request.addAdvice( new ICapabilityAdvice()
        {

            public boolean isApplicable( String configSpec, boolean includeDefault, String id, Version version )
            {
                return true;
            }

            public IRequiredCapability[] getRequiredCapabilities( InstallableUnitDescription iu )
            {
                return extraRequirements;
            }

            public IProvidedCapability[] getProvidedCapabilities( InstallableUnitDescription iu )
            {
                return null;
            }

            public IRequiredCapability[] getMetaRequiredCapabilities( InstallableUnitDescription iu )
            {
                return null;
            }
        } );
        IPublisherAction[] actions = getPublisherActions( location, packaging, artifactId, version, environments );

        PublisherResult result = new PublisherResult();

        new Publisher( request, result ).publish( actions, monitor );

        Collector unitsCollector = result.query( InstallableUnitQuery.ANY, new Collector(), monitor );

        if ( units != null )
        {
            units.addAll( unitsCollector.toCollection() );
        }

        if ( artifacts != null )
        {
            artifacts.addAll( artifactsRepository.getArtifactDescriptors() );
        }
    }

    private IPublisherAction[] getPublisherActions( File location, String packaging, String id, String version,
                                                    List<Properties> environments )
    {
        if ( P2Resolver.TYPE_ECLIPSE_PLUGIN.equals( packaging )
            || P2Resolver.TYPE_ECLIPSE_TEST_PLUGIN.equals( packaging ) )
        {
            return new IPublisherAction[] { new BundlesAction( new File[] { location } ) };
        }
        else if ( P2Resolver.TYPE_ECLIPSE_FEATURE.equals( packaging ) )
        {
            return new IPublisherAction[] { new FeaturesAction( new File[] { location } ) };
        }
        else if ( P2Resolver.TYPE_ECLIPSE_APPLICATION.equals( packaging ) )
        {
            String product = new File( location, id + ".product" ).getAbsolutePath();
            try
            {
                IProductDescriptor productDescriptor = new ProductFile( product );
                if ( dependenciesOnly )
                {
                    return new IPublisherAction[] { new ProductDependenciesAction( productDescriptor, environments ) };
                }
                else
                {
                    return new IPublisherAction[] { new ProductAction( product, productDescriptor, null, null ) };
                }
            }
            catch ( Exception e )
            {
                throw new RuntimeException( e );
            }
        }
        else if ( P2Resolver.TYPE_ECLIPSE_UPDATE_SITE.equals( packaging ) )
        {
            if ( dependenciesOnly )
            {
                return new IPublisherAction[] { new SiteDependenciesAction( location, id, version ) };
            }
            else
            {
                return new IPublisherAction[] { new SiteXMLAction( location.toURI(), null ) };
            }
        }
        else if ( location.isFile() && location.getName().endsWith( ".jar" ) )
        {
            return new IPublisherAction[] { new BundlesAction( new File[] { location } ) };
        }

        throw new IllegalArgumentException();
    }

    public boolean isSupported( String type )
    {
        return Arrays.asList( SUPPORTED_TYPES ).contains( type );
    }
}
