/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
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