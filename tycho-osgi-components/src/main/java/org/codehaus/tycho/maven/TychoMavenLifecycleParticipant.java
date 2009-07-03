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
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
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
        List<MavenProject> projects = session.getProjects();
        MavenExecutionRequest request = session.getRequest();

        for ( MavenProject project : projects )
        {
            // TODO Do I need to interpolate anything? 
            Properties properties = new Properties();
            properties.putAll( project.getProperties() );
            properties.putAll( session.getExecutionProperties() ); // session wins

            project.setContextValue( TychoConstants.CTX_MERGED_PROPERTIES, properties );

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
