package org.codehaus.tycho.osgitest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.tycho.osgitools.OsgiState;
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

	/**
	 * Generates config.ini file.
	 * 
	 */
	public void createConfiguration(File work, File targetPlatform, Set<File> testBundles) throws MojoExecutionException {
		try {
			Properties p = new Properties();

			FileInputStream fis = new FileInputStream(new File(targetPlatform, "configuration/config.ini"));;
			try {
				p.load(fis);
			} finally {
				fis.close();
			}

			String osgiBundles = p.getProperty("osgi.bundles");
			String newOsgiBundles = createOsgiBundlesProperty(targetPlatform, osgiBundles, testBundles);
			p.setProperty("osgi.bundles", newOsgiBundles);

			// @see SimpleConfiguratorConstants#PROP_KEY_EXCLUSIVE_INSTALLATION
			p.setProperty("org.eclipse.equinox.simpleconfigurator.exclusiveInstallation", "false");

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
			FileOutputStream fos = new FileOutputStream(new File(work, "configuration/config.ini"));
			try {
				p.store(fos, null);
			} finally {
				fos.close();
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Exception creating config.ini", e);
		}
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
				return appendAbsolutePath(new File(targetPlatform, path));
			}
		} 

		return null;
	}

	private static void addRequiredProperties(Properties properties, File targetPlatform) {
		if (!properties.containsKey("osgi.install.area"))
			properties.setProperty("osgi.install.area", "file:" + targetPlatform.getAbsolutePath().replace('\\', '/'));
		if (!properties.containsKey("osgi.configuration.cascaded"))
			properties.setProperty("osgi.configuration.cascaded", "true");
		if (!properties.containsKey("osgi.framework"))
			properties.setProperty("osgi.framework", "org.eclipse.osgi");
		if (!properties.containsKey("osgi.bundles.defaultStartLevel"))
			properties.setProperty("osgi.bundles.defaultStartLevel", "4");
	}

}
