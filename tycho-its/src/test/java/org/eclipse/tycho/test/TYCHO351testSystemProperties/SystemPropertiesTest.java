package org.eclipse.tycho.test.TYCHO351testSystemProperties;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class SystemPropertiesTest
    extends AbstractTychoIntegrationTest
{

    @Test
    public void exportProduct()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO351testSystemProperties" );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
    }

}
