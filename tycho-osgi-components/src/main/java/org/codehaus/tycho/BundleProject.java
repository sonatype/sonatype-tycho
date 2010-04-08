package org.codehaus.tycho;

import java.util.List;

import org.apache.maven.project.MavenProject;

public interface BundleProject
    extends TychoProject
{
    public List<ClasspathEntry> getClasspath( MavenProject project );
}
