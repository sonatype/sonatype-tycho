package org.sonatype.tycho.test.TYCHO192sourceBundles;

import java.io.File;
import java.io.FileFilter;
import java.util.jar.JarFile;

import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class Tycho192SourceBundleTest extends AbstractTychoIntegrationTest {
	
	@Test
	public void testDefaultSourceBundleSuffix() throws Exception {
		Verifier verifier = getVerifier("/TYCHO192sourceBundles", false);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		File[] sourceJars = new File(verifier.getBasedir(),
				"helloworld.updatesite/target/site/plugins")
				.listFiles(new FileFilter() {

					public boolean accept(File pathname) {
						return pathname.isFile()
								&& pathname.getName().startsWith(
										"helloworld.source_");
					}
				});
		Assert.assertEquals(1, sourceJars.length);
		JarFile sourceJar = new JarFile(sourceJars[0]);
		try {
			Assert.assertNotNull(sourceJar
					.getEntry("helloworld/MessageProvider.java"));
		} finally {
			sourceJar.close();
		}
	}

}
