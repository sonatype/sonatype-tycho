package org.sonatype.tycho.test.tycho136;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class Tycho136SourceMojoTest extends AbstractTychoIntegrationTest {

	@Test
	public void projectA() throws Exception {
		Verifier verifier = getVerifier("tycho136/projectA");

		verifier.executeGoal("install");
		verifier.verifyErrorFreeLog();
	}
}
