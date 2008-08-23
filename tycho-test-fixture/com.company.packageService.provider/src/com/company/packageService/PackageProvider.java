package com.company.packageService;

public class PackageProvider {
	public static String getFragmentClassName() throws ClassNotFoundException {
		return "com.company.packageService.FragmentProvider";
	}
	public static Class getFragmentClass() throws ClassNotFoundException {
		ClassLoader loader = PackageProvider.class.getClassLoader();
		return loader.loadClass(getFragmentClassName());
	}
	public static String providerName() {
		return PackageProvider.class.getName();
	}
}