package org.codehaus.tycho.osgitools.targetplatform;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.PluginRef;
import org.codehaus.tycho.osgitools.OsgiState;

@Component( role = EclipseTargetPlatformFactory.class )
public class EclipseTargetPlatformFactory extends AbstractLogEnabled {

	public static final String PACKAGING_ECLIPSE_INSTALLATION = "eclipse-installation";
	public static final String PACKAGING_ECLIPSE_EXTENSION_LOCATION = "eclipse-extension-location";
	public static final String PACKAGING_ECLIPSE_PLUGIN = "eclipse-plugin";
	public static final String PACKAGING_ECLIPSE_FEATURE = "eclipse-feature";

	@Requirement
	private ArtifactResolver artifactResolver;

	@Requirement
	private ArtifactFactory artifactFactory;

	public EclipseTargetPlatformFactory() {
	}

	public void createTargetPlatform(List<MavenProject> projects, ArtifactRepository localRepository, OsgiState state) {

		File installation = getEclipseInstallation(projects);
		if (installation != null) {
			createTargetPlatform(state, installation);
		}
		
		Set<File> extensionLocations = getEclipseLocations(projects, PACKAGING_ECLIPSE_EXTENSION_LOCATION, false);
		for (File extensionLocation : extensionLocations)
		{
			addExtensionLocation(state, extensionLocation);
		}

		Set<File> features = new LinkedHashSet<File>();
		Set<File> bundles = new LinkedHashSet<File>();

		Map<Artifact, Exception> exceptions = new HashMap<Artifact, Exception>();

		// no P2 support for now, will add later
		for (MavenProject project : projects) {
			Map<String, Artifact> versionMap = project.getManagedVersionMap();
			if (versionMap != null) {
				for (Artifact artifact : versionMap.values()) {
					try {
						if (PACKAGING_ECLIPSE_FEATURE.equals(artifact.getType())) {
							resolveFeature(artifact, features, bundles, project.getRemoteArtifactRepositories(), localRepository);
						} else if (PACKAGING_ECLIPSE_PLUGIN.equals(artifact.getType())) {
							resolvePlugin(artifact, bundles, project.getRemoteArtifactRepositories(), localRepository);
						}
					} catch (Exception e) {
						exceptions.put(artifact, e);
					}
				}
			}
		}

		if (!exceptions.isEmpty()) {
			getLogger().warn("There were exceptions resolving build target platform");
			for (Map.Entry<Artifact, Exception> e : exceptions.entrySet()) {
				getLogger().warn(e.getKey() + " " + e.getValue().getMessage());
			}
		}
		
		state.addSite(new File(localRepository.getBasedir()), features, bundles);

	}

	public void createTargetPlatform(OsgiState state, File installation) {
		state.setTargetPlatform(installation);

		EclipseInstallationLayout finder = new EclipseInstallationLayout(getLogger(), installation);
		Set<File> sites = finder.getSites();
		for (File site : sites) {
			Set<File> features = finder.getFeatures(site);
			Set<File> bundles = finder.getPlugins(site);
			state.addSite(site, features, bundles);
		}
	}

	private void addExtensionLocation(OsgiState state, File location) {
		EclipseInstallationLayout finder = new EclipseInstallationLayout(getLogger(), location);

		Set<File> features = finder.getFeatures(location);
		Set<File> bundles = finder.getPlugins(location);
		state.addSite(location, features, bundles);
	}

	private void resolveFeature(Artifact artifact, Set<File> features, Set<File> bundles, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository) throws AbstractArtifactResolutionException, IOException, XmlPullParserException {
		resolveArtifact(artifact, remoteRepositories, localRepository);
		Feature feature = Feature.readJar(artifact.getFile());
//		File featureDir = unpackFeature(artifact, feature, state);
		if (features.add(artifact.getFile())) {
			for (PluginRef ref : feature.getPlugins()) {
				try {
					Artifact includedArtifact = artifactFactory.createArtifact(ref.getMavenGroupId(), ref.getId(), ref.getMavenVersion(), null, PACKAGING_ECLIPSE_PLUGIN);
					resolvePlugin(includedArtifact, bundles, remoteRepositories, localRepository);
				} catch (Exception e) {
					getLogger().warn(e.getMessage());
				}
			}
			for (Feature.FeatureRef ref : feature.getIncludedFeatures()) {
				try {
					Artifact includedArtifact = artifactFactory.createArtifact(ref.getMavenGroupId(), ref.getId(), ref.getMavenVersion(), null, PACKAGING_ECLIPSE_FEATURE);
					resolveFeature(includedArtifact, features, bundles, remoteRepositories, localRepository);
				} catch (Exception e) {
					getLogger().warn(e.getMessage());
				}
			}
		}
	}

//	private File unpackFeature(Artifact artifact, Feature feature, OsgiState state) throws AbstractArtifactResolutionException, IOException {
//		File dstDir = state.getFeatureDir(feature.getId(), feature.getVersion());
//		ZipUnArchiver unzipper = new ZipUnArchiver();
//		unzipper.setSourceFile(artifact.getFile());
//		unzipper.setDestDirectory(dstDir);
//		unzipper.extract();
//		return dstDir;
//	}

	private void assertResolved(Artifact artifact) throws ArtifactNotFoundException {
		if (!artifact.isResolved() || artifact.getFile() == null || !artifact.getFile().canRead()) {
			throw new ArtifactNotFoundException("Artifact is not resolved", artifact);
		}
	}

	private void resolvePlugin(Artifact artifact, Set<File> bundles, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository) throws AbstractArtifactResolutionException {
		resolveArtifact(artifact, remoteRepositories, localRepository);
		bundles.add(artifact.getFile());
	}

	private void resolveArtifact(Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository) throws AbstractArtifactResolutionException	{
		artifactResolver.resolve(artifact, remoteRepositories, localRepository);
		assertResolved(artifact);
	}

	private File getEclipseInstallation(List<MavenProject> projects) {
		Set<File> locations = getEclipseLocations(projects, PACKAGING_ECLIPSE_INSTALLATION, true);
		return (!locations.isEmpty())? locations.iterator().next(): null;
	}

	private Set<File> getEclipseLocations(List<MavenProject> projects, String packaging, boolean singleton) {
		LinkedHashSet<File> installations = new LinkedHashSet<File>();
		for (MavenProject project : projects) {
			Map<String, Artifact> versionMap = project.getManagedVersionMap();
			if (versionMap != null) {
				for (Artifact artifact : versionMap.values()) {
					if (packaging.equals(artifact.getType())) {
						if (!singleton || installations.size() <= 0) {
							installations.add(artifact.getFile());
						} else {
							if (!installations.contains(artifact.getFile())) {
								throw new TargetPlatformException("No more than one eclipse-installation and/or eclipse-distributions");
							}
						}
					}
				}
			}
		}
		return installations;
	}
}
