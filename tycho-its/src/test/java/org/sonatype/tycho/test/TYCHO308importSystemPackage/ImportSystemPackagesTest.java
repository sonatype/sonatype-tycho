package org.sonatype.tycho.test.TYCHO308importSystemPackage;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;


public class ImportSystemPackagesTest
    extends AbstractTychoIntegrationTest
{
    @Test
    public void testImportSystemPackages()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO308importSystemPackage" );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();

        // TODO is there a good way to test changes to native launcher?
    }

}
