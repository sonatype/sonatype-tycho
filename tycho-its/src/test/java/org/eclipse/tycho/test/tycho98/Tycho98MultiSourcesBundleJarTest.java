package org.eclipse.tycho.test.tycho98;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class Tycho98MultiSourcesBundleJarTest extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("tycho98");

		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();
	}
}
