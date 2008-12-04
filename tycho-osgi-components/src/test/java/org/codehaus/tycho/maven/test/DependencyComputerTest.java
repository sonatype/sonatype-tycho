package org.codehaus.tycho.maven.test;

import java.io.File;
import java.util.List;

import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.tycho.osgitools.DependencyComputer;
import org.codehaus.tycho.osgitools.OsgiState;
import org.codehaus.tycho.osgitools.DependencyComputer.DependencyEntry;
import org.codehaus.tycho.testing.AbstractTychoMojoTestCase;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.junit.Assert;
import org.junit.Test;

public class DependencyComputerTest extends AbstractTychoMojoTestCase {

	private Maven maven;
	private DependencyComputer dependencyComputer;
	protected OsgiState state;

	protected void setUp() throws Exception {
		super.setUp();
		maven = (Maven) lookup(Maven.ROLE);
		state = (OsgiState) lookup(OsgiState.class);
		dependencyComputer = (DependencyComputer) lookup(DependencyComputer.class);
	}

	@Test
	public void testExportPackage() throws Exception {
		File basedir = getBasedir("projects/exportpackage");
		File pom = new File(basedir, "pom.xml");
        MavenExecutionRequest request = newMavenExecutionRequest(pom);
		MavenExecutionResult result = new DefaultMavenExecutionResult();
		maven.createReactorManager(request, result);

		BundleDescription bundle = state.getBundleDescription(new File(basedir, "bundle"));
		List<DependencyEntry> dependencies = dependencyComputer.computeDependencies(bundle);
		Assert.assertEquals(2, dependencies.size());
		Assert.assertEquals("dep", dependencies.get(0).desc.getSymbolicName());
		Assert.assertEquals("dep2", dependencies.get(1).desc.getSymbolicName());
	}
}
