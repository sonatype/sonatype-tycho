package org.codehaus.tycho.eclipsepackaging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
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
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * XXX dirty hack
 * 
 * @goal update-site
 */
public class UpdateSiteMojo extends AbstractMojo implements Contextualizable {

	private static final int KBYTE = 1024;

	/** @parameter expression="${project.build.directory}/site" */
	private File target;

	/** @parameter expression="${project.build.directory}/features" */
	private File features;
	
	/** @parameter expression="${project.basedir}" */
	private File basedir;

	private PlexusContainer plexus;

	/** @component */
	private ArtifactFactory artifactFactory;

	/** @component */
	private ArtifactResolver resolver;

	/** @parameter expression="${localRepository}" */
	private ArtifactRepository localRepository;

	/** @parameter expression="${session}" */
	private MavenSession session;

	/** @parameter */
	private boolean inlineArchives;

	private static final SAXBuilder builder = new SAXBuilder();
	private static final Format format = Format.getPrettyFormat();
	private static final XMLOutputter outputter = new XMLOutputter(format);

	public void execute() throws MojoExecutionException, MojoFailureException {
		target.mkdirs();

		try {
			File siteFile = new File(basedir, "site.xml");
			Document doc = builder.build(siteFile);
			Element root = doc.getRootElement();

			Map<String, String> archives = new HashMap<String, String>();
			for (Element archive : (List<Element>) root.getChildren("archive")) {
				String path = archive.getAttributeValue("path");
				String url = archive.getAttributeValue("url");
				archives.put(path, url);
			}

			List features = root.getChildren("feature");
			for (Iterator i = features.iterator(); i.hasNext(); ) {
				Element feature = (Element) i.next();

				packageFeature(feature, archives);
			}

			if (inlineArchives) {
				root.removeChildren("archive");
			}

			OutputStream os = new BufferedOutputStream(new FileOutputStream(new File(target, "site.xml")));
			try {
				outputter.output(doc, os);
			} finally {
				os.close();
			}
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void packageFeature(Element feature, Map<String, String> archives) throws Exception {
		String artifactId = feature.getAttribute("id").getValue();
		String version = expandVerstion(feature.getAttribute("version").getValue());

		File basedir = new File(this.basedir, "../" + artifactId).getAbsoluteFile();

		if (basedir.exists() && new File(basedir, "build.properties").exists()) {
			Document doc = builder.build(new File(basedir, "feature.xml"));
			List plugins = doc.getRootElement().getChildren("plugin");

			for (Iterator i = plugins.iterator(); i.hasNext(); ) {
				Element plugin = (Element) i.next();

				packagePlugin(plugin, archives);
			}

			doc.getRootElement().setAttribute("version", version);
			features.mkdirs();
			File featureFile = new File(features, artifactId + "-feature.xml");
			OutputStream os = new BufferedOutputStream(new FileOutputStream(featureFile));
			try {
				outputter.output(doc, os);
			} finally {
				os.close();
			}

			String url = "features/" + artifactId + "_" + version + ".jar";
			feature.setAttribute("url", url);
			feature.setAttribute("version", version);
			File outputJar = new File(target, url);
			outputJar.getParentFile().mkdirs();
			Properties props = new Properties();
			props.load(new FileInputStream(new File(basedir, "build.properties")));
			String[] binIncludes = props.getProperty("bin.includes").split(",");
			String files[] = Util.getIncludedFiles(basedir,	binIncludes);

			JarArchiver jarArchiver = (JarArchiver) plexus.lookup(JarArchiver.ROLE, "jar");
			jarArchiver.setDestFile(outputJar);

			for (int i = 0; i < files.length; i++) {
				String fileName = files[i];
				File f = "feature.xml".equals(fileName) ? featureFile: new File(basedir, fileName);
				if (!f.isDirectory())
				{
					jarArchiver.addFile(f, fileName);
				}
			}

			jarArchiver.createArchive();

		}
	}

	private static final SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HHmm");

	private String expandVerstion(String version) {
		if (version.endsWith(".qualifier")) {
			version = version.substring(0, version.lastIndexOf('.') + 1);
			version = version + df.format(new Date());
		}
		return version;
	}

	private void packagePlugin(Element plugin, Map<String, String> archives) throws Exception {
		String artifactId = plugin.getAttributeValue("id");
		String version = plugin.getAttributeValue("version");

		String path = "plugins/" + artifactId + "_" + version + ".jar";
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

		Artifact artifact = null;

		// we don't have good way to map Bundle-SymbolicName into groupId/artifactId
		// so lets try to guess
		Set groupIds = new LinkedHashSet();
		String groupId = artifactId;
		do {
			groupIds.add(groupId);
			int idx = groupId.lastIndexOf('.');
			groupId = idx > 0 ? groupId.substring(0, idx): null;
		} while (groupId != null);

		if ("0.0.0".equals(version)) {
			// look in the reactor first (is there a better way?)
			List projects = session.getSortedProjects();
			for (Iterator i = projects.iterator(); i.hasNext(); ) {
				MavenProject other = (MavenProject) i.next();
				if (artifactId.equals(other.getArtifactId()) && groupIds.contains(other.getGroupId())) {
					artifact = other.getArtifact();
					break;
				}
			}

			// find latest available
			if (artifact == null) {
				// XXX how do I find latest version?
			}
		}

		Exception exception = null;
		if (artifact == null) {
			if (version.endsWith(".qualifier")) {
				version = version.substring(0, version.lastIndexOf('.')) + "-SNAPSHOT";
			}
			for (Iterator i = groupIds.iterator(); i.hasNext(); ) {
				groupId = (String) i.next();
				artifact = artifactFactory.createBuildArtifact(groupId, artifactId, version, "jar");
				try {
					resolver.resolve(artifact, new ArrayList(), localRepository);
					break;
				} catch (ArtifactNotFoundException e) {
					exception = e;
				}
			}
		}

		if (artifact == null || artifact.getFile() == null || !artifact.getFile().exists()) {
			throw new MojoExecutionException("Can't find artifact for bundle " + artifactId, exception);
		}

		Manifest mf;
		JarFile jar = new JarFile(artifact.getFile());
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
		plugin.setAttribute("version", bundleVersion);

		File outputJar = new File(target, "plugins/" + artifactId + "_" + bundleVersion + ".jar");
		outputJar.getParentFile().mkdirs();
		FileUtils.copyFile(artifact.getFile(), outputJar);

		plugin.setAttribute("download-size", Long.toString(outputJar.length() / KBYTE));
		plugin.setAttribute("install-size", Long.toString(installSize / KBYTE) );
	}

	public void contextualize(Context ctx) throws ContextException {
		plexus = (PlexusContainer) ctx.get( PlexusConstants.PLEXUS_KEY );
	}

}
