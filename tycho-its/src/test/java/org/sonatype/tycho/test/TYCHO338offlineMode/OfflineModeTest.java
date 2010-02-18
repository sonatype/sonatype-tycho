package org.sonatype.tycho.test.TYCHO338offlineMode;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.codehaus.tycho.model.Target;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;
import org.sonatype.tycho.test.util.HttpServer;

public class OfflineModeTest
    extends AbstractTychoIntegrationTest
{

    private HttpServer server;

    @Before
    public void startServer()
        throws Exception
    {
        server = HttpServer.startServer();
    }

    @After
    public void stopServer()
        throws Exception
    {
        server.stop();
    }

    @Test
    @SuppressWarnings( "unchecked" )
    public void test()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO338offlineMode", false );
        String url = server.addServer( "test", new File( verifier.getBasedir(), "repo" ) );
        verifier.getSystemProperties().setProperty( "p2.repo", url );

        File platformFile = new File( verifier.getBasedir(), "platform.target" );
        Target platform = Target.read( platformFile );
        platform.getLocations().get( 0 ).setRepositoryLocation( url );
        Target.write( platform, platformFile );

        new File( verifier.localRepo, ".meta/p2-metadata.properties" ).delete();

        verifier.setLogFileName( "log-online.txt" );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
        List<String> urls = server.getAccessedUrls( "test" );
        assertFalse( urls.isEmpty() );
        urls.clear();

        verifier.getCliOptions().add( "--offline" );
        verifier.setLogFileName( "log-offline.txt" );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
        urls = server.getAccessedUrls( "test" );
        assertTrue( urls.toString(), urls.isEmpty() );
    }

}
