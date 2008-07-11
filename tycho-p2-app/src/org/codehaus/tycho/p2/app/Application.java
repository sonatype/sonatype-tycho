package org.codehaus.tycho.p2.app;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

@SuppressWarnings("restriction")
public class Application implements IApplication {

	private ArrayList<ServiceReference> serviceReferences = new ArrayList<ServiceReference>();
	private IDirector director;
	private IMetadataRepositoryManager metadataRepoMan;
	private IArtifactRepositoryManager artifactRepoMan;
	private IProfileRegistry profileRegistry;
	private BundleContext bundleContext;
	private IProgressMonitor monitor;

	public Object start(IApplicationContext context) throws Exception {
		Map<String, Object> args = (Map<String, Object>) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);

		/*
		 * ~/.m2/p2/             <= -p2base
		 *  |- p2/
		 *  |- pool/             <= bundle pool
		 *  \- targets/
		 *    |- id1/                <= profile install folder
		 *      \- configuration     <= profile configuration
		 *    ...
		 *    \- idN/
		 *      \- configuration
		 */

		bundleContext = Activator.getContext();
		getServices();

		List<String> repositories = (List<String>) args.get("-repositories");
		for (String repository : repositories) {
			addRepository(new URL(repository));
		}

		String p2base = (String) args.get("-p2base");
		String profileName = (String) args.get("-profile");

		IProfile profile = profileRegistry.getProfile(profileName);
		if (profile == null) {
			String profileDir = getProfileDir(p2base, profileName);
			
			Map<String, String> properties = new HashMap<String, String>();
			properties.put(IProfile.PROP_INSTALL_FOLDER, profileDir);
			properties.put(IProfile.PROP_CONFIGURATION_FOLDER, profileDir + "/configuration");
			properties.put(IProfile.PROP_ENVIRONMENTS, (String) args.get("-environments"));
			properties.put(IProfile.PROP_FLAVOR, "tooling");
			properties.put(IProfile.PROP_CACHE, p2base + "/pool");
			properties.put(IProfile.PROP_INSTALL_FEATURES, "true");
//			properties.put("eclipse.touchpoint.launcherName", "eclipse.exe");

			profile = profileRegistry.addProfile(profileName, properties);
		} else {
			//	validateProfile();
			
		}

		Object logger = args.get("-logger");
		monitor = (logger != null)? new PlexusProgressMonitor(logger): new NullProgressMonitor();

		String profileDir = (String) profile.getProperties().get(IProfile.PROP_INSTALL_FOLDER);

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		ArrayList<IInstallableUnit> rootIUs = new ArrayList<IInstallableUnit>();
		for (Map.Entry<String, String> rootIU : ((Map<String, String>) args.get("-rootIUs")).entrySet()) {
			rootIUs.add(getInstallableUnit(rootIU.getKey(), rootIU.getValue()));
		}

		// make sure p2 creates basic platform configuration
		rootIUs.add(getInstallableUnit("tooling.org.eclipse.update.feature.default", null));
		rootIUs.add(getInstallableUnit("tooling.osgi.bundle.default", null));
		rootIUs.add(getInstallableUnit("toolingorg.eclipse.equinox.launcher", null));
		rootIUs.add(getInstallableUnit("toolingorg.eclipse.equinox.simpleconfigurator", null));

		request.addInstallableUnits(rootIUs.toArray(new IInstallableUnit[rootIUs.size()]));


		IStatus status = director.provision(request, null, monitor);
		if (!status.isOK())
			throw new CoreException(status);

		ungetServicers();
		return profileDir;
	}

	private String getProfileDir(String p2base, String profileName) {
		return p2base + "/targets/" + profileName.hashCode();
	}

	private IInstallableUnit getInstallableUnit(String id, String versionStr) {
		Version version = Version.parseVersion(versionStr);
		VersionRange range = VersionRange.emptyRange;
		if (version != null && !version.equals(Version.emptyVersion))
			range = new VersionRange(version, true, version, true);
		Query query = new InstallableUnitQuery(id, range);
		Collector collector = new Collector();
		Iterator matches = metadataRepoMan.query(query, collector, null).iterator();
		// pick the newest match
		IInstallableUnit newest = null;
		while (matches.hasNext()) {
			IInstallableUnit candidate = (IInstallableUnit) matches.next();
			if (newest == null || (newest.getVersion().compareTo(candidate.getVersion()) < 0))
				newest = candidate;
		}
		if (newest == null) {
			throw new RuntimeException("Can't find root IU " + id + "/" + range.toString());
		}
		return newest;
	}

	private void addRepository(URL url) throws ProvisionException {
		metadataRepoMan.loadRepository(url, monitor);
		artifactRepoMan.loadRepository(url, monitor);
	}

	public void stop() {
		// TODO Auto-generated method stub

	}

	private void ungetServicers() {
		for (ServiceReference sr :  serviceReferences) {
			bundleContext.ungetService(sr);
		}
		serviceReferences.clear();
	}

	private Object getService(String name) throws CoreException {
		ServiceReference ref = bundleContext.getServiceReference(name);
		if (ref == null)
			throw new RuntimeException("Can't find service " + name);
		Object service = bundleContext.getService(ref);
		if (service == null)
			throw new RuntimeException("Can't find service " + name);
		serviceReferences.add(ref);
		return service;
	}

	private void getServices() throws CoreException {
		//obtain required services
		ungetServicers();
		director = (IDirector) getService(IDirector.class.getName());
		metadataRepoMan = (IMetadataRepositoryManager) getService(IMetadataRepositoryManager.class.getName());
		artifactRepoMan = (IArtifactRepositoryManager) getService(IArtifactRepositoryManager.class.getName());
		profileRegistry = (IProfileRegistry) getService(IProfileRegistry.class.getName());
	}

}
