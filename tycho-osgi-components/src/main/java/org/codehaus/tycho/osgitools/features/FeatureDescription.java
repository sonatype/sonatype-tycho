package org.codehaus.tycho.osgitools.features;

import java.io.File;

import org.codehaus.tycho.model.Feature;
import org.osgi.framework.Version;

public interface FeatureDescription {

	String getId();

	Version getVersion();

	File getLocation();

	Feature getFeature();

}
