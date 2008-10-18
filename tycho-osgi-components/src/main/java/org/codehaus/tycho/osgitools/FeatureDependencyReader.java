package org.codehaus.tycho.osgitools;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.osgitools.features.FeatureDescription;

/**
 * @plexus.component role="org.codehaus.tycho.maven.DependenciesReader"
 *                   role-hint="eclipse-feature"
 */
public class FeatureDependencyReader extends AbstractDependenciesReader {

	public List<Dependency> getDependencies(MavenProject project) throws MavenExecutionException {

		FeatureDescription description = state.getFeatureDescription(project);

		if (description == null) {
			return NO_DEPENDENCIES;
		}

		ArrayList<Dependency> result = new ArrayList<Dependency>();

		Feature feature = description.getFeature();

		result.addAll(getPluginsDependencies(feature.getPlugins()));
		result.addAll(getFeaturesDependencies(feature.getIncludedFeatures()));

		return result;
	}

}
