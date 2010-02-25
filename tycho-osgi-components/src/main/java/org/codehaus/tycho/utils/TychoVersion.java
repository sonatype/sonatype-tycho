package org.codehaus.tycho.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TychoVersion {

	private static final String DEFAULT_TYCHO_VERSION = "0.8.0-SNAPSHOT";

	public static String getTychoVersion() {
		ClassLoader cl = TychoVersion.class.getClassLoader();
		InputStream is = cl.getResourceAsStream("META-INF/maven/org.sonatype.tycho/tycho-osgi-components/pom.properties");
		String version = DEFAULT_TYCHO_VERSION;
		if (is != null) {
			try {
				try {
					Properties p = new Properties();
					p.load(is);
					version = p.getProperty("version", DEFAULT_TYCHO_VERSION);
				} finally {
					is.close();
				}
			} catch (IOException e) {
				// getLogger().debug("Could not read pom.properties", e);
			}
		}
		return version;
	}

}
