package org.codehaus.tycho.osgitest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.Arg;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.tycho.osgitools.OsgiState;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.Version;

/**
 * @phase integration-test
 * @goal test
 * @requiresProject true
 * @requiresDependencyResolution runtime
 */
public class TestMojo extends AbstractMojo {

	/**
	 * @parameter default-value="${project.build.directory}/work"
	 */
	private File work;

	/**
	 * @parameter expression="${project}"
	 */
	private MavenProject project;

	/**
	 * @parameter expression="${debugPort}"
	 */
	private int debugPort;

	/**
	 * List of patterns (separated by commas) used to specify the tests that
	 * should be included in testing. When not specified and whent the
	 * <code>test</code> parameter is not specified, the default includes will
	 * be
	 * <code>**&#47;Test*.java   **&#47;*Test.java   **&#47;*TestCase.java</code>
	 * 
	 * @parameter
	 */
	private List includes;

	/**
	 * List of patterns (separated by commas) used to specify the tests that
	 * should be excluded in testing. When not specified and whent the
	 * <code>test</code> parameter is not specified, the default excludes will
	 * be
	 * <code>**&#47;Abstract*Test.java  **&#47;Abstract*TestCase.java **&#47;*$*</code>
	 * 
	 * @parameter
	 */
	private List excludes;

	/**
	 * Specify this parameter if you want to use the test pattern matching
	 * notation, Ant pattern matching, to select tests to run. The Ant pattern
	 * will be used to create an include pattern formatted like
	 * <code>**&#47;${test}.java</code> When used, the <code>includes</code>
	 * and <code>excludes</code> patterns parameters are ignored
	 * 
	 * @parameter expression="${test}"
	 */
	private String test;

	/**
	 * @parameter expression="${maven.test.skipExec}" default-value="false"
	 */
	private boolean skipExec;

	/**
	 * @parameter expression="${maven.test.skip}" default-value="false"
	 */
	private boolean skip;

	/**
	 * The directory containing generated test classes of the project being
	 * tested.
	 * 
	 * @parameter expression="${project.build.outputDirectory}"
	 * @required
	 */
	private File testClassesDirectory;

	/**
	 * Base directory where all reports are written to.
	 * 
	 * @parameter expression="${project.build.directory}/surefire-reports"
	 */
	private File reportsDirectory;

	/** @parameter expression="${project.build.directory}/surefire.properties" */
	private File surefireProperties;

	/** @parameter expression="${project.build.directory}/dev.properties" */
	private File devProperties;

	/** @component */
	private OsgiState state;

	/** @parameter default-value="false" */
	private boolean useUIHarness;

	/**
	 * @parameter expression="${plugin.artifacts}"
	 */
	private List<Artifact> pluginArtifacts;

	/**
     * Arbitrary JVM options to set on the command line.
     * 
     * @parameter
     */
    private String argLine;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip || skipExec) {
			return;
		}

		File targetPlatform = state.getTargetPlaform();

		if (targetPlatform == null) {
			getLog().info("Cannot determinate build target platform location -- not executing tests");
			return;
		}

		work.mkdirs();

		createConfiguration(targetPlatform);
		createDevProperties();
		createSurefireProperties();

		reportsDirectory.mkdirs();

		String testBundle = null;
		boolean succeeded = runTest(targetPlatform, testBundle , test);
		
		if (succeeded) {
			getLog().info("All tests passed!");
		} else {
            throw new MojoFailureException("There are test failures.\n\nPlease refer to " + reportsDirectory + " for the individual test results.");
		}
	}

	private void createSurefireProperties() throws MojoExecutionException {
		Properties p = new Properties();

		BundleDescription bundle = state.getBundleDescription(project);
		p.put("testpluginname", bundle.getSymbolicName());
		p.put("testclassesdirectory", testClassesDirectory.getAbsolutePath());
		p.put("reportsdirectory", reportsDirectory.getAbsolutePath());

		p.put("includes", includes != null? getIncludesExcludes(includes): "**/Test*.class,**/*Test.class,**/*TestCase.class");
		p.put("excludes", excludes != null? getIncludesExcludes(excludes): "**/Abstract*Test.class,**/Abstract*TestCase.class,**/*$*");

		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(surefireProperties));
			try {
				p.store(out, null);
			} finally {
				out.close();
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Can't write test launcher properties file", e);
		}
	}

	private String getIncludesExcludes(List<String> patterns) {
		StringBuilder sb = new StringBuilder();
		for (String pattern : patterns) {
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(pattern);
		}
		return sb.toString();
	}

	private boolean runTest(File targetPlatform, String testBundle, String className) throws MojoExecutionException {
		int result;

		try {
			String workspace = new File(work, "data").getAbsolutePath();
			
			FileUtils.deleteDirectory(workspace);

			Commandline cli = new Commandline();

			cli.setWorkingDirectory(project.getBasedir());

			String executable = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
			if (File.separatorChar == '\\') {
				executable = executable + ".exe";
			}
			cli.setExecutable(executable);

			if (debugPort > 0) {
				cli.addArguments(new String[] {
					"-Xdebug",
					"-Xrunjdwp:transport=dt_socket,address=" + debugPort + ",server=y,suspend=y" });
			}
			cli.addArguments(new String[] {
				"-Dosgi.noShutdown=false",
			});

			if (argLine != null) {
				Arg arg = cli.createArg();
				arg.setLine(argLine);
			}

			cli.addArguments(new String[] {
				"-jar", getEclipseLauncher().getAbsolutePath(),
			});

			cli.addArguments(new String[] {
				// "-debug", "-consolelog", "-console",
				"-data", workspace,
				"-dev", devProperties.toURI().toURL().toExternalForm(),
				"-install", targetPlatform.getAbsolutePath(),
				"-configuration", new File(work, "configuration").getAbsolutePath(),
				"-application",	getTestApplication(),
				"-testproperties", surefireProperties.getAbsolutePath(), 
			});

			getLog().info(cli.toString());

			result = CommandLineUtils.executeCommandLine(cli, new StreamConsumer() {
				public void consumeLine(String line) {
					System.out.println(line);
				}
			}, new StreamConsumer() {
				public void consumeLine(String line) {
					System.err.println(line);
				}
			});
		} catch (Exception e) {
			throw new MojoExecutionException("Error while executing platform", e);
		}

		return result == 0;
	}

	private String getTestApplication() {
		if (useUIHarness) {
			Version osgiVersion = getPlatformVersion();
			if (osgiVersion.getMajor() == 3 && osgiVersion.getMinor() == 2) {
				return "org.codehaus.tycho.surefire.osgibooter.uitest32";
			} else {
				return "org.codehaus.tycho.surefire.osgibooter.uitest";
			}
		} else {
			return "org.codehaus.tycho.surefire.osgibooter.headlesstest";
		}
	}

	private File getEclipseLauncher() throws IOException {
		Version osgiVersion = getPlatformVersion();
		if (osgiVersion.getMajor() == 3 && osgiVersion.getMinor() == 2) {
			return new File(state.getTargetPlaform(), "startup.jar").getCanonicalFile();
		} else {
			// assume eclipse 3.3 or 3.4
			BundleDescription launcher = state.getBundleDescription("org.eclipse.equinox.launcher", OsgiState.HIGHEST_VERSION);
			return new File(launcher.getLocation()).getCanonicalFile();
		}
	}

	private Version getPlatformVersion() {
		BundleDescription osgi = state.getBundleDescription("org.eclipse.osgi", OsgiState.HIGHEST_VERSION);
		return osgi.getVersion();
	}

	private void createConfiguration(File targetPlatform) throws MojoExecutionException {
		try {
			Properties p = new Properties();

			FileInputStream fis = new FileInputStream(new File(targetPlatform, "configuration/config.ini"));;
			try {
				p.load(fis);
			} finally {
				fis.close();
			}

			String osgiBundles = p.getProperty("osgi.bundles");
			String newOsgiBundles = createOsgiBundlesProperty(osgiBundles);
			p.setProperty("osgi.bundles", newOsgiBundles);

			// @see SimpleConfiguratorConstants#PROP_KEY_EXCLUSIVE_INSTALLATION
			p.setProperty("org.eclipse.equinox.simpleconfigurator.exclusiveInstallation", "false");

			new File(work, "configuration").mkdir();
			FileOutputStream fos = new FileOutputStream(new File(work, "configuration/config.ini"));
			try {
				p.store(fos, null);
			} finally {
				fos.close();
			}
		} catch (IOException e) {
			throw new MojoExecutionException("", e);
		}
	}

	private String createOsgiBundlesProperty(String osgiBundles) throws IOException, MojoExecutionException {
		StringBuilder result = new StringBuilder(osgiBundles);
		for (BundleDescription bundle : getReactorBundles()) {
			result.append(",");
			MavenProject project = state.getMavenProject(bundle);
			if ("eclipse-test-plugin".equals(project.getPackaging())) {
				appendAbsolutePath(result, project.getBasedir());
			} else {
				appendAbsolutePath(result, project.getArtifact().getFile());
			}
		}
		result.append(",");
		appendAbsolutePath(result, getOsgiSurefireBooterPlugin());
		return result.toString();
	}

	private File getOsgiSurefireBooterPlugin() throws MojoExecutionException {
		for (Artifact artifact : pluginArtifacts) {
			if ("org.codehaus.tycho".equals(artifact.getGroupId()) && "tycho-surefire-osgi-booter".equals(artifact.getArtifactId())) {
				return artifact.getFile();
			}
		}
		throw new MojoExecutionException("Unable to locate org.codehaus.tycho:tycho-surefire-osgi-booter");
	}

	private void createDevProperties() throws MojoExecutionException {
		Properties dev = new Properties();
		for (BundleDescription bundle : getReactorBundles()) {
			MavenProject project = state.getMavenProject(bundle);
			if ("eclipse-test-plugin".equals(project.getPackaging())) {
				Build build = project.getBuild();
				dev.put(bundle.getSymbolicName(), build.getOutputDirectory() + "," + build.getTestOutputDirectory());
			}
		}

		try {
			OutputStream os = new BufferedOutputStream(new FileOutputStream(devProperties));
			try {
				dev.store(os, null);
			} finally {
				os.close();
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Can't create osgi dev properties file", e);
		}
	}

	private Set<BundleDescription> getReactorBundles() {
		Set<BundleDescription> reactorBundles = new LinkedHashSet<BundleDescription>();
		reactorBundles.add(state.getBundleDescription(project));
		for (BundleDescription desc : state.getBundles()) {
			MavenProject project = state.getMavenProject(desc);
			if (project != null) {
				reactorBundles.add(desc);
			}
		}
		return reactorBundles;
	}

	private void appendAbsolutePath(StringBuilder result, File file) throws IOException {
		String url = file.getAbsolutePath().replace('\\', '/');
		result.append("reference:file:" + url);
	}
}
