package org.codehaus.tycho.osgitools.features;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.tycho.model.Feature;
import org.osgi.framework.Version;

public class FeatureDescriptionImpl implements FeatureDescription {

	private final String name;

	private final Version version;

	private final File location;
	
	private final Feature feature;
	
	private final Map<String, Object> userProperties = new HashMap<String, Object>();

	public FeatureDescriptionImpl(Feature feature, File location) {
		this.feature = feature;
		this.name = feature.getId();
		this.location = location;
		this.version = new Version(feature.getVersion());
	}

	public File getLocation() {
		return this.location;
	}

	public String getId() {
		return this.name;
	}

	public Version getVersion() {
		return this.version;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof FeatureDescription))
			return false;
		FeatureDescription other = (FeatureDescription) obj;
		if (name == null) {
			if (other.getId() != null)
				return false;
		} else if (!name.equals(other.getId()))
			return false;
		if (version == null) {
			if (other.getVersion() != null)
				return false;
		} else if (!version.equals(other.getVersion()))
			return false;
		return true;
	}

	public Feature getFeature() {
		return feature;
	}

	@Override
	public String toString() {
		return name + "_" + version.toString();
	}

	public void setUserProperty(String key, Object value) {
		userProperties.put(key, value);
	}

	public Object getUserProperty(String key) {
		return userProperties.get(key);
	}

}
