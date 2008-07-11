/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.codehaus.tycho.p2.app;

import org.eclipse.equinox.internal.p2.core.ProvisioningEventBus;
import org.eclipse.equinox.internal.p2.director.SimpleDirector;
import org.eclipse.equinox.internal.p2.director.SimplePlanner;
import org.eclipse.equinox.internal.p2.engine.MetadataCache;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.p2.garbagecollector.GarbageCollector;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Copy&paste from Activator of o.e.e.p2.exemplarysetup bundle.
 * 
 * @author igor
 */
@SuppressWarnings("restriction")
public class Activator implements BundleActivator {
	public static BundleContext context;

	private IProvisioningEventBus bus;
	private ServiceRegistration registrationBus;

	private MetadataRepositoryManager defaultManager;
	private ServiceRegistration registrationDefaultManager;

	//	private ArtifactRepositoryManager artifactRepoManager;
	//	private ServiceRegistration registrationArtifactRepoManager;

	private IProfileRegistry profileRegistry;
	private ServiceRegistration registrationProfileRegistry;

	private IDirector director;
	private ServiceRegistration registrationDirector;

	private IPlanner planner;
	private ServiceRegistration registrationPlanner;

	public void start(BundleContext aContext) throws Exception {
		//Need to do the configuration of all the bits and pieces:
		Activator.context = aContext;

		registerEventBus();
		//create the profile registry
		registerProfileRegistry();
		//create metadata repositories
		registerDefaultMetadataRepoManager();
		registerMetadataCache();

		//create the director and planner.  The planner must be
		//registered first because the director finds it in its constructor.
		registerPlanner();
		registerDirector();
		startGarbageCollector();

		//create artifact repositories
		//		registerDefaultArtifactRepoManager();
	}

	private void startGarbageCollector() {
		new GarbageCollector();
	}

	public void stop(BundleContext aContext) throws Exception {
		//		unregisterDefaultArtifactRepoManager();
		unregisterDirector();
		unregisterPlanner();
		unregisterDefaultMetadataRepoManager();
		unregisterProfileRegistry();
		unregisterEventBus();
		Activator.context = null;

	}

	private void registerDirector() {
		director = new SimpleDirector();
		registrationDirector = context.registerService(IDirector.class.getName(), director, null);
	}

	private void unregisterDirector() {
		registrationDirector.unregister();
		registrationDirector = null;
		director = null;
	}

	private void registerPlanner() {
		planner = new SimplePlanner();
		registrationPlanner = context.registerService(IPlanner.class.getName(), planner, null);
	}

	private void unregisterPlanner() {
		registrationPlanner.unregister();
		registrationPlanner = null;
		planner = null;
	}

	private void registerProfileRegistry() {
		profileRegistry = new SimpleProfileRegistry();
		registrationProfileRegistry = context.registerService(IProfileRegistry.class.getName(), profileRegistry, null);
	}

	private void unregisterProfileRegistry() {
		registrationProfileRegistry.unregister();
		registrationProfileRegistry = null;
		profileRegistry = null;
	}

	private void registerDefaultMetadataRepoManager() {
		defaultManager = new MetadataRepositoryManager();
		registrationDefaultManager = context.registerService(IMetadataRepositoryManager.class.getName(), defaultManager, null);
	}

	private void unregisterDefaultMetadataRepoManager() {
		registrationDefaultManager.unregister();
		registrationDefaultManager = null;
		defaultManager = null;
	}

	//	private void registerDefaultArtifactRepoManager() {
	//		artifactRepoManager = new ArtifactRepositoryManager();
	//		registrationArtifactRepoManager = context.registerService(IArtifactRepositoryManager.class.getName(), artifactRepoManager, null);
	//	}
	//
	//	private void unregisterDefaultArtifactRepoManager() {
	//		registrationArtifactRepoManager.unregister();
	//		artifactRepoManager = null;
	//	}

	private void registerMetadataCache() {
		//TODO: we need to start metadata cache at a specific time, because it relies on IMetadataRepositoryManager
		//being present. Really, MetadataCache should just wait until metadata repository service
		//is available and then initialize itself
		new MetadataCache();
	}

	private void registerEventBus() {
		bus = new ProvisioningEventBus();
		registrationBus = context.registerService(IProvisioningEventBus.SERVICE_NAME, bus, null);
	}

	private void unregisterEventBus() {
		registrationBus.unregister();
		registrationBus = null;
		bus.close();
	}

	public static BundleContext getContext() {
		return context;
	}
}
