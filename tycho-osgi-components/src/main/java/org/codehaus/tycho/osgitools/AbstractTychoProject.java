package org.codehaus.tycho.osgitools;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TychoConstants;

public abstract class AbstractTychoProject
    extends AbstractLogEnabled
    implements TychoProject
{

    public TargetPlatform getTargetPlatform( MavenProject project )
    {
        TargetPlatform targetPlatform = (TargetPlatform) project.getContextValue( TychoConstants.CTX_TARGET_PLATFORM );
        if ( targetPlatform == null )
        {
            throw new IllegalStateException( "No associated target platform for project " + project.toString()  );
        }
        return targetPlatform;
    }

    public void setTargetPlatform( MavenSession session, MavenProject project, TargetPlatform targetPlatform )
    {
        project.setContextValue( TychoConstants.CTX_TARGET_PLATFORM, targetPlatform );
    }

    public void setupProject( MavenSession session, MavenProject project )
    {
        // do nothing by default
    }

    public abstract void resolve( MavenProject project );
}
