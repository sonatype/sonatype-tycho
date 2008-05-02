package org.codehaus.tycho.plugin.osgi;

import java.io.File;
import java.io.FileFilter;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.tycho.osgitools.OsgiStateController;
import org.eclipse.osgi.service.resolver.ResolverError;

/**
 * Verify a local directory as an OSGi install
 * 
 * @goal verify-directory
 * @requiresProject false
 * @author tom
 * 
 */
public class ApplicationDependencyVerifierMojo extends AbstractMojo
{

	/**
	 * @parameter expression="${install}"
	 * @required
	 */
	private File install;

	/**
	 * @parameter default-value="true"
	 * 
	 */
	private boolean failOnError;

	/** @parameter expression="${project.build.directory}" */
	private File outputDir;

	public void execute() throws MojoExecutionException, MojoFailureException
	{
		OsgiStateController state = new OsgiStateController(outputDir);

		File[] jars = install.listFiles(new FileFilter()
		{
			public boolean accept(File pathname)
			{
				return pathname.getName().endsWith(".jar")
						|| pathname.isDirectory();
			}
		});

		for (int i = 0; i < jars.length; i++) {
			File jar = jars[i];
			try
			{
				state.addBundle(jar);
			}
			catch (Exception e)
			{
				getLog().error("Error adding bundle " + jar);
				// throw new MojoExecutionException("Error adding bundle", e);
			}
		}
		state.resolveState();

		ResolverError[] errors = state.getRelevantErrors();

		for (int i = 0; i < errors.length; i++) {
			ResolverError error = errors[i];
			getLog().error("Bundle "  + error.getBundle().getSymbolicName() + " - " + error.toString());
		}
		
		if (errors.length > 0 && failOnError)
		{
			throw new MojoFailureException(
					"Errors found while verifying installation");
		}

	}

}
