package org.eclipse.tycho.test.tycho001;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class P2MetadataGenerationTest extends AbstractTychoIntegrationTest {

	@Test
	public void test() throws Exception {
        Verifier verifier = getVerifier("tycho001");

        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        File site = new File(verifier.getBasedir(), "site/target/site");
        Assert.assertTrue(new File(site, "artifacts.xml").canRead());
        Assert.assertTrue(new File(site, "content.xml").canRead());

	}

}
