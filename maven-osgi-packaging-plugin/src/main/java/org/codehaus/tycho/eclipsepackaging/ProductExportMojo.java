package org.codehaus.tycho.eclipsepackaging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.ArchiveEntryUtils;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.PluginRef;
import org.codehaus.tycho.model.ProductConfiguration;
import org.codehaus.tycho.model.Feature.FeatureRef;
import org.codehaus.tycho.osgitools.OsgiState;
import org.codehaus.tycho.osgitools.features.FeatureDescription;
import org.codehaus.tycho.osgitools.utils.PlatformPropertiesUtils;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.Version;

/**
 * @goal product-export
 */
public class ProductExportMojo extends AbstractMojo implements Contextualizable {

	private PlexusContainer plexus;

	/** @component */
	private OsgiState state;

	/**
	 * @parameter expression="${project}"
	 * @required
	 */
	protected MavenProject project;

	/** @parameter expression="${project.build.directory}/product" */
	private File target;

	/** @parameter expression="${project.build.directory}/product/plugins" */
	private File pluginsFolder;

	/** @parameter expression="${project.build.directory}/product/features" */
	private File featuresFolder;

	/**
	 * The product configuration, a .product file.
	 * 
	 * This file manages all aspects of a product definition from its
	 * constituent plug-ins to configuration files to branding.
	 * 
	 * @parameter expression="${productConfiguration}"
	 */
	private File productConfigurationFile;

	/**
	 * Parsed product configuration file
	 */
	private ProductConfiguration productConfiguration;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (productConfigurationFile == null) {
			File basedir = project.getBasedir();
			File productCfg = new File(basedir, project.getArtifactId()
					+ ".product");
			if (productCfg.exists()) {
				productConfigurationFile = productCfg;
			}
		}

		if (productConfigurationFile == null) {
			throw new MojoExecutionException(
					"Product configuration file not expecified");
		}
		if (!productConfigurationFile.exists()) {
			throw new MojoExecutionException(
					"Product configuration file not found "
							+ productConfigurationFile.getAbsolutePath());
		}

		try {
			getLog().debug("Parsing productConfiguration");
			productConfiguration = ProductConfiguration
					.read(productConfigurationFile);
		} catch (IOException e) {
			throw new MojoExecutionException(
					"Error reading product configuration file", e);
		} catch (XmlPullParserException e) {
			throw new MojoExecutionException(
					"Error parsing product configuration file", e);
		}

		generateEclipseProduct();
		generateConfigIni();

		pluginsFolder.mkdirs();
		if (productConfiguration.getUseFeatures()) {
			featuresFolder.mkdirs();
			copyFeatures(productConfiguration.getFeatures());
		} else {
			copyPlugins(productConfiguration.getPlugins());
		}

		copyExecutable();

		packProduct();
	}

	private void packProduct() throws MojoExecutionException {
		ZipArchiver zipper;
		try {
			zipper = (ZipArchiver) plexus.lookup(ZipArchiver.ROLE, "zip");
		} catch (ComponentLookupException e) {
			throw new MojoExecutionException("Unable to resolve ZipArchiver", e);
		}

		File destFile = new File(project.getBuild().getDirectory(), project
				.getBuild().getFinalName()
				+ ".zip");

		try {
			zipper.addDirectory(target);
			zipper.setDestFile(destFile);
			zipper.createArchive();
		} catch (Exception e) {
			throw new MojoExecutionException("Error packing product", e);
		}

		project.getArtifact().setFile(destFile);
	}

	private boolean isEclipse32Platform() {
		Version osgiVersion = state.getPlatformVersion();
		return osgiVersion.getMajor() == 3 && osgiVersion.getMinor() == 2;
	}

	private void generateEclipseProduct() throws MojoExecutionException {
		getLog().debug("Generating .eclipseproduct");
		Properties props = new Properties();
		setPropertyIfNotNull(props, "version", productConfiguration
				.getVersion());
		setPropertyIfNotNull(props, "name", productConfiguration.getName());
		setPropertyIfNotNull(props, "id", productConfiguration.getId());

		target.mkdirs();

		File eclipseproduct = new File(target, ".eclipseproduct");
		try {
			FileOutputStream fos = new FileOutputStream(eclipseproduct);
			props.store(fos, "Eclipse Product File");
			fos.close();
		} catch (IOException e) {
			throw new MojoExecutionException(
					"Error creating .eclipseproduct file.", e);
		}
	}

	private void generateConfigIni() throws MojoExecutionException {
		getLog().debug("Generating config.ini");
		Properties props = new Properties();
		String splash = productConfiguration.getId().split("\\.")[0];
		setPropertyIfNotNull(props, "osgi.splashPath",
				"platform:/base/plugins/" + splash);

		setPropertyIfNotNull(props, "eclipse.product", productConfiguration
				.getId());
		// TODO check if there are any other levels
		setPropertyIfNotNull(props, "osgi.bundles.defaultStartLevel", "4");

		if (productConfiguration.getUseFeatures()) {
			setPropertyIfNotNull(props, "osgi.bundles",
					getFeaturesOsgiBundles());
		} else {
			setPropertyIfNotNull(props, "osgi.bundles", getPluginsOsgiBundles());
		}

		File configsFolder = new File(target, "configuration");
		configsFolder.mkdirs();

		File configIni = new File(configsFolder, "config.ini");
		try {
			FileOutputStream fos = new FileOutputStream(configIni);
			props.store(fos, "Product Runtime Configuration File");
			fos.close();
		} catch (IOException e) {
			throw new MojoExecutionException(
					"Error creating .eclipseproduct file.", e);
		}
	}

	private String getFeaturesOsgiBundles() {
		// TODO how does eclipse know this?
		return "org.eclipse.equinox.common@2:start,org.eclipse.update.configurator@3:start,org.eclipse.core.runtime@start";
	}

	private String getPluginsOsgiBundles() {
		List<PluginRef> plugins = productConfiguration.getPlugins();
		StringBuilder buf = new StringBuilder(plugins.size() * 10);
		for (PluginRef plugin : plugins) {
			// reverse engineering discovered
			// this plugin is not present on config.ini, and if so nothing
			// starts
			if ("org.eclipse.osgi".equals(plugin.getId())) {
				continue;
			}

			if (buf.length() != 0) {
				buf.append(',');
			}

			buf.append(plugin.getId());

			// reverse engineering discovered
			// the final bundle has @start after runtime
			if ("org.eclipse.core.runtime".equals(plugin.getId())) {
				buf.append("@start");
			}

			if (isEclipse32Platform()
					&& "org.eclipse.equinox.common".equals(plugin.getId())) {
				buf.append("@2:start");
			}
		}

		// required plugins, RCP didn't start without both
		if (!isEclipse32Platform()) {
			if (buf.length() != 0) {
				buf.append(',');
			}
			String ws = state
					.getPlatformProperty(PlatformPropertiesUtils.OSGI_WS);
			String os = state
					.getPlatformProperty(PlatformPropertiesUtils.OSGI_OS);
			String arch = state
					.getPlatformProperty(PlatformPropertiesUtils.OSGI_ARCH);

			buf.append("org.eclipse.equinox.launcher,");
			buf.append("org.eclipse.equinox.launcher." + ws + "." + os + "."
					+ arch);
		}

		return buf.toString();
	}

	private void copyFeatures(List<FeatureRef> features)
			throws MojoExecutionException {
		getLog().debug("copying " + features.size() + " features ");

		for (FeatureRef feature : features) {
			copyFeature(feature);
		}
	}

	private void copyFeature(FeatureRef feature) throws MojoExecutionException {
		String featureId = feature.getId();
		String featureVersion = feature.getVersion();

		FeatureDescription featureDescription = state.getFeatureDescription(featureId, featureVersion);
		if (featureDescription == null) {
			throw new MojoExecutionException("Unable to resolve feature " + featureId + "_" + featureVersion);
		}

		featureVersion = state.getFinalVersion(featureDescription).toString();

		Feature featureRef = featureDescription.getFeature();

		MavenProject project = state.getMavenProject(featureDescription);

		de.schlichtherle.io.File source;
		if (project == null) {
			getLog().debug("feature = bundle: " + featureDescription.getLocation());
			source = new de.schlichtherle.io.File(featureDescription.getLocation());
		} else {
			getLog().debug("feature = project: " + project.getArtifact());
			source = new de.schlichtherle.io.File(project.getArtifact().getFile());
		}

		File targetFolder = new File(featuresFolder, featureRef.getId() + "_" + featureVersion);
		targetFolder.mkdirs();

		source.copyAllTo(targetFolder);

		List<FeatureRef> featureRefs = featureRef.getIncludedFeatures();
		for (FeatureRef fRef : featureRefs) {
			copyFeature(fRef);
		}

		// copy all plugins from all features
		List<PluginRef> pluginRefs = featureRef.getPlugins();
		for (PluginRef pluginRef : pluginRefs) {
			if (matchCurrentPlataform(pluginRef)) {
				copyPlugin(pluginRef.getId(), pluginRef.getVersion(), pluginRef.isUnpack());
			}
		}

	}

	private boolean matchCurrentPlataform(PluginRef pluginRef) {
		String ws = state.getPlatformProperty(PlatformPropertiesUtils.OSGI_WS);
		String os = state.getPlatformProperty(PlatformPropertiesUtils.OSGI_OS);
		String arch = state
				.getPlatformProperty(PlatformPropertiesUtils.OSGI_ARCH);

		String pluginWs = pluginRef.getWs();
		String pluginOs = pluginRef.getOs();
		String pluginArch = pluginRef.getArch();

		return (pluginWs == null || ws.equals(pluginWs)) && //
				(pluginOs == null || os.equals(pluginOs)) && //
				(pluginArch == null || arch.equals(pluginArch));
	}

	private void copyPlugins(Collection<PluginRef> plugins)
			throws MojoExecutionException {
		getLog().debug("copying " + plugins.size() + " plugins ");

		for (PluginRef plugin : plugins) {
			copyPlugin(plugin.getId(), plugin.getVersion(), plugin.isUnpack());
		}

		// required plugins, RCP didn't start without both
		if (!isEclipse32Platform()) {
			String ws = state
					.getPlatformProperty(PlatformPropertiesUtils.OSGI_WS);
			String os = state
					.getPlatformProperty(PlatformPropertiesUtils.OSGI_OS);
			String arch = state
					.getPlatformProperty(PlatformPropertiesUtils.OSGI_ARCH);

			copyPlugin("org.eclipse.equinox.launcher", null, false);
			copyPlugin("org.eclipse.equinox.launcher." + ws + "." + os + "."
					+ arch, null, false);
		}
	}

	private String copyPlugin(String bundleId, String bundleVersion,
			boolean unpack) throws MojoExecutionException {
		getLog().debug("Copying plugin " + bundleId + "_" + bundleVersion);

		if (bundleVersion == null || "0.0.0".equals(bundleVersion)) {
			bundleVersion = OsgiState.HIGHEST_VERSION;
		}

		BundleDescription bundle = state.getBundleDescription(bundleId,
				bundleVersion);
		if (bundle == null) {
			throw new MojoExecutionException("Plugin '" + bundleId + "_"
					+ bundleVersion + "' not found!");
		}

		MavenProject bundleProject = state.getMavenProject(bundle);
		File source;
		if (bundleProject != null) {
			source = bundleProject.getArtifact().getFile();
		} else {
			source = new File(bundle.getLocation());
		}

		bundleVersion = state.getFinalVersion(bundle).toString();

		File target = new File(pluginsFolder, bundleId + "_" + bundleVersion
				+ ".jar");
		if (source.isDirectory()) {
			copyToDirectory(source, pluginsFolder);
		} else if (unpack) {
			// when unpacked doesn't have .jar extension
			String path = target.getAbsolutePath();
			path = path.substring(0, path.length() - 4);
			target = new File(path);
			new de.schlichtherle.io.File(source).copyAllTo(target);
			// unpackToDirectory(source, target);
		} else {
			copyToFile(source, target);
		}

		return bundleVersion;
	}

	private void copyToFile(File source, File target)
			throws MojoExecutionException {
		try {
			target.getParentFile().mkdirs();

			if (source.isFile()) {
				FileUtils.copyFile(source, target);
			} else if (source.isDirectory()) {
				FileUtils.copyDirectory(source, target);
			} else {
				getLog().warn("Skipping bundle " + source.getAbsolutePath());
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to copy "
					+ source.getName(), e);
		}
	}

	private void copyToDirectory(File source, File targetFolder)
			throws MojoExecutionException {
		try {
			if (source.isFile()) {
				FileUtils.copyFileToDirectory(source, targetFolder);
			} else if (source.isDirectory()) {
				FileUtils.copyDirectoryToDirectory(source, targetFolder);
			} else {
				getLog().warn("Skipping bundle " + source.getAbsolutePath());
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to copy "
					+ source.getName(), e);
		}
	}

	private void copyExecutable() throws MojoExecutionException {
		getLog().debug("Creating launcher.exe");

		FeatureDescription feature;
		// eclipse 3.2
		if (isEclipse32Platform()) {
			feature = state.getFeatureDescription(
					"org.eclipse.platform.launchers", null);
		} else {
			feature = state.getFeatureDescription(
					"org.eclipse.equinox.executable", null);
		}

		if (feature == null) {
			throw new MojoExecutionException("RPC delta feature not found!");
		}

		File location = feature.getLocation();

		String ws = state.getPlatformProperty(PlatformPropertiesUtils.OSGI_WS);
		String os = state.getPlatformProperty(PlatformPropertiesUtils.OSGI_OS);
		String arch = state
				.getPlatformProperty(PlatformPropertiesUtils.OSGI_ARCH);

		File osLauncher = new File(location, "bin/" + ws + "/" + os + "/"
				+ arch);

		try {
			// Don't copy eclipsec file
			IOFileFilter eclipsecFilter = FileFilterUtils
					.notFileFilter(FileFilterUtils.prefixFileFilter("eclipsec"));
			FileUtils.copyDirectory(osLauncher, target, eclipsecFilter);
		} catch (IOException e) {
			throw new MojoExecutionException(
					"Unable to copy launcher executable", e);
		}

		File launcher = getLauncher();

		// make launcher executable
		try {
			getLog().debug("running chmod");
			ArchiveEntryUtils.chmod(launcher, 0755, null);
		} catch (ArchiverException e) {
			throw new MojoExecutionException(
					"Unable to make launcher being executable", e);
		}

		// Rename launcher
		if (productConfiguration.getLauncher() != null
				&& productConfiguration.getLauncher().getName() != null) {
			String launcherName = productConfiguration.getLauncher().getName();
			getLog().debug("Renaming launcher to " + launcherName);

			String newName = launcherName;

			// win32 has extensions
			if (PlatformPropertiesUtils.OS_WIN32.equals(os)) {
				String extension = FilenameUtils.getExtension(launcher
						.getAbsolutePath());
				newName = launcherName + "." + extension;
			}

			launcher.renameTo(new File(launcher.getParentFile(), newName));

		}

		// eclipse 3.2
		if (isEclipse32Platform()) {
			File startUpJar = new File(location, "bin/startup.jar");
			try {
				FileUtils.copyFileToDirectory(startUpJar, target);
			} catch (IOException e) {
				throw new MojoExecutionException(
						"Unable to copy startup.jar executable", e);
			}
		}

	}

	private File getLauncher() throws MojoExecutionException {
		String os = state.getPlatformProperty(PlatformPropertiesUtils.OSGI_OS);

		if (PlatformPropertiesUtils.OS_WIN32.equals(os)) {
			return new File(target, "launcher.exe");
		}

		if (PlatformPropertiesUtils.OS_LINUX.equals(os)
				|| PlatformPropertiesUtils.OS_SOLARIS.equals(os)
				|| PlatformPropertiesUtils.OS_HPUX.equals(os)
				|| PlatformPropertiesUtils.OS_AIX.equals(os)) {
			return new File(target, "launcher");
		}

		if (PlatformPropertiesUtils.OS_MACOSX.equals(os)) {
			// TODO need to check this at macos
			return new File(target, "Eclipse.app/Contents/MacOS/launcher");
		}

		throw new MojoExecutionException("Unexpected OS: " + os);
	}

	private void setPropertyIfNotNull(Properties properties, String key,
			String value) {
		if (value != null) {
			properties.setProperty(key, value);
		}
	}

	public void contextualize(Context ctx) throws ContextException {
		plexus = (PlexusContainer) ctx.get(PlexusConstants.PLEXUS_KEY);
	}

}
