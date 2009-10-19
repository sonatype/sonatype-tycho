package org.sonatype.tycho.test.tycho32;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class TYCHO32Test extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
        Verifier verifier = getVerifier("TYCHO32");

        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
		
        File testReport = new File(verifier.getBasedir(), "bundle.tests/target/surefire-reports/TEST-bundle.tests.SystemPropertyTest.xml");
        Assert.assertTrue(testReport.exists());
	}
}
