package org.sonatype.tycho.test.tycho136;

import java.io.File;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class Tycho136GenerateIndividualSourceBundlesTest extends AbstractTychoIntegrationTest {

	@Test
	public void projectC() throws Exception {
		Verifier verifier = getVerifier("tycho136/projectC");

		verifier.executeGoal("install");
		verifier.verifyErrorFreeLog();
		
		File basedir = new File(verifier.getBasedir());
		File sourceFeature = new File(basedir, "SiteC/target/site/features/FeatureC.source_1.0.0.jar");
		Assert.assertTrue("Site should generate FeatureC.source", sourceFeature.exists());
		File sourcePluginC = new File(basedir, "SiteC/target/site/plugins/PluginC.source_1.0.0.jar");
		Assert.assertTrue("Site should generate PluginC.source", sourcePluginC.exists());
		File sourcePluginCExtra = new File(basedir, "SiteC/target/site/plugins/PluginC.Extra.source_1.0.0.jar");
		Assert.assertTrue("Site should generate PluginC.Extra.source", sourcePluginCExtra.exists());
	}
}
