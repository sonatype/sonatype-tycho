package org.eclipse.tycho.test.TYCHO0460testBundleShape;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TestBundleShapeTest
    extends AbstractTychoIntegrationTest
{
    @Test
    public void test()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO0460testBundleShape" );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
    }
}
