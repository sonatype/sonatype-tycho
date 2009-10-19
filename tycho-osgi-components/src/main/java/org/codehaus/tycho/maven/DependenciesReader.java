package org.codehaus.tycho.maven;

import java.util.List;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.FeatureResolutionState;

public interface DependenciesReader
{
    public List<Dependency> getDependencies( MavenSession session, MavenProject project )
        throws MavenExecutionException;

    public FeatureResolutionState getFeatureResolutionState( MavenSession session, MavenProject project );

    public BundleResolutionState getBundleResolutionState( MavenSession session, MavenProject project );

}
