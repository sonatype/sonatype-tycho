package org.sonatype.tycho.test.TYCHO0356runSingleTest;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class RunSingleTestTest
    extends AbstractTychoIntegrationTest
{
    @Test
    public void test()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO0356runSingleTest" );
        verifier.getCliOptions().add("-Dtest=bundle.WorkingTest");
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
    }
}
