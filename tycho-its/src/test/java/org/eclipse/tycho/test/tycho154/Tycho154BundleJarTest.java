package org.eclipse.tycho.test.tycho154;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class Tycho154BundleJarTest extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("tycho154");

		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();
	}
}
