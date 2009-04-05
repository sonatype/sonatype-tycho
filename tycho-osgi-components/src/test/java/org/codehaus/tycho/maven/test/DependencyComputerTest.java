package org.codehaus.tycho.maven.test;

import java.io.File;
import java.util.List;

import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.TychoSession;
import org.codehaus.tycho.maven.EclipseMaven;
import org.codehaus.tycho.osgitools.DependencyComputer;
import org.codehaus.tycho.osgitools.DependencyComputer.DependencyEntry;
import org.codehaus.tycho.testing.AbstractTychoMojoTestCase;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.junit.Assert;
import org.junit.Test;

public class DependencyComputerTest
    extends AbstractTychoMojoTestCase
{

    private EclipseMaven maven;

    private DependencyComputer dependencyComputer;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        maven = (EclipseMaven) lookup( Maven.ROLE );
        dependencyComputer = (DependencyComputer) lookup( DependencyComputer.class );
    }

    @Test
    public void testExportPackage()
        throws Exception
    {
        File basedir = getBasedir( "projects/exportpackage" );
        File pom = new File( basedir, "pom.xml" );
        MavenExecutionRequest request = newMavenExecutionRequest( pom );
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        ReactorManager reactorManager = maven.createReactorManager( request, result );

        TychoSession tychoSession = maven.getTychoSession();

        MavenProject project = tychoSession.getMavenProject( new File( basedir, "bundle" ) );
        BundleResolutionState bundleResolutionState = tychoSession.getBundleResolutionState( project );

        BundleDescription bundle = bundleResolutionState.getBundleByLocation( project.getBasedir() );
        List<DependencyEntry> dependencies = dependencyComputer.computeDependencies( bundleResolutionState, bundle );
        Assert.assertEquals( 2, dependencies.size() );
        Assert.assertEquals( "dep", dependencies.get( 0 ).desc.getSymbolicName() );
        Assert.assertEquals( "dep2", dependencies.get( 1 ).desc.getSymbolicName() );
    }
}
