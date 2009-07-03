package org.codehaus.tycho;

import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;


/**
 * Target platform content resolver.
 * 
 * TODO This interface and its implementations require further refinement.
 *      I need to decide if new resolver instance is required for each project.
 */
public interface TargetPlatformResolver
{
    public TargetPlatform resolvePlatform( MavenProject project, List<Dependency> dependencies );

    public void setMavenProjects( List<MavenProject> projects );

    public void setLocalRepository( ArtifactRepository localRepository );

}
