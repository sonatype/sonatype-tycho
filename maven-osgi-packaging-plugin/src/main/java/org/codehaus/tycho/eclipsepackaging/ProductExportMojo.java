package org.codehaus.tycho.eclipsepackaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.util.ArchiveEntryUtils;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.tycho.eclipsepackaging.product.Feature;
import org.codehaus.tycho.eclipsepackaging.product.Plugin;
import org.codehaus.tycho.eclipsepackaging.product.ProductConfiguration;
import org.codehaus.tycho.eclipsepackaging.product.ProductConfigurationConverter;
import org.codehaus.tycho.model.Feature.FeatureRef;
import org.codehaus.tycho.model.Feature.PluginRef;
import org.codehaus.tycho.osgitools.OsgiState;
import org.codehaus.tycho.osgitools.features.FeatureDescription;
import org.codehaus.tycho.osgitools.utils.PlatformPropertiesUtils;
import org.eclipse.osgi.service.resolver.BundleDescription;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;

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

		XStream xs = new XStream();
		xs.registerConverter(new ProductConfigurationConverter(getLog()));
		xs.processAnnotations(ProductConfiguration.class);

		try {
			getLog().debug("Parsing productConfiguration");
			productConfiguration = (ProductConfiguration) xs
					.fromXML(new FileInputStream(productConfigurationFile));
		} catch (IOException e) {
			throw new MojoExecutionException(
					"Error reading product configuration file", e);
		} catch (XStreamException e) {
			throw new MojoExecutionException(
					"Error parsing product configuration file", e);
		}

		addOsgiLauncherPlugins();

		generateEclipseProduct();
		generateConfigIni();
		if (productConfiguration.getUseFeatures()) {
			copyFeatures(productConfiguration.getFeatures());
		} else {
			copyPlugins(productConfiguration.getPlugins());
		}
		copyExecutable();
	}

	private void addOsgiLauncherPlugins() {
		String ws = state.getPlatformProperty(PlatformPropertiesUtils.OSGI_WS);
		String os = state.getPlatformProperty(PlatformPropertiesUtils.OSGI_OS);
		String arch = state
				.getPlatformProperty(PlatformPropertiesUtils.OSGI_ARCH);

		// required plugins, RCP didn't start without both
		// org.eclipse.equinox.launcher
		productConfiguration.getPlugins().add(
				new Plugin("org.eclipse.equinox.launcher", null));

		// org.eclipse.equinox.launcher.win32.win32.x86
		productConfiguration.getPlugins().add(
				new Plugin("org.eclipse.equinox.launcher." + ws + "." + os
						+ "." + arch, null));
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
		setPropertyIfNotNull(props, "osgi.splashPath",
				"platform:/base/plugins/"
						+ productConfiguration.getPlugins().get(0).getId());
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
		List<Plugin> plugins = productConfiguration.getPlugins();
		StringBuilder buf = new StringBuilder(plugins.size() * 10);
		for (Plugin plugin : plugins) {
			if (buf.length() != 0) {
				buf.append(',');
			}
			// reverse engineering discovered
			// this plugin is not present on config.ini, and if so nothing
			// starts
			if ("org.eclipse.osgi".equals(plugin.getId())) {
				continue;
			}

			buf.append(plugin.getId());

			// reverse engineering discovered
			// the final bundle has @start after runtime
			if ("org.eclipse.core.runtime".equals(plugin.getId())) {
				buf.append("@start");
			}
		}

		return buf.toString();
	}

	private void copyFeatures(List<Feature> features)
			throws MojoExecutionException {
		getLog().debug("Coping " + features.size() + " features ");
		File featuresFolder = new File(target, "features");
		featuresFolder.mkdirs();
		Set<Plugin> plugins = new LinkedHashSet<Plugin>();

		for (Feature feature : features) {
			plugins.addAll(copyFeature(feature, featuresFolder));
		}

		copyPlugins(plugins);
	}

	private List<Plugin> copyFeature(Feature feature, File featuresFolder)
			throws MojoExecutionException {
		String featureId = feature.getId();
		String featureVersion = feature.getVersion();
		FeatureDescription bundle = state.getFeatureDescription(featureId,
				featureVersion);
		org.codehaus.tycho.model.Feature featureRef;
		if (bundle != null) {
			try {
				featureRef = org.codehaus.tycho.model.Feature.read(new File(
						bundle.getLocation(), "feature.xml"));
			} catch (Exception e) {
				throw new MojoExecutionException(
						"Error reading feature.xml for " + featureId);
			}
		} else {
			featureRef = state.getFeature(featureId, featureVersion);
		}

		if (featureRef == null) {
			throw new MojoExecutionException("Unable to resolve feature "
					+ featureId);
		}
		MavenProject project = state.getMavenProject(featureRef);
		if (project != null) {
			File source = project.getArtifact().getFile();

			ZipUnArchiver unArchiver;
			try {
				unArchiver = (ZipUnArchiver) plexus.lookup(ZipUnArchiver.ROLE,
						"zip");
			} catch (ComponentLookupException e) {
				throw new MojoExecutionException(
						"Unable to resolve ZipUnArchiver", e);
			}

			unArchiver.setDestDirectory(featuresFolder.getParentFile());
			unArchiver.setSourceFile(source);

			FileSelector[] fileSelectors = new FileSelector[] { new FileSelector() {
				public boolean isSelected(FileInfo fileInfo) throws IOException {
					return fileInfo.getName().startsWith("features");
				}
			} };
			unArchiver.setFileSelectors(fileSelectors);
			try {
				unArchiver.extract();
			} catch (Exception e) {
				throw new MojoExecutionException("Error extracting feature "
						+ featureId, e);
			}

			// copyToDirectory(file, featuresFolder);
		} else {
			File source = bundle.getLocation();
			copyToDirectory(source, featuresFolder);
		}

		List<FeatureRef> featureRefs = featureRef.getIncludedFeatures();
		List<Plugin> plugins = new ArrayList<Plugin>();
		for (FeatureRef fRef : featureRefs) {
			plugins.addAll(copyFeature(new Feature(fRef.getId(), fRef
					.getVersion()), featuresFolder));
		}

		// copy all plugins from all features
		List<PluginRef> pluginRefs = featureRef.getPlugins();
		for (PluginRef pluginRef : pluginRefs) {
			if (matchCurrentPlataform(pluginRef)) {
				plugins.add(new Plugin(pluginRef.getId(), pluginRef
						.getVersion()));
			}
		}

		return plugins;
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

	private void copyPlugins(Collection<Plugin> plugins)
			throws MojoExecutionException {
		getLog().debug("Coping " + plugins.size() + " plugins ");
		File pluginsFolder = new File(target, "plugins");
		pluginsFolder.mkdirs();

		for (Plugin plugin : plugins) {
			String bundleId = plugin.getId();
			String bundleVersion = plugin.getVersion();
			if (bundleVersion == null || "0.0.0".equals(bundleVersion)) {
				bundleVersion = OsgiState.HIGHEST_VERSION;
			}

			BundleDescription bundle = state.getBundleDescription(bundleId,
					bundleVersion);
			if (bundle == null) {
				throw new MojoExecutionException("Plugin " + bundleId
						+ " not found!");
			}

			MavenProject bundleProject = state.getMavenProject(bundle);
			if (bundleProject != null) {
				File source = bundleProject.getArtifact().getFile();
				File target = new File(pluginsFolder, bundleProject
						.getArtifactId()
						+ "_" + bundleProject.getVersion() + ".jar");
				copyToFile(source, target);
			} else {
				File source = new File(bundle.getLocation());
				copyToDirectory(source, pluginsFolder);
			}

		}
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

		FeatureDescription feature = state.getFeatureDescription(
				"org.eclipse.equinox.executable", null);

		if (feature == null) {
			String msg = "RPC delta feature not found!";
			throw new MojoExecutionException(msg,
					new ArtifactResolutionException(msg, "",
							"org.eclipse.equinox.executable", "",
							"eclipse-feature", null, null));
		}

		String ws = state.getPlatformProperty(PlatformPropertiesUtils.OSGI_WS);
		String os = state.getPlatformProperty(PlatformPropertiesUtils.OSGI_OS);
		String arch = state
				.getPlatformProperty(PlatformPropertiesUtils.OSGI_ARCH);
		File location = feature.getLocation();

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
		if (!PlatformPropertiesUtils.OS_WIN32.equals(os)) {
			try {
				ArchiveEntryUtils.chmod(launcher, 777, null);
			} catch (Exception e) {
				throw new MojoExecutionException(
						"Unable to make launcher being executable", e);
			}
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
