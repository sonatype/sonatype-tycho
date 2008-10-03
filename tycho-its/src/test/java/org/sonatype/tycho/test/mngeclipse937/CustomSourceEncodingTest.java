package org.sonatype.tycho.test.mngeclipse937;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class CustomSourceEncodingTest extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
        Verifier verifier = getVerifier("MNGECLIPSE937");
        
        verifier.getCliOptions().add("-Dfile.encoding=US-ASCII");

        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
		
	}
}
