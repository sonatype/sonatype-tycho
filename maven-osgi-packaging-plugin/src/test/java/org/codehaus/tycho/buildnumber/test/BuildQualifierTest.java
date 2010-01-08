package org.codehaus.tycho.buildnumber.test;

import java.io.File;
import java.util.ArrayList;

import org.apache.maven.Maven;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.buildversion.BuildQualifierMojo;
import org.codehaus.tycho.testing.AbstractTychoMojoTestCase;

public class BuildQualifierTest
    extends AbstractTychoMojoTestCase
{

    protected Maven maven;
    
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        
        maven = lookup(Maven.class);
    }
    
    @Override
    protected void tearDown()
        throws Exception
    {
        maven = null;

        super.tearDown();
    }

    public void testForceContextQualifier()
        throws Exception
    {
        /*
         * This test covers all scenarios that involve forceContextQualifier
         * mojo parameter, i.e. setting -DforceContextQualifier=... on cli,
         * specifying forceContextQualifier project property and setting
         * forceContextQualifier using explicit mojo configuration.
         * 
         *  
         */

        File basedir = getBasedir( "projects/buildqualifier" );

        File pom = new File( basedir, "p001/pom.xml" );

        MavenExecutionRequest request = newMavenExecutionRequest( pom );
        request.getProjectBuildingRequest().setProcessPlugins( false );

        MavenProject project = getProject( request );
        project.getProperties().put( BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY, "garbage" );
        
        ArrayList<MavenProject> projects = new ArrayList<MavenProject>();
        projects.add(project);

        MavenSession session = new MavenSession(getContainer(), request, null, projects);

        BuildQualifierMojo mojo = getMojo( project, session );

        setVariableValueToObject( mojo, "forceContextQualifier", "foo-bar" );

        mojo.execute();

        assertEquals( "foo-bar", project.getProperties().get( BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY ) );
    }

    public void testBuildProperties()
        throws Exception
    {
        File basedir = getBasedir( "projects/buildqualifier" );

        File pom = new File( basedir, "p002/pom.xml" );

        MavenExecutionRequest request = newMavenExecutionRequest( pom );
        request.getProjectBuildingRequest().setProcessPlugins( false );

        MavenProject project = getProject( request );
        project.getProperties().put( BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY, "garbage" );

        ArrayList<MavenProject> projects = new ArrayList<MavenProject>();
        projects.add(project);

        MavenSession session = new MavenSession(getContainer(), request, null, projects);

        BuildQualifierMojo mojo = getMojo( project, session );

        mojo.execute();

        assertEquals( "blah", project.getProperties().get( BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY ) );
    }

    public void testTimestamp()
        throws Exception
    {
        File basedir = getBasedir( "projects/buildqualifier" );

        File pom = new File( basedir, "p001/pom.xml" );

        MavenExecutionRequest request = newMavenExecutionRequest( pom );
        request.getProjectBuildingRequest().setProcessPlugins( false );

        MavenProject project = getProject( request );
        project.getProperties().put( BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY, "garbage" );

        ArrayList<MavenProject> projects = new ArrayList<MavenProject>();
        projects.add(project);

        MavenSession session = new MavenSession(getContainer(), request, null, projects);

        BuildQualifierMojo mojo = getMojo( project, session );

        mojo.execute();

        String firstTimestamp = (String) project.getProperties().get( BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY );
        
        // lets do it again
        Thread.sleep( 500L );

        project = getProject( request );
        assertNull( project.getProperties().get( BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY ) );
        mojo = getMojo( project, session );
        mojo.execute();

        assertEquals( firstTimestamp, project.getProperties().get( BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY ) );
    }
    
    private MavenProject getProject( MavenExecutionRequest request )
        throws Exception
    {
        MavenExecutionResult result = maven.execute( request );
        return result.getProject();
    }

    private BuildQualifierMojo getMojo(MavenProject project, MavenSession session) throws Exception {
        BuildQualifierMojo mojo = (BuildQualifierMojo) lookupMojo("build-qualifier", project.getFile());
        setVariableValueToObject(mojo, "project", project);

        setVariableValueToObject( mojo, "session", session );

        return mojo;
    }

}
