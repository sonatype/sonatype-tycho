package org.codehaus.tycho.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Goal to create a JAR-package containing all the source files of a osgi
 * project.
 * 
 * @extendsPlugin source
 * @extendsGoal jar
 * @goal plugin-source
 * @phase package
 */
public class OsgiSourceMojo extends AbstractSourceJarMojo {

	/**
	 * If set to true, compiler will use source folders defined in
	 * build.properties file and will ignore
	 * ${project.compileSourceRoots}/${project.testCompileSourceRoots}.
	 * 
	 * Compilation will fail with an error, if this parameter is set to true but
	 * the project does not have valid build.properties file.
	 * 
	 * @parameter default-value="true"
	 */
	private boolean usePdeSourceRoots;

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	protected List<String> getSources(MavenProject p)
			throws MojoExecutionException {
		if (usePdeSourceRoots) {
			Properties props = getBuildProperties();
			if (props.containsKey("source..")) {
				String sourceRaw = props.getProperty("source..");
				List<String> sources = new ArrayList<String>();
				for (String source : sourceRaw.split(",")) {
					sources.add(new File(project.getBasedir(), source)
							.getAbsolutePath());
				}
				return sources;
			} else {
				throw new MojoExecutionException(
						"Source folder not found at build.properties");
			}
		} else {
			return p.getCompileSourceRoots();
		}
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	protected List getResources(MavenProject p) {
		if (excludeResources) {
			return Collections.EMPTY_LIST;
		}
		if (usePdeSourceRoots) {
			return Collections.EMPTY_LIST;
		}

		return p.getResources();
	}

	/** {@inheritDoc} */
	protected String getClassifier() {
		return "sources";
	}

	// TODO check how to fix this code duplicated
	private Properties getBuildProperties() throws MojoExecutionException {
		File file = new File(project.getBasedir(), "build.properties");
		if (!file.canRead()) {
			throw new MojoExecutionException(
					"Unable to read build.properties file");
		}

		Properties buildProperties = new Properties();
		try {
			InputStream is = new FileInputStream(file);
			try {
				buildProperties.load(is);
			} finally {
				is.close();
			}
		} catch (IOException e) {
			throw new MojoExecutionException(
					"Exception reading build.properties file", e);
		}
		return buildProperties;
	}

}
