package org.codehaus.tycho.eclipsepackaging;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.osgi.framework.Version;

public class VersionExpander {

	private static final String QUALIFIER = "qualifier";

	/**
	 * Enforces the following conventions
	 * 
	 * For release artifacts, maven and eclipse versions match literally.
	 * 
	 * For snapshot artifacts, base version is the same, maven version has -SNAPSHOT
	 * and eclipse version has .qualifier
	 */
	public void validateVersion(MavenProject project, Version version) throws MojoExecutionException {
//		Artifact artifact = project.getArtifact();
//		boolean valid;
//		if (artifact.isSnapshot()) {
//			Version baseVersion = Version.parseVersion(getBaseVersion(artifact));
//			valid =  baseVersion.getMajor() == version.getMajor()
//				&& baseVersion.getMinor() == version.getMinor()
//				&& baseVersion.getMicro() == version.getMicro()
//				&& QUALIFIER.equals(version.getQualifier());
//		} else {
//			Version artifactVersion = Version.parseVersion(artifact.getVersion());
//			valid = artifactVersion.equals(version);
//		}
//		if (!valid) {
//			// XXX better error message
//			throw new MojoExecutionException("Inconsistent maven and eclipse versions " + artifact.getId());
//		}
	}

	private String getBaseVersion(Artifact artifact) {
		String version = artifact.getBaseVersion();
		return version.substring(0, version.length() - Artifact.SNAPSHOT_VERSION.length() - 1);
	}

	/**
	 * Returns true is version is a snapshot version, i.e. qualifier is ".qualifier"
	 */
	public boolean isSnapshotVersion(Version version) {
		return QUALIFIER.equals(version.getQualifier());
	}

	public Version expandVersion(Version version, String qualifier) {
		if (isSnapshotVersion(version)) {
			return new Version(version.getMajor(), version.getMinor(), version.getMicro(), qualifier);
		}
		
		return version;
	}
}
