/*******************************************************************************
 * Copyright (c)  2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.codehaus.tycho.osgitools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.tycho.osgitools.utils.PlatformPropertiesUtils;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import copy.org.eclipse.core.runtime.internal.adaptor.PluginConverterImpl;

/**
 * @plexus.component role="org.codehaus.tycho.osgitools.OsgiState"
 */
public class OsgiStateController extends AbstractLogEnabled implements OsgiState {
	
	// Filter properties
	public final static String OSGI_WS = "osgi.ws"; //$NON-NLS-1$

	public final static String OSGI_OS = "osgi.os"; //$NON-NLS-1$

	public final static String OSGI_ARCH = "osgi.arch"; //$NON-NLS-1$

	public final static String OSGI_NL = "osgi.nl"; //$NON-NLS-1$

	public final static String ANY = "*"; //$NON-NLS-1$

	private static final String PROFILE_EXTENSION = ".profile"; //$NON-NLS-1$

	public final static String SYSTEM_PACKAGES = "org.osgi.framework.system.packages"; //$NON-NLS-1$

	/** maven project bundle user property */
	private static final String PROP_MAVEN_PROJECT = "MavenProject";

	private static final String PROP_MANIFEST = "BundleManifest";

	private StateObjectFactory factory = StateObjectFactory.defaultFactory;

	private State state;

	private long id = 0;

	private Map/* <Long, String> */patchBundles;

	private File outputDir;

	private Properties platformProperties;

	public static BundleDescription[] getDependentBundles(BundleDescription root) {
		if (root == null)
			return new BundleDescription[0];
		BundleDescription[] imported = getImportedBundles(root);
		BundleDescription[] required = getRequiredBundles(root);
		BundleDescription[] dependents = new BundleDescription[imported.length
				+ required.length];
		System.arraycopy(imported, 0, dependents, 0, imported.length);
		System.arraycopy(required, 0, dependents, imported.length,
				required.length);
		return dependents;
	}

	public static BundleDescription[] getImportedBundles(BundleDescription root) {
		if (root == null)
			return new BundleDescription[0];
		ExportPackageDescription[] packages = root.getResolvedImports();
		ArrayList/* <BundleDescription> */resolvedImports = new ArrayList/* <BundleDescription> */(
				packages.length);
		for (int i = 0; i < packages.length; i++)
			if (!root.getLocation().equals(
					packages[i].getExporter().getLocation())
					&& !resolvedImports.contains(packages[i].getExporter()))
				resolvedImports.add(packages[i].getExporter());
		return (BundleDescription[]) resolvedImports
				.toArray(new BundleDescription[resolvedImports.size()]);
	}

	public static BundleDescription[] getRequiredBundles(BundleDescription root) {
		if (root == null)
			return new BundleDescription[0];
		return root.getResolvedRequires();
	}

	public OsgiStateController() {
		patchBundles = new HashMap();
	}

	private void loadTargetPlatform(File platform) {
		getLogger().info("Using " + platform.getAbsolutePath() + " eclipse target platform");

		EclipsePluginPathFinder finder = new EclipsePluginPathFinder();

		Set<File> bundles = finder.getPlugins(platform);

		if (bundles == null || bundles.size() == 0) {
			throw new RuntimeException("No bundles found!");
		}

		getLogger().info("Found " + bundles.size() + " bundles");

		for (File bundle : bundles) {
			try {
				addBundle(bundle);
			} catch (BundleException e) {
				getLogger().info("Could not add bundle: " + bundle);
			}
		}
	}

	private long getNextId() {
		return ++id;
	}

	public BundleDescription addBundle(File bundleLocation)
			throws BundleException {
		if (bundleLocation == null || !bundleLocation.exists())
			throw new IllegalArgumentException("bundleLocation not found: "
					+ bundleLocation);
		Dictionary manifest = loadManifestAttributes(bundleLocation);
		if (manifest == null)
			throw new BundleException("manifest not found in " + bundleLocation);
		return addBundle(manifest, bundleLocation, false);
	}

	public BundleDescription addBundle(File manifestLocation,
			File bundleLocation, boolean override) throws BundleException {
		if (bundleLocation == null || !bundleLocation.exists())
			throw new IllegalArgumentException("bundleLocation not found: "
					+ bundleLocation);
		Dictionary manifest = loadManifestAttributes(manifestLocation);
		if (manifest == null)
			throw new IllegalArgumentException("manifest not found in "
					+ manifestLocation);
		return addBundle(manifest, bundleLocation, override);
	}

	private Dictionary loadManifestAttributes(File bundleLocation) throws BundleException {
		Manifest m = loadManifest(bundleLocation);
		if (m == null) {
			return null;
		}

		Dictionary manifest = manifestToProperties(m.getMainAttributes());

		// enforce symbolic name
		if (manifest.get(Constants.BUNDLE_SYMBOLICNAME) == null) {
			// TODO maybe derive symbolic name from artifactId/groupId if we
			// have them?
			return null;
		}

		// enforce bundle classpath
		if (manifest.get(Constants.BUNDLE_CLASSPATH) == null) {
			manifest.put(Constants.BUNDLE_CLASSPATH, "."); //$NON-NLS-1$
		}

		return manifest;
	}

	// Return a dictionary representing a manifest. The data may result from
	// plugin.xml conversion
	public Manifest loadManifest(File bundleLocation) {
		InputStream manifestStream = null;
		ZipFile jarFile = null;
		try {
			if (bundleLocation.isFile()) {
				String name = bundleLocation.getName();
				if (name.toLowerCase().endsWith(".jar")) {
					jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
					ZipEntry manifestEntry = jarFile
							.getEntry(JarFile.MANIFEST_NAME);
					if (manifestEntry != null) {
						manifestStream = jarFile.getInputStream(manifestEntry);
					} else {
						File converted = convertPluginManifest(bundleLocation);
						manifestStream = new FileInputStream(new File(converted,
								JarFile.MANIFEST_NAME));
					}
				} else {
					manifestStream = new FileInputStream(bundleLocation);
				}
			} else {
				manifestStream = new FileInputStream(new File(bundleLocation,
						JarFile.MANIFEST_NAME));
			}
			
			if (manifestStream != null) {
				return new Manifest(manifestStream);
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		} catch (PluginConversionException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		} finally {
			
		}
		return null;
	}

	private File convertPluginManifest(File bundleLocation) throws PluginConversionException {
		PluginConverterImpl converter = new PluginConverterImpl(null, null);
		String name = bundleLocation.getName();
		if (name.endsWith(".jar")) {
			name = name.substring(0, name.length() - 4);
		}
		File manifestFile = new File(outputDir, name + "/META-INF/MANIFEST.MF");
		manifestFile.getParentFile().mkdirs();
		converter.convertManifest(
				bundleLocation,
				manifestFile,
				false /*compatibility*/, 
				"3.2" /*target version*/, 
				false /*don't analyse jars to set export-package*/,
				null /*devProperties*/);
		if (manifestFile.exists()) {
			return manifestFile.getParentFile().getParentFile();
		}
		return null;
	}

	private Properties manifestToProperties(Attributes d) {
		Iterator iter = d.keySet().iterator();
		Properties result = new Properties();
		while (iter.hasNext()) {
			Attributes.Name key = (Attributes.Name) iter.next();
			result.put(key.toString(), d.get(key));
		}
		return result;
	}

	private String fillPatchData(Dictionary manifest) {
		if (manifest.get("Eclipse-ExtensibleAPI") != null) {
			return "Eclipse-ExtensibleAPI: true";
		}

		if (manifest.get("Eclipse-PatchFragment") != null) {
			return "Eclipse-PatchFragment: true";
		}
		return null;
	}

	private BundleDescription addBundle(Dictionary enhancedManifest,
			File bundleLocation, boolean override) throws BundleException {
		// TODO Qualifier Replacement. do we do this for maven?
		// updateVersionNumber(enhancedManifest);
		BundleDescription descriptor;
		descriptor = factory.createBundleDescription(state, enhancedManifest,
				bundleLocation.getAbsolutePath(), getNextId());
		String patchValue = fillPatchData(enhancedManifest);
		if (patchValue != null)
			patchBundles.put(new Long(descriptor.getBundleId()), patchValue);
		// rememberQualifierTagPresence(descriptor);

		setUserProperty(descriptor, PROP_MANIFEST, enhancedManifest);

		if (override) {
			BundleDescription[] conflicts = state.getBundles(descriptor.getSymbolicName());
			if (conflicts != null) {
				for (BundleDescription conflict : conflicts) {
					state.removeBundle(conflict);
					getLogger().warn(conflict.toString() + " has been replaced by another bundle with the same symbolic name " + descriptor.toString());
				}
			}
		}

		state.addBundle(descriptor);
		return descriptor;
	}

	public StateHelper getStateHelper() {
		return state.getStateHelper();
	}

	public Map getPatchData() {
		return patchBundles;
	}

	public BundleDescription getResolvedBundle(String bundleId) {
		BundleDescription[] description = state.getBundles(bundleId);
		if (description == null)
			return null;
		for (int i = 0; i < description.length; i++) {
			if (description[i].isResolved())
				return description[i];
		}
		return null;
	}

	public void resolveState() {
		if (platformProperties != null) {
			state.setPlatformProperties(platformProperties);
		}
		state.resolve(false);
	}

	private File getOSGiLocation() {
		BundleDescription osgiBundle = state
				.getBundle("org.eclipse.osgi", null); //$NON-NLS-1$
		if (osgiBundle == null)
			return null;
		return new File(osgiBundle.getLocation());
	}

	private String[] getJavaProfiles() {
		String[] javaProfiles;
		File osgiLocation = getOSGiLocation();
		if (osgiLocation == null)
			return null;
		if (osgiLocation.isDirectory())
			javaProfiles = getDirJavaProfiles(osgiLocation);
		else
			javaProfiles = getJarJavaProfiles(osgiLocation);
		return javaProfiles;
	}

	private Properties getJavaProfileProperties() {
		String[] javaProfiles = getJavaProfiles();
		String profile;
		if (javaProfiles != null && javaProfiles.length > 0)
			profile = javaProfiles[0];
		else
			return null;

		File location = getOSGiLocation();
		if (location == null)
			return null;
		InputStream is = null;
		ZipFile zipFile = null;
		try {
			if (location.isDirectory()) {
				is = new FileInputStream(new File(location, profile));
			} else {
				zipFile = null;
				try {
					zipFile = new ZipFile(location, ZipFile.OPEN_READ);
					ZipEntry entry = zipFile.getEntry(profile);
					if (entry != null)
						is = zipFile.getInputStream(entry);
				} catch (IOException e) {
					// nothing to do
				}
			}
			Properties props = new Properties();
			props.load(is);
			return props;
		} catch (IOException e) {
			// nothing to do
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					// nothing to do
				}
			if (zipFile != null)
				try {
					zipFile.close();
				} catch (IOException e) {
					// nothing to do
				}
		}
		return null;
	}

	private String[] getDirJavaProfiles(File bundleLocation) {
		// try the profile list first
		File profileList = new File(bundleLocation, "profile.list");
		if (profileList.exists())
			try {
				return getJavaProfiles(new FileInputStream(profileList));
			} catch (IOException e) {
				// this should not happen because we just checked if the file
				// exists
			}
		String[] profiles = bundleLocation.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(PROFILE_EXTENSION);
			}
		});
		return sortProfiles(profiles);
	}

	private String[] sortProfiles(String[] profiles) {
		Arrays.sort(profiles, new Comparator() {
			public int compare(Object o1, Object o2) {
					String p1 = (String) o1;
					String p2 = (String) o2;

					// need to make sure J2SE profiles are sorted ahead of all
					// other
					// profiles
					if (p1.startsWith("J2SE") && !p2.startsWith("J2SE"))
						return -1;
					if (!p1.startsWith("J2SE") && p2.startsWith("J2SE"))
						return 1;
					return -p1.compareTo(p2);
			}
		});
		return profiles;
	}

	private String[] getJarJavaProfiles(File bundleLocation) {
        ZipFile zipFile = null;
        ArrayList results = new ArrayList(6);
        try {
            zipFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
            ZipEntry profileList = zipFile.getEntry("profile.list");
            if (profileList != null)
                try {
                    return getJavaProfiles(zipFile.getInputStream(profileList));
                }
                catch (IOException e) {
                    // this should not happen, just incase do the default
                }

            Enumeration entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                String entryName = ((ZipEntry) entries.nextElement()).getName();
                if (entryName.indexOf('/') < 0
                        && entryName.endsWith(PROFILE_EXTENSION))
                    results.add(entryName);
            }
        }
        catch (IOException e) {
            // nothing to do
        }
        finally {
            if (zipFile != null)
                try {
                    zipFile.close();
                }
                catch (IOException e) {
                    // nothing to do
                }
        }
        return sortProfiles((String[]) results.toArray(new String[results
                .size()]));
    }

	private String[] getJavaProfiles(InputStream is) throws IOException {
		Properties props = new Properties();
		props.load(is);
		return ManifestElement.getArrayFromList(props
				.getProperty("java.profiles"), ","); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public State getState() {
		return state;
	}

	public BundleDescription[] getBundles() {
		return state.getBundles();
	}

	public ResolverError[] getResolverErrors(BundleDescription bundle) {
		return state.getResolverErrors(bundle);
	}

	public ResolverError[] getRelevantErrors() {
        BundleDescription[] bundles = state.getBundles();
        List errors = new ArrayList();
        for (int i = 0; i < bundles.length; i++) {
            BundleDescription bundle = bundles[i];
            ResolverError[] bundleErrors = state.getResolverErrors(bundle);
            for (int j = 0; j < bundleErrors.length; j++) {
                ResolverError error = bundleErrors[j];
                VersionConstraint constraint = error.getUnsatisfiedConstraint();
                String required = constraint.getName();
                if (constraint instanceof BundleSpecification || constraint instanceof HostSpecification) {
                    if (state.getBundles(required).length == 0) {
                        errors.add(error);
                    }
                } else if (constraint instanceof ImportPackageSpecification) {
                    boolean found = false;
                    BundleDescription[] bds = state.getBundles();
                    for (int k = 0; k < bds.length; k++) {
                        BundleDescription bd = bds[k];
                        for (int l = 0; l < bd.getExportPackages().length; l++) {
							ExportPackageDescription d = bd.getExportPackages()[l];
                            if (d.getName().equals(required)) {
                                found = true;
                            }
                        }
                    }
                    if (!found) {
                        errors.add(error);
                    }
                } else {
                    errors.add(error);
                }
            }
        }

        return (ResolverError[]) errors
                .toArray(new ResolverError[errors.size()]);
    }

	public ResolverError[] getRelevantErrors(BundleDescription bundle) {
		Set errors = new LinkedHashSet();
		getRelevantErrors(errors, bundle);
        return (ResolverError[]) errors.toArray(new ResolverError[errors.size()]);
	}

	private void getRelevantErrors(Set errors, BundleDescription bundle) {
		ResolverError[] bundleErrors = state.getResolverErrors(bundle);
        for (int j = 0; j < bundleErrors.length; j++) {
            ResolverError error = bundleErrors[j];

            VersionConstraint constraint = error.getUnsatisfiedConstraint();
            String required = constraint.getName();
            
            if (constraint instanceof BundleSpecification || constraint instanceof HostSpecification) {
                BundleDescription[] requiredBundles = state.getBundles(required);
                for (int i = 0; i < requiredBundles.length; i++) {
                	getRelevantErrors(errors, requiredBundles[i]);
                }
            } else if (constraint instanceof ImportPackageSpecification) {
                boolean found = false;
                BundleDescription[] bds = state.getBundles();
                for (int k = 0; k < bds.length; k++) {
                    BundleDescription bd = bds[k];
                    for (int l = 0; l < bd.getExportPackages().length; l++) {
						ExportPackageDescription d = bd.getExportPackages()[l];
                        if (d.getName().equals(required)) {
                            found = true;
                        }
                    }
                }
                if (!found) {
                    errors.add(error);
                }
            } else {
                errors.add(error);
            }
        }
	}

	public ResolverError[] getAllErrors() {
        BundleDescription[] bundles = state.getBundles();
        Set errors = new LinkedHashSet();
        for (int i = 0; i < bundles.length; i++) {
            BundleDescription bundle = bundles[i];
            ResolverError[] bundleErrors = state.getResolverErrors(bundle);
            if (bundleErrors != null ) {
            	errors.addAll(Arrays.asList(bundleErrors));
            }
        }
        return (ResolverError[]) errors.toArray(new ResolverError[errors.size()]);
	}

	public BundleDescription[] getDependencies(BundleDescription desc) {
		Set set = new TreeSet();
		addBundleAndDependencies(desc, set, true);
		BundleDescription[] dependencies = new BundleDescription[set.size() - 1];
		int i = 0;
		for (Iterator it = set.iterator(); it.hasNext(); ) {
			long bundleId = ((Long)it.next()).longValue();
			if (desc.getBundleId() != bundleId) {
				dependencies[i] = state.getBundle(bundleId);
				i++;
			}
		}
		return dependencies;
	}
	
	// copy&paste from org.eclipse.pde.internal.core.DependencyManager
	private static void addBundleAndDependencies(BundleDescription desc, Set set, boolean includeOptional) {
		if (desc != null && set.add(new Long(desc.getBundleId()))) {
			BundleSpecification[] required = desc.getRequiredBundles();
			for (int i = 0; i < required.length; i++) {
				if (includeOptional || !required[i].isOptional())
					addBundleAndDependencies((BundleDescription) required[i].getSupplier(), set, includeOptional);
			}
			ImportPackageSpecification[] importedPkgs = desc.getImportPackages();
			for (int i = 0; i < importedPkgs.length; i++) {
				ExportPackageDescription exporter = (ExportPackageDescription) importedPkgs[i].getSupplier();
				// Continue if the Imported Package is unresolved of the package is optional and don't want optional packages
				if (exporter == null || (!includeOptional && Constants.RESOLUTION_OPTIONAL.equals(importedPkgs[i].getDirective(Constants.RESOLUTION_DIRECTIVE))))
					continue;
				addBundleAndDependencies(exporter.getExporter(), set, includeOptional);
			}
			BundleDescription[] fragments = desc.getFragments();
			for (int i = 0; i < fragments.length; i++) {
				if (!fragments[i].isResolved())
					continue;
				String id = fragments[i].getSymbolicName();
				if (!"org.eclipse.ui.workbench.compatibility".equals(id)) //$NON-NLS-1$
					addBundleAndDependencies(fragments[i], set, includeOptional);
			}
			HostSpecification host = desc.getHost();
			if (host != null)
				addBundleAndDependencies((BundleDescription) host.getSupplier(), set, includeOptional);
		}
	}

	public BundleDescription getBundleDescription(MavenProject project) {
		String location = project.getFile().getParentFile().getAbsolutePath();
		return state.getBundleByLocation(location);
	}

	public BundleDescription addBundle(MavenProject project) throws BundleException {
		File basedir = project.getBasedir();
		File mf = new File(basedir, "META-INF/MANIFEST.MF");
		if (mf.canRead()) {
			BundleDescription desc = addBundle(mf, basedir, true);
			setUserProperty(desc, PROP_MAVEN_PROJECT, project);
			return desc;
		}
		return null;
	}

	private static void setUserProperty(BundleDescription desc, String name, Object value) throws BundleException {
		Object userObject = desc.getUserObject();
		
		if (userObject != null && !(userObject instanceof Map)) {
			throw new BundleException("Unexpected user object " + desc.toString());
		}
	
		Map props = (Map) userObject;
		if (props == null) {
			props = new HashMap();
			desc.setUserObject(props);
		}
		
		props.put(name, value);
	}

	private static Object getUserProperty(BundleDescription desc, String name) {
		Object userObject = desc.getUserObject();
		if (userObject instanceof Map) {
			return ((Map) userObject).get(name);
		}
		return null;
	}

	public MavenProject getMavenProject(BundleDescription desc) {
		return (MavenProject) getUserProperty(desc, PROP_MAVEN_PROJECT);
	}

	public String getGroupId(BundleDescription desc) {
		return getManifestAttribute(desc, ATTR_GROUP_ID);
	}

	public void init(File workspace, Properties props) {
		state = factory.createState(true);
		platformProperties = new Properties();

		String property = props.getProperty("tycho.targetPlatform");
		if (property != null) {
			if (workspace != null) {
				try {
					this.outputDir = new File(workspace, "TYCHO").getCanonicalFile();
				} catch (IOException e) {
					// hmmm
				}
			}
			if (this.outputDir == null) {
				try {
					this.outputDir = File.createTempFile("TYCHO", null);
				} catch (IOException e) {
					// double hmmm
					throw new RuntimeException(e);
				}
			}
			this.outputDir.mkdirs();

			File location = new File(property);
			loadTargetPlatform(location);

			platformProperties.put(OSGI_OS, PlatformPropertiesUtils.getOS(props));
			platformProperties.put(OSGI_WS, PlatformPropertiesUtils.getWS(props));
			platformProperties.put(OSGI_ARCH, PlatformPropertiesUtils.getArch(props));
//			platformProperties.put(OSGI_NL, props.getProperty("tycho." + OSGI_NL, "en_US"));

			// Set the JRE profile
			Properties profileProps = getJavaProfileProperties();
			if (profileProps != null) {
				String systemPackages = profileProps.getProperty(SYSTEM_PACKAGES);
				if (systemPackages != null) {
					platformProperties.put(SYSTEM_PACKAGES, systemPackages);
				}
				String ee = profileProps.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
				if (ee != null) {
					platformProperties.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, ee);
				}
			}
		}
	}

	public BundleDescription getBundleDescription(String symbolicName, String version) {
		try {
			return state.getBundle(symbolicName, new Version(version));
		} catch (NumberFormatException e) {
			return null;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public void assertResolved(BundleDescription desc) throws BundleException {
		if (!desc.isResolved())
		{
			StringBuffer msg = new StringBuffer();
			msg.append("Bundle ").append(desc.getSymbolicName()).append(" cannot be resolved\n");
			msg.append("Resolution errors:\n");
			ResolverError[] errors = getResolverErrors(desc);
			for (int i = 0; i < errors.length; i++) {
				ResolverError error = errors[i];
				msg.append("   Bundle ").append(error.getBundle().getSymbolicName())
						.append(" - ").append(error.toString()).append("\n");
			}

			throw new BundleException(msg.toString());
		}
	}

	public String getManifestAttribute(BundleDescription desc, String attr) {
		Dictionary mf =  (Dictionary) getUserProperty(desc, PROP_MANIFEST);
		if (mf != null) {
			return (String) mf.get(attr);
		}
		return null;
	}
}
 