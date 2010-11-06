package org.sonatype.tycho.p2.impl.publisher;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.actions.ICapabilityAdvice;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.sonatype.tycho.p2.IArtifactFacade;
import org.sonatype.tycho.p2.impl.publisher.repo.TransientArtifactRepository;

@SuppressWarnings( "restriction" )
public abstract class AbstractMetadataGenerator
{
    private IProgressMonitor monitor = new NullProgressMonitor();

    protected void generateMetadata( IArtifactFacade artifact, List<Map<String, String>> environments,
                                     Set<IInstallableUnit> units, Set<IArtifactDescriptor> artifacts )
    {
        TransientArtifactRepository artifactsRepository = new TransientArtifactRepository();
        PublisherInfo publisherInfo = new PublisherInfo();
        publisherInfo.setArtifactRepository( artifactsRepository );
        for ( IPublisherAdvice advice : getPublisherAdvice( artifact ) )
        {
            publisherInfo.addAdvice( advice );
        }
        List<IPublisherAction> actions = getPublisherActions( artifact, environments );
        publish( units, artifacts, artifactsRepository, publisherInfo, actions );
    }

    protected abstract List<IPublisherAction> getPublisherActions( IArtifactFacade artifact,
                                                                   List<Map<String, String>> environments );

    protected abstract List<IPublisherAdvice> getPublisherAdvice( IArtifactFacade artifact );

    protected ICapabilityAdvice getExtraEntriesAdvice( IArtifactFacade artifact )
    {
        final IRequirement[] extraRequirements = extractExtraEntriesAsIURequirement( artifact.getLocation() );
        return new ICapabilityAdvice()
        {
            public boolean isApplicable( String configSpec, boolean includeDefault, String id, Version version )
            {
                return true;
            }

            public IRequirement[] getRequiredCapabilities( InstallableUnitDescription iu )
            {
                return extraRequirements;
            }

            public IProvidedCapability[] getProvidedCapabilities( InstallableUnitDescription iu )
            {
                return null;
            }

            public IRequirement[] getMetaRequiredCapabilities( InstallableUnitDescription iu )
            {
                return null;
            }
        };
    }

    private IRequirement[] extractExtraEntriesAsIURequirement( File location )
    {
        Properties buildProperties = loadProperties( location );
        if ( buildProperties == null || buildProperties.size() == 0 )
            return null;
        ArrayList<IRequirement> result = new ArrayList<IRequirement>();
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
        return result.toArray( new IRequirement[result.size()] );
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

    private void createRequirementFromExtraClasspathProperty( ArrayList<IRequirement> result, String[] urls )
    {
        for ( int i = 0; i < urls.length; i++ )
        {
            createRequirementFromPlatformURL( result, urls[i].trim() );
        }
    }

    private void createRequirementFromPlatformURL( ArrayList<IRequirement> result, String url )
    {
        Pattern platformURL = Pattern.compile( "platform:/(plugin|fragment)/([^/]*)(/)*.*" );
        Matcher m = platformURL.matcher( url );
        if ( m.matches() )
            result.add( MetadataFactory.createRequirement( IInstallableUnit.NAMESPACE_IU_ID, m.group( 2 ),
                                                           VersionRange.emptyRange, null, false, false ) );
    }

    private void publish( Set<IInstallableUnit> units, Set<IArtifactDescriptor> artifacts,
                          TransientArtifactRepository artifactsRepository, PublisherInfo publisherInfo,
                          List<IPublisherAction> actions )
    {
        PublisherResult result = new PublisherResult();

        Publisher publisher = new Publisher( publisherInfo, result );

        IStatus status = publisher.publish( actions.toArray( new IPublisherAction[actions.size()] ), monitor );

        if ( !status.isOK() )
        {
            throw new RuntimeException( new CoreException( status ) );
        }

        if ( units != null )
        {
            units.addAll( result.getIUs( null, null ) );
        }

        if ( artifacts != null )
        {
            artifacts.addAll( artifactsRepository.getArtifactDescriptors() );
        }
    }

}
