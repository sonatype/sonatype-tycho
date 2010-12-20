package org.codehaus.tycho.osgitools;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformConfiguration;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.osgitools.targetplatform.MultiEnvironmentTargetPlatform;
import org.codehaus.tycho.utils.TychoProjectUtils;

public abstract class AbstractTychoProject
    extends AbstractLogEnabled
    implements TychoProject
{

    public TargetPlatform getTargetPlatform( MavenProject project )
    {
        return TychoProjectUtils.getTargetPlatform( project );
    }

    public TargetPlatform getTargetPlatform( MavenProject project, TargetEnvironment environment )
    {
        TargetPlatform platform = getTargetPlatform( project );

        if ( environment != null && platform instanceof MultiEnvironmentTargetPlatform )
        {
            platform = ( (MultiEnvironmentTargetPlatform) platform ).getPlatform( environment );

            if ( platform == null )
            {
                throw new IllegalStateException( "Unsupported runtime environment " + environment.toString()
                    + " for project " + project.toString() );
            }
        }

        return platform;
    }

    public void setTargetPlatform( MavenSession session, MavenProject project, TargetPlatform targetPlatform )
    {
        project.setContextValue( TychoConstants.CTX_TARGET_PLATFORM, targetPlatform );
    }

    public void setupProject( MavenSession session, MavenProject project )
    {
        // do nothing by default
    }

    public abstract void resolve( MavenSession session, MavenProject project );

    protected TargetEnvironment[] getEnvironments( MavenProject project, TargetEnvironment environment )
    {
        if ( environment != null )
        {
            return new TargetEnvironment[] { environment };
        }

        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration( project );
        if ( configuration.isImplicitTargetEnvironment() )
        {
            return null; // any
        }

        // all specified
        List<TargetEnvironment> environments = configuration.getEnvironments();
        return environments.toArray( new TargetEnvironment[environments.size()] );
    }

}
