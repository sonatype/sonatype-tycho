package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.UpdateSite;

/**
  * @plexus.component role="org.codehaus.tycho.maven.DependenciesReader"
 * 		role-hint="eclipse-update-site"
 */
public class UpdateSiteDependenciesReader extends AbstractDependenciesReader {

    /** @plexus.requirement */
	private OsgiState state;

	public List<Dependency> getDependencies(MavenProject project) throws MavenExecutionException {
		try {
			UpdateSite site = UpdateSite.read(new File(project.getBasedir(), "site.xml"));

			Set<Dependency> result = new LinkedHashSet<Dependency>();
			for (UpdateSite.FeatureRef featureRef : site.getFeatures()) {
				Feature feature = state.getFeature(featureRef.getId(), featureRef.getVersion());
				Dependency dependency = newProjectDependency(state.getMavenProject(feature));
				if (dependency != null) {
					result.add(dependency);
				}
			}

			return new ArrayList<Dependency>(result);
		} catch (Exception e) {
			throw new MavenExecutionException(e.getMessage(), project.getFile());
		}
	}

}
