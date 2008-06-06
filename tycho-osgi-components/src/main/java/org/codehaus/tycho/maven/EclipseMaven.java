package org.codehaus.tycho.maven;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.DefaultMaven;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.tycho.osgitools.OsgiState;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.BundleException;

public class EclipseMaven extends DefaultMaven {

	private OsgiState state;

	@Override
	protected List getProjects(MavenExecutionRequest request) throws MavenExecutionException {
		List<MavenProject> projects = super.getProjects(request);

		File workspace = null;
		if (projects.size() > 0) {
			MavenProject parent = projects.get(0);
			workspace = new File(parent.getBuild().getDirectory());
		}

		Properties props = getGlobalProperties(request);
		state.init(workspace, props);

		for (Iterator it = projects.iterator(); it.hasNext(); ) {
			MavenProject project = (MavenProject) it.next();
			try {
				state.addBundle(project);
			} catch (BundleException e) {
				throw new MavenExecutionException(e.getMessage(), project.getFile());
			}
		}

		state.resolveState();

		for (Iterator it = projects.iterator(); it.hasNext(); ) {
			MavenProject project = (MavenProject) it.next();
			BundleDescription bundleDescription = state.getBundleDescription(project);
			if (bundleDescription == null) {
				continue;
			}

			BundleDescription[] requiredBundles = state.getDependencies(bundleDescription);
			for (int i = 0; i < requiredBundles.length; i++) {
				Dependency dependency = new Dependency();
				BundleDescription supplier = requiredBundles[i].getSupplier().getSupplier();
				dependency.setGroupId(EclipseMavenProjetBuilder.getGroupId(state, supplier));
				dependency.setArtifactId(supplier.getSymbolicName());
				dependency.setVersion(supplier.getVersion().toString());

				project.getModel().addDependency(dependency);
			}
		}
		return projects;
	}
	
	// XXX must be an easier way
	private Properties getGlobalProperties(MavenExecutionRequest request) {
		List<String> activeProfiles = request.getActiveProfiles();
		Map<String, Profile> profiles = request.getProfileManager().getProfilesById();

		Properties props = new Properties();
		for (String profileName : activeProfiles) {
			Profile profile = profiles.get(profileName);
			props.putAll(profile.getProperties());
		}

		props.putAll(request.getProperties());
		props.putAll(request.getUserProperties());

		return props;
	}
}
