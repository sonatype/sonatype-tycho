package org.codehaus.tycho.osgitools.targetplatform;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.tycho.ArtifactKey;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.TargetPlatformResolver;

public abstract class AbstractTargetPlatformResolver
    extends AbstractLogEnabled
    implements TargetPlatformResolver
{
    protected boolean isSubdir( File parent, File child )
    {
        return child.getAbsolutePath().startsWith( parent.getAbsolutePath() );
    }

    protected void addProjects( MavenSession session, DefaultTargetPlatform platform )
    {
        File parentDir = null;

        for ( MavenProject project : session.getProjects() )
        {
            try
            {
                TychoProject dr = (TychoProject) session.lookup( TychoProject.class.getName(), project.getPackaging() );

                ArtifactKey key = dr.getArtifactKey( project );

                platform.removeAll( key.getType(), key.getId() );

                platform.addMavenProject( key, project );

                if ( parentDir == null || isSubdir( project.getBasedir(), parentDir ) )
                {
                    parentDir = project.getBasedir();
                }
            }
            catch ( ComponentLookupException e )
            {
                // fully expected
            }
        }

        platform.addSite( parentDir );
    }

}
