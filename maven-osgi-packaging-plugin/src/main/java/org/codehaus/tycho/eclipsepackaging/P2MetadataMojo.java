package org.codehaus.tycho.eclipsepackaging;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

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
//	private P2Facade p2;

	public void execute() throws MojoExecutionException, MojoFailureException {
//		try {
//			generateMetadata();
//		} catch (MojoFailureException e) {
//			throw e;
//		} catch (Exception e) {
//			throw new MojoExecutionException("Cannot generate P2 metadata", e);
//		}
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
			"-jar", getEquinoxLauncher().getAbsolutePath(),
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

	private File getEquinoxLauncher() throws MojoFailureException {
		// XXX dirty hack
        String p2location = null; // p2.getP2RuntimeLocation();
		DirectoryScanner ds = new DirectoryScanner();
		ds.setBasedir(p2location);
        ds.setIncludes(new String[] {
            "plugins/org.eclipse.equinox.launcher_*.jar"
        });
        ds.scan();
        String[] includedFiles = ds.getIncludedFiles();
        if (includedFiles == null || includedFiles.length != 1) {
			throw new MojoFailureException("Can't locate org.eclipse.equinox.launcher bundle in " + p2location);
        }
		return new File(p2location, includedFiles[0]);
	}

}
