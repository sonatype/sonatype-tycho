package org.sonatype.tycho.test.tycho154;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class Tycho154BundleJarTest extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("tycho154");

		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();
	}
}
