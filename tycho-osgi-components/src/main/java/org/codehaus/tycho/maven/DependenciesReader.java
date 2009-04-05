package org.codehaus.tycho.maven;

import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.tycho.TychoSession;
import org.sonatype.tycho.TargetPlatformResolver;

public interface DependenciesReader
{

    static final String ROLE = DependenciesReader.class.getName();

    public static final String DEPENDENCY_GROUP_ID = ":tycho:";

    List<Dependency> getDependencies( MavenProject project, TychoSession session )
        throws MavenExecutionException;

    void addProject( TargetPlatformResolver platformResolver, MavenProject project );
}
