package org.sonatype.tycho.test.tycho109;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.cli.CommandLineUtils;
import org.apache.maven.it.util.cli.Commandline;
import org.apache.maven.it.util.cli.StreamConsumer;
import org.apache.maven.it.util.cli.WriterStreamConsumer;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class Tycho109ProductExportTest extends AbstractTychoIntegrationTest {

	private static final String WINDOWS_OS = "windows";

	private static final String MAC_OS = "mac os x";

	private static final String MAC_OS_DARWIN = "darwin";

	private static final String LINUX_OS = "linux";

	@Test
	public void exportPluginProduct() throws Exception {
		Verifier verifier;
		if (isEclipse32Platform()) {
			verifier = getVerifier("/tycho109/eclipse32/plugin-rcp");
		} else {
			verifier = getVerifier("/tycho109/plugin-rcp");
		}

		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		File basedir = new File(verifier.getBasedir());
		File output = new File(basedir, "target/product");

		Assert.assertTrue("Exported product folder not found", output
				.isDirectory());

		File launcher = getLauncher(output, "headless");
		Assert.assertTrue("Launcher not found\n" + launcher, launcher.isFile());
		Assert.assertTrue("config.ini not found", new File(output,
				"configuration/config.ini").isFile());

		File plugins = new File(output, "plugins");
		Assert.assertTrue("Plugins not found", plugins.isDirectory());

		if (isEclipse32Platform()) {
			Assert.assertEquals("No found the expected plugins number", 10,
					plugins.list().length);
		} else {
			Assert.assertEquals("No found the expected plugins number", 12,
					plugins.list().length);
		}

		// launch to be sure
		Commandline cmd = new Commandline();
		cmd.setExecutable(launcher.getAbsolutePath());

		StringWriter logWriter = new StringWriter();
		StreamConsumer out = new WriterStreamConsumer(logWriter);
		StreamConsumer err = new WriterStreamConsumer(logWriter);
		CommandLineUtils.executeCommandLine(cmd, out, err);
		Assert.assertTrue("Didn't get a controled exit\n"
				+ logWriter.toString(), logWriter.toString().startsWith(
				"Headless application OK!"));
	}

	@Test
	public void exportFeatureProduct() throws Exception {
		Verifier verifier;
		if (isEclipse32Platform()) {
			verifier = getVerifier("/tycho109/eclipse32/feature-rcp");
		} else {
			verifier = getVerifier("/tycho109/feature-rcp");
		}

		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		File basedir = new File(verifier.getBasedir());
		File output = new File(basedir, "HeadlessFeature/target/product");

		Assert.assertTrue("Exported product folder not found\n"
				+ output.getAbsolutePath(), output.isDirectory());
		File launcher = getLauncher(output, null);
		Assert.assertTrue("Launcher not found\n" + launcher, launcher.isFile());
		Assert.assertTrue("config.ini not found", new File(output,
				"configuration/config.ini").isFile());

		File plugins = new File(output, "plugins");
		Assert.assertTrue("Plugins folder not found", plugins.isDirectory());
		// On linux the number is not same, can't rely on that
		/*
		 * Assert.assertTrue("No found the expected plugins number", 324,
		 * plugins.list().length);
		 */

		File features = new File(output, "features");
		Assert.assertTrue("Features folder not found", features.isDirectory());
		// On linux the number is not same, can't rely on that
		/*
		 * Assert.assertEquals("No found the expected features number", 18,
		 * features.list().length);
		 */

		// launch to be sure
		Commandline cmd = new Commandline();
		cmd.setExecutable(launcher.getAbsolutePath());

		StringWriter logWriter = new StringWriter();
		StreamConsumer out = new WriterStreamConsumer(logWriter);
		StreamConsumer err = new WriterStreamConsumer(logWriter);
		CommandLineUtils.executeCommandLine(cmd, out, err);
		Assert.assertTrue("Didn't get a controled exit\n"
				+ logWriter.toString(), logWriter.toString().startsWith(
				"Headless application OK!"));
	}

	private File getLauncher(File output, String expectedName) {
		if (expectedName == null) {
			expectedName = "launcher";
		}
		String os = System.getProperty("os.name").toLowerCase();
		if (os.startsWith(WINDOWS_OS)) {
			return new File(output, expectedName + ".exe");
		} else if (os.startsWith(LINUX_OS)) {
			return new File(output, expectedName);
		} else if (os.startsWith(MAC_OS) || os.startsWith(MAC_OS_DARWIN)) {
			return new File(output, "Eclipse.app/Contents/MacOS/"
					+ expectedName);
		} else {
			Assert.fail("Unable to determine launcher to current OS: " + os);
			return null;
		}
	}

	private boolean isEclipse32Platform() throws IOException {
		return new File(getTargetPlatforn(), "startup.jar").exists();
	}

}
