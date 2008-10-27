package org.sonatype.tycho.test.tycho98;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class MNGECLIPSE999TestDIrectTestPluginInvocation extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("tycho98");

		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();

		verifier.executeGoal("org.codehaus.tycho:maven-osgi-test-plugin:test");
	}

}
