package org.codehaus.tycho.osgitest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.tycho.model.Platform;
import org.codehaus.tycho.osgitools.OsgiState;
import org.codehaus.tycho.osgitools.OsgiStateController;
import org.eclipse.osgi.service.resolver.BundleDescription;

/**
 * Helper to generate eclipse config.ini file
 * 
 * See org.eclipse.pde.internal.ui.launcher.LaunchConfigurationHelper#createConfigIniFile.
 * 
 * @author igor
 */
public class ConfigurationHelper {

	private final OsgiState state;

	ConfigurationHelper(OsgiState state) {
		this.state = state;
	}

	public void createConfiguration(File work, File targetPlatform, Set<File> testBundles) throws MojoExecutionException {
		try {
			Properties p = new Properties();

			FileInputStream fis = new FileInputStream(new File(targetPlatform, OsgiState.CONFIG_INI_PATH));
			try {
				p.load(fis);
			} finally {
				fis.close();
			}

			String newOsgiBundles;

			if (shouldUseP2()) {
				createBundlesInfoFile(work);
				createPlatformXmlFile(work);
				newOsgiBundles = "org.eclipse.equinox.simpleconfigurator@1:start";
			} else if (shouldUseUpdateManager()) {
				createPlatformXmlFile(work);
				newOsgiBundles = "org.eclipse.equinox.common@2:start, org.eclipse.update.configurator@3:start, org.eclipse.core.runtime@start";
			} else /*use plain equinox*/ {
				newOsgiBundles = toOsgiBundles(state.getBundles());
			}

			p.setProperty("osgi.bundles", newOsgiBundles);

			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=234069
			p.setProperty("osgi.bundlefile.limit", "100");

			// @see SimpleConfiguratorConstants#PROP_KEY_EXCLUSIVE_INSTALLATION
//			p.setProperty("org.eclipse.equinox.simpleconfigurator.exclusiveInstallation", "false");

			addRequiredProperties(p, targetPlatform);

			// fix osgi.framework
			String url = p.getProperty("osgi.framework");
			if (url != null) {
				File file;
				BundleDescription desc = state.getBundleDescription(url, OsgiState.HIGHEST_VERSION);
				if (desc != null) {
					url = "file:" + new File(desc.getLocation()).getAbsolutePath().replace('\\', '/');
				} else if (url.startsWith("file:")) {
					String path = url.substring("file:".length());
					file = new File(path);
					if (!file.isAbsolute()) {
						file = new File(targetPlatform, path);
					}
					url = "file:" + file.getAbsolutePath().replace('\\', '/');
				}
			}
			if (url != null) {
				p.setProperty("osgi.framework", url);
			}

			new File(work, "configuration").mkdir();
			FileOutputStream fos = new FileOutputStream(new File(work, OsgiState.CONFIG_INI_PATH));
			try {
				p.store(fos, null);
			} finally {
				fos.close();
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Exception creating config.ini", e);
		}
	}

	private void createPlatformXmlFile(File work) throws IOException {
		Platform platform = state.getPlatform();
		
		Platform.write(platform, new File(work, OsgiState.PLATFORM_XML_PATH));
	}

	private static final Map<String, Integer> START_LEVEL = new HashMap<String, Integer>();
	
	static {
		START_LEVEL.put("org.eclipse.equinox.common", 2);
		START_LEVEL.put("org.eclipse.core.runtime", 4);
		START_LEVEL.put("org.eclipse.equinox.simpleconfigurator", 1);
		START_LEVEL.put("org.eclipse.update.configurator", 3);
		START_LEVEL.put("org.eclipse.osgi", -1);
	}

	private void createBundlesInfoFile(File work) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (BundleDescription bundle : state.getBundles()) {
			// TODO shame on me!
			File location = ((OsgiStateController)state).getBundleLocation(bundle);

			// TODO another dirty hack -- compensate for .qualifier expansion
			Manifest manifest = state.loadManifest(location);
			Attributes attributes = manifest.getMainAttributes();
			String version = attributes.getValue("Bundle-Version");

			sb.append(bundle.getSymbolicName()).append(',');
			sb.append(version).append(',');
			sb.append(location.toURL().toExternalForm()).append(',');

			Integer level = START_LEVEL.get(bundle.getSymbolicName());
			if (level != null) {
				sb.append(level).append(','); // start level
				sb.append("true"); // autostart
			} else {
				sb.append("4").append(','); // start level
				sb.append("false"); // autostart
			}
			sb.append('\n');
		}
		fileWrite(new File(work, OsgiState.BUNDLES_INFO_PATH), sb.toString());
	}

	private static void fileWrite(File file, String data) throws IOException {
		file.getParentFile().mkdirs();
		FileUtils.fileWrite(file.getAbsolutePath(), data);
	}

	private boolean shouldUseUpdateManager() {
		return state.getBundleDescription("org.eclipse.update.configurator", OsgiState.HIGHEST_VERSION) != null;
	}

	private boolean shouldUseP2() {
		return state.getBundleDescription("org.eclipse.equinox.simpleconfigurator", OsgiState.HIGHEST_VERSION) != null;
	}

	private String toOsgiBundles(BundleDescription[] bundles) throws IOException {
		StringBuilder result = new StringBuilder();
		for (BundleDescription bundle : bundles) {
			Integer level = START_LEVEL.get(bundle.getSymbolicName());
			if (level != null && level.intValue() == -1) {
				continue; // system bundle
			}
			if (result.length() > 0) {
				result.append(",");
			}
			File file = new File(bundle.getLocation());
			result.append(appendAbsolutePath(file));
			if (level != null) {
				result.append('@').append(level).append(":start");
			}
		}
		return result.toString();
	}

	private String createOsgiBundlesProperty(File targetPlatform, String osgiBundles, Set<File> testPlugins) throws IOException, MojoExecutionException {
		StringBuilder result = new StringBuilder();
		
		StringTokenizer st = new StringTokenizer(osgiBundles, ",");
		while(st.hasMoreTokens()) {
			if (result.length() > 0) {
				result.append(",");
			}
			String t = st.nextToken().trim();
			int at = t.indexOf('@');
			String url =  getPlatformURL(targetPlatform, at > 0 ? t.substring(0, at) : t);
			if (url != null) {
				result.append(url);
				if (at > 0) {
					result.append(t.substring(at));
				}
			} else {
				result.append(t);
			}
		}

		for (File file : testPlugins) {
			if (result.length() > 0) {
				result.append(",");
			}
			result.append(appendAbsolutePath(file));
		}
		return result.toString();
	}

	private String appendAbsolutePath(File file) throws IOException {
		String url = file.getAbsolutePath().replace('\\', '/');
		return "reference:file:" + url;
	}

	private String getPlatformURL(File targetPlatform, String id) throws IOException {
		BundleDescription desc = state.getBundleDescription(id, OsgiState.HIGHEST_VERSION);
		if (desc != null) {
			return appendAbsolutePath(new File(desc.getLocation()));
		} else if (id.startsWith("reference:file:")) {
			String path = id.substring("reference:file:".length());
			if (!new File(path).isAbsolute()) {
				return appendAbsolutePath(new File(targetPlatform, "plugins/" + path));
			}
		}

		return null;
	}

	private static void addRequiredProperties(Properties properties, File targetPlatform) {
		if (!properties.containsKey("osgi.install.area"))
			properties.setProperty("osgi.install.area", "file:" + targetPlatform.getAbsolutePath().replace('\\', '/'));
//		if (!properties.containsKey("osgi.configuration.cascaded"))
			properties.setProperty("osgi.configuration.cascaded", "false");
		if (!properties.containsKey("osgi.framework"))
			properties.setProperty("osgi.framework", "org.eclipse.osgi");
		if (!properties.containsKey("osgi.bundles.defaultStartLevel"))
			properties.setProperty("osgi.bundles.defaultStartLevel", "4");
	}

}
