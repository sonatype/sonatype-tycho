package org.sonatype.tycho.test.TYCHO338offlineMode;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class OfflineModeTest
    extends AbstractTychoIntegrationTest
{

    @Test
    @SuppressWarnings("unchecked")
    public void test()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO338offlineMode", false );
        new File( verifier.localRepo, ".meta/p2-metadata.properties" ).delete();
        File basedir = new File( verifier.getBasedir() );

        verifier.getSystemProperties().setProperty( "p2.repo", new File( basedir, "repo" ).toURI().toString() );
        verifier.setLogFileName( "log-online.txt" );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();

        verifier.getSystemProperties().setProperty( "p2.repo", new File( basedir, "void" ).toURI().toString() );
        verifier.getCliOptions().add( "--offline" );
        verifier.setLogFileName( "log-offline.txt" );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
    }

}
