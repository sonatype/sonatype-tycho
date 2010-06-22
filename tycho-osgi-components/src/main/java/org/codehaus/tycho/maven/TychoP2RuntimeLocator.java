package org.codehaus.tycho.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.tycho.utils.TychoVersion;

@Component( role = TychoP2RuntimeLocator.class )
public class TychoP2RuntimeLocator
{
    @Requirement
    private Logger logger;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;

    @Requirement( hint = "zip" )
    private UnArchiver unArchiver;

    public File locateTychoP2Runtime( MavenSession session )
        throws MavenExecutionException
    {
        String p2Version = TychoVersion.getTychoVersion();

        Artifact p2Runtime =
            repositorySystem.createArtifact( "org.sonatype.tycho", "tycho-p2-runtime", p2Version, "zip" );

        File p2Directory =
            new File( session.getLocalRepository().getBasedir(), session.getLocalRepository().pathOf( p2Runtime ) );
        p2Directory = new File( p2Directory.getParentFile(), "eclipse" );

        if ( p2Directory.exists() && !p2Runtime.isSnapshot() )
        {
            return p2Directory;
        }

        logger.debug( "Resolving P2 runtime" );

        List<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
        for ( MavenProject project : session.getProjects() )
        {
            repositories.addAll( project.getPluginArtifactRepositories() );
        }
        repositories = repositorySystem.getEffectiveRepositories( repositories );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact( p2Runtime );
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
            if ( logger.isDebugEnabled() )
            {
                logger.warn( "Could not resolve tycho-p2-runtime", e );
            }
            else
            {
                logger.warn( "Could not resolve tycho-p2-runtime" );
            }
            return null;
        }

        if ( p2Runtime.getFile().lastModified() > p2Directory.lastModified() )
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

            unArchiver.setSourceFile( p2Runtime.getFile() );
            unArchiver.setDestDirectory( p2Directory.getParentFile() );
            try
            {
                unArchiver.extract();
            }
            catch ( ArchiverException e )
            {
                throw new MavenExecutionException( "Failed to unpack P2 runtime: " + e.getMessage(), e );
            }

            p2Directory.setLastModified( p2Runtime.getFile().lastModified() );
        }

        return p2Directory;
    }

}
