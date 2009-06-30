package org.codehaus.tycho;


public interface TychoConstants {
	static final String ATTR_GROUP_ID = "MavenArtifact-GroupId";
	static final String ATTR_ARTIFACT_ID = "MavenArtifact-ArtifactId";
	static final String ATTR_BASE_VERSION = "MavenArtifact-BaseVersion";

	static final String HIGHEST_VERSION = "highest version";

	static final String CONFIG_INI_PATH = "configuration/config.ini";
	static final String BUNDLES_INFO_PATH = "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info";
	static final String PLATFORM_XML_PATH = "configuration/org.eclipse.update/platform.xml";

	static final String CTX_BASENAME = TychoConstants.class.getName();
    static final String CTX_FEATURE_RESOLUTION_STATE = CTX_BASENAME + "/featureResolutionState";
    static final String CTX_TARGET_PLATFORM = CTX_BASENAME + "/targetPlatform";
    static final String CTX_BUNDLE_RESOLUTION_STATE = CTX_BASENAME + "/bundleResolutionState";
    static final String CTX_ECLIPSE_PLUGIN_PROJECT = CTX_BASENAME + "/eclipsePluginProject";
    static final String CTX_EXPANDED_VERSION = CTX_BASENAME + "/expandedVersion";
}
