package org.sonatype.tycho.test.TYCHO0460testBundleShape;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

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
