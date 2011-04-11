package org.eclipse.tycho.test.tycho98;

import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

// TODO kinda hack, need to create separate project structure somehow
public class MNGECLIPSE999DIrectTestPluginInvocationTest extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
		Verifier verifier = getVerifier("tycho98");

		verifier.executeGoals(Arrays.asList(new String[] {"package", "org.eclipse.tycho:maven-osgi-test-plugin:test"}));
		verifier.verifyErrorFreeLog();
	}

}
