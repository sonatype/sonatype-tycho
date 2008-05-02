package org.codehaus.tycho.osgitools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class BundleFile {

	private Manifest manifest;

	private Properties p = new Properties();

	public BundleFile(Manifest manifest, File file) {
		this.manifest = manifest;

		String localization = manifest.getMainAttributes().getValue("Bundle-Localization");
		if (!file.getName().equalsIgnoreCase("manifest.mf") && localization != null) {
			try {
				if (file.isDirectory()) {
					File localizationFile = new File(file, localization
							+ ".properties");
					if (localizationFile.exists()) {
						FileInputStream fis = new FileInputStream(localizationFile);
						p.load(fis);
						fis.close();
					}
				} else {
					JarFile jar = new JarFile(file, false);
					try {
						JarEntry je = jar.getJarEntry(localization + ".properties");
						if (je != null) {
							p.load(jar.getInputStream(je));
						}
					} finally {
						jar.close();
					}
				}
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	public boolean isBundle() {
		return manifest != null
				&& manifest.getMainAttributes().getValue("Bundle-SymbolicName") != null;
	}

	public String getSymbolicName() {
		String symbolicName = manifest.getMainAttributes().getValue(
				"Bundle-SymbolicName");
		return symbolicName != null ? symbolicName.split(";")[0] : null;
	}

	public String getVersion() {
		return manifest.getMainAttributes().getValue("Bundle-Version");
	}

	public String getName() {
		return getAttribute("Bundle-Name");
	}

	public String getDescription() {
		return getAttribute("Bundle-Description");
	}

	public String getCopyright() {
		return getAttribute("Bundle-Copyright");
	}

	public String getURL() {
		return getAttribute("Bundle-DocUrl");
	}

	public String getOrganization() {
		return getAttribute("Bundle-Vendor");
	}

	public String getAttribute(String attribute) {
		String result = manifest.getMainAttributes().getValue(attribute);
		if (result == null) {
			return null;
		}

		if (result.startsWith("%")) {
			String v = p.getProperty(result.substring(1));
			if (v != null) {
				result = v;
			}
		}

		return result;
	}

	public String getContactAddress() {
		return getAttribute("Bundle-ContactAddress");
	}
}
