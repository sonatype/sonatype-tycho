package org.eclipse.tycho.osgitools;

import java.io.File;
import java.io.IOException;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.ArtifactDependencyVisitor;
import org.eclipse.tycho.ArtifactDependencyWalker;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoProject;
import org.eclipse.tycho.model.UpdateSite;

@Component( role = TychoProject.class, hint = org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_UPDATE_SITE )
public class UpdateSiteProject
    extends AbstractArtifactBasedProject
{

    @Override
    protected ArtifactDependencyWalker newDependencyWalker( MavenProject project, TargetEnvironment environment )
    {
        final UpdateSite site = loadSite( project );
        return new AbstractArtifactDependencyWalker( getTargetPlatform( project, environment ),
                                                     getEnvironments( project, environment ) )
        {
            public void walk( ArtifactDependencyVisitor visitor )
            {
                traverseUpdateSite( site, visitor );
            }
        };
    }

    private UpdateSite loadSite( MavenProject project )
    {
        File file = new File( project.getBasedir(), UpdateSite.SITE_XML );
        try
        {
            return UpdateSite.read( file );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Could not read site.xml " + file.getAbsolutePath(), e );
        }
    }

    public ArtifactKey getArtifactKey( ReactorProject project )
    {
        return new DefaultArtifactKey( org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_UPDATE_SITE, project.getArtifactId(), getOsgiVersion( project ) );
    }
}
