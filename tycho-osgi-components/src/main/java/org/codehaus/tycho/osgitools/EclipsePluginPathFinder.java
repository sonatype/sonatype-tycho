package org.codehaus.tycho.osgitools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

/**
 * Finds bundles in Eclipse installation. 
 * 
 * See http://wiki.eclipse.org/Equinox_p2_Getting_Started
 * See http://mea-bloga.blogspot.com/2008/04/new-target-platform-preference.html
 * 
 * @author igor
 *
 */
public class EclipsePluginPathFinder {

	public List<File> getFeatures(File targetPlatform) {
		List<File> result = new ArrayList<File>();

		for (File site : getSites(targetPlatform)) {
			File[] plugins = new File(site, "features").listFiles();
			if (plugins != null) {
				result.addAll(Arrays.asList(plugins));
			}
		}

		return result;
	}

	public Set<File> getPlugins(File targetPlatform) {

		Set<File> result = null;

//		try {
//			result = new LinkedHashSet<File>(readBundlesTxt(targetPlatform));
//		} catch (IOException e) {
//			// oops
//		}
//
//		if (result != null) {
//			return result;
//		}

		result = new LinkedHashSet<File>();

		for (File site : getSites(targetPlatform)) {
			addPlugins(result, new File(site, "plugins").listFiles());
		}
		addPlugins(result, new File(targetPlatform, "dropins").listFiles());

		return result;
	}

	private void addPlugins(Set<File> result, File[] plugins) {
		if (plugins != null) {
			for (File plugin : plugins) {
				if (plugin.isDirectory() && new File(plugin, "META-INF/MANIFEST.MF").canRead()) {
					result.add(plugin);
				} else if (plugin.isFile() && plugin.getName().endsWith(".jar")) {
					result.add(plugin);
				}
			}
		}
	}

	public Set<File> getSites(File targetPlatform) {
		Set<File> result = new LinkedHashSet<File>();

		if (new File(targetPlatform, "plugins").isDirectory()) {
			result.add(targetPlatform);
		}

		File platform = new File(targetPlatform, "configuration/org.eclipse.update/platform.xml");
		if (platform.canRead()) {
			try {
				FileInputStream is = new FileInputStream(platform);
				try {
					XmlStreamReader reader = new XmlStreamReader(is);
					Xpp3Dom dom = Xpp3DomBuilder.build(reader);
					Xpp3Dom[] sites = dom.getChildren("site");
					for (Xpp3Dom site : sites) {
						String enabled = site.getAttribute("enabled");
						if (enabled == null || Boolean.parseBoolean(enabled)) {
							File dir = parsePlatformURL(targetPlatform, site.getAttribute("url"));
							if (dir != null) {
								result.add(dir);
							}
						}
					}
				} finally {
					is.close();
				}
			} catch (Exception e) {
				// too bad
			}
		}

		addLinks(result, targetPlatform, new File(targetPlatform, "links"));

		// deal with dropins folder
		File dropins = new File(targetPlatform, "dropins");

		File[] dropinsFiles = dropins.listFiles();
		if (dropinsFiles != null) {
			for (File dropinsFile : dropinsFiles) {
				File plugins = new File(dropinsFile, "plugins");
				if (plugins.isDirectory()) {
					result.add(plugins.getParentFile());
					continue;
				}
				plugins = new File(dropinsFile, "eclipse/plugins");
				if (plugins.isDirectory()) {
					result.add(plugins.getParentFile());
				}
			}
		}

		addLinks(result, targetPlatform, dropins);

		return result;
	}

	private void addLinks(Set<File> result, File targetPlatform, File linksFolder) {
		File[] links = linksFolder.listFiles();
		if (links != null) { 
			for (File link : links) {
				if (link.isFile() && link.canRead() && link.getName().endsWith(".link")) {
					Properties props = new Properties();
					try {
						InputStream is = new FileInputStream(link);
						try {
							props.load(is);
						} finally {
							is.close();
						}
						String path = props.getProperty("path");
						if (path != null && path.length() > 0) {
							File dir;
							if (path.startsWith(".")) {
								dir = new File(targetPlatform, path);
							} else {
								dir = new File(path);
							}
							dir = dir.getCanonicalFile();
							if (dir.isDirectory()) {
								result.add(dir);
							}
						}
					} catch (Exception e) {
						continue;
					}
				}
			}
		}
	}

	private static final String PLATFORM_BASE_PREFIX = "platform:/base/";
	private static final String FILE_PREFIX = "file:";
	
	private File parsePlatformURL(File platformBase, String url) {
		if (url == null) {
			return null;
		}

		url = url.replace('\\', '/');

		String relPath = null;
		if (url.startsWith(PLATFORM_BASE_PREFIX)) {
			relPath = url.substring(PLATFORM_BASE_PREFIX.length());
		} else if (url.startsWith(FILE_PREFIX)) {
			relPath = url.substring(FILE_PREFIX.length());
		}

		if (relPath == null) {
			return null;
		}
		
		return new File(platformBase, relPath);
	}

	private List<File> readBundlesTxt(File platformBase) throws IOException {
		File bundlesInfo = new File(platformBase, "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
		if (!bundlesInfo.isFile() || !bundlesInfo.canRead()) {
			return null;
		}

		ArrayList<File> plugins = new ArrayList<File>();

		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(bundlesInfo)));
		String line;
		try {
			while ((line = r.readLine()) != null) {
				if (line.startsWith("#")) //$NON-NLS-1$
					continue;
				line = line.trim();
				if (line.length() == 0)
					continue;

				// (expectedState is an integer).
				if (line.startsWith("org.eclipse.equinox.simpleconfigurator.baseUrl" + "=")) { //$NON-NLS-1$ //$NON-NLS-2$
					continue;
				}

				StringTokenizer tok = new StringTokenizer(line, ",");
				String symbolicName = tok.nextToken();
				String version = tok.nextToken();
				String location = tok.nextToken();
				
				plugins.add(parsePlatformURL(platformBase, location));
			}
		} finally {
			r.close();
		}
		
		return plugins;
	}

}
