package org.sonatype.tycho.test.tycho136;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class Tycho136GeneratePluginTest extends AbstractTychoIntegrationTest {

	@Test
	public void projectB() throws Exception {
		Verifier verifier = getVerifier("tycho136/projectB");

		verifier.executeGoal("install");
		verifier.verifyErrorFreeLog();
	}
}
