package org.codehaus.tycho.eclipsepackaging;

import java.io.File;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.jar.JarArchiver;


/**
 * @phase package
 * @goal package-feature
 * @requiresProject
 * @requiresDependencyResolution runtime
 * 
 */
public class PackageFeatureMojo extends AbstractMojo {

	/**
	 * The maven archiver to use.
	 * 
	 * @parameter
	 */
	private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

	/**
	 * @parameter expression="${project}"
	 * @required
	 */
	protected MavenProject project;

	/**
	 * Temporary assembly directory
	 * 
	 * @parameter expression="${project.build.directory}/deploy"
	 */
	private File deployDirectory;

	/**
	 * @parameter expression="${project.build.directory}"
	 */
	private File outputDirectory;

	/**
	 * Name of the generated JAR.
	 * 
	 * @parameter alias="jarName" expression="${project.build.finalName}"
	 * @required
	 */
	private String finalName;

	/**
	 * The Jar archiver.
	 * 
	 * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
	 * @required
	 */
	private JarArchiver jarArchiver;

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			File outputJar = new File(outputDirectory, finalName + ".jar");
			MavenArchiver archiver = new MavenArchiver();
			archiver.setArchiver(jarArchiver);
			archiver.setOutputFile(outputJar);
			archiver.getArchiver().addDirectory(deployDirectory,
					new String[] { "**/*" }, new String[0]);

			archiver.createArchive(project, archive);

			project.getArtifact().setFile(outputJar);

		} catch (Exception e) {
			throw new MojoExecutionException("", e);
		}
	}

}