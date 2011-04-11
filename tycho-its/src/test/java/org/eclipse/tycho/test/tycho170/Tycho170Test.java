package org.eclipse.tycho.test.tycho170;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class Tycho170Test extends AbstractTychoIntegrationTest {
	
	@Test
	public void test() throws Exception {
        Verifier verifier = getVerifier("TYCHO170");

        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
		
        File testReport = new File(verifier.getBasedir(), "bundle.tests/target/surefire-reports/TEST-bundle.tests.BundleClassTest.xml");
        Assert.assertTrue(testReport.exists());
	}

}
