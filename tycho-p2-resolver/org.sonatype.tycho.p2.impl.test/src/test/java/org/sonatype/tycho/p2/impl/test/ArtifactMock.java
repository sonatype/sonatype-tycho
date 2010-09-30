package org.sonatype.tycho.p2.impl.test;

import java.io.File;

import org.sonatype.tycho.p2.IArtifactFacade;

public class ArtifactMock implements IArtifactFacade {


	public ArtifactMock(File location, String groupId, String artifactId,
			String version, String packagingType, String sourceBundleSuffix, boolean hasSourceBundle) {
		this.location = location;
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.packagingType = packagingType;
		this.sourceBundleSuffix = sourceBundleSuffix;
		this.hasSourceBundle  = hasSourceBundle;
	}

	public ArtifactMock(File location, String groupId, String artifactId,
			String version, String packagingType) {
		this(location, groupId, artifactId, version, packagingType,
		     ".source", false);
	}

	private File location;
	private String groupId;
	private String artifactId;
	private String version;
	private String packagingType;
	private boolean hasSourceBundle = false;
	private String sourceBundleSuffix;

	public File getLocation() {
		return location;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public String getPackagingType() {
		return packagingType;
	}

	public String getSourceBundleSuffix() {
		return sourceBundleSuffix;
	}

	public boolean hasSourceBundle() {
		return hasSourceBundle;
	}

}
