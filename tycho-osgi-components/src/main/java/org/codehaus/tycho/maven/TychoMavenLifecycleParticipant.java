package org.codehaus.tycho.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformConfiguration;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.model.Target;
import org.codehaus.tycho.osgitools.AbstractTychoProject;
import org.codehaus.tycho.osgitools.BundleReader;
import org.codehaus.tycho.osgitools.DebugUtils;
import org.codehaus.tycho.osgitools.DefaultBundleReader;
import org.codehaus.tycho.osgitools.targetplatform.LocalTargetPlatformResolver;
import org.codehaus.tycho.utils.PlatformPropertiesUtils;
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
    private TychoP2RuntimeLocator p2runtime;

    @Requirement
    private EquinoxLocator equinoxLocator;

    @Requirement
    private EquinoxEmbedder equinoxEmbedder;

    @Requirement
    private BundleReader bundleReader;

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

        System.setProperty( "osgi.framework.useSystemProperties", "false" ); //$NON-NLS-1$ //$NON-NLS-2$

        File localRepository = new File( session.getLocalRepository().getBasedir() );
        ( (DefaultBundleReader) bundleReader ).setLocationRepository( localRepository );

        File p2Directory = p2runtime.locateTychoP2Runtime( session );
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

                if ( logger.isDebugEnabled() && DebugUtils.isDebugEnabled( session, project ) )
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append( "Resolved target platform for project " ).append( project ).append( "\n" );
                    targetPlatform.toDebugString( sb, "  " );
                    logger.debug( sb.toString() );
                }

                dr.setTargetPlatform( session, project, targetPlatform );

                dr.resolve( session, project );

                MavenDependencyCollector dependencyCollector = new MavenDependencyCollector( project, logger );
                dr.getDependencyWalker( project ).walk( dependencyCollector );

                if ( logger.isDebugEnabled() && DebugUtils.isDebugEnabled( session, project ) )
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append( "Injected dependencies for " ).append( project.toString() ).append( "\n" );
                    for ( Dependency dependency : project.getDependencies() )
                    {
                        sb.append( "  " ).append( dependency.toString() );
                    }
                    logger.debug( sb.toString() );
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
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "target-platform-configuration for " + project.toString() + ":\n"
                        + configuration.toString() );
                }

                addTargetEnvironments( result, project, configuration );

                result.setResolver( getTargetPlatformResolver( configuration ) );

                result.setTarget( getTarget( session, project, configuration ) );

                result.setPomDependencies( getPomDependencies( configuration ) );

                result.setAllowConflictingDependencies( getAllowConflictingDependencies( configuration ) );

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

    private Boolean getAllowConflictingDependencies( Xpp3Dom configuration )
    {
        Xpp3Dom allowConflictingDependenciesDom = configuration.getChild( "allowConflictingDependencies" );
        if ( allowConflictingDependenciesDom == null )
        {
            return null;
        }

        return Boolean.parseBoolean( allowConflictingDependenciesDom.getValue() );
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

            try
            {
                ( (LocalTargetPlatformResolver) resolver ).setLocation( new File( property ) );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "Could not create target platform", e );
            }

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

}
