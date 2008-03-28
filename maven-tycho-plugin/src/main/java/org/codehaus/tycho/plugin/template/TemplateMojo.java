package org.codehaus.tycho.plugin.template;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.Writer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;

/**
 * Processes a Velocity template. Adds the project dependencies as a
 * variable for use in the template. Used in webstart projects.
 * 
 * @goal template
 * @author tom
 *
 */
public class TemplateMojo extends AbstractMojo {

	/**
	 * @parameter expression=${template}"
	 * @required
	 */
	public String template;

	/**
	 * @parameter expression=${target}"
	 * @required
	 */
	public File target;

	/**
	 * @parameter expression="${pluginsDirectory}"
	 * @required 
	 */
	
	private File installDirectory;

	/**
	 * @parameter expression="${project}"
	 */
	public MavenProject project;

	public void execute() throws MojoExecutionException, MojoFailureException {

		String[] jarFiles = new File(installDirectory, "plugins").list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		});

		for (int i = 0; i < jarFiles.length; i++) {
			jarFiles[i] =  "plugins/" + jarFiles[i];
			
		}
		
		createFeatureJNLP(jarFiles);

	}

	private void createFeatureJNLP(String[] pluginLocations)
			throws MojoExecutionException {
		for (int i = 0; i < pluginLocations.length; i++) {
			pluginLocations[i] = pluginLocations[i].replace('\\', '/');
		}

		try {
			VelocityEngine ve = new VelocityEngine();
			ve.setProperty("resource.loader.class", FileResourceLoader.class.getName());
			ve.setProperty("file.resource.loader.path",
					project.getBasedir().getPath());
			
			ve.init();
			VelocityContext context = new VelocityContext();

			context.put("plugins", pluginLocations);

			Writer w = new FileWriter(target);

			ve.getTemplate(template).merge(context, w);

			w.close();
		} catch (Exception e) {
			throw new MojoExecutionException(
					"Error creating feature jnlp with velocity", e);
		}
	}
}
