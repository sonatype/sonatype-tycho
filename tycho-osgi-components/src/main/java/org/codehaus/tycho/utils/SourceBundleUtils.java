package org.codehaus.tycho.utils;

import org.apache.maven.project.MavenProject;
import org.sonatype.tycho.ArtifactKey;

public class SourceBundleUtils {

	private static final String SUFFIX_PROPERTY = "sourceBundleSuffix";
	public static final String SOURCE_BUNDLE_SUFFIX = ".source";
	public static final String ARTIFACT_CLASSIFIER = "sources";

	private SourceBundleUtils() {
		// no instances
	}

	public static String getSourceBundleSuffix(MavenProject project) {
		String packaging = project.getPackaging();
		if (!(ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals(packaging) || ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN
				.equals(packaging))) {
			return null;
		}
		// TODO should rather use MavenSession to get effective properties?
		String suffix = System.getProperty(SUFFIX_PROPERTY);
		if (suffix != null) {
			return suffix;
		} 
		suffix = project.getProperties().getProperty(SUFFIX_PROPERTY);
		if (suffix != null) {
			return suffix;
		} 
		return SOURCE_BUNDLE_SUFFIX;
	}

}
