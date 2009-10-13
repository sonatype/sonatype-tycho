package org.sonatype.tycho.test.TYCHO0290appArgs;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

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
