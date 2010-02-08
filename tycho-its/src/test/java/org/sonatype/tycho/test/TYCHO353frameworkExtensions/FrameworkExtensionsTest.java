package org.sonatype.tycho.test.TYCHO353frameworkExtensions;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class FrameworkExtensionsTest
    extends AbstractTychoIntegrationTest
{

    @Test
    public void testFrameworkExtensions()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO353frameworkExtensions" );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
    }

}
