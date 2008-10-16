package org.codehaus.tycho.p2;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.tycho.osgitools.utils.PlatformPropertiesUtils;
import org.eclipse.core.runtime.adaptor.EclipseStarter;

/**
 * @plexus.component role="org.codehaus.tycho.p2.P2"
 */
public class DefaultP2 extends AbstractLogEnabled implements P2 {

	public String materializeTargetPlatform(String key, List<String> repositories, List<Artifact> rootIUs, Properties props) throws MavenExecutionException {
		System.setProperty("osgi.framework.useSystemProperties", "false"); //$NON-NLS-1$ //$NON-NLS-2$

		try {
			Map<String, String> properties = new HashMap<String, String>();
			properties.put("osgi.install.area", getP2RuntimeLocation());
			properties.put("osgi.syspath", getP2RuntimeLocation() + "/plugins");
			properties.put("osgi.configuration.area", getP2RuntimeLocation() + "/configuration");

			properties.put("eclipse.p2.data.area", getPPBase() + "/p2");

			String[] args = {
					"-application", "org.codehaus.tycho.p2.materializeTargetPlatform",
					"-install", getP2RuntimeLocation(),
//					"-console", 
//					"-debug", "-consolelog", 
//					"-noexit",
//					"-initialize", "-clean",
			};

			Map<String, Object> appArgs = new HashMap<String, Object>();
			appArgs.put("-p2base", getPPBase());
			appArgs.put("-profile", key);
			appArgs.put("-repositories", repositories);
			appArgs.put("-environments", getEnvironments(props));
			Map<String, String> rootIUVersionMap = new HashMap<String, String>();
			for (Artifact rootIU : rootIUs) {
				rootIUVersionMap.put(rootIU.getArtifactId(), rootIU.getVersion());
			}
			appArgs.put("-rootIUs", rootIUVersionMap);
			appArgs.put("-logger", getLogger());

			EclipseStarter.setInitialProperties(properties);
			EclipseStarter.startup(args, null);
			try {
				return (String) EclipseStarter.run(appArgs);
			} finally {
				EclipseStarter.shutdown();
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new MavenExecutionException(e.getMessage(), (IOException) null);
		}
	}

	private String getPPBase() throws MavenExecutionException {
		File bundleLocation;
		String property = System.getProperty("tycho.p2.data");
		if (property != null) {
			bundleLocation = new File(property);
		} else {
			bundleLocation = new File(System.getProperty("user.home"), ".m2/p2");			
		}
		try {
			bundleLocation.mkdirs();
			String location = bundleLocation.getCanonicalPath().replace('\\', '/');
			getLogger().info("Using p2 data area " + location);
			return location;
		} catch (IOException e) {
			throw new MavenExecutionException("Can't determine p2 bundle pool location", e);
		}
	}

	public String getP2RuntimeLocation() throws  MavenExecutionException {
		// nasty hack
		File ppLocation;
		String property = System.getProperty("tycho.p2.location");
		if (property == null) {
			property = System.getProperty("maven.home");
			if (property == null) {
				throw new MavenExecutionException("Can't locate p2 installation location (either maven.home or tycho.p2.location system property must be set)", (IOException) null);
			}
			ppLocation = new File(property, "p2");
		} else {
			ppLocation = new File(property);
		}

		try {
			String location = new File(ppLocation, "eclipse").getCanonicalPath().replace('\\', '/');
			getLogger().info("Using tycho p2 runtime " + location);
			return location;
		} catch (IOException e) {
			throw new MavenExecutionException("Can't locate p2 installation location", e);
		}
	}

	private String getEnvironments(Properties props) {
		StringBuilder sb = new StringBuilder();
		sb.append("osgi.os=").append(PlatformPropertiesUtils.getOS(props));
		sb.append(",osgi.ws=").append(PlatformPropertiesUtils.getWS(props));
		sb.append(",osgi.arch=").append(PlatformPropertiesUtils.getArch(props));
		return sb.toString();
	}
}
