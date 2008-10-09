package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.Platform;
import org.codehaus.tycho.osgitools.features.FeatureDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

public interface OsgiState {
	static final String ROLE = OsgiState.class.getName();
	
	static final String ATTR_GROUP_ID = "MavenArtifact-GroupId";
	static final String ATTR_ARTIFACT_ID = "MavenArtifact-ArtifactId";
	static final String ATTR_BASE_VERSION = "MavenArtifact-BaseVersion";

	static final String HIGHEST_VERSION = "highest version";

	static final String PACKAGING_ECLIPSE_PLUGIN = "eclipse-plugin";
	static final String PACKAGING_ECLIPSE_TEST_PLUGIN = "eclipse-test-plugin";
	static final String PACKAGING_ECLIPSE_FEATURE = "eclipse-feature";

	static final String CONFIG_INI_PATH = "configuration/config.ini";
	static final String BUNDLES_INFO_PATH = "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info";
	static final String PLATFORM_XML_PATH = "configuration/org.eclipse.update/platform.xml";

	BundleDescription addBundle(File manifest) throws BundleException;
	BundleDescription addBundle(File manifest, boolean override) throws BundleException;

	/**
	 * Returns all direct and indirect dependencies of the bundle.
	 * 
	 * Fragments attached to the bundle are NOT included, however,
	 * fragments attached to bundles referenced by the bundle are.
	 */
	BundleDescription[] getDependencies(BundleDescription bundle);

	void resolveState();

	Manifest loadManifest(File bundleLocation);

	/**
	 * Returns all bundles known to this state.
	 */
	BundleDescription[] getBundles();

	StateHelper getStateHelper();

	BundleDescription getBundleDescription(MavenProject project);

	ResolverError[] getResolverErrors(BundleDescription thisBundle);

	void addProject(MavenProject project) throws BundleException;

	MavenProject getMavenProject(BundleDescription model);

	void reset(Properties props);

	BundleDescription getBundleDescription(String symbolicName, String version);

	String getGroupId(BundleDescription desc);

	void assertResolved(BundleDescription desc) throws BundleException;

	String getManifestAttribute(BundleDescription desc, String attr);

	File getTargetPlaform();

	BundleDescription getBundleDescription(File location);

	Feature getFeature(String id, String version);

	MavenProject getMavenProject(Feature feature);

	Feature getFeature(MavenProject project);

	FeatureDescription getFeatureDescription(String id, String version);

	FeatureDescription getFeatureDescription(Feature feature);

	String getPlatformProperty(String key);

	/**
	 * Returns Platform object that includes all features and bundles of
	 * the current platform.
	 */
	Platform getPlatform();

	void setTargetPlatform(File installation);
	void addSite(File site, Set<File> features, Set<File> bundles);

	Version getPlatformVersion();

}
