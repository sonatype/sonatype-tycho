package org.sonatype.tycho.test.TYCHO0373autostartDSbundle;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class AutostartBundleTest
    extends AbstractTychoIntegrationTest
{
    @Test
    public void implicitDSAutostart()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO0373autostartDSbundle/implicit/ds.test" );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void explicitBundleStartLevel()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO0373autostartDSbundle/explicit" );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
    }
}
