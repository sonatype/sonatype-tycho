package org.sonatype.tycho.test.mngeclipse949;

import java.io.File;
import java.io.FileFilter;

import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class PackageDirectoryBundleFromTargetPlatformTest extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
        Verifier verifier = getVerifier("MNGECLIPSE949");

        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        File[] sitePlugins = new File(verifier.getBasedir(), "site/target/site/plugins").listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isFile() 
						&& pathname.getName().startsWith("org.eclipse.platform")
						&& pathname.getName().endsWith(".jar");
			}
        });
        Assert.assertEquals(1, sitePlugins.length);

        File[] productPlugins = new File(verifier.getBasedir(), "product/target/product/eclipse/plugins").listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isDirectory() 
						&& pathname.getName().startsWith("org.eclipse.platform");
			}
        });
        Assert.assertEquals(1, productPlugins.length);
	}
	
}
