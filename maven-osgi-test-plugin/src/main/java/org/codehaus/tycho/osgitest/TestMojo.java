package org.codehaus.tycho.osgitest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.surefire.battery.DirectoryBattery;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.tycho.osgitools.BundleFile;
import org.codehaus.tycho.osgitools.OsgiStateController;

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
	 * @parameter default-value="${project.basedir}/src/test/configuration/config.ini"
	 */
	private File configIni;

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
	 * @parameter expression="${project.build.testOutputDirectory}"
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

	public void execute() throws MojoExecutionException {
		if (skip || skipExec) {
			return;
		}
		
		if (!configIni.exists()) {
			getLog().info("config.ini does not exists - not executing tests");
			return;
		}

		work.mkdirs();

		copyConfigIni();

		reportsDirectory.mkdirs();

		File file = new File(project.getBasedir(), JarFile.MANIFEST_NAME);
		OsgiStateController state = new OsgiStateController(outputDir);
		BundleFile bundle = new BundleFile(state.loadManifest(file), file);
		String testBundle = bundle.getSymbolicName();

		List tests = getTests();
		for (Iterator iter = tests.iterator(); iter.hasNext();) {
			String test = (String) iter.next();
			runTest(testBundle, test);
		}
	}

	private void runTest(String testBundle, String className)
			throws MojoExecutionException {
		try {
			File output = new File(reportsDirectory, className + ".xml");

			Commandline cli = new Commandline();

			cli.setWorkingDirectory(work.getAbsolutePath());

			String executable = System.getProperty("java.home") + File.separator
					+ "bin" + File.separator + "java";
			if (File.separatorChar == '\\') {
				executable = executable + ".exe";
			}
			cli.setExecutable(executable);

			Artifact launcher = getPluginArtifact("org.eclipse", "org.eclipse.osgi");

			// cli.addArguments(args);
			if (debugPort > 0) {
				cli.addArguments(new String[] {
						"-Xdebug",
						"-Xrunjdwp:transport=dt_socket,address=" + debugPort
								+ ",server=y,suspend=y" });
			}
			cli.addArguments(new String[] {
					"-Dosgi.install.area=" + new File(project.getBuild().getDirectory()).getAbsolutePath(),
					"-Dosgi.noShutdown=false",
			});
			cli.addArguments(new String[] { "-jar", 
					launcher.getFile().getAbsolutePath() });
			cli
					.addArguments(new String[] {
							"-application",
							"org.codehaus.tycho.junit4.runner.coretestapplication",
							"formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,"
									+ output.getAbsolutePath(), "-data",
							new File(work, "data").getAbsolutePath(),
							"-configuration",
							new File(work, "configuration").getAbsolutePath(),
							"-testPluginName", testBundle, "-className",
							className });

			CommandLineUtils.executeCommandLine(cli, new StreamConsumer() {
				public void consumeLine(String line) {
					System.out.println("Test.Out>" + line);
				}
			}, new StreamConsumer() {
				public void consumeLine(String line) {
					System.err.println("Test.Err>" + line);
				}
			});
		} catch (CommandLineException e) {
			throw new MojoExecutionException("Error while executing platform",
					e);
		}
	}

	private Artifact getPluginArtifact(String groupId, String artifactId)
			throws MojoExecutionException {
		for (Iterator iter = pluginArtifacts.iterator(); iter.hasNext();) {
			Artifact artifact = (Artifact) iter.next();
			if (artifact.getGroupId().equals(groupId)
					&& (artifact.getArtifactId().equals(artifactId))) {
				return artifact;
			}
		}
		throw new MojoExecutionException("Could not find artifact " + groupId
				+ ":" + artifactId);
	}

	private void copyConfigIni() throws MojoExecutionException {
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(configIni);
			Properties p = new Properties();
			p.load(fis);
			fis.close();

			String osgiBundles = p.getProperty("osgi.bundles");
			String newOsgiBundles = createOsgiBundlesProperty(osgiBundles);
			p.setProperty("osgi.bundles", newOsgiBundles);

			Artifact systemArtifact = (Artifact) project.getArtifactMap().get(
					"org.eclipse:org.eclipse.osgi");

			if (systemArtifact == null) {
				systemArtifact = getPluginArtifact("org.eclipse",
						"org.eclipse.osgi");
			}
			p.setProperty("osgi.framework", systemArtifact.getFile().toURI()
					.toURL().toString());

			new File(work, "configuration").mkdir();
			fos = new FileOutputStream(new File(work,
					"configuration/config.ini"));
			p.store(fos, null);
		} catch (FileNotFoundException e) {
			throw new MojoExecutionException("", e);
		} catch (IOException e) {
			throw new MojoExecutionException("", e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
				}
			}
		}

	}

	private Artifact getTestArtifact() {
		List attachedArtifacts = project.getAttachedArtifacts();
		for (Iterator iter = attachedArtifacts.iterator(); iter.hasNext();) {
			Artifact artifact = (Artifact) iter.next();
			if ("tests".equals(artifact.getClassifier())) {
				return artifact;
			}
		}
		return null;
	}

	private String createOsgiBundlesProperty(String osgiBundles)
			throws MojoExecutionException {
		String[] s = osgiBundles.split(",");
		for (int i = 0; i < s.length; i++) {
			s[i] = s[i].trim();
		}
		StringBuffer result = new StringBuffer();
		appendAbsolutePath(result, projectArtifact.getFile());
		result.append(",");
		Artifact testArtifact = getTestArtifact();
		if (testArtifact != null) {
			appendAbsolutePath(result, testArtifact.getFile());
			result.append(",");
		}
		appendAbsolutePath(result, getPluginArtifact("org.codehaus.tycho", "org.codehaus.tycho.junit4.runner").getFile());
		result.append(",");
		result.append(getPluginArtifact("org.apache.ant", "org.apache.ant")
				.getFile().getAbsolutePath());
		for (Iterator iter = runtimeArtifacts.iterator(); iter.hasNext();) {
			Artifact bundle = (Artifact) iter.next();
			File file = bundle.getFile();
			if (!file.exists()) {
				throw new MojoExecutionException("File " + file.getAbsolutePath() + "  does not exist");
			}
			OsgiStateController state = new OsgiStateController(outputDir);
			BundleFile bundleFile = new BundleFile(state.loadManifest(file), file);
			String symbolicName = bundleFile.getSymbolicName();
			if (symbolicName == null || symbolicName.equals("org.eclipse.osgi")) {
				continue;
			}
			if (result.length() > 0) {
				result.append(",");
			}
			appendAbsolutePath(result, file);
			for (int i = 0; i < s.length; i++) {
				if (s[i].startsWith(symbolicName + "@")) {
					String startLevel = s[i].substring(symbolicName.length());
					result.append(startLevel);
					break;
				}
			}
		}
		return result.toString();
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
