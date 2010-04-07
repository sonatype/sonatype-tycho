package org.sonatype.tycho.test.TYCHO0404pomDependencyConsiderExtraClasspath;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class PomDependencyConsiderExtraClasspathTest
    extends AbstractTychoIntegrationTest
{
    @Test
    public void test()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO0404pomDependencyConsiderExtraClasspath", false );
        FileUtils.deleteDirectory( new File( verifier.localRepo, "TYCHO0404pomDependencyConsiderExtraClasspath") );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
    }

}
