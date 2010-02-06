package org.sonatype.tycho.test.TYCHO351testSystemProperties;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

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
