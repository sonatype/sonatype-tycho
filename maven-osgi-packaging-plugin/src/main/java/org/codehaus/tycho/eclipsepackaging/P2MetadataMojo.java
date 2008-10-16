package org.codehaus.tycho.eclipsepackaging;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.tycho.p2.P2;

/**
 * @goal p2-metadata
 */
public class P2MetadataMojo extends AbstractMojo {

	/**
	 * @parameter expression="${project}"
	 * @required
	 */
	protected MavenProject project;

	/**
	 * Metadata repository name
	 * 
	 * @parameter default-value="${project.name}"
	 * @required
	 */
	private String metadataRepositoryName;

	/**
	 * Generated update site location (must match update-site mojo configuration)
	 *  
	 * @parameter expression="${project.build.directory}/site" 
	 */
	private File target;

	/**
	 * Artifact repository name
	 * 
	 * @parameter default-value="${project.name} Artifacts"
	 * @required
	 */
	private String artifactRepositoryName;

	/**
     * Kill the forked test process after a certain number of seconds.  If set to 0,
     * wait forever for the process, never timing out.
     * 
     * @parameter expression="${p2.timeout}"
     */
    private int forkedProcessTimeoutInSeconds;

    /**
     * Arbitrary JVM options to set on the command line.
     * 
     * @parameter
     */
    private String argLine;

	/** @component */
	private P2 p2;

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			generateMetadata();
		} catch (MojoFailureException e) {
			throw e;
//		} catch (MojoExecutionException e) {
//			throw e;
		} catch (Exception e) {
			throw new MojoExecutionException("Cannot generate P2 metadata", e);
		}
	}
	
	private void generateMetadata() throws Exception {
		Commandline cli = new Commandline();

		cli.setWorkingDirectory(project.getBasedir());

		String executable = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		if (File.separatorChar == '\\') {
			executable = executable + ".exe";
		}
		cli.setExecutable(executable);

		cli.addArguments(new String[] {
			"-jar", getEclipseLauncher().getAbsolutePath(),
		});
		
		cli.addArguments(new String[] {
			"-nosplash",
			"-application", "org.eclipse.equinox.p2.metadata.generator.EclipseGenerator",
			"-updateSite", getUpdateSiteLocation().getCanonicalPath(),
			"-site", new File(getUpdateSiteLocation(), "site.xml").toURL().toExternalForm(),
			"-metadataRepository",	getUpdateSiteLocation().toURL().toExternalForm(),
			"-metadataRepositoryName", metadataRepositoryName,
			"-artifactRepository", getUpdateSiteLocation().toURL().toExternalForm(),
			"-artifactRepositoryName", artifactRepositoryName,
			"-noDefaultIUs",
			"-vmargs", argLine,
		});

		getLog().info("Command line:\n\t" + cli.toString());

		StreamConsumer out = new StreamConsumer() {
			public void consumeLine(String line) {
				System.out.println(line);
			}
		};

		StreamConsumer err = new StreamConsumer() {
			public void consumeLine(String line) {
				System.err.println(line);
			}
		};

		int result = CommandLineUtils.executeCommandLine(cli, out, err,	forkedProcessTimeoutInSeconds);
		if (result != 0) {
            throw new MojoFailureException("P2 return code was " + result);
		}
	}

	private File getUpdateSiteLocation() {
		return target;
	}

	private File getEclipseLauncher() throws MojoFailureException {
		// XXX dirty hack
		try {
			return new File(p2.getP2RuntimeLocation(), "plugins/org.eclipse.equinox.launcher_1.0.101.R34x_v20080819.jar");
		} catch (MavenExecutionException e) {
			throw new MojoFailureException("Can't locate P2 runtime", e);
		}
	}

}
