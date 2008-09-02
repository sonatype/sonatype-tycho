package org.sonatype.tycho.test.tycho122;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Test;
import org.sonatype.tycho.test.utils.TestProperties;

public class Tycho122GeneratePomFileTest
{

    private static final String tychoDir;

    private static final String eclipseDir;

    private static final String localRepo;

    static
    {
        tychoDir = TestProperties.getString( "tycho-dir" );
        eclipseDir = TestProperties.getString( "eclipse-dir" );
        localRepo = TestProperties.getString( "local-repo" );
    }

    @Test
    public void generatePom()
        throws IOException, VerificationException
    {
        System.setProperty( "maven.home", tychoDir );

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/tycho122/tycho.demo" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.getCliOptions().add( "-Dtycho.targetPlatform=" + eclipseDir );
        verifier.getVerifierProperties().put( "use.mavenRepoLocal", "true" );
        verifier.setLocalRepo( localRepo );

        verifier.setAutoclean( false );
        verifier.executeGoal( "org.codehaus.tycho:maven-tycho-plugin:generate-poms" );

        File pom = new File( testDir, "pom.xml" );
        Assert.assertTrue( "Must generate the pom.xml", pom.exists() );
    }

}
