package org.sonatype.tycho.test.TYCHO309pomDependencyConsider;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class PomDependencyConsiderTest
    extends AbstractTychoIntegrationTest
{
    @Test
    public void testPomDependenciesConsider()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO309pomDependencyConsider/artifact" );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();

        verifier = getVerifier( "/TYCHO309pomDependencyConsider", false );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();

        File basedir = new File( verifier.getBasedir() );

        Assert.assertTrue( new File( basedir,
                                     "site/target/site/plugins/TYCHO309pomDependencyConsider.artifact_0.0.1.SNAPSHOT.jar" ).canRead() );
    }

}
