package org.codehaus.tycho.buildnumber.test;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.SilentLog;
import org.codehaus.tycho.buildversion.ValidateVersionMojo;
import org.codehaus.tycho.testing.AbstractTychoMojoTestCase;

public class ValidateVersionTest
    extends AbstractTychoMojoTestCase
{
    
    private ValidateVersionMojo mojo;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        mojo = new ValidateVersionMojo();
        mojo.setLog( new SilentLog() );
    }

    public void testValidateVersion()
        throws MojoExecutionException
    {

        mojo.validateReleaseVersion( "1.2.3", "1.2.3" );
        mojo.validateSnapshotVersion( "1.2.3-SNAPSHOT", "1.2.3.qualifier" );

        assertInvalidSnapshotVersion( "1.2.3-SNAPSHOT", "1.2.3" );
        assertInvalidSnapshotVersion( "1.2.3-SNAPSHOT", "1.2.0.qualifier" );
    }

    private void assertInvalidSnapshotVersion( String maven, String osgi )
    {
        try
        {
            mojo.validateSnapshotVersion( maven, osgi );
            fail();
        }
        catch ( MojoExecutionException e )
        {
            // expected
        }
    }
}
