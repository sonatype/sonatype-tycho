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