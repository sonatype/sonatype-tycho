package org.sonatype.tycho.resolver;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

public interface TychoDependencyResolver
{
    /**
     * All reactor projects involved in the build
     */
    public void setupProjects( MavenSession session, List<MavenProject> projects );

    public void resolveProject( MavenSession session, MavenProject project );

    public void traverse( MavenProject project, DependencyVisitor visitor );
}
