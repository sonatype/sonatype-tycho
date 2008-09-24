package org.codehaus.tycho.eclipsepackaging;

public class Plugin {

	private String artifactId;

	public Plugin() {
		super();
	}

	public Plugin(String artifactId, String version) {
		this();
		this.artifactId = artifactId;
		this.version = version;
	}

	private String version;

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

}
