package org.sonatype.tycho.test.mngeclipse1007;

import java.io.File;
import java.util.zip.ZipFile;

import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class BinExcludedTest extends AbstractTychoIntegrationTest {
	
	@Test
	public void test() throws Exception {
        Verifier verifier = getVerifier("MNGECLIPSE1007");

        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();
		
        ZipFile zip = new ZipFile(new File(verifier.getBasedir(), "target/MNGECLIPSE1007-1.0.0.jar"));
        
        try {
        	Assert.assertNotNull(zip.getEntry("files/included.txt"));
        	Assert.assertNull(zip.getEntry("files/excluded.txt"));
        } finally {
        	zip.close();
        }
	}

}
