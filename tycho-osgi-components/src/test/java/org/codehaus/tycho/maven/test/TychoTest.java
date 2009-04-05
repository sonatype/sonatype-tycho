package org.codehaus.tycho.maven.test;

import java.io.File;
import java.util.List;

import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.TychoSession;
import org.codehaus.tycho.maven.EclipseMaven;
import org.codehaus.tycho.testing.AbstractTychoMojoTestCase;
import org.eclipse.osgi.service.resolver.BundleDescription;

public class TychoTest extends AbstractTychoMojoTestCase {

	protected Maven maven;

	protected Logger logger;

	protected void setUp() throws Exception {
		super.setUp();
		if (getContainer().hasComponent(Maven.ROLE, "test")) {
			maven = (Maven) lookup(Maven.ROLE, "test");
		} else {
			// default over to the main project builder...
			maven = (Maven) lookup(Maven.ROLE);
		}
		logger = new SilentLog();
	}

	public void testModuleOrder() throws Exception {
		File pom = new File(getBasedir("projects/moduleorder"), "pom.xml");

        MavenExecutionRequest request = newMavenExecutionRequest(pom);

		MavenExecutionResult result = new DefaultMavenExecutionResult();

		ReactorManager reactorManager = maven.createReactorManager(request, result);

		List projects = reactorManager.getSortedProjects();
		assertEquals(5, projects.size());

		MavenProject p002 = (MavenProject) projects.get(1);
		MavenProject p001 = (MavenProject) projects.get(2);
		MavenProject p004 = (MavenProject) projects.get(3); // feature
		MavenProject p003 = (MavenProject) projects.get(4); // site

		assertEquals("moduleorder.p001", p001.getArtifactId());
		assertEquals("moduleorder.p002", p002.getArtifactId());
		assertEquals("moduleorder.p003", p003.getArtifactId());
		assertEquals("moduleorder.p004", p004.getArtifactId());
	}

	public void testResolutionError() throws Exception {
		File pom = new File(getBasedir("projects/resolutionerror/p001"), "pom.xml");

        MavenExecutionRequest request = newMavenExecutionRequest(pom);

		MavenExecutionResult result = new DefaultMavenExecutionResult();

		maven.createReactorManager(request, result);

		List<Exception> exceptions = result.getExceptions();
		assertEquals(1, exceptions.size());
		assertTrue(exceptions.get(0).getMessage().contains("Missing Constraint: Import-Package: moduleorder.p002"));
	}

	public void testResolutionError_t001_errorInTargetPlatform() throws Exception {
		File platform = new File(getBasedir(), "src/test/resources/projects/resolutionerror/t001/platform");
		File pom = new File(getBasedir("projects/resolutionerror/t001/p003"), "pom.xml");

        MavenExecutionRequest request = newMavenExecutionRequest(pom);
		request.getProperties().put("tycho.targetPlatform", platform.getCanonicalPath());

		MavenExecutionResult result = new DefaultMavenExecutionResult();

		maven.createReactorManager(request, result);

		List<Exception> exceptions = result.getExceptions();
		assertEquals(1, exceptions.size());
		assertTrue(exceptions.get(0).getMessage().contains("Missing Constraint: Require-Bundle: moduleorder.p004"));
		assertTrue(exceptions.get(0).getMessage().contains("Platform filter did not match"));
	}

	public void testProjectPriority() throws Exception {
		File platform = new File(getBasedir(), "src/test/resources/projects/projectpriority/platform");
		File pom = new File(getBasedir("projects/projectpriority"), "pom.xml");

		MavenExecutionRequest request = newMavenExecutionRequest(pom);
		request.getProperties().put("tycho.targetPlatform", platform.getCanonicalPath());
		MavenExecutionResult result = new DefaultMavenExecutionResult();
		ReactorManager reactorManager = maven.createReactorManager(request, result);

		assertEquals(0, result.getExceptions().size());

		List projects = reactorManager.getSortedProjects();

		MavenProject p002 = (MavenProject) projects.get(2);

		List<Dependency> dependencies = p002.getModel().getDependencies();
		Dependency dependency = dependencies.get(0);
		assertEquals("0.0.1", dependency.getVersion());
	}

	public void _testRemoteTargetPlatform() throws Exception {
		File pom = new File(getBasedir("projects/remoterepo/p001"), "pom.xml");

		MavenExecutionRequest request = newMavenExecutionRequest(pom);
		MavenExecutionResult result = new DefaultMavenExecutionResult();
		ReactorManager reactorManager = maven.createReactorManager(request, result);

		assertEquals(0, result.getExceptions().size());

		List projects = reactorManager.getSortedProjects();

		MavenProject p001 = (MavenProject) projects.get(0);

		assertEquals("remoterepo", p001.getGroupId());
		assertEquals("remoterepo.p001", p001.getArtifactId());
		assertEquals("1.0.0", p001.getVersion());
		
	}

	public void _testPomless() throws Exception {
		File pom = new File(getBasedir("projects/pomless/p001"), "pom.xml");

        MavenExecutionRequest request = newMavenExecutionRequest(pom);

		MavenExecutionResult result = new DefaultMavenExecutionResult();

		ReactorManager reactorManager = maven.createReactorManager(request, result);
		
		if (result.getExceptions().size() > 0) {
			for (Exception e : (List<Exception>) result.getExceptions()) {
				e.printStackTrace();
			}
		}

		List projects = reactorManager.getSortedProjects();

		MavenProject p001 = (MavenProject) projects.get(0);
		
		assertEquals("pomless.p001", p001.getArtifactId());
	}

	public void testFragment() throws Exception {
		File pom = new File(getBasedir("projects/fragment"), "pom.xml");

        MavenExecutionRequest request = newMavenExecutionRequest(pom);

		MavenExecutionResult result = new DefaultMavenExecutionResult();

		ReactorManager reactorManager = maven.createReactorManager(request, result);

		List<MavenProject> projects = reactorManager.getSortedProjects();

		MavenProject host = projects.get(1);
		MavenProject dep = projects.get(2);
		MavenProject fragment = projects.get(3);
		MavenProject fragment2 = projects.get(4);
		MavenProject client = projects.get(5);

		assertEquals("host", host.getArtifactId());
		// host does not know anything about fragments
		List<Dependency> hostDependencies = host.getModel().getDependencies();
		assertEquals(0, hostDependencies.size());

		assertEquals("fragment", fragment.getArtifactId());
		List<Dependency> fragmentDependencies = fragment.getModel().getDependencies();
		// host first, then fragment dependency
		assertEquals(2, fragmentDependencies.size());
		assertEquals("host", fragmentDependencies.get(0).getArtifactId()); 
		assertEquals("dep", fragmentDependencies.get(1).getArtifactId());

		assertEquals("fragment2", fragment2.getArtifactId());
		// host only
		List<Dependency> fragment2Dependencies = fragment2.getModel().getDependencies();
		assertEquals(1, fragment2Dependencies.size());
		assertEquals("host", fragment2Dependencies.get(0).getArtifactId());

		assertEquals("client", client.getArtifactId());
		// depends on host and because host has ExtensibleAPI also depends fragment and fragent2
		List<Dependency> clientDependencies = client.getModel().getDependencies();
		assertEquals(3, clientDependencies.size());
		assertEquals("host", clientDependencies.get(0).getArtifactId());
		assertEquals("fragment", clientDependencies.get(1).getArtifactId());
		assertEquals("fragment2", clientDependencies.get(2).getArtifactId());
	}

	public void testPre30() throws Exception {
        File pom = new File(getBasedir("projects/dummy"), "pom.xml");

	    MavenExecutionRequest request = newMavenExecutionRequest(pom);
        request.getProperties().put("tycho.targetPlatform", new File("src/test/resources/targetplatforms/pre-3.0").getCanonicalPath());
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        ReactorManager reactorManager = maven.createReactorManager(request, result);

        MavenProject project = (MavenProject) reactorManager.getSortedProjects().get(0);

        TychoSession session = ((EclipseMaven) maven).getTychoSession();
        BundleResolutionState state = session.getBundleResolutionState( project );

		assertNotNull(state.getBundle("testjar", "1.0.0"));
		assertNotNull(state.getBundle("testdir", "1.0.0"));

		assertTrue(new File(project.getBuild().getDirectory(), "manifests/testdir_1.0.0/META-INF/MANIFEST.MF").canRead());
		assertTrue(new File(project.getBuild().getDirectory(), "manifests/testjar_1.0.0/META-INF/MANIFEST.MF").canRead());
	}

	public void testMNGECLIPSE942() throws Exception {
        File pom = new File(getBasedir("projects/dummy"), "pom.xml");

        MavenExecutionRequest request = newMavenExecutionRequest(pom);
        request.getProperties().put("tycho.targetPlatform", new File("src/test/resources/targetplatforms/MNGECLIPSE-942").getCanonicalPath());
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        ReactorManager reactorManager = maven.createReactorManager(request, result);

        MavenProject project = (MavenProject) reactorManager.getSortedProjects().get(0);

        TychoSession session = ((EclipseMaven) maven).getTychoSession();
        BundleResolutionState state = session.getBundleResolutionState( project );

		List<BundleDescription> bundles = state.getBundles();

		assertEquals(1, bundles.size());
		assertEquals("org.junit4.nl_ru", bundles.get(0).getSymbolicName());
	}
	
	public void testAddHocExtensionLocation() throws Exception {
        File targetPlatform = new File("src/test/resources/targetplatforms/simple");
        File extensionLocation = new File("src/test/resources/targetplatforms/adhoclocation");

        File pom = new File(getBasedir("projects/adhoclocations"), "pom.xml");

        MavenExecutionRequest request = newMavenExecutionRequest(pom);
        request.getProperties().put("targetPlatform", targetPlatform.getCanonicalPath());
        request.getProperties().put("extensionLocation", extensionLocation.getCanonicalPath());
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        ReactorManager reactorManager = maven.createReactorManager(request, result);

        TychoSession session = ((EclipseMaven) maven).getTychoSession();
        BundleResolutionState state = session.getBundleResolutionState( (MavenProject) reactorManager.getSortedProjects().get(0) );

        List<BundleDescription> bundles = state.getBundles();

        assertEquals(1, bundles.size());
        assertEquals("testjar", bundles.get(0).getSymbolicName());
	}
}
