package org.codehaus.tycho.maven;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.DefaultMaven;
import org.apache.maven.Maven;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.Activation;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.tycho.osgitools.OsgiState;
import org.codehaus.tycho.osgitools.targetplatform.EclipseTargetPlatformFactory;
import org.codehaus.tycho.osgitools.utils.TychoVersion;
import org.osgi.framework.BundleException;

@Component(role = Maven.class)
public class EclipseMaven extends DefaultMaven {

    @Requirement
	private OsgiState state;

    @Requirement
	private EclipseTargetPlatformFactory factory;

	@Override
	protected List getProjects(MavenExecutionRequest request) throws MavenExecutionException {
		request.setProperty("tycho-version", TychoVersion.getTychoVersion());
		List<MavenProject> projects = super.getProjects(request);

		resolveOSGiState(projects, request);

		return projects;
	}

	private void resolveOSGiState(List<MavenProject> projects, MavenExecutionRequest request) throws MavenExecutionException {

		Properties props = getGlobalProperties(request);

		state.reset(props);

		String mode = props.getProperty("tycho.mode");
		
		if (!"maven".equals(mode)) {
			String property = props.getProperty("tycho.targetPlatform");
			if (property != null) {
	 			getLogger().info("Build target platform tycho.targetPlatform=" + property 
	 					+ "\n. This overrides target platform specified in pom.xml files, if any.");
				factory.createTargetPlatform(state, new File(property));
	 		} else {
	 			factory.createTargetPlatform(projects, request.getLocalRepository(), state);
	 		}
		}

		for (MavenProject project : projects) {
			try {
				state.addProject(project);
			} catch (BundleException e) {
				throw new MavenExecutionException(e.getMessage(), project.getFile());
			}
		}

		if (!"maven".equals(mode)) {
			state.resolveState();
	
			for (MavenProject project : projects) {
				try {
					DependenciesReader dr = (DependenciesReader) container.lookup(DependenciesReader.class, project.getPackaging());
					if (dr != null) {
						for (Dependency dependency : dr.getDependencies(project)) {
							project.getModel().addDependency(dependency);
						}
					}
				} catch (ComponentLookupException e) {
					// no biggie 
				}
			}
		}
	}

	// XXX there must be an easier way
	private Properties getGlobalProperties(MavenExecutionRequest request) {
		List<String> activeProfiles = request.getActiveProfiles();
		Map<String, Profile> profiles = request.getProfileManager().getProfilesById();

		Properties props = new Properties();
		props.putAll(System.getProperties());
		for (Profile profile : profiles.values()) {
			Activation activation = profile.getActivation();
			if ((activation != null && activation.isActiveByDefault()) || activeProfiles.contains(profile.getId())) {
				props.putAll(profile.getProperties());
			}
		}

		props.putAll(request.getProperties());
		props.putAll(request.getUserProperties());

		return props;
	}

}
