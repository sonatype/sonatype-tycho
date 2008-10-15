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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.Platform;
import org.codehaus.tycho.osgitools.features.FeatureDescription;
import org.codehaus.tycho.osgitools.features.FeatureDescriptionImpl;
import org.codehaus.tycho.osgitools.utils.ExecutionEnvironmentUtils;
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
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import copy.org.eclipse.core.runtime.internal.adaptor.PluginConverterImpl;

/**
 * @plexus.component role="org.codehaus.tycho.osgitools.OsgiState"
 */
@SuppressWarnings("unchecked")
public class OsgiStateController extends AbstractLogEnabled implements OsgiState {
	
	/** maven project bundle user property */
	private static final String PROP_MAVEN_PROJECT = "MavenProject";

	private static final String PROP_MANIFEST = "BundleManifest";

	private StateObjectFactory factory = StateObjectFactory.defaultFactory;

	private State state;

	private long id = 0;

	private File cacheDir;

	private Properties platformProperties;

	private File targetPlatform;

	private Set<FeatureDescription> features;

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
	}

	private long getNextId() {
		return ++id;
	}

	public BundleDescription addBundle(File bundleLocation) throws BundleException {
		return addBundle(bundleLocation, false);
	}
	public BundleDescription addBundle(File bundleLocation, boolean override)
			throws BundleException {
		if (bundleLocation == null || !bundleLocation.exists())
			throw new IllegalArgumentException("bundleLocation not found: "
					+ bundleLocation);
		Dictionary manifest = loadManifestAttributes(bundleLocation);
		if (manifest == null)
			throw new BundleException("manifest not found in " + bundleLocation);
		return addBundle(manifest, bundleLocation, override);
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

	private Dictionary loadManifestAttributes(File bundleLocation) {
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

	public Manifest loadManifest(File bundleLocation) {
		try {
			if (bundleLocation.isDirectory()) {
				File m = new File(bundleLocation, JarFile.MANIFEST_NAME);
				if (m.canRead()) {
					return loadManifestFile(m);
				}
				m = convertPluginManifest(bundleLocation); 
				if (m != null && m.canRead()) {
					return loadManifestFile(m);
				}
				return null;
			}
	
			// it's a file, make sure we can read it
			if (!bundleLocation.canRead()) {
				return null;
			}
	
			// file but not a jar, assume it is MANIFEST.MF
			if (!bundleLocation.getName().toLowerCase().endsWith(".jar")) {
				return loadManifestFile(bundleLocation);
			} 
	
			// it is a jar, lets see if it has OSGi bundle manifest
			ZipFile jar = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
			try {
				ZipEntry me = jar.getEntry(JarFile.MANIFEST_NAME);
				if (me != null) {
					InputStream is = jar.getInputStream(me);
					try {
						Manifest mf = new Manifest(is);
						if (mf.getMainAttributes().getValue("Bundle-SymbolicName") != null) {
							return mf;
						}
					} finally {
						is.close();
					}
				}
			} finally {
				jar.close();
			}
	
			// it is a jar, does not have OSGi bundle manifest, lets try plugin.xml/fragment.xml
			File m = convertPluginManifest(bundleLocation); 
			if (m != null && m.canRead()) {
				return loadManifestFile(m);
			}
		} catch (IOException e) {
			getLogger().warn("Exception reading bundle manifest", e);
		} catch (PluginConversionException e) {
			getLogger().warn("Exception reading bundle manifest: " + e.getMessage());
		}
		
		// not a bundle
		return null;
	}

	private Manifest loadManifestFile(File m) throws IOException {
		if (!m.canRead()) {
			return null;
		}
		InputStream is = new FileInputStream(m);
		try {
			return new Manifest(is);
		} finally {
			is.close();
		}
	}

	private File convertPluginManifest(File bundleLocation) throws PluginConversionException {
		PluginConverterImpl converter = new PluginConverterImpl(null, null);
		String name = bundleLocation.getName();
		if (name.endsWith(".jar")) {
			name = name.substring(0, name.length() - 4);
		}
		File manifestFile = new File(getManifestsDir(), name + "/META-INF/MANIFEST.MF");
		manifestFile.getParentFile().mkdirs();
		converter.convertManifest(
				bundleLocation,
				manifestFile,
				false /*compatibility*/, 
				"3.2" /*target version*/, 
				true /*analyse jars to set export-package*/,
				null /*devProperties*/);
		if (manifestFile.exists()) {
			return manifestFile;
		}
		return null;
	}

	private File getManifestsDir() {
		return new File(cacheDir, "manifests");
	}

	public File getFeatureDir(String id, String version) {
		File dir = new File(cacheDir, "features/" + id + "_" + version);
		dir.mkdirs();
		return dir;
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

	private BundleDescription addBundle(Dictionary enhancedManifest,
			File bundleLocation, boolean override) throws BundleException {

		addSite(bundleLocation.getParentFile());
		
		// TODO Qualifier Replacement. do we do this for maven?
		// updateVersionNumber(enhancedManifest);
		BundleDescription descriptor;
		descriptor = factory.createBundleDescription(state, enhancedManifest,
				bundleLocation.getAbsolutePath(), getNextId());
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

		if (getLogger().isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("Resolved OSGi state\n");
			for (BundleDescription bundle : state.getBundles()) {
				if (!bundle.isResolved()) {
					sb.append("NOT ");
				}
				sb.append("RESOLVED ");
				sb.append(bundle.toString()).append(" : ").append(bundle.getLocation());
				sb.append('\n');
				for (ResolverError error : state.getResolverErrors(bundle)) {
					sb.append('\t').append(error.toString()).append('\n');
				}
			}
			getLogger().debug(sb.toString());
		}
	}

	public State getState() {
		return state;
	}

	public BundleDescription[] getBundles() {
		return state.getBundles();
	}

	public ResolverError[] getResolverErrors(BundleDescription bundle) {
		Set<ResolverError> errors = new LinkedHashSet<ResolverError>();
		getRelevantErrors(errors, bundle);
        return (ResolverError[]) errors.toArray(new ResolverError[errors.size()]);
	}

	private void getRelevantErrors(Set<ResolverError> errors, BundleDescription bundle) {
		ResolverError[] bundleErrors = state.getResolverErrors(bundle);
        for (int j = 0; j < bundleErrors.length; j++) {
            ResolverError error = bundleErrors[j];
            errors.add(error);

            VersionConstraint constraint = error.getUnsatisfiedConstraint();
            if (constraint instanceof BundleSpecification || constraint instanceof HostSpecification) {
                BundleDescription[] requiredBundles = state.getBundles(constraint.getName());
                for (int i = 0; i < requiredBundles.length; i++) {
                	getRelevantErrors(errors, requiredBundles[i]);
                }
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
		Set<Long> bundleIds = new LinkedHashSet<Long>();
		addBundleAndDependencies(desc, bundleIds, true);
		ArrayList<BundleDescription> dependencies = new ArrayList<BundleDescription>();
		for (long bundleId : bundleIds) {
			if (desc.getBundleId() != bundleId) {
				BundleDescription dependency = state.getBundle(bundleId);
				BundleDescription supplier = dependency.getSupplier().getSupplier();
				HostSpecification host = supplier.getHost();
				if (host == null || !desc.equals(host.getSupplier())) {
					dependencies.add(dependency);
				}
			}
		}
		return dependencies.toArray(new BundleDescription[dependencies.size()]);
	}

	/**
	 * Code below is copy&paste from org.eclipse.pde.internal.core.DependencyManager
	 * which seems to calculate runtime dependencies. In particular, it adds
	 * fragments' dependencies to the host bundle (see TychoTest#testFragment unit test).
	 * This may or may not cause problems...
	 * 
	 * RequiredPluginsClasspathContainer#computePluginEntries has the logic to
	 * calculate compile-time dependencies in IDE.
	 * 
	 * TODO find the code used by PDE/Build  
	 */
	private static void addBundleAndDependencies(BundleDescription desc, Set<Long> bundleIds, boolean includeOptional) {
		if (desc != null && bundleIds.add(new Long(desc.getBundleId()))) {
			BundleSpecification[] required = desc.getRequiredBundles();
			for (int i = 0; i < required.length; i++) {
				if (includeOptional || !required[i].isOptional())
					addBundleAndDependencies((BundleDescription) required[i].getSupplier(), bundleIds, includeOptional);
			}
			ImportPackageSpecification[] importedPkgs = desc.getImportPackages();
			for (int i = 0; i < importedPkgs.length; i++) {
				ExportPackageDescription exporter = (ExportPackageDescription) importedPkgs[i].getSupplier();
				// Continue if the Imported Package is unresolved of the package is optional and don't want optional packages
				if (exporter == null || (!includeOptional && Constants.RESOLUTION_OPTIONAL.equals(importedPkgs[i].getDirective(Constants.RESOLUTION_DIRECTIVE))))
					continue;
				addBundleAndDependencies(exporter.getExporter(), bundleIds, includeOptional);
			}
			BundleDescription[] fragments = desc.getFragments();
			for (int i = 0; i < fragments.length; i++) {
				if (!fragments[i].isResolved())
					continue;
				String id = fragments[i].getSymbolicName();
				if (!"org.eclipse.ui.workbench.compatibility".equals(id)) //$NON-NLS-1$
					addBundleAndDependencies(fragments[i], bundleIds, includeOptional);
			}
			HostSpecification host = desc.getHost();
			if (host != null)
				addBundleAndDependencies((BundleDescription) host.getSupplier(), bundleIds, includeOptional);
		}
	}

	public BundleDescription getBundleDescription(MavenProject project) {
		String location = project.getFile().getParentFile().getAbsolutePath();
		return state.getBundleByLocation(location);
	}

	public BundleDescription getBundleDescription(File location) {
		String absolutePath = location.getAbsolutePath();
		return state.getBundleByLocation(absolutePath);
	}

	public void addProject(MavenProject project) throws BundleException {
		File basedir = project.getBasedir();
		if (PACKAGING_ECLIPSE_PLUGIN.equals(project.getPackaging()) 
				|| PACKAGING_ECLIPSE_TEST_PLUGIN.equals(project.getPackaging())) {
			File mf = new File(basedir, "META-INF/MANIFEST.MF");
			if (mf.canRead()) {
				BundleDescription desc = addBundle(mf, basedir, true);

				String groupId = getManifestAttribute(desc, ATTR_GROUP_ID);
				if (groupId != null && !groupId.equals(project.getGroupId())) {
					throw new BundleException("groupId speicified in bundle manifest does not match pom.xml");
				}

				setUserProperty(desc, PROP_MAVEN_PROJECT, project);
			}
		} else if (PACKAGING_ECLIPSE_FEATURE.equals(project.getPackaging())) {
			try {
				Feature feature = Feature.read(new File(basedir, Feature.FEATURE_XML));
				feature.setUserProperty(PROP_MAVEN_PROJECT, project);
				File location = project.getFile().getParentFile().getAbsoluteFile();
				addFeature(location, feature);
			} catch (Exception e) {
				throw new BundleException("Exception reading eclipse feature", e);
			}
		}
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
		MavenProject mavenProject = getMavenProject(desc);
		if (mavenProject != null) {
			return mavenProject.getGroupId();
		}
		return getManifestAttribute(desc, ATTR_GROUP_ID);
	}

	public void reset(Properties props) {
		features = new LinkedHashSet<FeatureDescription>();
		sites = new LinkedHashSet<String>();

		state = factory.createState(true);

		platformProperties = new Properties(props);
		platformProperties.put(PlatformPropertiesUtils.OSGI_OS, PlatformPropertiesUtils.getOS(platformProperties));
		platformProperties.put(PlatformPropertiesUtils.OSGI_WS, PlatformPropertiesUtils.getWS(platformProperties));
		platformProperties.put(PlatformPropertiesUtils.OSGI_ARCH, PlatformPropertiesUtils.getArch(platformProperties));
//			platformProperties.put(OSGI_NL, props.getProperty("tycho." + OSGI_NL, "en_US"));

		// Set the JRE profile
		ExecutionEnvironmentUtils.loadVMProfile(platformProperties);

		initCacheDir(props);

		this.targetPlatform = targetPlatform;

	}

	public void dump() {
//		getLogger().info("Bundles count " + bundles.size());
//		if (getLogger().isDebugEnabled()) {
//			StringBuilder sb = new StringBuilder();
//			for (Map.Entry<File, Set<File>> site : bundles.entrySet()) {
//				sb.append("Site ").append(site.getKey().getAbsolutePath()).append('\n');
//				for (File bundle : site.getValue()) {
//					sb.append('\t').append(bundle.getAbsolutePath()).append('\n');
//				}
//			}
//			getLogger().debug(sb.toString());
//		}
//
//		getLogger().info("Features count " + features.size());
//		if (getLogger().isDebugEnabled()) {
//			StringBuilder sb = new StringBuilder();
//			for (Map.Entry<File, Set<File>> site : features.entrySet()) {
//				sb.append("Site ").append(site.getKey().getAbsolutePath()).append('\n');
//				for (File feature : site.getValue()) {
//					sb.append('\t').append(feature.getAbsolutePath()).append('\n');
//				}
//			}
//			getLogger().debug(sb.toString());
//		}
	}

	private void initCacheDir(Properties props) {
		cacheDir = null;

		String property = props.getProperty("tycho.cacheDir");
		if (property != null) {
			cacheDir = new File(property);
		}

		if (cacheDir == null) {
			property = System.getProperty("user.home");
			if (property != null) {
				cacheDir = new File(property, ".m2/tycho");
			}
		}
	}

	public BundleDescription getBundleDescription(String symbolicName, String version) {
		try {
			if (HIGHEST_VERSION == version) {
				return getBundleDescription(symbolicName);
			}
			return state.getBundle(symbolicName, new Version(version));
		} catch (NumberFormatException e) {
			return null;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private BundleDescription getBundleDescription(String symbolicName) {
		BundleDescription[] bundles = state.getBundles(symbolicName);
		BundleDescription highest = null;
		if (bundles != null) {
			for (BundleDescription desc : bundles) {
				if (highest == null || highest.getVersion().compareTo(desc.getVersion()) < 0) {
					highest = desc;
				}
			}
		}
		return highest;
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

	public File getTargetPlaform() {
		return targetPlatform;
	}

	public Feature getFeature(String id, String version) {
		FeatureDescription desc = getFeatureDescription(id, version);
		if (desc != null) {
			return desc.getFeature();
		}
		return null;
	}

	public MavenProject getMavenProject(Feature feature) {
		return (MavenProject) feature.getUserProperty(PROP_MAVEN_PROJECT);
	}

	public Feature getFeature(MavenProject project) {
		File location = project.getFile().getParentFile().getAbsoluteFile();
		for (FeatureDescription feature : features) {
			if (feature.getLocation().equals(location)) {
				return feature.getFeature();
			}
		}
		return null;
	}

	private static final Version VERSION_0_0_0 = new Version("0.0.0");

	public FeatureDescription getFeatureDescription(String id, String version) {
		if(id == null) {
			return null;
		}

		// features with matching id, sorted by version, highest version first
		SortedMap<Version, FeatureDescription> features = new TreeMap<Version, FeatureDescription>(
				new Comparator<Version>() {
					public int compare(Version o1, Version o2) {
						return - o1.compareTo(o2);
					};
				}
		);

		for (FeatureDescription desc : this.features) {
			if (id.equals(desc.getId())) {
				features.put(desc.getVersion(), desc);
			}
		}
		
		if (version == null) {
			return features.get(features.firstKey()); // latest version
		}

		Version parsedVersion = new Version(version);
		if (VERSION_0_0_0.equals(parsedVersion)) {
			return features.get(features.firstKey()); // latest version
		}

		FeatureDescription perfectMatch = features.get(parsedVersion);

		if (perfectMatch != null) {
			return perfectMatch; // perfect match
		}

		boolean qualified = !"".equals(parsedVersion.getQualifier());

		if (qualified) {
			return null; // must be perfect match for qualified versions
		}

		for (FeatureDescription desc : features.values()) {
			if (baseVersionEquals(parsedVersion, desc.getVersion())) {
				return desc;
			}
		}

		return null;
	}

	private boolean baseVersionEquals(Version v1, Version v2) {
		return v1.getMajor() == v2.getMajor()
				&& v1.getMinor() == v2.getMinor()
				&& v1.getMicro() == v2.getMicro();
	}

	public FeatureDescription getFeatureDescription(Feature feature) {
		if(feature == null) {
			return null;
		}

		return getFeatureDescription(feature.getId(), feature.getVersion());
	}

	public String getPlatformProperty(String key) {
		return platformProperties.getProperty(key);
	}

	Set<String> sites = new LinkedHashSet<String>();

	private Version platformVersion;

	/**
	 * Quick-n-dirty. Create transient Platform that includes all 
	 * features and bundles known to this State.
	 */
	public Platform getPlatform() {
		Platform platform = new Platform();
		
		Map<String, List<String>> sitePlugins = new LinkedHashMap<String, List<String>>();
		Map<String, List<Platform.Feature>> siteFeatures = new LinkedHashMap<String, List<Platform.Feature>>();

		for (BundleDescription bundle : getBundles()) {
			File location = getBundleLocation(bundle);
			String siteUrl = getSiteUrl(location);
			if (siteUrl == null) {
				throw new RuntimeException("Can't determine site for bundle " + bundle.toString() + " at " + location.getAbsolutePath());
			}
			List<String> plugins = sitePlugins.get(siteUrl);
			if (plugins == null) {
				plugins = new ArrayList<String>();
				sitePlugins.put(siteUrl, plugins);
			}
			plugins.add(getRelativeUrl(siteUrl, location));
		}
		
		// lets pretend I know what I am doing
		for (FeatureDescription feature : features) {
			String siteUrl = getSiteUrl(feature.getLocation());
			if (siteUrl == null) {
				throw new RuntimeException("Can't determine site for feature " + feature.getId() + " at " + feature.getLocation().getAbsolutePath());
			}
			List<Platform.Feature> features = siteFeatures.get(siteUrl);
			if (features == null) {
				features = new ArrayList<Platform.Feature>();
				siteFeatures.put(siteUrl, features);
			}
			features.add(getFeature(siteUrl, feature));
		}
		
		Set<String> sites = new LinkedHashSet<String>();
		sites.addAll(sitePlugins.keySet());
		sites.addAll(siteFeatures.keySet());
		for (String siteUrl : sites) {
			Platform.Site site = new Platform.Site(siteUrl);
			site.setPlugins(sitePlugins.get(siteUrl));
			site.setFeatures(siteFeatures.get(siteUrl));
			
			platform.addSite(site);
		}

		return platform;
	}

	public File getBundleLocation(BundleDescription bundle) {
		MavenProject project = getMavenProject(bundle);
		if (project != null) {
			if ("eclipse-test-plugin".equals(project.getPackaging())) {
				return project.getBasedir();
			} else if (project.getArtifact().getFile() != null) {
				return project.getArtifact().getFile();
			}
		}
		
		return new File(bundle.getLocation());
	}

	private String getSiteUrl(File location) {
		String locationStr = toUrl(location);
		for (String siteUrl : sites) {
			if (locationStr.startsWith(siteUrl)) {
				return siteUrl;
			}
		}
		return null;
		// throw new RuntimeException("Can't determine site location for " + locationStr);
	}

	private Platform.Feature getFeature(String siteUrl, FeatureDescription feature) {
		Platform.Feature result = new Platform.Feature();
		result.setId(feature.getId());
		result.setVersion(feature.getVersion().toString());
		result.setUrl(getRelativeUrl(siteUrl, feature.getLocation()));
		return result;
	}

	private String getRelativeUrl(String siteUrl, File location) {
		String locationStr = toUrl(location);
		if (!locationStr.startsWith(siteUrl)) {
			throw new IllegalArgumentException();
		}
		return locationStr.substring(siteUrl.length());
	}

	private void addSite(File basedir) {
		String siteUrl = getSiteUrl(basedir);
		if (siteUrl == null) {
			sites.add(toUrl(basedir));
		}
	}
	
	public void setTargetPlatform(File location) {
		this.targetPlatform = location;
	}

	private String toUrl(File file) {
		try {
			return file.getCanonicalFile().toURL().toExternalForm();
		} catch (IOException e) {
			throw new RuntimeException("Unexpected IOException", e);
		}
	}

	public void addSite(File site, Set<File> features, Set<File> bundles) {
		addSite(site);
		
		for (File feature : features) {
			addFeature(feature);
		}
		
		for (File bundle : bundles) {
			try {
				addBundle(bundle);
			} catch (BundleException e) {
				getLogger().info("Could not add bundle: " + bundle);
			}
		}

	}

	private void addFeature(File featureLocation) {
		try {
			Feature feature;
			if (featureLocation.isDirectory()) {
				feature = Feature.read(new File(featureLocation, Feature.FEATURE_XML));
			} else {
				// eclipse does NOT support packed features
				feature = Feature.readJar(featureLocation);
			}
			addFeature(featureLocation, feature);
		} catch (IOException e) {
			getLogger().warn("Could not read feature " + featureLocation, e);
		} catch (XmlPullParserException e) {
			getLogger().warn("Could not parse feature " + featureLocation, e);
		}

	}

	private void addFeature(File featureLocation, Feature feature) {
		FeatureDescription description = new FeatureDescriptionImpl(feature, featureLocation);
		features.add(description);
	}

	public Version getPlatformVersion() {
		if (platformVersion == null) {
			BundleDescription osgi = getBundleDescription("org.eclipse.osgi",
					OsgiState.HIGHEST_VERSION);
			platformVersion = osgi.getVersion();
		}
		return platformVersion;
	}
}
