package org.sonatype.tycho.test.mngeclipse1105;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class PackageRootFilesTest
    extends AbstractTychoIntegrationTest
{
    @Test
    public void test()
        throws Exception
    {
        Verifier verifier = getVerifier( "MNGECLIPSE1105rootfiles" );

        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();

        File licenseFile = new File( verifier.getBasedir(), "target/product/eclipse/license.txt" );
        Assert.assertTrue( licenseFile.exists() );
    }

}
