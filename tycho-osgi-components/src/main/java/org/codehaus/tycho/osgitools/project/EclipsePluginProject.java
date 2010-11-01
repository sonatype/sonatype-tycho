package org.codehaus.tycho.osgitools.project;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.sonatype.tycho.resolver.DependentMavenProjectProxy;

public interface EclipsePluginProject {

	public DependentMavenProjectProxy getMavenProject();

	/**
	 * http://help.eclipse.org/ganymede/index.jsp?topic=/org.eclipse.pde.doc.user/reference/pde_feature_generating_build.htm 
	 */
	public Properties getBuildProperties();

	public List<BuildOutputJar> getOutputJars();

	public BuildOutputJar getDotOutputJar();

	public Map<String, BuildOutputJar> getOutputJarMap();
}
