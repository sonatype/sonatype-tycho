package org.codehaus.tycho.osgitools.targetplatform;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.tycho.DefaultTargetPlatform;
import org.codehaus.tycho.TargetPlatformResolver;

public abstract class AbstractTargetPlatformResolver
    extends AbstractLogEnabled
    implements TargetPlatformResolver
{
    protected boolean isSubdir( File parent, File child )
    {
        return child.getAbsolutePath().startsWith( parent.getAbsolutePath() );
    }

    protected DefaultTargetPlatform createPlatform()
    {
        DefaultTargetPlatform platform = new DefaultTargetPlatform();

        return platform;
    }

    protected void addProjects( MavenSession session, DefaultTargetPlatform platform )
    {
        File parentDir = null;

        for ( MavenProject otherProject : session.getProjects() )
        {
            platform.addArtifactFile( otherProject.getPackaging(), otherProject.getBasedir() );

            if ( parentDir == null || isSubdir( otherProject.getBasedir(), parentDir ) )
            {
                parentDir = otherProject.getBasedir();
            }
        }

        platform.addSite( parentDir );
    }

}
