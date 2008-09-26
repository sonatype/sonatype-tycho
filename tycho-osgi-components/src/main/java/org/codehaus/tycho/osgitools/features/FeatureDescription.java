package org.codehaus.tycho.osgitools.features;

import java.io.File;

import org.osgi.framework.Version;

public interface FeatureDescription {

	String getName();

	Version getVersion();

	File getLocation();

}
