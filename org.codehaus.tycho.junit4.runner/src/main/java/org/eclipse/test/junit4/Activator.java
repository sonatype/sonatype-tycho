package org.eclipse.test.junit4;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;

public class Activator implements BundleActivator {

	private BundleContext context;
	private static Activator instance;

	public void start(BundleContext context) throws Exception {
		this.context = context;
		instance = this;
	}

	public void stop(BundleContext context) throws Exception {
	}
	
	public Class loadClass(String bundleId, String className) throws ClassNotFoundException {
		PackageAdmin packageAdmin = (PackageAdmin) context.getService(context
				.getServiceReference(PackageAdmin.class.getName()));
		if (packageAdmin == null)
			return null;
		Bundle[] bundles = packageAdmin.getBundles(bundleId, null);
		if (bundles == null)
			throw new ClassNotFoundException("Bundle " + bundleId + " not found");

		for (int i = 0; i < bundles.length; i++) {
			if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundles[i].loadClass(className);
			}
		}
		return null;
	}
	
	public static Activator getInstance() {
		return instance;
	}
}
