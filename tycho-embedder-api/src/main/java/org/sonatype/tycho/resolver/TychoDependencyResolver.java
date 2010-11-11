package org.sonatype.tycho.resolver;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.sonatype.tycho.ReactorProject;

public interface TychoDependencyResolver
{
    public void setupProject( MavenSession session, MavenProject project, ReactorProject reactorProject );

    public void resolveProject( MavenSession session, MavenProject project, List<ReactorProject> reactorProjects );

    public void traverse( MavenProject project, DependencyVisitor visitor );
}
