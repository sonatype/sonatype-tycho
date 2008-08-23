package com.company.packageService.user;

import com.company.packageService.PackageProvider;

public class Main {
	public static void main(String[] args) throws ClassNotFoundException {
		String result = PackageProvider.providerName();
		if (null == result) {
			throw new Error("null PackageProvider result");
		}
		Class c = getFragmentClass();
		
		if (null == c) {
			throw new Error("null fragment class: " + getFragmentClassName());
		}
	}
	// copied from provider
	public static String getFragmentClassName() {
		return "com.company.packageService.FragmentProvider";
	}
	public static Class getFragmentClass() throws ClassNotFoundException {
		ClassLoader loader = PackageProvider.class.getClassLoader();
		return loader.loadClass(getFragmentClassName());
	}
}