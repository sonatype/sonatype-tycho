package org.codehaus.tycho.maven;

import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reactor.MavenExecutionException;


public interface DependenciesReader {

	static final String ROLE = DependenciesReader.class.getName();

	List<Dependency> getDependencies(MavenProject project) throws MavenExecutionException;
}
