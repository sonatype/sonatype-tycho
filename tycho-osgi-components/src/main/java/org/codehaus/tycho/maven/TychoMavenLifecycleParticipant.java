package org.codehaus.tycho.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
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
import org.codehaus.tycho.PlatformPropertiesUtils;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformConfiguration;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.model.Target;
import org.codehaus.tycho.osgitools.targetplatform.LocalTargetPlatformResolver;
import org.codehaus.tycho.osgitools.targetplatform.Tycho03TargetPlatformResolver;
import org.codehaus.tycho.osgitools.utils.TychoVersion;

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

    public void afterProjectsRead( MavenSession session )
        throws MavenExecutionException
    {
        if ( "maven".equals( session.getExecutionProperties().get( "tycho.mode" ) ) )
        {
            return;
        }

        List<MavenProject> projects = session.getProjects();
        MavenExecutionRequest request = session.getRequest();

        for ( MavenProject project : projects )
        {
            Properties properties = new Properties();
            properties.putAll( project.getProperties() );
            properties.putAll( session.getExecutionProperties() ); // session wins
            project.setContextValue( TychoConstants.CTX_MERGED_PROPERTIES, properties );

            TargetPlatformConfiguration configuration = getTargetPlatformConfiguration( session, project );
            project.setContextValue( TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION, configuration );

            TargetPlatformResolver resolver = lookupPlatformResolver( container, project );

            resolver.setLocalRepository( request.getLocalRepository() );

            resolver.setMavenProjects( new ArrayList<MavenProject>( projects ) );

            try
            {
                DependenciesReader dr =
                    (DependenciesReader) container.lookup( DependenciesReader.class, project.getPackaging() );
                logger.info( "Resolving target platform for project " + project );
                TargetPlatform targetPlatform = resolver.resolvePlatform( project, null );
                project.setContextValue( TychoConstants.CTX_TARGET_PLATFORM, targetPlatform );
                for ( Dependency dependency : dr.getDependencies( session, project ) )
                {
                    project.getModel().addDependency( dependency );
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
        Plugin plugin = project.getPlugin( "org.codehaus.tycho:target-platform-configuration" );

        if ( plugin != null )
        {
            Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();

            if ( configuration != null )
            {
                result.setEnvironment( getTargetEnvironment( configuration ) );

                result.setResolver( getTargetPlatformResolver( configuration ) );

                result.setTarget( getTarget( session, project, configuration ) );
            }
        }

        // applying defaults
        if ( result.getEnvironment() == null )
        {
            // Otherwise, use project or execution properties, if provided
            Properties properties = (Properties) project.getContextValue( TychoConstants.CTX_MERGED_PROPERTIES );

            // Otherwise, use current system os/ws/nl/arch
            String os = PlatformPropertiesUtils.getOS( properties );
            String ws = PlatformPropertiesUtils.getWS( properties );
            String arch = PlatformPropertiesUtils.getArch( properties );

            result.setEnvironment( new TargetEnvironment( os, ws, arch, null /* nl */) );
        }

        return result;
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
            Artifact artifact = repositorySystem.createArtifactWithClassifier( groupId, artifactId, version, "target", classifier );
            ArtifactResolutionRequest request = new ArtifactResolutionRequest();
            request.setArtifact( artifact );
            request.setLocalRepository( session.getLocalRepository() );
            request.setRemoteRepostories( project.getRemoteArtifactRepositories() );
            ArtifactResolutionResult result = repositorySystem.resolve( request );

            if ( ! artifact.isResolved() )
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

    private TargetEnvironment getTargetEnvironment( Xpp3Dom configuration )
    {
        Xpp3Dom environmentDom = configuration.getChild( "environment" );
        if ( environmentDom == null )
        {
            return null;
        }

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
            resolverRole = Tycho03TargetPlatformResolver.ROLE_HINT;
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

    public void afterSessionStart( MavenSession session )
    {
        session.getUserProperties().setProperty( "tycho-version", TychoVersion.getTychoVersion() );
    }

}
