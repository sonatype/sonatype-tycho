package org.codehaus.tycho.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.DefaultMaven;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.Activation;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.tycho.osgitools.OsgiState;
import org.codehaus.tycho.p2.P2;
import org.codehaus.tycho.p2.P2ArtifactRepositoryLayout;
import org.osgi.framework.BundleException;

public class EclipseMaven extends DefaultMaven {

	private OsgiState state;
	private P2 p2;

	@Override
	protected List getProjects(MavenExecutionRequest request) throws MavenExecutionException {
		request.getProperties().put("tycho-version", state.getTychoVersion());
		List<MavenProject> projects = super.getProjects(request);

		calculateConcreteState(projects, request);

		try {
			resolveOSGiState(projects, request);
		} finally {
			restoreDynamicState(projects, request);
		}
		return projects;
	}

	public void restoreDynamicState(List<MavenProject> projects, MavenExecutionRequest request) {
		for (MavenProject project : projects) {
			try {
				projectBuilder.restoreDynamicState(project, request.getProjectBuildingConfiguration());
			} catch (ModelInterpolationException e) {
				// ignore
			}
		}
	}

	public void calculateConcreteState(List<MavenProject> projects, MavenExecutionRequest request) {
		for (MavenProject project : projects) {
			try {
				projectBuilder.calculateConcreteState(project, request.getProjectBuildingConfiguration());
			} catch (ModelInterpolationException e) {
				// ignore
			}
		}
	}

	private void resolveOSGiState(List<MavenProject> projects,
			MavenExecutionRequest request) throws MavenExecutionException {
		Properties props = getGlobalProperties(request);

		File workspace = null;
		File targetPlatform = null;
		if (projects.size() > 0) {
			MavenProject parent = projects.get(0);
			workspace = new File(parent.getBuild().getDirectory());

			List<String> repositories = getP2Repositories(projects, request);
			List<Artifact> rootIUs = getP2Dependencies(projects, request);

			if (!rootIUs.isEmpty()) {
				String key = getProfileName(parent);
				String location = p2.materializeTargetPlatform(key, repositories, rootIUs, props);
				targetPlatform = new File(location);
			}
		}

		state.init(targetPlatform, workspace, props);

		for (MavenProject project : projects) {
			try {
				state.addProject(project);
			} catch (BundleException e) {
				throw new MavenExecutionException(e.getMessage(), project.getFile());
			}
		}

		state.resolveState();

		try {
			for (MavenProject project : projects) {
				DependenciesReader dr = (DependenciesReader) container.lookup(DependenciesReader.class, project.getPackaging());
					if (dr != null) {
					for (Dependency dependency : dr.getDependencies(project)) {
						project.getModel().addDependency(dependency);
					}
				}
			}
		} catch (ComponentLookupException e) {
			// no biggie 
		}
	}

	private String getProfileName(MavenProject parent) throws MavenExecutionException {
		try {
			return parent.getBasedir().getCanonicalPath();
		} catch (IOException e) {
			throw new MavenExecutionException("Can't create p2 profile name", e);
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

	private List<String> getP2Repositories(List<MavenProject> projects, MavenExecutionRequest request) {
		Set<String> result = new LinkedHashSet<String>();
		List<ArtifactRepository> remoteRepositories = (List<ArtifactRepository>) request.getRemoteRepositories();
		addP2Repositories(result, remoteRepositories);
		for (MavenProject project : projects) {
			addP2Repositories(result, (List<ArtifactRepository>) project.getRemoteArtifactRepositories());
		}
		return new ArrayList<String>(result);
	}

	private void addP2Repositories(Set<String> result,	List<ArtifactRepository> remoteRepositories) {
		if (remoteRepositories != null) {
			for (ArtifactRepository repository : remoteRepositories) {
				if (repository.getLayout() instanceof P2ArtifactRepositoryLayout) {
					result.add(repository.getUrl());
				}
			}
		}
	}

	private List<Artifact> getP2Dependencies(List<MavenProject> projects, MavenExecutionRequest request) {
		Set<Artifact> result = new LinkedHashSet<Artifact>();
		for (MavenProject project : projects) {
			Map<String, Artifact> versionMap = project.getManagedVersionMap();
			if (versionMap != null) {
				for (Artifact artifact : versionMap.values()) {
					if ("osgi-bundle".equals(artifact.getType())) {
						result.add(artifact);
					}
				}
			}
		}
		return new ArrayList<Artifact>(result);
	}
}
