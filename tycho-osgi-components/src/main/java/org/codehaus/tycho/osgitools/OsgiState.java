package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Manifest;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.model.Feature;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.osgi.framework.BundleException;

public interface OsgiState {
	static final String ROLE = OsgiState.class.getName();
	
	static final String ATTR_GROUP_ID = "MavenArtifact-GroupId";
	static final String ATTR_ARTIFACT_ID = "MavenArtifact-ArtifactId";
	static final String ATTR_BASE_VERSION = "MavenArtifact-BaseVersion";

	static final String HIGHEST_VERSION = "highest version";

	static final String PACKAGING_ECLIPSE_PLUGIN = "eclipse-plugin";
	static final String PACKAGING_ECLIPSE_TEST_PLUGIN = "eclipse-test-plugin";
	static final String PACKAGING_ECLIPSE_FEATURE = "eclipse-feature";

	BundleDescription addBundle(File manifest) throws BundleException;

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

	Map getPatchData();

	BundleDescription getBundleDescription(MavenProject project);

	ResolverError[] getResolverErrors(BundleDescription thisBundle);

	void addProject(MavenProject project) throws BundleException;

	MavenProject getMavenProject(BundleDescription model);

	void init(File targetPlatform, Properties props);

	BundleDescription getBundleDescription(String symbolicName, String version);

	String getGroupId(BundleDescription desc);

	void assertResolved(BundleDescription desc) throws BundleException;

	String getManifestAttribute(BundleDescription desc, String attr);

	File getTargetPlaform();

	BundleDescription getBundleDescription(File location);

	Feature getFeature(String id, String version);

	MavenProject getMavenProject(Feature feature);

	Feature getFeature(MavenProject project);

}
