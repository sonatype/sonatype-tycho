package org.sonatype.tycho.test.tycho98;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class Tycho98MultiSourcesBundleJarTest extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("tycho98");

		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();
	}
}
