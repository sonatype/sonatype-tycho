package org.sonatype.tycho.p2;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
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
    private static final String[] SUPPORTED_TYPES =
        { P2Resolver.TYPE_ECLIPSE_PLUGIN, P2Resolver.TYPE_ECLIPSE_TEST_PLUGIN, P2Resolver.TYPE_ECLIPSE_FEATURE,
            P2Resolver.TYPE_ECLIPSE_UPDATE_SITE, P2Resolver.TYPE_ECLIPSE_APPLICATION };

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

        generateMetadata( location, packaging, groupId, artifactId, version, units, artifactDescriptors );

        new MetadataIO().writeXML( units, content );
        new ArtifactsIO().writeXML( artifactDescriptors, artifacts );
    }

    public void generateMetadata( File location, String packaging, String groupId, String artifactId, String version,
                                  Set<IInstallableUnit> units, Set<IArtifactDescriptor> artifacts )
    {
        TransientArtifactRepository artifactsRepository = new TransientArtifactRepository();

        PublisherInfo request = new PublisherInfo();
        request.setArtifactRepository( artifactsRepository );
        request.setMetadataRepository( new TransientMetadataRepository() );

        request.addAdvice( new MavenPropertiesAdvice( groupId, artifactId, version ) );

        IPublisherAction[] actions = getPublisherActions( location, packaging, artifactId, version );

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

    private IPublisherAction[] getPublisherActions( File location, String packaging, String id, String version )
    {
        if ( P2Resolver.TYPE_ECLIPSE_PLUGIN.equals( packaging ) || P2Resolver.TYPE_ECLIPSE_TEST_PLUGIN.equals( packaging ) )
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
                return new IPublisherAction[] { new ProductAction( product, productDescriptor, null, null ) };
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
