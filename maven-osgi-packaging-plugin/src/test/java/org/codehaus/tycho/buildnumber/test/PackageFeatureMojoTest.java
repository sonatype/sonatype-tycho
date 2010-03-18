package org.codehaus.tycho.buildnumber.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.Maven;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.eclipsepackaging.PackageFeatureMojo;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.testing.AbstractTychoMojoTestCase;

public class PackageFeatureMojoTest
    extends AbstractTychoMojoTestCase
{

    protected Maven maven;

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

    public void testFeatureXmlGeneration()
        throws Exception
    {
        File basedir = getBasedir( "projects/featureXmlGeneration" );
        File platform = new File( "src/test/resources/projects/featureXmlGeneration/eclipse" );
        List<MavenProject> projects = getSortedProjects( basedir, platform );

        MavenProject project = projects.get( 1 );

        PackageFeatureMojo mojo = (PackageFeatureMojo) lookupMojo( "package-feature", project.getFile() );
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "session", newMavenSession( project, projects ) );

        mojo.execute();

        Feature feature = Feature.read( new File( "target/projects/featureXmlGeneration/feature/target/feature.xml" ) );

        assertEquals( "4.8.1.v20100302", feature.getPlugins().get( 0 ).getVersion() );
    }
}
