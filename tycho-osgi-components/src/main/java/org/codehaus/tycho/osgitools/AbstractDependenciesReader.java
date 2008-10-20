package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.tycho.maven.DependenciesReader;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.PluginRef;
import org.codehaus.tycho.model.Feature.FeatureRef;
import org.codehaus.tycho.osgitools.features.FeatureDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;

public abstract class AbstractDependenciesReader extends AbstractLogEnabled implements DependenciesReader {

	protected static final List<Dependency> NO_DEPENDENCIES = new ArrayList<Dependency>();

	/** @plexus.requirement */
	protected OsgiState state;

	
	protected Dependency newExternalDependency(String location, String groupId, String artifactId, String version) {
		File file = new File(location);
		if (!file.exists() || !file.isFile() || !file.canRead()) {
			getLogger().warn("Dependency at location " + location + " can not be represented in Maven model and will not be visible to non-OSGi aware Maven plugins");
			return null;
		}

		Dependency dependency = new Dependency();
		dependency.setArtifactId(artifactId);
		dependency.setGroupId(groupId);
		dependency.setVersion(version);
		dependency.setScope(Artifact.SCOPE_SYSTEM);
		dependency.setSystemPath(location);
		return dependency;
	}

	protected Dependency newProjectDependency(MavenProject otherProject) {
		if (otherProject == null) {
			return null;
		}

		Dependency dependency = new Dependency();
		dependency.setArtifactId(otherProject.getArtifactId());
		dependency.setGroupId(otherProject.getGroupId());
		dependency.setVersion(otherProject.getVersion());
		dependency.setType(otherProject.getPackaging());
		dependency.setScope(Artifact.SCOPE_PROVIDED);
		return dependency;
	}

	protected Collection<? extends Dependency> getFeaturesDependencies(List<FeatureRef> features) {
		Collection<Dependency> result = new ArrayList<Dependency>();
		for (Feature.FeatureRef featureRef : features) {
			FeatureDescription otherFeature = state.getFeatureDescription(featureRef.getId(), featureRef.getVersion());
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

	protected Collection<? extends Dependency> getPluginsDependencies(List<PluginRef> plugins) {
		Collection<Dependency> result = new ArrayList<Dependency>();
		for (PluginRef pluginRef : plugins) {
			BundleDescription bundle = state.getBundleDescription(pluginRef
					.getId(), getPluginVersion(pluginRef.getVersion()));
			if (bundle == null) {
				continue;
			}
			Dependency dependency = newProjectDependency(state
					.getMavenProject(bundle));
			if (dependency != null) {
				result.add(dependency);
			}
		}
		return result;
	}

	private String getPluginVersion(String version) {
		return version == null || "0.0.0".equals(version) ? OsgiState.HIGHEST_VERSION : version;
	}
	
}
