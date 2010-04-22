package org.codehaus.tycho.osgitools.targetplatform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.tycho.ArtifactKey;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformConfiguration;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.osgitools.BundleReader;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Constants;

/**
 * Creates target platform based on local eclipse installation.
 */
@Component( role = TargetPlatformResolver.class, hint = LocalTargetPlatformResolver.ROLE_HINT, instantiationStrategy = "per-lookup" )
public class LocalTargetPlatformResolver
    extends AbstractTargetPlatformResolver
    implements TargetPlatformResolver
{

    public static final String ROLE_HINT = "local";

    @Requirement
    private EclipseInstallationLayout layout;

    @Requirement
    private BundleReader manifestReader;

    @Requirement
    private ProjectDependenciesResolver projectDependenciesResolver;

    public TargetPlatform resolvePlatform( MavenSession session, MavenProject project, List<Dependency> dependencies )
    {
        DefaultTargetPlatform platform = new DefaultTargetPlatform();

        for ( File site : layout.getSites() )
        {
            platform.addSite( site );

            for ( File plugin : layout.getPlugins( site ) )
            {
                ArtifactKey artifactKey = getArtifactKey( session, plugin );

                if ( artifactKey != null )
                {
                    platform.addArtifactFile( artifactKey, plugin );
                }
            }

            for ( File feature : layout.getFeatures( site ) )
            {
                Feature desc = Feature.loadFeature( feature );
                ArtifactKey key = new ArtifactKey( TychoProject.ECLIPSE_FEATURE, desc.getId(), desc.getVersion() );

                platform.addArtifactFile( key, feature );
            }
        }

        addProjects( session, platform );
        addDependencies( session, project, platform );

        if ( platform.isEmpty() )
        {
            getLogger().warn( "Could not find any bundles or features in " + layout.getLocation() );
        }

        return platform;
    }

    private void addDependencies( MavenSession session, MavenProject project, DefaultTargetPlatform platform )
    {
        TargetPlatformConfiguration configuration =
            (TargetPlatformConfiguration) project.getContextValue( TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION );

        if ( configuration != null && TargetPlatformConfiguration.POM_DEPENDENCIES_CONSIDER.equals( configuration.getPomDependencies() ) )
        {
            Map<String, MavenProject> projectIds = new HashMap<String, MavenProject>( session.getProjects().size() * 2 );
            for ( MavenProject p : session.getProjects() )
            {
                String key = ArtifactUtils.key( p.getGroupId(), p.getArtifactId(), p.getVersion() );
                projectIds.put( key, p );
            }

            ArrayList<String> scopes = new ArrayList<String>();
            scopes.add( Artifact.SCOPE_COMPILE );
            Collection<Artifact> artifacts;
            try
            {
                artifacts = projectDependenciesResolver.resolve( project, scopes, session );
            }
            catch ( MultipleArtifactsNotFoundException e )
            {
                Collection<Artifact> missing = new HashSet<Artifact>( e.getMissingArtifacts() );

                for ( Iterator<Artifact> it = missing.iterator(); it.hasNext(); )
                {
                    Artifact a = it.next();
                    String key = ArtifactUtils.key( a.getGroupId(), a.getArtifactId(), a.getBaseVersion() );
                    if ( projectIds.containsKey( key ) )
                    {
                        it.remove();
                    }
                }

                if ( !missing.isEmpty() )
                {
                    throw new RuntimeException( "Could not resolve project dependencies", e );
                }

                artifacts = e.getResolvedArtifacts();
                artifacts.removeAll( e.getMissingArtifacts() );
            }
            catch ( AbstractArtifactResolutionException e )
            {
                throw new RuntimeException( "Could not resolve project dependencies", e );
            }
            for ( Artifact artifact : artifacts )
            {
                String key =
                    ArtifactUtils.key( artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion() );
                if ( projectIds.containsKey( key ) )
                {
                    MavenProject dependent = projectIds.get( key );
                    ArtifactKey artifactKey = getArtifactKey( session, dependent );
                    if ( artifactKey != null )
                    {
                        platform.removeAll( artifactKey.getType(), artifactKey.getId() );
                        platform.addMavenProject( artifactKey, dependent );
                        if ( getLogger().isDebugEnabled() )
                        {
                            getLogger().debug( "Add Maven Project " + artifactKey );
                        }                        
                    }
                }
                else
                {
                    File plugin = artifact.getFile();
                    ArtifactKey artifactKey = getArtifactKey( session, plugin );

                    if ( artifactKey != null )
                    {
                        platform.addArtifactFile( artifactKey, plugin );
                        if ( getLogger().isDebugEnabled() )
                        {
                            getLogger().debug( "Add Maven Project " + artifactKey );
                        }
                    }
                }
            }
        }
    }

    public ArtifactKey getArtifactKey( MavenSession session, MavenProject project )
    {
        Manifest mf = manifestReader.loadManifest( project.getBasedir() );
        
        if( mf == null)
        {
            return null;
        }

        ManifestElement[] id = manifestReader.parseHeader( Constants.BUNDLE_SYMBOLICNAME, mf );
        ManifestElement[] version = manifestReader.parseHeader( Constants.BUNDLE_VERSION, mf );

        if ( id == null || version == null )
        {
            return null;
        }

        ArtifactKey key = new ArtifactKey( TychoProject.ECLIPSE_PLUGIN, id[0].getValue(), version[0].getValue() );
        return key;
    }
    
    public ArtifactKey getArtifactKey( MavenSession session, File plugin )
    {
        Manifest mf = manifestReader.loadManifest( plugin );
        
        if( mf == null)
        {
            return null;
        }

        ManifestElement[] id = manifestReader.parseHeader( Constants.BUNDLE_SYMBOLICNAME, mf );
        ManifestElement[] version = manifestReader.parseHeader( Constants.BUNDLE_VERSION, mf );

        if ( id == null || version == null )
        {
            return null;
        }

        ArtifactKey key = new ArtifactKey( TychoProject.ECLIPSE_PLUGIN, id[0].getValue(), version[0].getValue() );
        return key;
    }    
    
    public void setLocation( File location )
        throws IOException
    {
        layout.setLocation( location.getCanonicalFile() );
    }
}
