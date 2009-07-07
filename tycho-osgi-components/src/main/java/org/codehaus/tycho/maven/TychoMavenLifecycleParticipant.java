package org.codehaus.tycho.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.tycho.PlatformPropertiesUtils;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.TychoConstants;
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

    public void afterProjectsRead( MavenSession session )
        throws MavenExecutionException
    {
        if ( "maven".equals( session.getExecutionProperties().get( "tycho.mode")  ) )
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

            TargetEnvironment environment = getTargetEnvironment( project );
            project.setContextValue( TychoConstants.CTX_TARGET_ENVIRONMENT, environment );

            TargetPlatformResolver resolver = lookupPlatformResolver( container, properties );

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

    private TargetEnvironment getTargetEnvironment( MavenProject project ) 
        throws MavenExecutionException
    {
        TargetEnvironment environment = null;

        // Use org.codehaus.tycho:target-platform-configuration/configuration/environment, if provided
        Plugin plugin = project.getPlugin( "org.codehaus.tycho:target-platform-configuration" );
        if ( plugin != null )
        {
            Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();

            environment = getTargetEnvironment( configuration );
        }
        
        if ( environment == null )
        {
            // Otherwise, use project or execution properties, if provided 
            Properties properties = (Properties) project.getContextValue( TychoConstants.CTX_MERGED_PROPERTIES );

            // Otherwise, use current system os/ws/nl/arch
            String os = PlatformPropertiesUtils.getOS( properties );
            String ws = PlatformPropertiesUtils.getWS( properties );
            String arch = PlatformPropertiesUtils.getArch( properties );

            environment = new TargetEnvironment( os, ws, arch, null /*nl*/ );
        }

        return environment;
    }

    private TargetEnvironment getTargetEnvironment( Xpp3Dom configuration )
    {
        if ( configuration == null )
        {
            return null;
        }
        
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

        return new TargetEnvironment( osDom.getValue(), wsDom.getValue(), archDom.getValue(), null /*nl*/ );
    }

    // TODO does not belong here
    public static TargetPlatformResolver lookupPlatformResolver( PlexusContainer container, Properties properties )
    {
        String property = properties.getProperty( "tycho.targetPlatform" );
        TargetPlatformResolver resolver;
        if ( property != null )
        {
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

        String resolverRole = properties.getProperty( "tycho.resolver", Tycho03TargetPlatformResolver.ROLE_HINT );
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
        session.getExecutionProperties().setProperty( "tycho-version", TychoVersion.getTychoVersion() );
    }

}
