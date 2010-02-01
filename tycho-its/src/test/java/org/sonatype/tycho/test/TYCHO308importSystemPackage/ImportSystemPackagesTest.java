package org.sonatype.tycho.test.TYCHO308importSystemPackage;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class ImportSystemPackagesTest
    extends AbstractTychoIntegrationTest
{
    @Test
    public void testLocalInstall()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO308importSystemPackage/local_install", false );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testP2Repository()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO308importSystemPackage/p2_repository", false );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
    }
}
