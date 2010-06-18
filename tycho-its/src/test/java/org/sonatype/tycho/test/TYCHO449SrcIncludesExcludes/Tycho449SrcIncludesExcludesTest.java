package org.sonatype.tycho.test.TYCHO449SrcIncludesExcludes;

import java.io.File;
import java.util.jar.JarFile;

import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class Tycho449SrcIncludesExcludesTest
    extends AbstractTychoIntegrationTest
{

    @Test
    public void testDefaultSourceBundleSuffix()
        throws Exception
    {
        Verifier verifier = getVerifier( "/TYCHO449SrcIncludesExcludes", false );
        verifier.executeGoal( "package" );
        verifier.verifyErrorFreeLog();
        JarFile sourceJar =
            new JarFile( new File( verifier.getBasedir(),
                                   "target/TestSourceIncludesExcludes-1.0.0-SNAPSHOT-sources.jar" ) );
        try
        {
            Assert.assertNull( sourceJar.getEntry( "resourceFolder/.hidden/toBeExcluded.txt" ) );
            Assert.assertNull( sourceJar.getEntry( "resourceFolder/.svn/" ) );
            Assert.assertNotNull( sourceJar.getEntry( "resourceFolder/test.txt" ) );
            Assert.assertNotNull( sourceJar.getEntry( "resource.txt" ) );
        }
        finally
        {
            sourceJar.close();
        }
    }

}
