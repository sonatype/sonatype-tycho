package org.codehaus.tycho.osgitools.targetplatform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.tycho.DefaultTargetPlatform;
import org.codehaus.tycho.TargetPlatformResolver;

public abstract class AbstractTargetPlatformResolver
    extends AbstractLogEnabled
    implements TargetPlatformResolver
{

    protected List<MavenProject> projects;

    protected ArtifactRepository localRepository;

    public void setMavenProjects( List<MavenProject> projects )
    {
        this.projects = new ArrayList<MavenProject>( projects );
    }

    protected boolean isSubdir( File parent, File child )
    {
        return child.getAbsolutePath().startsWith( parent.getAbsolutePath() );
    }

    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    protected DefaultTargetPlatform createPlatform()
    {
        DefaultTargetPlatform platform = new DefaultTargetPlatform();

        return platform;
    }

    protected void addProjects( DefaultTargetPlatform platform )
    {
        File parentDir = null;

        for ( MavenProject otherProject : projects )
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
