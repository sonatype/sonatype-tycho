package org.sonatype.tycho.test.tycho136;

import java.io.File;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class Tycho136GeneratePluginTest extends AbstractTychoIntegrationTest {

	@Test
	public void projectB() throws Exception {
		Verifier verifier = getVerifier("tycho136/projectB");

		verifier.executeGoal("install");
		verifier.verifyErrorFreeLog();
		
		File basedir = new File(verifier.getBasedir());
		File sourcePlugin = new File(basedir, "SiteB/target/site/plugins/PluginB.source_1.0.0.jar");
		Assert.assertTrue("Site should generate PluginB.source", sourcePlugin.exists());
	}
}
