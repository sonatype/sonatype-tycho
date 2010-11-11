package org.codehaus.tycho.resolver;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.tycho.ArtifactDependencyVisitor;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformConfiguration;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.maven.MavenDependencyCollector;
import org.codehaus.tycho.osgitools.AbstractTychoProject;
import org.codehaus.tycho.osgitools.DebugUtils;
import org.sonatype.tycho.ReactorProject;
import org.sonatype.tycho.resolver.DependencyVisitor;
import org.sonatype.tycho.resolver.TychoDependencyResolver;

@Component( role = TychoDependencyResolver.class )
public class DefaultTychoDependencyResolver
    implements TychoDependencyResolver
{
    @Requirement
    private Logger logger;

    @Requirement
    private DefaultTargetPlatformConfigurationReader configurationReader;

    @Requirement
    private DefaultTargetPlatformResolverFactory targetPlatformResolverLocator;

    @Requirement( role = TychoProject.class )
    private Map<String, TychoProject> projectTypes;

    public void setupProject( MavenSession session, MavenProject project, ReactorProject reactorProject )
    {
        AbstractTychoProject dr = (AbstractTychoProject) projectTypes.get( project.getPackaging() );
        if ( dr == null )
        {
            return;
        }

        // generic Eclipse/OSGi metadata

        dr.setupProject( session, project );

        // p2 metadata

        Properties properties = new Properties();
        properties.putAll( project.getProperties() );
        properties.putAll( session.getSystemProperties() ); // session wins
        properties.putAll( session.getUserProperties() );
        project.setContextValue( TychoConstants.CTX_MERGED_PROPERTIES, properties );

        TargetPlatformConfiguration configuration =
            configurationReader.getTargetPlatformConfiguration( session, project );
        project.setContextValue( TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION, configuration );

        TargetPlatformResolver resolver = targetPlatformResolverLocator.lookupPlatformResolver( project );

        resolver.setupProjects( session, project, reactorProject );
    }

    public void resolveProject( MavenSession session, MavenProject project, List<ReactorProject> reactorProjects )
    {
        AbstractTychoProject dr = (AbstractTychoProject) projectTypes.get( project.getPackaging() );
        if ( dr == null )
        {
            return;
        }

        TargetPlatformResolver resolver = targetPlatformResolverLocator.lookupPlatformResolver( project );

        logger.info( "Resolving target platform for project " + project );
        TargetPlatform targetPlatform =
            resolver.resolvePlatform( session, project, reactorProjects, null );

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

    public void traverse( MavenProject project, final DependencyVisitor visitor )
    {
        TychoProject tychoProject = projectTypes.get( project.getPackaging() );
        if ( tychoProject != null )
        {
            tychoProject.getDependencyWalker( project ).walk( new ArtifactDependencyVisitor()
            {
                public void visitPlugin( org.codehaus.tycho.PluginDescription plugin )
                {
                    visitor.visit( plugin );
                };

                public boolean visitFeature( org.codehaus.tycho.FeatureDescription feature )
                {
                    return visitor.visit( feature );
                };
            } );
        }
        else
        {
            // TODO do something!
        }
    }

}
