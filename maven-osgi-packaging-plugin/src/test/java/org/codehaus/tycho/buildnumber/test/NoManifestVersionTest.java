package org.codehaus.tycho.buildnumber.test;

import java.io.File;

import org.apache.maven.Maven;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.tycho.eclipsepackaging.PackagePluginMojo;
import org.codehaus.tycho.testing.AbstractTychoMojoTestCase;

public class NoManifestVersionTest
    extends AbstractTychoMojoTestCase
{

    private Maven maven;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        maven = lookup( Maven.class );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        maven = null;
        super.tearDown();
    }

    public void test()
        throws Exception
    {
        File basedir = getBasedir( "projects/noManifestVersion" );
        File pom = new File( basedir, "pom.xml" );

        MavenExecutionRequest request = newMavenExecutionRequest( pom );
        MavenExecutionResult result = maven.execute( request );

        PackagePluginMojo mojo = (PackagePluginMojo) lookupMojo( "package-plugin", pom );
    }
}
