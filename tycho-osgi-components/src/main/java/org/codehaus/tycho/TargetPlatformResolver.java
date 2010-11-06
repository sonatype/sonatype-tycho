package org.codehaus.tycho;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.sonatype.tycho.ReactorProject;

/**
 * Target platform content resolver. TODO This interface and its implementations require further refinement. I need to
 * decide if new resolver instance is required for each project.
 */
public interface TargetPlatformResolver
{
    public void setupProjects( MavenSession session, MavenProject project, ReactorProject reactorProject );

    public TargetPlatform resolvePlatform( MavenSession session, MavenProject project,
                                           List<ReactorProject> reactorProjects, List<Dependency> dependencies );
}
