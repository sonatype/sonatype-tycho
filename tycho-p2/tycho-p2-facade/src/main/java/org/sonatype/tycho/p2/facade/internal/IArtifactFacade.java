package org.sonatype.tycho.p2.facade.internal;

import java.io.File;

/**
 * Facade which provides an interface for common properties of a maven {@see
 * Artifact} or {@see MavenProject}. Needed to generate p2 metadata {@see
 * P2Generator} for both reactor projects and binary artifacts. For
 * eclipse-plugin reactor projects, also carries information about the
 * corresponding eclipse source bundle.
 */
public interface IArtifactFacade {

	public File getLocation();

	public String getGroupId();

	public String getArtifactId();

	public String getVersion();

	public String getPackagingType();

	/**
	 * Suffix which will be appended to the symbolc name of the bundle to
	 * generate the symbolic name of the corresponding source bundle. {@see
	 * OsgiSourceMojo#sourceBundleSuffix}. May return <code>null</code> if
	 * {@link #hasSourceBundle() } == false.
	 */
	public String getSourceBundleSuffix();

	/**
	 * whether a source bundle will be generated for this artifact.
	 */
	public boolean hasSourceBundle();

}
