package org.sonatype.tycho.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.FileUtils;

public abstract class AbstractTychoIntegrationTest {
	
	private static Properties props; 
	
	private static synchronized String getProperty(String key) throws IOException {
		if (props == null) {
			props = new Properties();
			ClassLoader cl = AbstractTychoIntegrationTest.class.getClassLoader();
			InputStream is = cl.getResourceAsStream("baseTest.properties");
			if (is != null) {
				try {
					props.load(is);
				} finally {
					is.close();
				}
			}
		}
		return props.getProperty(key);
	}
	
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

	protected Verifier getVerifier(String test) throws Exception {
        System.setProperty("maven.home", getTychoHome()); // XXX do I really need this?

        File testDir = getBasedir(test);

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.getCliOptions().add( "-Dtycho.targetPlatform=" + getTargetPlatforn() );
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

	protected String getLocalRepo() throws IOException {
		return getProperty("local-repo");
	}

	protected String getTargetPlatforn() throws IOException {
		return getProperty("eclipse-dir");
	}
	
	protected String getTychoHome() throws IOException {
		return getProperty("tycho-dir");
	}
}
