package org.codehaus.tycho.osgitools;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.tycho.ArtifactDependencyVisitor;
import org.codehaus.tycho.ArtifactDependencyWalker;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.model.Feature;
import org.sonatype.tycho.ArtifactKey;
import org.sonatype.tycho.ReactorProject;

@Component( role = TychoProject.class, hint = org.sonatype.tycho.ArtifactKey.TYPE_ECLIPSE_FEATURE )
public class EclipseFeatureProject
    extends AbstractArtifactBasedProject
{
    @Override
    protected ArtifactDependencyWalker newDependencyWalker( MavenProject project, TargetEnvironment environment )
    {
        final File location = project.getBasedir();
        final Feature feature = Feature.loadFeature( location );
        return new AbstractArtifactDependencyWalker( getTargetPlatform( project, environment ),
                                                     getEnvironments( project, environment ) )
        {
            public void walk( ArtifactDependencyVisitor visitor )
            {
                traverseFeature( location, feature, visitor );
            }
        };
    }

    public ArtifactKey getArtifactKey( ReactorProject project )
    {
        Feature feature = Feature.loadFeature( project.getBasedir() );
        return new DefaultArtifactKey( org.sonatype.tycho.ArtifactKey.TYPE_ECLIPSE_FEATURE, feature.getId(), feature.getVersion() );
    }
}
