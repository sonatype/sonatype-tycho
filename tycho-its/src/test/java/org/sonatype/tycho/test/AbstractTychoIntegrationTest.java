package org.sonatype.tycho.test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Assert;
import org.osgi.framework.Version;
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

    protected Verifier getVerifier(String test, boolean setTargetPlatform) throws Exception {
	    return getVerifier( test, setTargetPlatform, new File("settings.xml") );
	}

    @SuppressWarnings("unchecked")
	protected Verifier getVerifier(String test, boolean setTargetPlatform, File userSettings) throws Exception {
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
        verifier.getCliOptions().add("-s " + userSettings.getCanonicalPath());
        verifier.getVerifierProperties().put( "use.mavenRepoLocal", "true" );
        verifier.setLocalRepo( EnvironmentUtil.getLocalRepo() );

        // tell test maven to update snapshots. this is useful when ITs and main build
        // run on separate machines.
        if ( Boolean.getBoolean( "it.-U" ) )
        {
            verifier.getCliOptions().add("-U");
        }

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
        Assert.assertEquals( targetdir.getAbsolutePath() + "/" + pattern, 1, ds.getIncludedFiles().length );
        Assert.assertTrue( targetdir.getAbsolutePath() + "/" + pattern, new File( targetdir, ds.getIncludedFiles()[0] ).canRead() );
    }

    protected void assertDirectoryExists( File targetdir, String pattern )
    {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir( targetdir );
        ds.setIncludes( new String[] { pattern } );
        ds.scan();
        Assert.assertEquals( targetdir.getAbsolutePath() + "/" + pattern, 1, ds.getIncludedDirectories().length );
        Assert.assertTrue( targetdir.getAbsolutePath() + "/" + pattern, new File( targetdir, ds.getIncludedDirectories()[0] ).exists() );
    }

    protected void assertFileDoesNotExist( File targetdir, String pattern )
    {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir( targetdir );
        ds.setIncludes( new String[] { pattern } );
        ds.scan();
        Assert.assertEquals( targetdir.getAbsolutePath() + "/" + pattern, 0, ds.getIncludedFiles().length );
    }

    protected String toURI( File file ) throws IOException
    {
        return file.getCanonicalFile().toURI().normalize().toString();
    }


    protected void writeStringToFile( File iniFile, String string )
        throws IOException
    {
        OutputStream os = new BufferedOutputStream( new FileOutputStream( iniFile ) );
        try
        {
            IOUtil.copy( string, os );
        }
        finally
        {
            IOUtil.close( os );
        }
    }

    protected StringBuffer readFileToString( File iniFile )
        throws IOException
    {
        InputStream is = new BufferedInputStream( new FileInputStream( iniFile ) );
        try
        {
            StringWriter buffer = new StringWriter();

            IOUtil.copy( is, buffer, "UTF-8" );

            return buffer.getBuffer();
        }
        finally
        {
            IOUtil.close( is );
        }
    }
    
    /**
     * Returns approximate target platform version.
     */
    public static Version getEclipseVersion()
    {
        String location = EnvironmentUtil.getTargetPlatforn();

        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir( new File( location, "plugins" ) );
        ds.setIncludes( new String[] { "org.eclipse.osgi_*.jar" } );
        ds.scan();

        String[] files = ds.getIncludedFiles();
        if ( files == null || files.length < 1 )
        {
            throw new IllegalStateException( "Unable to determine version of the test target platform " + location );
        }

        String version = files[0].substring( "org.eclipse.osgi_".length(), files[0].length() - ".jar".length() );

        return Version.parseVersion( version );
    }
    
}
