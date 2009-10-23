package org.sonatype.tycho.test;

import java.io.File;
import java.io.IOException;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.sonatype.tycho.test.util.EnvironmentUtil;

public abstract class AbstractTychoIntegrationTest {
	
	protected File getBasedir(String test) throws IOException {
		File src = new File("projects", test).getCanonicalFile();
		File dst = new File("target/projects", test).getCanonicalFile();

		if (dst.isDirectory()) {
			FileUtils.deleteDirectory(dst);
		} else if (dst.isFile()) {
			if (!dst.delete()) {
				throw new IOException("Can't delete file " + dst.toString());
			}
		}
	
		FileUtils.copyDirectoryStructure(src, dst);
		
		return dst;
	}

	@SuppressWarnings("unchecked")
	protected Verifier getVerifier(String test, boolean setTargetPlatform) throws Exception {
		/*
		Test JVM can be started in debug mode by passing the following
		env to execute(...) methods.

        java.util.Map<String, String> env = new java.util.HashMap<String, String>();
        env.put("MAVEN_OPTS", "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000");
		 */
		
	    // oddly enough, Verifier uses this system property to locate maven install
        System.setProperty("maven.home", getMavenHome()); 

        File testDir = getBasedir(test);
        
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.getCliOptions().add("-Dmaven.home=" + getMavenHome());
        verifier.getCliOptions().add("-Dtycho-version=" + getTychoVersion());
        if (setTargetPlatform) {
        	verifier.getCliOptions().add( "-Dtycho.targetPlatform=" + getTargetPlatforn() );
        }
        verifier.getCliOptions().add("-X");
        verifier.getCliOptions().add("-s " + new File("settings.xml").getCanonicalPath());
//        verifier.getVerifierProperties().put( "use.mavenRepoLocal", "true" );
//        verifier.setLocalRepo( getLocalRepo() );
        
        String m2eState = System.getProperty("m2eclipse.workspace.state");
        String m2eResolver = System.getProperty("m2eclipse.workspace.resolver");

        if (m2eState != null && m2eResolver != null) {
        	verifier.getVerifierProperties().put("m2eclipse.workspace.state", m2eState);
        }

        return verifier;
		
	}

	protected Verifier getVerifier(String test) throws Exception {
		return getVerifier(test, true);
	}

	protected String getTargetPlatforn() {
		return  EnvironmentUtil.getTargetPlatforn();
	}
	
	protected String getMavenHome() {
		return EnvironmentUtil.getMavenHome();
	}

	protected String getTychoVersion() {
		return EnvironmentUtil.getTychoVersion();
	}

    protected void assertFileExists( File targetdir, String pattern )
    {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir( targetdir );
        ds.setIncludes( new String[] { pattern } );
        ds.scan();
        Assert.assertEquals( 1, ds.getIncludedFiles().length );
        Assert.assertTrue( new File( targetdir, ds.getIncludedFiles()[0] ).canRead() );
    }

    protected void assertFileDoesNotExist( File targetdir, String pattern )
    {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir( targetdir );
        ds.setIncludes( new String[] { pattern } );
        ds.scan();
        Assert.assertEquals( 0, ds.getIncludedFiles().length );
    }
}
