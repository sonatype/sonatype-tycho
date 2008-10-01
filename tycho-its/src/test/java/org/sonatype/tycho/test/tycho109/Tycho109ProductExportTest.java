package org.sonatype.tycho.test.tycho109;

import java.io.File;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class Tycho109ProductExportTest extends AbstractTychoIntegrationTest {

	private static final String WINDOWS_OS = "windows";

	private static final String MAC_OS = "mac os x";

	private static final String MAC_OS_DARWIN = "darwin";

	private static final String LINUX_OS = "linux";

	@Test
	public void exportPluginProduct() throws Exception {
		Verifier verifier = getVerifier("/tycho109/plugin-rcp/MyFirstRCP");
		verifier.setAutoclean(false);

		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		File basedir = new File(verifier.getBasedir());
		File output = new File(basedir, "target/product");

		Assert.assertTrue("Exported product folder not found", output
				.isDirectory());
		Assert.assertTrue("Launcher not found", getLauncher(output).isFile());
		Assert.assertTrue("config.ini not found", new File(output,
				"configuration/config.ini").isFile());
		File plugins = new File(output, "plugins");
		Assert.assertTrue("Plugins not found", plugins.isDirectory());
		Assert.assertEquals("No found the expected plugins number", 26, plugins
				.list().length);

	}

	private File getLauncher(File output) {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.startsWith(WINDOWS_OS)) {
			return new File(output, "launcher.exe");
		} else if (os.startsWith(LINUX_OS)) {
			return new File(output, "launcher");
		} else if (os.startsWith(MAC_OS) || os.startsWith(MAC_OS_DARWIN)) {
			return new File(output, "Eclipse.app/Contents/MacOS/launcher");
		} else {
			Assert.fail("Unable to determine launcher to current OS: " + os);
			return null;
		}
	}

}
