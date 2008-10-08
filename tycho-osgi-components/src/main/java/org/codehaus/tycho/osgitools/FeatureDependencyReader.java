package org.codehaus.tycho.osgitools;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.tycho.model.Feature;

/**
 * @plexus.component role="org.codehaus.tycho.maven.DependenciesReader"
 *                   role-hint="eclipse-feature"
 */
public class FeatureDependencyReader extends AbstractDependenciesReader {

	public List<Dependency> getDependencies(MavenProject project)
			throws MavenExecutionException {
		Feature feature = state.getFeature(project);

		if (feature == null) {
			return NO_DEPENDENCIES;
		}

		ArrayList<Dependency> result = new ArrayList<Dependency>();

		result.addAll(getPluginsDependencies(feature.getPlugins()));
		result.addAll(getFeaturesDependencies(feature.getIncludedFeatures()));

		return result;
	}

}
