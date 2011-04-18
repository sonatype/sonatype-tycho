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
package org.eclipse.tycho.p2.impl.test;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    private static BundleContext context;

    public void start(BundleContext context) throws Exception {
        this.context = context;
        for (Bundle bundle : context.getBundles()) {
            if ("org.eclipse.equinox.p2.exemplarysetup".equals(bundle.getSymbolicName())) {
                bundle.start(Bundle.START_TRANSIENT);
            }
        }
    }

    public void stop(BundleContext context) throws Exception {
    }

    public static BundleContext getContext() {
        return context;
    }
}
