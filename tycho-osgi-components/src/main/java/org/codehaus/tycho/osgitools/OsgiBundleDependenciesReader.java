package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.tycho.maven.EclipseMavenProjetBuilder;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.BundleException;

/**
 * @plexus.component role="org.codehaus.tycho.maven.DependenciesReader"
 * 		role-hint="eclipse-plugin"
 */
public class OsgiBundleDependenciesReader extends AbstractDependenciesReader {

    /** @plexus.requirement */
	private OsgiState state;

	public List<Dependency> getDependencies(MavenProject project) throws MavenExecutionException {
		BundleDescription bundleDescription = state.getBundleDescription(project);
		if (bundleDescription == null) {
			return NO_DEPENDENCIES ;
		}

		try {
			state.assertResolved(bundleDescription);
		} catch (BundleException e) {
			throw new MavenExecutionException(e.getMessage(), project.getFile());
		}

		ArrayList<Dependency> result = new ArrayList<Dependency>();

		BundleDescription[] requiredBundles = state.getDependencies(bundleDescription);
		for (int i = 0; i < requiredBundles.length; i++) {
			BundleDescription supplier = requiredBundles[i].getSupplier().getSupplier();

			MavenProject otherProject = state.getMavenProject(supplier);
			
			Dependency dependency;
			if (otherProject != null) {
				dependency = newProjectDependency(otherProject);
			} else {
				String groupId = EclipseMavenProjetBuilder.getGroupId(state, supplier);
				String artifactId = supplier.getSymbolicName();
				String version = supplier.getVersion().toString();

				dependency = newExternalDependency(supplier.getLocation(), groupId, artifactId, version);
			}

			if (dependency != null) {
				result.add(dependency);
			}
		}
		
		return result;
	}

	private Dependency newExternalDependency(String location, String groupId, String artifactId, String version) {
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

	private Dependency newProjectDependency(MavenProject otherProject) {
		Dependency dependency = new Dependency();
		dependency.setArtifactId(otherProject.getArtifactId());
		dependency.setGroupId(otherProject.getGroupId());
		dependency.setVersion(otherProject.getVersion());
		dependency.setScope(Artifact.SCOPE_PROVIDED);
		return dependency;
	}

}
