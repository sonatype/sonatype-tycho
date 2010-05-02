package org.codehaus.tycho.osgitools;

import java.io.File;
import java.io.IOException;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.tycho.ArtifactDependencyVisitor;
import org.codehaus.tycho.ArtifactDependencyWalker;
import org.codehaus.tycho.ArtifactKey;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.model.UpdateSite;

@Component( role = TychoProject.class, hint = TychoProject.ECLIPSE_UPDATE_SITE )
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

    public ArtifactKey getArtifactKey( MavenProject project )
    {
        return new ArtifactKey( TychoProject.ECLIPSE_UPDATE_SITE, project.getArtifactId(), project.getVersion() );
    }

}
