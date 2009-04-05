package org.codehaus.tycho.osgitools.targetplatform;

import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.sonatype.tycho.TargetPlatformResolver;

public interface MavenTargetPlatformResolver
    extends TargetPlatformResolver
{

    public void setMavenProjects(  List<MavenProject> projects );

    public void setLocalRepository( ArtifactRepository localRepository );

}
