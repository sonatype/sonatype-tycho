package org.codehaus.tycho.surefire.osgibooter;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public static final String PLUGIN_ID = "org.codehaus.tycho.surefire.osgibooter";
	private static BundleContext context;

	public Activator() {
	}

	public void start(BundleContext context) throws Exception {
		Activator.context = context;
	}

	public void stop(BundleContext context) throws Exception {
	}

	public static Bundle getBundle(String symbolicName) {
		for (Bundle bundle : context.getBundles()) {
			if (bundle.getSymbolicName().equals(symbolicName)) {
				return bundle;
			}
		}
		return null;
	}
}
