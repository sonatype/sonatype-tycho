package org.codehaus.tycho.maven.test;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.osgitools.OsgiState;
import org.codehaus.tycho.testing.AbstractTychoMojoTestCase;

public class TychoTest extends AbstractTychoMojoTestCase {

	protected Maven maven;

	protected OsgiState state;

	protected void setUp() throws Exception {
		super.setUp();
		if (getContainer().hasComponent(Maven.ROLE, "test")) {
			maven = (Maven) lookup(Maven.ROLE, "test");
		} else {
			// default over to the main project builder...
			maven = (Maven) lookup(Maven.ROLE);
		}
		state = (OsgiState) lookup(OsgiState.ROLE);
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

		assertEquals(1, result.getExceptions().size());
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

	public void testRemoteTargetPlatform() throws Exception {
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
		MavenProject fragment = projects.get(2);
		MavenProject client = projects.get(3);

		assertEquals("host", host.getArtifactId());
		assertEquals(0, host.getModel().getDependencies().size());

		assertEquals("fragment", fragment.getArtifactId());
		List<Dependency> fragmentDependencies = fragment.getModel().getDependencies();
		assertEquals(1, fragmentDependencies.size());
		assertEquals("host", fragmentDependencies.get(0).getArtifactId());

		assertEquals("client", client.getArtifactId());
		List<Dependency> clientDependencies = client.getModel().getDependencies();
		assertEquals(2, clientDependencies.size());
		assertEquals("host", clientDependencies.get(0).getArtifactId());
		assertEquals("fragment", clientDependencies.get(1).getArtifactId());
	}

	public void testPre30() throws Exception {
		File targetPlatform = new File("src/test/resources/targetplatforms/pre-3.0");
		File workspace = new File("target/workspace");
		Properties props = new Properties(System.getProperties());
		props.put("tycho.targetPlatform", targetPlatform.getCanonicalPath());
		state.init(null, workspace, props);

		assertNotNull(state.getBundleDescription("testjar", "1.0.0"));
		assertNotNull(state.getBundleDescription("testdir", "1.0.0"));
	}

}
