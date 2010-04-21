package org.codehaus.tycho.buildnumber.test;

import java.io.File;
import java.util.List;
import java.util.zip.ZipFile;

import junit.framework.Assert;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.eclipsepackaging.PackageUpdateSiteMojo;
import org.codehaus.tycho.testing.AbstractTychoMojoTestCase;

public class PackageUpdateSiteMojoTest
    extends AbstractTychoMojoTestCase
{

    private MavenProject project;

    private PackageUpdateSiteMojo packagemojo;

    private File targetFolder;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        File basedir = getBasedir( "projects/updateSitePackaging" );
        File platform = new File( "src/test/resources/eclipse" );

        List<MavenProject> projects = getSortedProjects( basedir, platform );

        project = projects.get( 0 );
        targetFolder = new File( project.getFile().getParent(), "target" );

        // simulate previous update site build.
        // performed by
        // org.sonatype.tycho:maven-osgi-packaging-plugin:${project.version}:update-site,org.sonatype.tycho:tycho-p2-plugin:${project.version}:update-site-p2-metadata,
        File siteFolder = new File( targetFolder, "site" );
        siteFolder.mkdirs();
        new File( siteFolder, "site.xml" ).createNewFile();
        new File( siteFolder, "content.xml" ).createNewFile();
        new File( siteFolder, "artifacts.xml" ).createNewFile();

        packagemojo = (PackageUpdateSiteMojo) lookupMojo( "update-site-packaging", project.getFile() );
        setVariableValueToObject( packagemojo, "project", project );
        setVariableValueToObject( packagemojo, "target", siteFolder );
    }

    public void testArchiveSite()
        throws Exception
    {
        setVariableValueToObject( packagemojo, "archiveSite", Boolean.TRUE );

        packagemojo.execute();

        File resultzip = new File( targetFolder, "site.zip" );
        Assert.assertTrue( resultzip.exists() );
        Assert.assertEquals( project.getArtifact().getFile(), resultzip );

        ZipFile zip = new ZipFile( resultzip );
        try
        {
            assertNotNull( zip.getEntry( "site.xml" ) );
            assertNotNull( zip.getEntry( "content.xml" ) );
        }
        finally
        {
            zip.close();
        }
    }

    public void testNoArchiveSite()
        throws Exception
    {
        // this is the default
        // setVariableValueToObject( packagemojo, "archiveSite", Boolean.FALSE );

        packagemojo.execute();

        File resultzip = new File( targetFolder, "site.zip" );
        Assert.assertTrue( resultzip.exists() );
        Assert.assertEquals( project.getArtifact().getFile(), resultzip );

        ZipFile zip = new ZipFile( resultzip );
        try
        {
            assertNotNull( zip.getEntry( "site.xml" ) );
            assertNull( zip.getEntry( "content.xml" ) );
        }
        finally
        {
            zip.close();
        }
    }

}
