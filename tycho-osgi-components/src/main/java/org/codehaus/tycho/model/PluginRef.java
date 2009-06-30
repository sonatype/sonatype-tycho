/**
 * 
 */
package org.codehaus.tycho.model;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class PluginRef {

	private final Xpp3Dom dom;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		result = prime * result
				+ ((getVersion() == null) ? 0 : getVersion().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof PluginRef))
			return false;
		PluginRef other = (PluginRef) obj;
		if (getId() == null) {
			if (other.getId() != null)
				return false;
		} else if (!getId().equals(other.getId()))
			return false;
		if (getVersion() == null) {
			if (other.getVersion() != null)
				return false;
		} else if (!getVersion().equals(other.getVersion()))
			return false;
		return true;
	}

	public PluginRef(Xpp3Dom dom) {
		this.dom = dom;
	}

	public PluginRef(String name) {
        this.dom = new Xpp3Dom(name);
    }

	public String getId() {
		return dom.getAttribute("id");
	}
	
	public void setId(String id) {
        dom.setAttribute("id", id);
	}

	public String getVersion() {
		return dom.getAttribute("version");
	}

    public void setVersion(String version) {
        dom.setAttribute("version", version);
    }

	public String getOs() {
		return dom.getAttribute("os");
	}

	public String getWs() {
		return dom.getAttribute("ws");
	}

	public String getArch() {
		return dom.getAttribute("arch");
	}

	public boolean isUnpack() {
		return Boolean.parseBoolean(dom.getAttribute("unpack"));
	}

	public void setUnpack(boolean unpack) {
	    dom.setAttribute( "unpack", Boolean.toString( unpack ) );
	}

	public void setDownloadSide(long size) {
		dom.setAttribute("download-size", Long.toString(size));
	}

	public void setInstallSize(long size) {
		dom.setAttribute("install-size", Long.toString(size));
	}

	public void setMavenGroupId(String groupId) {
		dom.setAttribute("maven-groupId", groupId);
	}

	public String getMavenGroupId() {
		return dom.getAttribute("maven-groupId");
	}

	public void setMavenBaseVersion(String version) {
		dom.setAttribute("maven-baseVersion", version);
	}

	public String getMavenVersion() {
		return dom.getAttribute("maven-baseVersion");
	}

	@Override
	public String toString()
	{
	    return getId() + "_" + getVersion();
	}
}