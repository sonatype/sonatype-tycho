package org.codehaus.tycho.utils;

import org.apache.maven.project.MavenProject;
import org.sonatype.tycho.ArtifactKey;
import org.sonatype.tycho.ReactorProject;

public class SourceBundleUtils {

	private static final String SUFFIX_PROPERTY = "sourceBundleSuffix";
	public static final String SOURCE_BUNDLE_SUFFIX = ".source";
	public static final String ARTIFACT_CLASSIFIER = "sources";

	private SourceBundleUtils() {
		// no instances
	}

	public static String getSourceBundleSuffix(ReactorProject bundleProject) {
		String packaging = bundleProject.getPackaging();
		if (!(ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals(packaging) || ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN
				.equals(packaging))) {
			return null;
		}
		// TODO should rather use MavenSession to get effective properties?
		String suffix = System.getProperty(SUFFIX_PROPERTY);
		if (suffix != null) {
			return suffix;
		} 
		suffix = bundleProject.getProperties().getProperty(SUFFIX_PROPERTY);
		if (suffix != null) {
			return suffix;
		} 
		return SOURCE_BUNDLE_SUFFIX;
	}

}
