package org.sonatype.tycho.test;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.Test;

public class MNGECLIPSE902Test extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
        Verifier verifier = getVerifier("MNGECLIPSE902");

        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
		
        File testReport = new File(verifier.getBasedir(), "p1.test/target/surefire-reports/TEST-p1.test.ATest.xml");
        Assert.assertTrue(testReport.exists());
	}
}
