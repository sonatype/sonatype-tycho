package org.sonatype.tycho.test.tycho136;

import java.io.File;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class Tycho136GenerateFeatureTest extends AbstractTychoIntegrationTest {

	@Test
	public void projectA() throws Exception {
		Verifier verifier = getVerifier("tycho136/projectA");

		verifier.executeGoal("install");
		verifier.verifyErrorFreeLog();

		File basedir = new File(verifier.getBasedir());
		File featureSource = new File(basedir, "SiteA/target/site/features/FeatureA.source_1.0.0.jar");
		Assert.assertTrue("Site should generate FeatureA.source", featureSource.exists());

		File featurePlugin = new File(basedir, "SiteA/target/site/plugins/FeatureA.source_1.0.0.jar");
		Assert.assertTrue("Site should generate FeatureA.source Plugin", featurePlugin.exists());
	}
}
