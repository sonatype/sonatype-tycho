package org.codehaus.tycho.osgitools;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.PluginRef;
import org.eclipse.osgi.service.resolver.BundleDescription;

/**
 * @plexus.component role="org.codehaus.tycho.maven.DependenciesReader"
* 		role-hint="eclipse-feature"
*/
public class FeatureDependencyReader extends AbstractDependenciesReader {

	/** @plexus.requirement */
	private OsgiState state;

	public List<Dependency> getDependencies(MavenProject project) throws MavenExecutionException {
		Feature feature = state.getFeature(project);

		if (feature == null) {
			return NO_DEPENDENCIES;
		}

		ArrayList<Dependency> result = new ArrayList<Dependency>();
		for (PluginRef pluginRef : feature.getPlugins()) {
			BundleDescription bundle = state.getBundleDescription(pluginRef.getId(), getPluginVersion(pluginRef.getVersion()));
			if (bundle == null) {
				continue;
			}
			Dependency dependency = newProjectDependency(state.getMavenProject(bundle));
			if (dependency != null) {
				result.add(dependency);
			}
		}

		for (Feature.FeatureRef featureRef : feature.getIncludedFeatures()) {
			Feature otherFeature = state.getFeature(featureRef.getId(), featureRef.getVersion());
			if (otherFeature == null) {
				continue;
			}
			Dependency dependency = newProjectDependency(state.getMavenProject(otherFeature));
			if (dependency != null) {
				result.add(dependency);
			}
		}
		return result;
	}

	private String getPluginVersion(String version) {
		return "0.0.0".equals(version)? OsgiState.HIGHEST_VERSION : version;
	}

}
