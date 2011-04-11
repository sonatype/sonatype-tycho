package org.eclipse.tycho.utils;

public class MavenArtifactRef {
	private String groupId;
	private String artifactId;
	private String version;
	private boolean optional;
	public String getGroupId() {
		return groupId;
	}
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	public String getArtifactId() {
		return artifactId;
	}
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public boolean isOptional() {
		return optional;
	}
	public void setOptional(boolean optional) {
		this.optional = optional;
	}
}
