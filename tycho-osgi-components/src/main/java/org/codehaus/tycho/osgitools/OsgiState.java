package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Manifest;

import org.apache.maven.project.MavenProject;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.osgi.framework.BundleException;

public interface OsgiState {
	static final String ROLE = OsgiState.class.getName();
	
	static final String ATTR_GROUP_ID = "MavenArtifact-GroupId";
	static final String ATTR_ARTIFACT_ID = "MavenArtifact-ArtifactId";
	static final String ATTR_BASE_VERSION = "MavenArtifact-BaseVersion";

	BundleDescription addBundle(File manifest) throws BundleException;

	BundleDescription[] getDependencies(BundleDescription desc);

	void resolveState();

	Manifest loadManifest(File bundleLocation);

	BundleDescription[] getBundles();

	StateHelper getStateHelper();

	Map getPatchData();

	Map getExtraData();

	BundleDescription getBundleDescription(MavenProject project);

	ResolverError[] getResolverErrors(BundleDescription thisBundle);

	BundleDescription addBundle(MavenProject project) throws BundleException;

	MavenProject getMavenProject(BundleDescription model);

	void init(File workspace, Properties props);

	BundleDescription getBundleDescription(String symbolicName, String version);

	String getGroupId(BundleDescription desc);

	void assertResolved(BundleDescription desc) throws BundleException;
}
