package org.sonatype.tycho.test.TYCHO0209tychoRepositoryRoundtrip;

import java.io.File;
import java.io.IOException;

import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.tycho.p2.facade.internal.DefaultTychoRepositoryIndex;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class TychoRepositoryRoundtripTest
    extends AbstractTychoIntegrationTest
{

    @Test
    public void testLocalMavenRepository()
        throws Exception
    {
        // build01
        Verifier v01 = getVerifier( "TYCHO0209tychoRepositoryRoundtrip/build01", false );
        v01.getCliOptions().add( "-Dp2.repo=" + toURI( new File( "repositories/e342" ) ) );
        v01.getCliOptions().add( "-Dmaven.test.skip=true" ); // see https://issues.sonatype.org/browse/TYCHO-282
        v01.executeGoal( "install" );
        v01.verifyErrorFreeLog();

        // build02, some dependencies come from loca, some from remote repositories
        Verifier v02 = getVerifier( "TYCHO0209tychoRepositoryRoundtrip/build02", false );
        v02.getCliOptions().add( "-Dp2.repo=" + toURI( new File( "repositories/e342" ) ) );
        v02.executeGoal( "install" );
        v02.verifyErrorFreeLog();
        File site = new File( v02.getBasedir(), "build02.site01/target/site" );
        Assert.assertEquals( 2, new File( site, "features" ).listFiles().length );
        Assert.assertEquals( 3, new File( site, "plugins" ).listFiles().length );

        // build03, all dependencies come from local repository
        Verifier v03 = getVerifier( "TYCHO0209tychoRepositoryRoundtrip/build03", false );
        v03.executeGoal( "install" );
        v03.verifyErrorFreeLog();
    }

    @Test
    public void testRemoteRepository()
        throws Exception
    {
        // cleanup old tycho index
        File localBasedir = new File( getLocalRepo() );
        new File( localBasedir, DefaultTychoRepositoryIndex.INDEX_RELPATH ).delete();

        // install build01
        Verifier v01 = getVerifier( "TYCHO0209tychoRepositoryRoundtrip/build01", false );
        v01.getCliOptions().add( "-Dp2.repo=" + toURI( new File( "repositories/e342" ) ) );
        v01.getCliOptions().add( "-Dmaven.test.skip=true" );
        v01.executeGoal( "install" );
        v01.verifyErrorFreeLog();

        // now lets fake remote repo
        String build01relpath = "org/codehaus/tycho/tychoits/tycho0209/build01";
        File remoteBasedir = new File( "target/remoterepo/" );
        FileUtils.copyDirectory( new File( localBasedir, build01relpath ), new File( remoteBasedir, build01relpath ) );
        FileUtils.copyFile( new File( localBasedir, DefaultTychoRepositoryIndex.INDEX_RELPATH), new File( remoteBasedir, DefaultTychoRepositoryIndex.INDEX_RELPATH ) );

        // cleanup localrepo once again
        new File( localBasedir, DefaultTychoRepositoryIndex.INDEX_RELPATH ).delete();
        FileUtils.deleteDirectory( new File( remoteBasedir, build01relpath ) );

        // build02
        Verifier v02 = getVerifier( "TYCHO0209tychoRepositoryRoundtrip/build02", false );
        v02.getCliOptions().add( "-Dp2.repo=" + toURI( new File( "repositories/e342" ) ) );
        v02.getCliOptions().add( "-Drepo.snapshots=" +  toURI( remoteBasedir ) );
        v02.executeGoal( "install" );
        v02.verifyErrorFreeLog();

        File site = new File( v02.getBasedir(), "build02.site01/target/site" );

        Assert.assertEquals( 2, new File( site, "features" ).listFiles().length );
        Assert.assertEquals( 3, new File( site, "plugins" ).listFiles().length );
    }

    private String toURI( File file ) throws IOException
    {
        return file.getCanonicalFile().toURI().normalize().toString();
    }
}
