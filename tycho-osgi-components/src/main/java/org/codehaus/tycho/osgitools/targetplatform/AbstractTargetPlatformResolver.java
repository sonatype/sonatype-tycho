package org.codehaus.tycho.osgitools.targetplatform;

import java.io.File;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.tycho.ArtifactKey;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.TychoProject;

public abstract class AbstractTargetPlatformResolver
    extends AbstractLogEnabled
    implements TargetPlatformResolver
{
    @Requirement( role = TychoProject.class )
    private Map<String, TychoProject> projectTypes;

    protected boolean isSubdir( File parent, File child )
    {
        return child.getAbsolutePath().startsWith( parent.getAbsolutePath() );
    }

    protected void addProjects( MavenSession session, DefaultTargetPlatform platform )
    {
        File parentDir = null;

        for ( MavenProject project : session.getProjects() )
        {
            TychoProject dr = projectTypes.get( project.getPackaging() );
            if ( dr != null )
            {
                ArtifactKey key = dr.getArtifactKey( project );
    
                platform.removeAll( key.getType(), key.getId() );
    
                platform.addMavenProject( key, project );
    
                if ( parentDir == null || isSubdir( project.getBasedir(), parentDir ) )
                {
                    parentDir = project.getBasedir();
                }
            }
        }

        platform.addSite( parentDir );
    }

}
