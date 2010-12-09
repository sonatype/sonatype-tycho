package org.sonatype.tycho.p2.facade.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.sonatype.tycho.equinox.EquinoxRuntimeLocator;

@Component( role = EquinoxRuntimeLocator.class )
public class TychoP2RuntimeLocator
    implements EquinoxRuntimeLocator
{
    /**
     * List of packages exported by the bundles/JARs that serve as facade between the Maven and the
     * OSGi class loaders.
     */
    public static final String[] SYSTEM_PACKAGES_EXTRA = { "org.sonatype.tycho.p2", "org.sonatype.tycho.p2.repository",
        "org.sonatype.tycho.p2.resolver", "org.sonatype.tycho.p2.tools", "org.sonatype.tycho.p2.tools.director",
        "org.sonatype.tycho.p2.tools.publisher", "org.sonatype.tycho.p2.tools.mirroring" };

    @Requirement
    private Logger logger;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;

    @Requirement
    private LegacySupport buildContext;

    @Requirement( hint = "zip" )
    private UnArchiver unArchiver;

    @Requirement
    private Map<String, TychoP2RuntimeMetadata> runtimeMetadata;

    public List<File> getRuntimeLocations()
        throws MavenExecutionException
    {
        MavenSession session = buildContext.getSession();

        return getRuntimeLocations( session );
    }

    public List<File> getRuntimeLocations( MavenSession session )
        throws MavenExecutionException
    {
        List<File> locations = new ArrayList<File>();

        TychoP2RuntimeMetadata framework = runtimeMetadata.get( TychoP2RuntimeMetadata.HINT_FRAMEWORK );
        if ( framework != null )
        {
            addRuntime( locations, session, framework );
        }

        for ( Map.Entry<String, TychoP2RuntimeMetadata> entry : runtimeMetadata.entrySet() )
        {
            if ( !TychoP2RuntimeMetadata.HINT_FRAMEWORK.equals( entry.getKey() ) )
            {
                addRuntime( locations, session, entry.getValue() );
            }
        }

        return locations;
    }

    private void addRuntime( List<File> locations, MavenSession session, TychoP2RuntimeMetadata framework )
        throws MavenExecutionException
    {
        for ( Dependency dependency : framework.getRuntimeArtifacts() )
        {
            locations.add( resolveRuntimeArtifact( session, dependency ) );
        }
    }

    private File resolveRuntimeArtifact( MavenSession session, Dependency d )
        throws MavenExecutionException
    {
        Artifact artifact =
            repositorySystem.createArtifact( d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getType() );

        if ( "zip".equals( d.getType() ) )
        {
            File p2Directory =
                new File( session.getLocalRepository().getBasedir(), session.getLocalRepository().pathOf( artifact ) );
            p2Directory = new File( p2Directory.getParentFile(), "eclipse" );

            if ( p2Directory.exists() && !artifact.isSnapshot() )
            {
                return p2Directory;
            }

            logger.debug( "Resolving P2 runtime" );

            resolveArtifact( session, artifact );

            if ( artifact.getFile().lastModified() > p2Directory.lastModified() )
            {
                logger.debug( "Unpacking P2 runtime to " + p2Directory );

                try
                {
                    FileUtils.deleteDirectory( p2Directory );
                }
                catch ( IOException e )
                {
                    logger.warn( "Failed to delete P2 runtime " + p2Directory + ": " + e.getMessage() );
                }

                unArchiver.setSourceFile( artifact.getFile() );
                unArchiver.setDestDirectory( p2Directory.getParentFile() );
                try
                {
                    unArchiver.extract();
                }
                catch ( ArchiverException e )
                {
                    throw new MavenExecutionException( "Failed to unpack P2 runtime: " + e.getMessage(), e );
                }

                p2Directory.setLastModified( artifact.getFile().lastModified() );
            }

            return p2Directory;
        }
        else
        {
            return resolveArtifact( session, artifact );
        }
    }

    private File resolveArtifact( MavenSession session, Artifact artifact )
        throws MavenExecutionException
    {
        List<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
        for ( MavenProject project : session.getProjects() )
        {
            repositories.addAll( project.getPluginArtifactRepositories() );
        }
        repositories = repositorySystem.getEffectiveRepositories( repositories );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact( artifact );
        request.setResolveRoot( true ).setResolveTransitively( false );
        request.setLocalRepository( session.getLocalRepository() );
        request.setRemoteRepositories( repositories );
        request.setCache( session.getRepositoryCache() );
        request.setOffline( session.isOffline() );
        request.setForceUpdate( session.getRequest().isUpdateSnapshots() );

        ArtifactResolutionResult result = repositorySystem.resolve( request );

        try
        {
            resolutionErrorHandler.throwErrors( request, result );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MavenExecutionException( "Could not resolve tycho-p2-runtime", e );
        }

        return artifact.getFile();
    }

    public List<String> getSystemPackagesExtra()
    {
        return Arrays.asList( SYSTEM_PACKAGES_EXTRA );
    }
}
