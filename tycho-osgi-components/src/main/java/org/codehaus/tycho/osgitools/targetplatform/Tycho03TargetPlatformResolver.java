package org.codehaus.tycho.osgitools.targetplatform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.tycho.DefaultTargetPlatform;
import org.codehaus.tycho.ProjectType;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.PluginRef;

/**
 * Maven-based target platform resolution implemented in tycho 0.3.0-DEV builds.
 */
@Component( role = TargetPlatformResolver.class, hint = Tycho03TargetPlatformResolver.ROLE_HINT, instantiationStrategy = "per-lookup" )
public class Tycho03TargetPlatformResolver
    extends AbstractLogEnabled
    implements TargetPlatformResolver
{

    public static final String ROLE_HINT = "tycho03";

    @Requirement
    private ArtifactResolver artifactResolver;

    @Requirement
    private ArtifactFactory artifactFactory;

    @Requirement
    private PlexusContainer plexus;

    private List<MavenProject> projects;

    private ArtifactRepository localRepository;

    private Properties properties;

    public void setMavenProjects( List<MavenProject> projects )
    {
        this.projects = new ArrayList<MavenProject>( projects );
    }

    public TargetPlatform resolvePlatform( MavenProject project )
    {
        Set<File> sites = new LinkedHashSet<File>();
        Set<File> features = new LinkedHashSet<File>();
        Set<File> bundles = new LinkedHashSet<File>();

        File installation = getEclipseInstallation( projects );
        if ( installation != null )
        {
            addEclipseInstallation( installation, sites, features, bundles );
        }
        Set<File> extensionLocations = getEclipseLocations( projects, ProjectType.ECLIPSE_EXTENSION_LOCATION, false );
        for ( File extensionLocation : extensionLocations )
        {
            addEclipseInstallation( extensionLocation, sites, features, bundles );
        }

        Map<Artifact, Exception> exceptions = new HashMap<Artifact, Exception>();

        for ( MavenProject otherProject : projects )
        {
            @SuppressWarnings( "unchecked" )
            Map<String, Artifact> versionMap = otherProject.getManagedVersionMap();
            if ( versionMap != null )
            {
                for ( Artifact artifact : versionMap.values() )
                {
                    try
                    {
                        if ( ProjectType.ECLIPSE_FEATURE.equals( artifact.getType() ) )
                        {
                            resolveFeature( artifact, features, bundles, otherProject.getRemoteArtifactRepositories(),
                                            localRepository );
                        }
                        else if ( ProjectType.OSGI_BUNDLE.equals( artifact.getType() )
                            || ProjectType.ECLIPSE_TEST_PLUGIN.equals( artifact.getType() ) )
                        {
                            resolvePlugin( artifact, bundles, otherProject.getRemoteArtifactRepositories(), localRepository );
                        }
                    }
                    catch ( Exception e )
                    {
                        exceptions.put( artifact, e );
                    }
                }
            }
        }

        DefaultTargetPlatform platform = new DefaultTargetPlatform();

        for ( File site : sites )
        {
            platform.addSite( site );
        }

        platform.addSite( new File( localRepository.getBasedir() ) );

        for ( File feature : features )
        {
            platform.addArtifactFile( ProjectType.ECLIPSE_FEATURE, feature );
        }

        for ( File bundle : bundles )
        {
            platform.addArtifactFile( ProjectType.OSGI_BUNDLE, bundle );
        }

        File parentDir = null;

        for ( MavenProject otherProject : projects )
        {
            platform.addArtifactFile( otherProject.getPackaging(), otherProject.getBasedir() );

            if ( parentDir == null || isSubdir( otherProject.getBasedir(), parentDir ) )
            {
                parentDir = otherProject.getBasedir();
            }
        }

        platform.addSite( parentDir );

        platform.setProperties( properties );

        return platform;
    }

    private boolean isSubdir( File parent, File child )
    {
        return child.getAbsolutePath().startsWith( parent.getAbsolutePath() );
    }

    private void addEclipseInstallation( File location, Set<File> sites, Set<File> features, Set<File> bundles )
    {
        EclipseInstallationLayout layout;
        try
        {
            layout = plexus.lookup( EclipseInstallationLayout.class );
        }
        catch ( ComponentLookupException e )
        {
            throw new RuntimeException( "Could not instantiate required component", e );
        }

        layout.setLocation( location );

        for ( File site : layout.getSites() )
        {
            sites.add( site );

            for ( File feature : layout.getFeatures( site ) )
            {
                features.add( feature );
            }

            for ( File bundle : layout.getPlugins( site ) )
            {
                bundles.add( bundle );
            }
        }
    }

    private void resolveFeature( Artifact artifact, Set<File> features, Set<File> bundles,
                                 List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository )
        throws AbstractArtifactResolutionException, IOException, XmlPullParserException
    {
        resolveArtifact( artifact, remoteRepositories, localRepository );
        Feature feature = Feature.readJar( artifact.getFile() );
        // File featureDir = unpackFeature(artifact, feature, state);
        if ( features.add( artifact.getFile() ) )
        {
            for ( PluginRef ref : feature.getPlugins() )
            {
                try
                {
                    Artifact includedArtifact =
                        artifactFactory.createArtifact( ref.getMavenGroupId(), ref.getId(), ref.getMavenVersion(),
                                                        null, ProjectType.OSGI_BUNDLE );
                    resolvePlugin( includedArtifact, bundles, remoteRepositories, localRepository );
                }
                catch ( Exception e )
                {
                    getLogger().warn( e.getMessage() );
                }
            }
            for ( Feature.FeatureRef ref : feature.getIncludedFeatures() )
            {
                try
                {
                    Artifact includedArtifact =
                        artifactFactory.createArtifact( ref.getMavenGroupId(), ref.getId(), ref.getMavenVersion(),
                                                        null, ProjectType.ECLIPSE_FEATURE );
                    resolveFeature( includedArtifact, features, bundles, remoteRepositories, localRepository );
                }
                catch ( Exception e )
                {
                    getLogger().warn( e.getMessage() );
                }
            }
        }
    }

    private void assertResolved( Artifact artifact )
        throws ArtifactNotFoundException
    {
        if ( !artifact.isResolved() || artifact.getFile() == null || !artifact.getFile().canRead() )
        {
            throw new ArtifactNotFoundException( "Artifact is not resolved", artifact );
        }
    }

    private void resolvePlugin( Artifact artifact, Set<File> bundles, List<ArtifactRepository> remoteRepositories,
                                ArtifactRepository localRepository )
        throws AbstractArtifactResolutionException
    {
        resolveArtifact( artifact, remoteRepositories, localRepository );
        bundles.add( artifact.getFile() );
    }

    private void resolveArtifact( Artifact artifact, List<ArtifactRepository> remoteRepositories,
                                  ArtifactRepository localRepository )
        throws AbstractArtifactResolutionException
    {
        artifactResolver.resolve( artifact, remoteRepositories, localRepository );
        assertResolved( artifact );
    }

    private File getEclipseInstallation( List<MavenProject> projects )
    {
        Set<File> locations = getEclipseLocations( projects, ProjectType.ECLIPSE_INSTALLATION, true );
        return ( !locations.isEmpty() ) ? locations.iterator().next() : null;
    }

    private Set<File> getEclipseLocations( List<MavenProject> projects, String packaging, boolean singleton )
    {
        LinkedHashSet<File> installations = new LinkedHashSet<File>();
        for ( MavenProject project : projects )
        {
            Map<String, Artifact> versionMap = project.getManagedVersionMap();
            if ( versionMap != null )
            {
                for ( Artifact artifact : versionMap.values() )
                {
                    if ( packaging.equals( artifact.getType() ) )
                    {
                        if ( !singleton || installations.size() <= 0 )
                        {
                            installations.add( artifact.getFile() );
                        }
                        else
                        {
                            if ( !installations.contains( artifact.getFile() ) )
                            {
                                throw new TargetPlatformException(
                                                                   "No more than one eclipse-installation and/or eclipse-distributions" );
                            }
                        }
                    }
                }
            }
        }
        return installations;
    }

    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    public void setProperties( Properties properties )
    {
        this.properties = properties;
    }

}
