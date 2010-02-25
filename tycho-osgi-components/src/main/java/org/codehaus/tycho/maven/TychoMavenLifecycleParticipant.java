package org.codehaus.tycho.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformConfiguration;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.model.Target;
import org.codehaus.tycho.osgitools.AbstractTychoProject;
import org.codehaus.tycho.osgitools.targetplatform.LocalTargetPlatformResolver;
import org.codehaus.tycho.utils.PlatformPropertiesUtils;
import org.codehaus.tycho.utils.TychoVersion;
import org.sonatype.tycho.osgi.EquinoxEmbedder;
import org.sonatype.tycho.osgi.EquinoxLocator;

@Component( role = AbstractMavenLifecycleParticipant.class, hint = "TychoMavenLifecycleListener" )
public class TychoMavenLifecycleParticipant
    extends AbstractMavenLifecycleParticipant
{

    @Requirement
    private PlexusContainer container;

    @Requirement
    private Logger logger;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;

    @Requirement
    private EquinoxLocator equinoxLocator;

    @Requirement( hint = "zip" )
    private UnArchiver unArchiver;

    @Requirement
    private EquinoxEmbedder equinoxEmbedder;

    public void afterProjectsRead( MavenSession session )
        throws MavenExecutionException
    {
        if ( "maven".equals( session.getUserProperties().get( "tycho.mode" ) ) )
        {
            return;
        }

        if ( session.getUserProperties().containsKey( "m2e.version" ) )
        {
            return;
        }

        File p2Directory = resolveEquinoxRuntime( session );
        if ( p2Directory != null )
        {
            equinoxLocator.setRuntimeLocation( p2Directory );
            logger.debug( "Using P2 runtime at " + p2Directory );
        }

        File secureStorage = new File( session.getLocalRepository().getBasedir(), ".meta/tycho.secure_storage" );
        equinoxEmbedder.setNonFrameworkArgs( new String[] { "-eclipse.keyring", secureStorage.getAbsolutePath(),
        // TODO "-eclipse.password", ""
        } );

        List<MavenProject> projects = session.getProjects();
        
        for ( MavenProject project : projects )
        {
            try
            {
                AbstractTychoProject dr =
                    (AbstractTychoProject) container.lookup( TychoProject.class, project.getPackaging() );

                dr.setupProject( session, project );
            }
            catch ( ComponentLookupException e )
            {
                // no biggie
            }
        }

        for ( MavenProject project : projects )
        {
            Properties properties = new Properties();
            properties.putAll( project.getProperties() );
            properties.putAll( session.getSystemProperties() ); // session wins
            properties.putAll( session.getUserProperties() );
            project.setContextValue( TychoConstants.CTX_MERGED_PROPERTIES, properties );

            TargetPlatformConfiguration configuration = getTargetPlatformConfiguration( session, project );
            project.setContextValue( TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION, configuration );

            TargetPlatformResolver resolver = lookupPlatformResolver( container, project );

            try
            {
                AbstractTychoProject dr =
                    (AbstractTychoProject) container.lookup( TychoProject.class, project.getPackaging() );

                logger.info( "Resolving target platform for project " + project );
                TargetPlatform targetPlatform = resolver.resolvePlatform( session, project, null );

                dr.setTargetPlatform( session, project, targetPlatform );

                dr.resolve( project );

                MavenDependencyCollector dependencyCollector = new MavenDependencyCollector( project, logger );
                dr.getDependencyWalker( project ).walk( dependencyCollector );

                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "Injected dependencies for " + project.toString() );
                    for ( Dependency dependency : project.getDependencies() )
                    {
                        logger.debug( "\t" + dependency.toString() );
                    }
                }
            }
            catch ( ComponentLookupException e )
            {
                // no biggie
            }
        }
    }

    private TargetPlatformConfiguration getTargetPlatformConfiguration( MavenSession session, MavenProject project )
    {
        TargetPlatformConfiguration result = new TargetPlatformConfiguration();

        // Use org.codehaus.tycho:target-platform-configuration/configuration/environment, if provided
        Plugin plugin = project.getPlugin( "org.sonatype.tycho:target-platform-configuration" );

        if ( plugin != null )
        {
            Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();

            if ( configuration != null )
            {
                addTargetEnvironments( result, project, configuration );

                result.setResolver( getTargetPlatformResolver( configuration ) );

                result.setTarget( getTarget( session, project, configuration ) );

                result.setPomDependencies( getPomDependencies( configuration ) );

                result.setIgnoreTychoRepositories( getIgnoreTychoRepositories( configuration ) );
            }
        }

        if ( result.getEnvironments().isEmpty() )
        {
            // applying defaults
            logger.warn( "No explicit target runtime environment configuration. Build is platform dependent." );

            // Otherwise, use project or execution properties, if provided
            Properties properties = (Properties) project.getContextValue( TychoConstants.CTX_MERGED_PROPERTIES );

            // Otherwise, use current system os/ws/nl/arch
            String os = PlatformPropertiesUtils.getOS( properties );
            String ws = PlatformPropertiesUtils.getWS( properties );
            String arch = PlatformPropertiesUtils.getArch( properties );

            result.addEnvironment( new TargetEnvironment( os, ws, arch, null /* nl */) );

            result.setImplicitTargetEnvironment( true );
        }
        else
        {
            result.setImplicitTargetEnvironment( false );
        }

        return result;
    }

    private void addTargetEnvironments( TargetPlatformConfiguration result, MavenProject project, Xpp3Dom configuration )
    {
        addDeprecatedTargetEnvironment( result, configuration );

        Xpp3Dom environmentsDom = configuration.getChild( "environments" );
        if ( environmentsDom != null )
        {
            for ( Xpp3Dom environmentDom : environmentsDom.getChildren( "environment" ) )
            {
                result.addEnvironment( newTargetEnvironment( environmentDom ) );
            }
        }
    }

    protected void addDeprecatedTargetEnvironment( TargetPlatformConfiguration result, Xpp3Dom configuration )
    {
        Xpp3Dom environmentDom = configuration.getChild( "environment" );
        if ( environmentDom != null )
        {
            logger.warn( "target-platform-configuration <environment/> element is deprecated, please use <environments/> instead." );
            result.addEnvironment( newTargetEnvironment( environmentDom ) );
        }
    }

    private boolean getIgnoreTychoRepositories( Xpp3Dom configuration )
    {
        Xpp3Dom ignoreTychoRepositoriesDom = configuration.getChild( "ignoreTychoRepositories" );
        if ( ignoreTychoRepositoriesDom == null )
        {
            return true;
        }

        return Boolean.parseBoolean( ignoreTychoRepositoriesDom.getValue() );
    }

    private String getPomDependencies( Xpp3Dom configuration )
    {
        Xpp3Dom pomDependenciesDom = configuration.getChild( "pomDependencies" );
        if ( pomDependenciesDom == null )
        {
            return null;
        }

        return pomDependenciesDom.getValue();
    }

    private Target getTarget( MavenSession session, MavenProject project, Xpp3Dom configuration )
    {
        Xpp3Dom targetDom = configuration.getChild( "target" );
        if ( targetDom == null )
        {
            return null;
        }

        Xpp3Dom artifactDom = targetDom.getChild( "artifact" );
        if ( artifactDom == null )
        {
            return null;
        }

        Xpp3Dom groupIdDom = artifactDom.getChild( "groupId" );
        Xpp3Dom artifactIdDom = artifactDom.getChild( "artifactId" );
        Xpp3Dom versionDom = artifactDom.getChild( "version" );
        if ( groupIdDom == null || artifactIdDom == null || versionDom == null )
        {
            return null;
        }
        Xpp3Dom classifierDom = artifactDom.getChild( "classifier" );

        String groupId = groupIdDom.getValue();
        String artifactId = artifactIdDom.getValue();
        String version = versionDom.getValue();
        String classifier = classifierDom != null ? classifierDom.getValue() : null;

        File targetFile = null;
        for ( MavenProject otherProject : session.getProjects() )
        {
            if ( groupId.equals( otherProject.getGroupId() ) && artifactId.equals( otherProject.getArtifactId() )
                && version.equals( otherProject.getVersion() ) )
            {
                targetFile = new File( otherProject.getBasedir(), classifier + ".target" );
                break;
            }
        }

        if ( targetFile == null )
        {
            Artifact artifact =
                repositorySystem.createArtifactWithClassifier( groupId, artifactId, version, "target", classifier );
            ArtifactResolutionRequest request = new ArtifactResolutionRequest();
            request.setArtifact( artifact );
            request.setLocalRepository( session.getLocalRepository() );
            request.setRemoteRepositories( project.getRemoteArtifactRepositories() );
            repositorySystem.resolve( request );

            if ( !artifact.isResolved() )
            {
                throw new RuntimeException( "Could not resolve target platform specification artifact " + artifact );
            }

            targetFile = artifact.getFile();
        }

        try
        {
            return Target.read( targetFile );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    private String getTargetPlatformResolver( Xpp3Dom configuration )
    {
        Xpp3Dom resolverDom = configuration.getChild( "resolver" );

        if ( resolverDom == null )
        {
            return null;
        }

        return resolverDom.getValue();
    }

    private TargetEnvironment newTargetEnvironment( Xpp3Dom environmentDom )
    {
        Xpp3Dom osDom = environmentDom.getChild( "os" );
        if ( osDom == null )
        {
            return null;
        }

        Xpp3Dom wsDom = environmentDom.getChild( "ws" );
        if ( wsDom == null )
        {
            return null;
        }

        Xpp3Dom archDom = environmentDom.getChild( "arch" );
        if ( archDom == null )
        {
            return null;
        }

        return new TargetEnvironment( osDom.getValue(), wsDom.getValue(), archDom.getValue(), null /* nl */);
    }

    // TODO does not belong here
    public static TargetPlatformResolver lookupPlatformResolver( PlexusContainer container, MavenProject project )
    {
        Properties properties = (Properties) project.getContextValue( TychoConstants.CTX_MERGED_PROPERTIES );
        TargetPlatformConfiguration configuration =
            (TargetPlatformConfiguration) project.getContextValue( TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION );

        String resolverRole = configuration.getTargetPlatformResolver();
        if ( resolverRole == null )
        {
            resolverRole = LocalTargetPlatformResolver.ROLE_HINT;
        }

        // not exactly pretty and not exactly the right place
        Logger logger;
        try
        {
            logger = container.lookup( Logger.class );
        }
        catch ( ComponentLookupException e1 )
        {
            logger = new ConsoleLogger( Logger.LEVEL_INFO, "Unexpected console logger" );
        }

        String property = properties.getProperty( "tycho.targetPlatform" );
        TargetPlatformResolver resolver;
        if ( property != null )
        {
            logger.info( "tycho.targetPlatform=" + property + " overrides project target platform resolver="
                + resolverRole );

            File location = new File( property );
            if ( !location.exists() || !location.isDirectory() )
            {
                throw new RuntimeException( "Invalid target platform location: " + property );
            }

            try
            {
                resolver = container.lookup( TargetPlatformResolver.class, LocalTargetPlatformResolver.ROLE_HINT );
            }
            catch ( ComponentLookupException e )
            {
                throw new RuntimeException( "Could not instantiate required component", e );
            }

            ( (LocalTargetPlatformResolver) resolver ).setLocation( new File( property ) );

            return resolver;
        }

        try
        {
            resolver = container.lookup( TargetPlatformResolver.class, resolverRole );
        }
        catch ( ComponentLookupException e )
        {
            throw new RuntimeException( "Could not instantiate required component", e );
        }

        return resolver;
    }

    private File resolveEquinoxRuntime( MavenSession session )
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
