package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.tycho.model.ProductConfiguration;

/**
 * @plexus.component role="org.codehaus.tycho.maven.DependenciesReader"
 *                   role-hint="eclipse-application"
 */
public class EclipseApplicationDependenciesReader extends
		AbstractDependenciesReader {

	public List<Dependency> getDependencies(MavenProject project)
			throws MavenExecutionException {
		// at present time there is no way to get plugin configuration here
		String productFilename = project.getArtifactId() + ".product";

		File productFile = new File(project.getBasedir(), productFilename);
		if (!productFile.exists()) {
			getLogger().warn("product file not found at " + productFile.getAbsolutePath());
			return NO_DEPENDENCIES;
		}

		ProductConfiguration product;
		try {
			product = ProductConfiguration.read(productFile);
		} catch (Exception e) {
			String m = e.getMessage();
			if (null == m) {
				m = e.getClass().getName();
			}
			MavenExecutionException me = new MavenExecutionException(m, project
					.getFile());
			me.initCause(e);
			throw me;
		}

		ArrayList<Dependency> result = new ArrayList<Dependency>();

		result.addAll(getPluginsDependencies(product.getPlugins()));
		result.addAll(getFeaturesDependencies(product.getFeatures()));

		return new ArrayList<Dependency>(result);
	}

}
