package org.eclipse.tycho.test.TYCHO353frameworkExtensions;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

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
