package org.codehaus.tycho.osgitest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.surefire.battery.DirectoryBattery;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.tycho.osgitools.BundleFile;
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
	 * @parameter expression="${project.runtimeArtifacts}"
	 */
	private List runtimeArtifacts;

	/**
	 * @parameter default-value="${project.build.directory}/work"
	 */
	private File work;

	/**
	 * @parameter expression="${project.artifact}"
	 */
	private Artifact projectArtifact;

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

	/**
	 * @parameter expression="${plugin.artifacts}"
	 */
	private List pluginArtifacts;

	/** @parameter expression="${project.build.directory}" */
	private File outputDir;
	

	/** @parameter expression="${project.build.directory}/dev.properties" */
	private File devProperties;
	

	/** @component */
	private OsgiState state;

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

		reportsDirectory.mkdirs();

		File file = new File(project.getBasedir(), JarFile.MANIFEST_NAME);
		BundleFile bundle = new BundleFile(state.loadManifest(file), file);
		String testBundle = bundle.getSymbolicName();

		List tests = getTests();
		if (tests.size() == 0) {
			new MojoFailureException( "No tests were executed!  (Set -DfailIfNoTests=false to ignore this error.)" );
		}

		boolean succeeded = true;
		for (Iterator iter = tests.iterator(); iter.hasNext();) {
			String test = (String) iter.next();
			succeeded &= runTest(targetPlatform, testBundle, test);
		}
		
		if (succeeded) {
			getLog().info("All tests passed!");
		} else {
            throw new MojoFailureException("There are test failures.\n\nPlease refer to " + reportsDirectory + " for the individual test results.");
		}
	}

	private boolean runTest(File targetPlatform, String testBundle, String className) throws MojoExecutionException {
		int result;

		try {
			File output = new File(reportsDirectory, className + ".txt");
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

			cli.addArguments(new String[] {
					"-Xmx512m",
					"-jar", getEclipseLauncher().getAbsolutePath(),
			});

			cli.addArguments(new String[] {
					"-os", "win32",	"-ws", "win32",	"-arch", "x86",
					"-noSplash", "-debug", "-consolelog",
					"-product", "org.eclipse.sdk.ide",
					"-data", workspace,
					"-dev", devProperties.toURI().toURL().toExternalForm(),
					"-install", targetPlatform.getAbsolutePath(),
					"-configuration", new File(work, "configuration").getAbsolutePath(),
					"-application",	"org.eclipse.test.uitestapplication",
					"-testpluginname", testBundle, 
					"-classname", className, 
					"formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter," + output.getAbsolutePath(), 
			});

			result = CommandLineUtils.executeCommandLine(cli, new StreamConsumer() {
				public void consumeLine(String line) {
					System.out.println("Test.Out>" + line);
				}
			}, new StreamConsumer() {
				public void consumeLine(String line) {
					System.err.println("Test.Err>" + line);
				}
			});
		} catch (Exception e) {
			throw new MojoExecutionException("Error while executing platform", e);
		}

		return result == 0;
	}

	private File getEclipseLauncher() throws IOException {
		BundleDescription osgi = state.getBundleDescription("org.eclipse.osgi", OsgiState.HIGHEST_VERSION);
		Version osgiVersion = osgi.getVersion();
		if (osgiVersion.getMajor() == 3 && osgiVersion.getMinor() == 2) {
			return new File(state.getTargetPlaform(), "startup.jar").getCanonicalFile();
		} else {
			// assume eclipse 3.3 or 3.4
			BundleDescription launcher = state.getBundleDescription("org.eclipse.equinox.launcher", OsgiState.HIGHEST_VERSION);
			return new File(launcher.getLocation()).getCanonicalFile();
		}
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

	private String createOsgiBundlesProperty(String osgiBundles) throws MojoExecutionException {
		StringBuffer result = new StringBuffer(osgiBundles);
		for (BundleDescription bundle : getReactorBundles()) {
			result.append(",");
			MavenProject project = state.getMavenProject(bundle);
			if ("eclipse-test-plugin".equals(project.getPackaging())) {
				appendAbsolutePath(result, project.getBasedir());
			} else {
				appendAbsolutePath(result, project.getArtifact().getFile());
			}
		}
		return result.toString();
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

	private Set<BundleDescription> getReactorBundles() throws MojoExecutionException {
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

	private void appendAbsolutePath(StringBuffer result, File file) {
		String url = file.getAbsolutePath().replace('\\', '/');
		result.append("reference:file:" + url);
	}

	private List getTests() throws MojoExecutionException {
		if (!testClassesDirectory.exists()) {
			return Collections.EMPTY_LIST;
		}

		// ----------------------------------------------------------------------
		// Check to see if we are running a single test. The raw parameter will
		// come through if it has not been set.
		// ----------------------------------------------------------------------
		try {
			DirectoryBattery battery;

			if (test != null) {
				// FooTest -> **/FooTest.java

				List includes = new ArrayList();

				List excludes = new ArrayList();

				String[] testRegexes = test.split(",");

				for (int i = 0; i < testRegexes.length; i++) {
					includes.add("**/" + testRegexes[i] + ".java");
				}

				battery = new DirectoryBattery(testClassesDirectory,
						new ArrayList(includes), new ArrayList(excludes));

			} else {
				// defaults here, qdox doesn't like the end javadoc value
				// Have to wrap in an ArrayList as surefire expects an ArrayList
				// instead of a List for some reason
				if (includes == null || includes.size() == 0) {
					includes = Arrays.asList(new String[] { "**/Test*.java",
							"**/*Test.java", "**/*TestCase.java" });
				}
				if (excludes == null || excludes.size() == 0) {
					excludes = Arrays.asList(new String[] {
							"**/Abstract*Test.java",
							"**/Abstract*TestCase.java", "**/*$*" });
				}

				battery = new DirectoryBattery(testClassesDirectory,
						new ArrayList(includes), new ArrayList(excludes));
			}

			return battery.getSubBatteryClassNames();
		} catch (Exception e) {
			throw new MojoExecutionException(
					"Error while discovering tests to run", e);
		}

	}

}
