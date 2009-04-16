package org.sonatype.tycho.test;

import java.io.File;
import java.io.IOException;

import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.FileUtils;
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
        System.setProperty("maven.home", getTychoHome()); 

        File testDir = getBasedir(test);
        
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.getCliOptions().add("-Dmaven.home=" + getTychoHome());
        if (setTargetPlatform) {
        	verifier.getCliOptions().add( "-Dtycho.targetPlatform=" + getTargetPlatforn() );
        }
        verifier.getCliOptions().add("-X");
        verifier.getVerifierProperties().put( "use.mavenRepoLocal", "true" );
        verifier.setLocalRepo( getLocalRepo() );
        
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

	protected String getLocalRepo() {
		return EnvironmentUtil.getLocalRepo();
	}

	protected String getTargetPlatforn() {
		return  EnvironmentUtil.getTargetPlatforn();
	}
	
	protected String getTychoHome() {
		return EnvironmentUtil.getTychoHome();
	}
}
