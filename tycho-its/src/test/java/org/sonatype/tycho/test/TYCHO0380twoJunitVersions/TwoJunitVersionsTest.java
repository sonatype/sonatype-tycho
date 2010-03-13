package org.sonatype.tycho.test.TYCHO0380twoJunitVersions;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class TwoJunitVersionsTest
    extends AbstractTychoIntegrationTest
{
    @Test
    public void test()
        throws Exception
    {
        String targetPlatform = new File( "repositories/junit4" ).getCanonicalPath();

        Verifier verifier = getVerifier( "/TYCHO0380twoJunitVersions", false );
        verifier.getCliOptions().add( "-Dtycho.targetPlatform=" + targetPlatform.replace( '\\', '/' ) );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();

        assertFileExists( new File( verifier.getBasedir() ), "target/surefire-reports/some.Test.txt" );
    }
}
