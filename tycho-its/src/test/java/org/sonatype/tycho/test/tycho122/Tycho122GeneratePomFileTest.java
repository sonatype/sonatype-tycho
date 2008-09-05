package org.sonatype.tycho.test.tycho122;

import java.io.File;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class Tycho122GeneratePomFileTest extends AbstractTychoIntegrationTest
{
    @Test
    public void generatePom() throws Exception {
        Verifier verifier = getVerifier("/tycho122/tycho.demo");

        verifier.setAutoclean( false );
        verifier.executeGoal( "org.codehaus.tycho:maven-tycho-plugin:generate-poms" );
        verifier.verifyErrorFreeLog();
        
        File pom = new File( verifier.getBasedir(), "pom.xml" );
        Assert.assertTrue( "Must generate the pom.xml", pom.exists() );
    }

}
