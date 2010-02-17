package org.sonatype.tycho.test.TYCHO357networkFailureTolerance;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class NetworkFailureToleranceTest
    extends AbstractTychoIntegrationTest
{

    @Test
    public void test()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO357networkFailureTolerance", false );
        verifier.getSystemProperties().setProperty( "p2.repo", new File( "repositories/e342" ).toURI().toString() );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
    }

}
