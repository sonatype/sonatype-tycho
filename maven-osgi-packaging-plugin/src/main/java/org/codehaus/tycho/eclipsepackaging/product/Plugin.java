package org.codehaus.tycho.eclipsepackaging.product;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("plugin")
public class Plugin {

	@XStreamAsAttribute
	private String id;
	
	@XStreamAsAttribute
	private Boolean fragment;

	@XStreamAsAttribute
	private String version;

	public Plugin() {
		super();
	}

	public Plugin(String id, String version) {
		this();
		this.id = id;
		this.version = version;
	}

	public String getId() {
		return id;
	}

	public Boolean getFragment() {
		return fragment;
	}

	public String getVersion() {
		return version;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setFragment(Boolean fragment) {
		this.fragment = fragment;
	}

	public void setVersion(String version) {
		this.version = version;
	}

}
