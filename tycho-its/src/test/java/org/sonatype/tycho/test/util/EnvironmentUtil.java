package org.sonatype.tycho.test.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class EnvironmentUtil {

	private static final Properties props;

	static {
		props = new Properties();
		ClassLoader cl = AbstractTychoIntegrationTest.class.getClassLoader();
		InputStream is = cl.getResourceAsStream("baseTest.properties");
		if (is != null) {
			try {
				try {
					props.load(is);
				} finally {
					is.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	static synchronized String getProperty(String key) {
		return props.getProperty(key);
	}

	private static final String WINDOWS_OS = "windows";

	private static final String MAC_OS = "mac os x";

	private static final String MAC_OS_DARWIN = "darwin";

	private static final String LINUX_OS = "linux";

	private static final String OS = System.getProperty("os.name")
			.toLowerCase();

	public static boolean isWindows() {
		return OS.startsWith(WINDOWS_OS);
	}

	public static boolean isLinux() {
		return OS.startsWith(LINUX_OS);
	}

	public static boolean isMac() {
		return OS.startsWith(MAC_OS) || OS.startsWith(MAC_OS_DARWIN);
	}

	// TODO find a more reliable way
	public static boolean isEclipse32Platform() {
		return new File(getTargetPlatforn(), "startup.jar").exists();
	}

	public static String getLocalRepo() {
		return getProperty("local-repo");
	}

	public static String getTargetPlatforn() {
		return getProperty("eclipse-dir");
	}

	public static String getTychoHome() {
		return getProperty("tycho-dir");
	}

	public static int getHttpServerPort() {
		String port = getProperty("server-port");
		return Integer.parseInt(port);
	}

}
