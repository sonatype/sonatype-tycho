package org.eclipse.tycho.test.TYCHO0290appArgs;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class AppArgsTest
    extends AbstractTychoIntegrationTest
{
    @Test
    public void exportProduct()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO0290appArgs" );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
    }
}
