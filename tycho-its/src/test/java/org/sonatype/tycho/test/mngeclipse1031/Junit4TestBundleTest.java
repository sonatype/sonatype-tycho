package org.sonatype.tycho.test.mngeclipse1031;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class Junit4TestBundleTest extends AbstractTychoIntegrationTest {
	
	@Test
	public void test() throws Exception {

        Verifier verifier = getVerifier("MNGECLIPSE1026");

        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        // TODO actualy validate generated JAR file
	}

}
