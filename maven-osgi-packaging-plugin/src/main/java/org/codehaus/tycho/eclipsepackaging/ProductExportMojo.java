package org.codehaus.tycho.eclipsepackaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.eclipsepackaging.product.Plugin;
import org.codehaus.tycho.eclipsepackaging.product.ProductConfiguration;
import org.codehaus.tycho.eclipsepackaging.product.ProductConfigurationConverter;
import org.codehaus.tycho.osgitools.OsgiState;
import org.codehaus.tycho.osgitools.features.FeatureDescription;
import org.codehaus.tycho.osgitools.utils.PlatformPropertiesUtils;
import org.eclipse.osgi.service.resolver.BundleDescription;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;

/**
 * @goal product-export
 */
public class ProductExportMojo extends AbstractMojo {

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
		copyPlugins();
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
		Properties props = new Properties();
		setPropertyIfNotNull(props, "osgi.splashPath",
				"platform:/base/plugins/"
						+ productConfiguration.getPlugins().get(0).getId());
		setPropertyIfNotNull(props, "eclipse.product", productConfiguration
				.getId());
		// TODO check if there are any other levels
		setPropertyIfNotNull(props, "osgi.bundles.defaultStartLevel", "4");
		setPropertyIfNotNull(props, "osgi.bundles", getPluginsIdsAsString());

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

	private String getPluginsIdsAsString() {
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

	private void copyPlugins() throws MojoExecutionException {
		List<Plugin> plugins = productConfiguration.getPlugins();
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

			File file;
			MavenProject bundleProject = state.getMavenProject(bundle);
			if (bundleProject != null) {
				file = bundleProject.getArtifact().getFile();
			} else {
				file = new File(bundle.getLocation());
			}

			try {
				if (file.isFile()) {
					FileUtils.copyFileToDirectory(file, pluginsFolder);
				} else if (file.isDirectory()) {
					FileUtils.copyDirectoryToDirectory(file, pluginsFolder);
				} else {
					getLog().warn(
							"Skipping plugin " + bundleId + "\n"
									+ file.getAbsolutePath());
				}
			} catch (IOException e) {
				throw new MojoExecutionException("Unable to copy plugin "
						+ bundleId, e);
			}

		}
	}

	private void copyExecutable() throws MojoExecutionException {
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
			FileUtils.copyDirectory(osLauncher, target);
		} catch (IOException e) {
			throw new MojoExecutionException(
					"Unable to copy launcher executable", e);
		}
	}

	private void setPropertyIfNotNull(Properties properties, String key,
			String value) {
		if (value != null) {
			properties.setProperty(key, value);
		}
	}

}
