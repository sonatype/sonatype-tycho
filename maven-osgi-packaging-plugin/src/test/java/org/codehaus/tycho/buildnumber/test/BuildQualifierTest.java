package org.codehaus.tycho.buildnumber.test;

import java.io.File;

import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.buildnumber.BuildQualifierMojo;
import org.codehaus.tycho.maven.EclipseMaven;
import org.codehaus.tycho.maven.TychoMavenSession;
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
        
        maven = (Maven) lookup(Maven.ROLE);
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

        MavenProject project = getProject( request );
        project.getProperties().put( BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY, "garbage" );

        MavenSession session = new TychoMavenSession(getContainer(), request, null, null, ((EclipseMaven) maven).getTychoSession());

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

        MavenProject project = getProject( request );
        project.getProperties().put( BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY, "garbage" );

        MavenSession session =
            new TychoMavenSession( getContainer(), request, null, null, ( (EclipseMaven) maven ).getTychoSession() );

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

        MavenProject project = getProject( request );
        project.getProperties().put( BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY, "garbage" );

        MavenSession session =
            new TychoMavenSession( getContainer(), request, null, null, ( (EclipseMaven) maven ).getTychoSession() );

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
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        ReactorManager reactorManager = maven.createReactorManager(request, result);

        MavenProject project = (MavenProject) reactorManager.getSortedProjects().get(0);
        
        assertTrue( result.getExceptions().toString(), result.getExceptions().isEmpty() );

        return project;
    }

    private BuildQualifierMojo getMojo(MavenProject project, MavenSession session) throws Exception {
        BuildQualifierMojo mojo = (BuildQualifierMojo) lookupMojo("build-qualifier", project.getFile());
        setVariableValueToObject(mojo, "project", project);

        setVariableValueToObject( mojo, "session", session );

        return mojo;
    }

}
