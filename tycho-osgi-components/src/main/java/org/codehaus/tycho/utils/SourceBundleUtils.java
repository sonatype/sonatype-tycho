package org.codehaus.tycho.utils;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.TychoProject;

public class SourceBundleUtils {

	private static final String SUFFIX_PROPERTY = "sourceBundleSuffix";
	public static final String SOURCE_BUNDLE_SUFFIX = ".source";
	public static final String ARTIFACT_CLASSIFIER = "sources";

	private SourceBundleUtils() {
		// no instances
	}

	public static String getSourceBundleSuffix(MavenProject project) {
		String packaging = project.getPackaging();
		if (!(TychoProject.ECLIPSE_PLUGIN.equals(packaging) || TychoProject.ECLIPSE_TEST_PLUGIN
				.equals(packaging))) {
			return null;
		}
		String suffix = project.getProperties().getProperty(SUFFIX_PROPERTY);
		// TODO we should rather check the configuration of OsgiSourceMojo
		if (suffix == null) {
			return SOURCE_BUNDLE_SUFFIX;
		} else {
			return suffix;
		}
	}

}
