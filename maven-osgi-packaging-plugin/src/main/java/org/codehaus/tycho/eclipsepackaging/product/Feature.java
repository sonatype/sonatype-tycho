package org.codehaus.tycho.eclipsepackaging.product;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("feature")
public class Feature {

	@XStreamAsAttribute
	private String id;

	@XStreamAsAttribute
	private String version;

	public Feature() {
		super();
	}

	public Feature(String id, String version) {
		this();
		this.id = id;
		this.version = version;
	}

	public String getId() {
		return id;
	}

	public String getVersion() {
		return version;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setVersion(String version) {
		this.version = version;
	}

}
