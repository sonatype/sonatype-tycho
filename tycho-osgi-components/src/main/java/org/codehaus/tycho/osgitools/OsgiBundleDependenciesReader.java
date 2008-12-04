package org.codehaus.tycho.osgitools;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.tycho.maven.DependenciesReader;
import org.codehaus.tycho.maven.EclipseMavenProjetBuilder;
import org.codehaus.tycho.osgitools.DependencyComputer.DependencyEntry;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.BundleException;

@Component( role = DependenciesReader.class, hint = "eclipse-plugin" )
public class OsgiBundleDependenciesReader extends AbstractDependenciesReader {

	@Requirement
	private DependencyComputer dependencyComputer;

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

		for (DependencyEntry entry : dependencyComputer.computeDependencies(bundleDescription)) {
			BundleDescription supplier = entry.desc;

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
}
