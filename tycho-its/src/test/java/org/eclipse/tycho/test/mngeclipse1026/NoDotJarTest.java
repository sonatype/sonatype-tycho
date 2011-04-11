package org.eclipse.tycho.test.mngeclipse1026;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class NoDotJarTest extends AbstractTychoIntegrationTest {
	
	@Test
	public void test() throws Exception {
        Verifier verifier = getVerifier("MNGECLIPSE1031/bundle.test");

        verifier.executeGoal("integration-test");
        verifier.verifyErrorFreeLog();
		
        File testReport = new File(verifier.getBasedir(), "target/surefire-reports/TEST-bundle.test.BundleTest.xml");
        Assert.assertTrue(testReport.exists());
	}

}
