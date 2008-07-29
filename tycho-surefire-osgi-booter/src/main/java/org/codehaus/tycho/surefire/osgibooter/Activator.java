package org.codehaus.tycho.surefire.osgibooter;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private static BundleContext context;

	public Activator() {
		// TODO Auto-generated constructor stub
	}

	public void start(BundleContext context) throws Exception {
		Activator.context = context;
		
	}

	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		
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
