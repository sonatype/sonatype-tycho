package org.sonatype.tycho.test.TYCHO321deployableFeature;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class DeployableFeatureTest
    extends AbstractTychoIntegrationTest
{

    @Test
    public void testDeployableFeature()
        throws Exception
    {
        Verifier v01 = getVerifier( "TYCHO321deployableFeature" );
        v01.executeGoal( "install" );
        v01.verifyErrorFreeLog();

        File site = new File( v01.getBasedir(), "target/site" );
        Assert.assertTrue( site.isDirectory() );

        Assert.assertTrue( new File( site, "features" ).list().length > 0 );
        Assert.assertTrue( new File( site, "plugins" ).list().length > 0 );

        Assert.assertTrue( new File( site, "artifacts.xml" ).isFile() );
        Assert.assertTrue( new File( site, "content.xml" ).isFile() );
    }

}
