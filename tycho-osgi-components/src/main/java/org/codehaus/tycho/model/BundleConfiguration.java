package org.codehaus.tycho.model;

import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * This object represents information of a bundle. This class is a copy of the
 * BundleInfo class in org.eclipse.equinox.simpleconfigurator
 * 
 */
public class BundleConfiguration {
	public static int NO_STARTLEVEL = -1; 
	Xpp3Dom configuration = null;

	public BundleConfiguration(Xpp3Dom config) {
		configuration = config;
	}
	
	public boolean isStarted() {
		return Boolean.parseBoolean(configuration.getAttribute("autoStart"));
	}
	
	public String getId() {
		return configuration.getAttribute("id");
	}
	
	public int getStartLevel() {
		String sl = configuration.getAttribute("startLevel");
		if (sl != null)
			return Integer.decode(sl).intValue();
		return -1;
	}
}
