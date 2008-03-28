package org.codehaus.tycho.targetplatform;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Removes the plugins and markers directories
 * 
 * @goal clean
 * @requiresDependencyResolution compile
 * 
 */
public class CleanMojo extends AbstractMojo {

	/**
	 * @parameter default-value="${project.build.directory}/plugins"
	 */
	private File pluginsDir;

	/**
	 * @parameter default-value="${project.build.directory}/markers"
	 */
	private File markersDirectory;

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			FileUtils.deleteDirectory(pluginsDir);
			FileUtils.deleteDirectory(markersDirectory);
		} catch (IOException e) {
			throw new MojoExecutionException("Could not delete directory", e);
		}
	}
}
