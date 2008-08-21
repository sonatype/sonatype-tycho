package org.codehaus.tycho.eclipsepackaging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.IFeatureRef;
import org.codehaus.tycho.model.UpdateSite;
import org.codehaus.tycho.osgitools.OsgiState;
import org.eclipse.osgi.service.resolver.BundleDescription;

/**
 * XXX dirty hack
 * 
 * @goal update-site
 */
public class UpdateSiteMojo extends AbstractMojo implements Contextualizable {

	private static final int KBYTE = 1024;

	/** @component */
	private OsgiState state;

	/** @parameter expression="${project.build.directory}/site" */
	private File target;

	/** @parameter expression="${project.build.directory}/features" */
	private File features;
	
	/** @parameter expression="${project.basedir}" */
	private File basedir;

	private PlexusContainer plexus;

	/** @parameter */
	private boolean inlineArchives;

	public void execute() throws MojoExecutionException, MojoFailureException {
		target.mkdirs();

		try {

			UpdateSite site = UpdateSite.read(new File(basedir, "site.xml"));

			Map<String, String> archives = site.getArchives();

			for (UpdateSite.FeatureRef feature : site.getFeatures()) {
				packageFeature(feature, archives);
			}

			if (inlineArchives) {
				site.removeArchives();
			}

			UpdateSite.write(site, new File(target, "site.xml"));
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void packageFeature(IFeatureRef featureRef, Map<String, String> archives) throws Exception {
		Feature feature = state.getFeature(featureRef.getId(), featureRef.getVersion());

		if (feature == null) {
			return;
		}

		String artifactId = feature.getId();
		String version = expandVerstion(feature.getVersion());

		String url = "features/" + artifactId + "_" + version + ".jar";
		File outputJar = new File(target, url);


		if (!outputJar.canRead()) {

			MavenProject featureProject = state.getMavenProject(feature);
			if (featureProject != null) {
				feature = new Feature(feature);

				for (Feature.PluginRef plugin : feature.getPlugins()) {
					packagePlugin(plugin, archives);
				}
	
				for (IFeatureRef includedRef : feature.getIncludedFeatures()) {
					packageFeature(includedRef, archives);
				}
	
				feature.setVersion(version);
				features.mkdirs();
				File featureFile = new File(features, artifactId + "-feature.xml");
				Feature.write(feature, featureFile);
	
				outputJar.getParentFile().mkdirs();
				Properties props = new Properties();
				props.load(new FileInputStream(new File(featureProject.getBasedir(), "build.properties")));
				String[] binIncludes = props.getProperty("bin.includes").split(",");
				String files[] = Util.getIncludedFiles(featureProject.getBasedir(),	binIncludes);
	
				JarArchiver jarArchiver = (JarArchiver) plexus.lookup(JarArchiver.ROLE, "jar");
				jarArchiver.setDestFile(outputJar);
	
				for (int i = 0; i < files.length; i++) {
					String fileName = files[i];
					File f = "feature.xml".equals(fileName) ? featureFile: new File(featureProject.getBasedir(), fileName);
					if (!f.isDirectory())
					{
						jarArchiver.addFile(f, fileName);
					}
				}
	
				jarArchiver.createArchive();
			} else {
				// TODO include external features
			}
		}

		if (featureRef instanceof UpdateSite.FeatureRef) {
			((UpdateSite.FeatureRef) featureRef).setUrl(url);
		}
		featureRef.setVersion(version);
	}

	private static final SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HHmm");

	private String expandVerstion(String version) {
		if (version.endsWith(".qualifier")) {
			version = version.substring(0, version.lastIndexOf('.') + 1);
			version = version + df.format(new Date());
		}
		return version;
	}

	private void packagePlugin(Feature.PluginRef plugin, Map<String, String> archives) throws Exception {
		String bundleId = plugin.getId();
		String version = plugin.getVersion();

		String path = "plugins/" + bundleId + "_" + version + ".jar";
		if (archives.containsKey(path)) {
			if (inlineArchives) {
				URL url = new URL(archives.get(path));
				InputStream is = url.openStream();
				try {
					OutputStream os = new BufferedOutputStream(new FileOutputStream(new File(target, path)));
					try {
						IOUtil.copy(is, os);
					} finally {
						os.close();
					}
				} finally {
					is.close();
				}
			}
			return;
		}

		if ("0.0.0".equals(version)) {
			version = OsgiState.HIGHEST_VERSION;
		}

		BundleDescription bundle = state.getBundleDescription(bundleId, version);
		if (bundle == null) {
			throw new MojoExecutionException("Can't find bundle " + bundleId);
		}

		File file;
		MavenProject bundleProject = state.getMavenProject(bundle);
		if (bundleProject != null) {
			file = bundleProject.getArtifact().getFile();
		} else {
			file = new File(bundle.getLocation());
			if (file.isDirectory()) {
				throw new MojoExecutionException("Directory based bundle " + bundleId);
			}
		}

		Manifest mf;
		JarFile jar = new JarFile(file);
		long installSize = 0;
		try {
			mf = jar.getManifest();
			
			Enumeration entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = (JarEntry) entries.nextElement();
				long entrySize = entry.getSize();
				if (entrySize > 0) {
					installSize += entrySize;
				}
			}
		} finally {
			jar.close();
		}

		String bundleVersion = mf.getMainAttributes().getValue("Bundle-Version");
		plugin.setVersion(bundleVersion);

		File outputJar = new File(target, "plugins/" + bundleId + "_" + bundleVersion + ".jar");
		outputJar.getParentFile().mkdirs();
		FileUtils.copyFile(file, outputJar);

		plugin.setDownloadSide(outputJar.length() / KBYTE);
		plugin.setInstallSize(installSize / KBYTE);
	}

	public void contextualize(Context ctx) throws ContextException {
		plexus = (PlexusContainer) ctx.get( PlexusConstants.PLEXUS_KEY );
	}

}
