package org.codehaus.tycho.osgitools.project;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.eclipse.osgi.service.resolver.BundleDescription;

public interface EclipsePluginProject {

	public MavenProject getMavenProject();

	public BundleDescription getBundleDescription();

	public Properties getBuildProperties();

	public List<BuildOutputJar> getOutputJars();

	public BuildOutputJar getDotOutputJar();

	public Map<String, BuildOutputJar> getOutputJarMap();
}
