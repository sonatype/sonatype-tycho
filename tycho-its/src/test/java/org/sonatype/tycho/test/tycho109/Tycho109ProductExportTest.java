package org.sonatype.tycho.test.tycho109;

import static org.sonatype.tycho.test.util.EnvironmentUtil.isEclipse32Platform;
import static org.sonatype.tycho.test.util.EnvironmentUtil.isLinux;
import static org.sonatype.tycho.test.util.EnvironmentUtil.isMac;
import static org.sonatype.tycho.test.util.EnvironmentUtil.isWindows;

import java.io.File;
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
		File output = new File(basedir, "HeadlessProduct/target/product");

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
		
		//MNGECLIPSE-974
		File headlessPlugin = new File(plugins, "HeadlessPlugin_1.0.0");
		Assert.assertTrue("Plugin should be unpacked", headlessPlugin.isDirectory());

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
		Assert.assertTrue("Didn't get a controlled exit\n"
				+ logWriter.toString(), logWriter.toString().startsWith(
				"Headless application OK!"));
	}

	private File getLauncher(File output, String expectedName) {
		if (expectedName == null) {
			expectedName = "launcher";
		}
		if (isWindows()) {
			return new File(output, expectedName + ".exe");
		} else if (isLinux()) {
			return new File(output, expectedName);
		} else if (isMac()) {
			return new File(output, "Eclipse.app/Contents/MacOS/"
					+ expectedName);
		} else {
			Assert.fail("Unable to determine launcher to current OS");
			return null;
		}
	}

	@Test
	public void exportPluginRcpApplication() throws Exception {
		if (isEclipse32Platform()) {
			// regression test for TYCHO-199, no need to verify on e32 
			return;
		}

		Verifier verifier = getVerifier("/tycho109/plugin-rcp-app");

		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}
}
