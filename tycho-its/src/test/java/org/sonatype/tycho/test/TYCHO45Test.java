package org.sonatype.tycho.test;

import org.apache.maven.it.Verifier;
import org.junit.Test;

public class TYCHO45Test extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
        Verifier verifier = getVerifier("TYCHO45");

        // generate poms
        verifier.getCliOptions().add("-DgroupId=tycho45");
        verifier.getCliOptions().add("-DtestSuite=tests.suite");
        verifier.setAutoclean( false );
        verifier.executeGoal( "org.codehaus.tycho:maven-tycho-plugin:generate-poms" );
        verifier.verifyErrorFreeLog();

        // run the build
        verifier.getCliOptions().add("-DtestClass=tests.suite.AllTests");
        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
	}
}
