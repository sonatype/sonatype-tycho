package org.sonatype.tycho.test.TYCHO365exportProductWithStartLevel;

import java.io.File;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

import de.schlichtherle.io.FileInputStream;

public class TYCHO365exportProductWithStartLevel extends AbstractTychoIntegrationTest {


	@Test
	public void exportPluginRcpApplication() throws Exception {
		Verifier verifier = getVerifier("/TYCHO365exportProductWithStartLevel/plugin-rcp");
		verifier.getCliOptions().add("-Dosgi.os=macosx -Dosgi.ws=cocoa -Dosgi.arch=x86_64");
		
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		
		File configFile = new File(verifier.getBasedir(), "HeadlessProduct/target/linux.gtk.x86_64/eclipse/configuration/config.ini");
		Assert.assertTrue(configFile.canRead());
		Properties configIni = new Properties();
		FileInputStream fis = new FileInputStream(configFile);
		configIni.load(fis);
		fis.close();
		
		String osgiBundles = configIni.getProperty("osgi.bundles");
		Assert.assertNotNull(osgiBundles);
		Assert.assertTrue(osgiBundles.contains("org.eclipse.core.contenttype@7:start"));
		Assert.assertTrue(osgiBundles.contains("org.eclipse.equinox.preferences@6"));
		Assert.assertTrue(osgiBundles.contains("org.eclipse.core.runtime"));
	}

	@Test
	public void exportFeatureRCPApplication() throws Exception {
		Verifier verifier = getVerifier("/TYCHO365exportProductWithStartLevel/feature-rcp");
		verifier.getCliOptions().add("-Dosgi.os=macosx -Dosgi.ws=cocoa -Dosgi.arch=x86_64");
		
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		
		File configFile = new File(verifier.getBasedir(), "HeadlessProduct/target/linux.gtk.x86_64/eclipse/configuration/config.ini");
		Assert.assertTrue(configFile.canRead());
		Properties configIni = new Properties();
		FileInputStream fis = new FileInputStream(configFile);
		configIni.load(fis);
		fis.close();
		
		String osgiBundles = configIni.getProperty("osgi.bundles");
		Assert.assertNotNull(osgiBundles);
		Assert.assertTrue(osgiBundles.contains("org.eclipse.core.contenttype@7:start"));
		Assert.assertTrue(osgiBundles.contains("org.eclipse.equinox.preferences@6"));
		Assert.assertFalse(osgiBundles.contains("org.eclipse.core.runtime"));
	}
}
