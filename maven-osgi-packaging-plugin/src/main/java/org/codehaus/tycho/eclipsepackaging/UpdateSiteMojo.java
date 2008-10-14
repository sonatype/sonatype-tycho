package org.codehaus.tycho.eclipsepackaging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.jar.JarSignMojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.gzip.GZipCompressor;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest.Attribute;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.tycho.eclipsepackaging.pack200.Pack200Archiver;
import org.codehaus.tycho.eclipsepackaging.product.Plugin;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.IFeatureRef;
import org.codehaus.tycho.model.PluginRef;
import org.codehaus.tycho.model.UpdateSite;
import org.codehaus.tycho.osgitools.OsgiState;
import org.codehaus.tycho.osgitools.features.FeatureDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;

/**
 * @goal update-site
 */
public class UpdateSiteMojo extends AbstractMojo implements Contextualizable {

	private static final int KBYTE = 1024;

	/** @component */
	private OsgiState state;

	/** @component */
	private org.apache.maven.artifact.factory.ArtifactFactory artifactFactory;

	/** @component */
	private org.apache.maven.artifact.resolver.ArtifactResolver resolver;

	/**@parameter expression="${localRepository}" */
	private org.apache.maven.artifact.repository.ArtifactRepository localRepository;

	/** @parameter expression="${project.remoteArtifactRepositories}" */
	private java.util.List<ArtifactRepository> remoteRepositories;

	/** @parameter expression="${project.build.directory}/site" */
	private File target;

	/** @parameter expression="${project.build.directory}/features" */
	private File features;

	/** @parameter expression="${project.build.directory}/plugins" */
	private File plugins;
	
	/** @parameter expression="${project.basedir}" */
	private File basedir;

	private PlexusContainer plexus;

	/** @parameter */
	private boolean inlineArchives;

	/**
	 * @parameter expression="${buildNumber}"
	 */
	protected String qualifier;

	/**
	 * @parameter expression="${project}"
	 * @required
	 */
	protected MavenProject project;
	
	/**
	 * When true you must inform the following parameters too:
	 * keystore, storepass, alias and keypass
	 * 
	 * @parameter expression="${sign}" default-value="false"
	 */
	private boolean sign;

    /**
     * Tells the keystore location
     * 
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${keystore}"
     */
    private File keystore;

    /**
     * Specifies the password which is required to access the keystore.
     * 
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${storepass}"
     */
    private String storepass;

    /**
     * The alias for the keystore entry containing the private key needed to generate the signature
     * 
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${alias}"
     */
    private String alias;

    /**
     * Specifies the password used to protect the private key of the keystore entry addressed by the alias
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${keypass}"
     */
    private String keypass;
	
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		target.mkdirs();

		try {

			UpdateSite site = UpdateSite.read(new File(basedir, "site.xml"));

			Map<String, String> archives = site.getArchives();

			for (UpdateSite.FeatureRef feature : site.getFeatures()) {
				packageFeature(feature, archives, site.isPack200());
			}

			if (inlineArchives) {
				site.removeArchives();
			}

			File file = new File(target, "site.xml");
			UpdateSite.write(site, file);

			project.getArtifact().setFile(file);
		} catch (MojoExecutionException e) {
			throw e;
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void packageFeature(IFeatureRef featureRef, Map<String, String> archives, boolean isPack200) throws Exception {
		FeatureDescription feature = state.getFeatureDescription(featureRef.getId(), featureRef.getVersion());

		if (feature == null) { 
			throw new ArtifactResolutionException("Feature " + featureRef.getId() + " not found", "", featureRef.getId(), featureRef.getVersion(), "eclipse-feature", null, null)  ;
		}

		String artifactId = feature.getId();
		String version = expandVerstion(feature.getVersion().toString());

		String url = "features/" + artifactId + "_" + version + ".jar";
		File outputJar = new File(target, url);

		if (!outputJar.canRead()) {

			MavenProject featureProject = state.getMavenProject(feature.getFeature());
			Properties props = new Properties();
			if (featureProject != null) {
				props.load(new FileInputStream(new File(featureProject.getBasedir(), "build.properties")));

				packageIncludedArtifacts(feature, props, archives, isPack200);
	
				feature.getFeature().setVersion(version);

				File featureDir = new File(features, artifactId);
				featureDir.mkdirs();
				unpackToDir(featureProject.getArtifact().getFile(), featureDir);
				Feature.write(feature.getFeature(), new File(featureDir, Feature.FEATURE_XML));
				outputJar.getParentFile().mkdirs();
				packDir(featureDir, outputJar);
//				FileUtils.copyFile(featureProject.getArtifact().getFile(), outputJar);
				
				if(sign) {
					signJar(outputJar);
				}
				if(isPack200) {
			        shipPack200(outputJar, url);
				}
				
			} else {
				packageIncludedArtifacts(feature, props, archives, isPack200);

				File src = feature.getLocation();
				if (src.isDirectory()) {
					packDir(src, outputJar);
				} else {
					FileUtils.copyFile(src, outputJar);
				}
			}
		}

		if (featureRef instanceof UpdateSite.FeatureRef) {
			((UpdateSite.FeatureRef) featureRef).setUrl(url);
		}
		featureRef.setVersion(version);
	}

	private void packageIncludedArtifacts(FeatureDescription feature, Properties buildProperties, Map<String, String> archives, boolean isPack200) throws Exception {
		List<PluginRef> plugins = feature.getFeature().getPlugins();
		for (PluginRef plugin : plugins) {
			String pluginId = plugin.getId();
			//check if should be generated
			String key = "generate.plugin@" + pluginId;
			if(buildProperties.containsKey(key)) {
				//plugins copy
				List<PluginRef> filteredPlugins = new ArrayList<PluginRef>(plugins);
				//generate source plugin shouldn't be present at generation
				filteredPlugins.remove(plugin);
				
				generateSourcePlugin(pluginId, filteredPlugins, feature.getVersion().toString(), isPack200);
			} else {
				packagePlugin(plugin, archives, isPack200);
			}
		}

		for (IFeatureRef includedRef : feature.getFeature().getIncludedFeatures()) {
//					String key = "generate.feature@" + includedRef.getId();
			//check if should be generated
//					if(props.containsKey(key)) {
//						boolean individualSourceBundle = "true".equals(props.getProperty("individualSourceBundles"));
//						if(individualSourceBundle) {
//							generateIndividualSourceFeature(includedRef, props.getProperty(key), isPack200);
//						} else {
//							generateSourceFeature(includedRef, props.getProperty(key), isPack200);
//						}
//					} else {
				packageFeature(includedRef, archives, isPack200);
//					}
		}
	}

	private void packDir(File sourceDir, File targetZip) throws MojoExecutionException {
		ZipArchiver archiver ;
		try {
			archiver = (ZipArchiver) plexus.lookup(ZipArchiver.ROLE, "zip");
		} catch (ComponentLookupException e) {
			throw new MojoExecutionException("Unable to resolve ZipArchiver", e);
		}
		
		archiver.setDestFile(targetZip);
		try {
			archiver.addDirectory(sourceDir);
			archiver.createArchive();
		} catch (Exception e) {
			throw new MojoExecutionException("Error packing zip", e);
		}
	}

	private void unpackToDir(File sourceZip, File targetDir) throws MojoExecutionException {
		ZipUnArchiver unArchiver ;
		try {
			unArchiver = (ZipUnArchiver) plexus.lookup(ZipUnArchiver.ROLE, "zip");
		} catch (ComponentLookupException e) {
			throw new MojoExecutionException("Unable to resolve ZipUnArchiver", e);
		}
		
		unArchiver.setSourceFile(sourceZip);
		unArchiver.setDestDirectory(targetDir);
		try {
			unArchiver.extract();
		} catch (ArchiverException e) {
			throw new MojoExecutionException("Error extracting zip", e);
		}
	}

	private void generateIndividualSourceFeature(IFeatureRef generateFeature, String baseFeatureId, boolean isPack200) throws Exception {
		Feature baseFeature = state.getFeature(baseFeatureId, generateFeature.getVersion());

		String artifactId = generateFeature.getId();
		String version = baseFeature.getVersion();
		generateFeature.setVersion(version);
		List<Plugin> plugins = new ArrayList<Plugin>();

		
		for (PluginRef pluginRef : baseFeature.getPlugins()) {
			String bundleId = pluginRef.getId();
			String bundleVersion = getPluginVersion(pluginRef);
			plugins.add(new Plugin(bundleId + ".source", bundleVersion));
			
			generateIndividualSourceBundle(bundleId, bundleVersion, isPack200);
		}

		generateSourceFeature(isPack200, artifactId, version, plugins);
		
	}

	private void generateSourceFeature(IFeatureRef generateFeature, String baseFeatureId,
			boolean isPack200) throws Exception {
		Feature baseFeature = state.getFeature(baseFeatureId, generateFeature.getVersion());

		String artifactId = generateFeature.getId();
		String version = baseFeature.getVersion();
		generateFeature.setVersion(version);
		
		List<Plugin> plugins = new ArrayList<Plugin>();
		plugins.add(new Plugin(artifactId, version));

		generateSourceFeature(isPack200, artifactId, version, plugins);

		generateSourcePlugin(artifactId, baseFeature.getPlugins(), version, isPack200);
	}

	private void generateSourceFeature(boolean isPack200, String artifactId,
			String version, List<Plugin> plugins) throws IOException, ComponentLookupException,
			ArchiverException, MojoExecutionException {
		File featureFile = new File(features, artifactId + "-feature.xml");
		
		//TODO check if tycho already has something to write a new feature
		FileWriter fw = new FileWriter(featureFile);
		fw.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append('\n');
		fw.append("<feature id=\"" + artifactId + "\" version=\"" + version + "\" primary=\"false\" >").append('\n');
		for (Plugin plugin : plugins) {
			fw.append("\t<plugin id=\"" + plugin.getId() + "\" version=\"" + plugin.getVersion() + "\" />").append('\n');
		}
		fw.append("</feature>").append('\n');
		fw.flush();
		fw.close();

		String url = "features/" + artifactId + "_" + version + ".jar";
		File outputJar = new File(target, url);

		//TODO refactor to promote reuse with packageFeature
		JarArchiver jarArchiver = (JarArchiver) plexus.lookup(JarArchiver.ROLE, "jar");
		jarArchiver.setDestFile(outputJar);
		jarArchiver.addFile(featureFile, "feature.xml");
		jarArchiver.createArchive();

		if(sign) {
			signJar(outputJar);
		}
		if(isPack200) {
	        shipPack200(outputJar, url);
		}
	}

	private void generateIndividualSourceBundle(String bundleId,
			String bundleVersion, boolean isPack200) throws Exception {
		File pluginFolder = new File(this.plugins, bundleId );
		pluginFolder.mkdirs();

		Artifact bundleSourceArtifact = getSourceBundle(bundleId, bundleVersion);
		Util.extractJar(bundleSourceArtifact.getFile(), pluginFolder);
		
		org.codehaus.plexus.archiver.jar.Manifest manifest = new org.codehaus.plexus.archiver.jar.Manifest(new FileReader(new File(pluginFolder, "META-INF/MANIFEST.MF")));
		manifest.getMainSection().addAttributeAndCheck(new Attribute("Eclipse-SourceBundle", bundleId + ";version=\"" + bundleSourceArtifact.getVersion() + "\""));
		
		String url = "plugins/" + bundleId + ".source_" + bundleSourceArtifact.getVersion() + ".jar";
		File outputJar = new File(target, url);

		JarArchiver jarArchiver = (JarArchiver) plexus.lookup(JarArchiver.ROLE, "jar");
		jarArchiver.setDestFile(outputJar);
		jarArchiver.addDirectory(pluginFolder);
		jarArchiver.addConfiguredManifest(manifest);
		jarArchiver.createArchive();

		if(sign) {
			signJar(outputJar);
		}
		if(isPack200) {
	        shipPack200(outputJar, url);
		}
	}
	
	private void generateSourcePlugin(String artifactId, List<PluginRef> plugins, String version, boolean isPack200) throws Exception {
		File pluginFolder = new File(this.plugins, artifactId );
		pluginFolder.mkdirs();

		File pluginFile = new File(pluginFolder, "plugin.xml");
		
		//TODO check if tycho already has something to write a new feature
		FileWriter fw = new FileWriter(pluginFile);
		fw.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append('\n');
		fw.append("<?eclipse version=\"3.0\"?>").append('\n');
		fw.append("<plugin>").append('\n');
		fw.append("\t<extension point = \"org.eclipse.pde.core.source\">").append('\n');
		fw.append("\t\t<location path=\"src\" />").append('\n');
		fw.append("\t</extension>").append('\n');
		fw.append("</plugin>").append('\n');
		fw.flush();
		fw.close();
		
		for (PluginRef pluginRef : plugins) {
			String bundleId = pluginRef.getId();
			String bundleVersion = getPluginVersion(pluginRef);
			
			Artifact bundleSourceArtifact = getSourceBundle(bundleId, bundleVersion);
			
			File pluginSrc = new File(pluginFolder, "src/" + bundleId + "_" + bundleSourceArtifact.getVersion() + "/src.zip");
			FileUtils.copyFile(bundleSourceArtifact.getFile(), pluginSrc);
		}

		String url = "plugins/" + artifactId + "_" + version + ".jar";
		File outputJar = new File(target, url);

		//TODO refactor to promote reuse with packageFeature
		JarArchiver jarArchiver = (JarArchiver) plexus.lookup(JarArchiver.ROLE, "jar");
		jarArchiver.setDestFile(outputJar);
		jarArchiver.addDirectory(pluginFolder);
		jarArchiver.createArchive();

		if(sign) {
			signJar(outputJar);
		}
		if(isPack200) {
	        shipPack200(outputJar, url);
		}
		
	}

	private Artifact getSourceBundle(String bundleId, String bundleVersion) throws MojoExecutionException, ArtifactResolutionException, ArtifactNotFoundException {
		BundleDescription bundle = state.getBundleDescription(bundleId, bundleVersion);
		if (bundle == null) {
			throw new MojoExecutionException("Can't find bundle " + bundleId);
		}
		MavenProject bundleProject = state.getMavenProject(bundle);

		//resolve source artifact
		Artifact bundleSourceArtifact = artifactFactory.createArtifactWithClassifier(bundleProject.getGroupId(), bundleProject.getArtifactId(), bundleProject.getVersion(), "jar", "sources");
		resolver.resolve(bundleSourceArtifact, remoteRepositories, localRepository);
		return bundleSourceArtifact;
	}

	private String getPluginVersion(PluginRef pluginRef) throws MojoExecutionException {
		String bundleVersion = pluginRef.getVersion();
		
		if ("0.0.0".equals(bundleVersion)) {
			bundleVersion = OsgiState.HIGHEST_VERSION;
		}
		
		BundleDescription bundle = state.getBundleDescription(pluginRef.getId(), bundleVersion);
		if (bundle == null) {
			throw new MojoExecutionException("Can't find bundle " + pluginRef.getId());
		}
		MavenProject bundleProject = state.getMavenProject(bundle);
		
		return bundleProject.getVersion();
	}

	private String expandVerstion(String version) {
		if (qualifier != null && version.endsWith(".qualifier")) {
			version = version.substring(0, version.lastIndexOf('.') + 1);
			version = version + qualifier;
		}
		return version;
	}

	private void packagePlugin(PluginRef plugin, Map<String, String> archives, boolean isPack200) throws Exception {
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
			
			Enumeration<JarEntry> entries = jar.entries();
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

		String url = "plugins/" + bundleId + "_" + bundleVersion + ".jar";
		File outputJar = new File(target, url);
		outputJar.getParentFile().mkdirs();
		FileUtils.copyFile(file, outputJar);

		if(sign) {
			signJar(outputJar);
		}
		if(isPack200) {
	        shipPack200(outputJar, url);
		}
		

		plugin.setDownloadSide(outputJar.length() / KBYTE);
		plugin.setInstallSize(installSize / KBYTE);
	}

	private void shipPack200(File jar, String url)
			throws ArchiverException, IOException, ComponentLookupException {
		File outputPack = new File(jar.getParentFile(), jar.getName() + ".pack");
		
		Pack200Archiver packArchiver = new Pack200Archiver();
		packArchiver.setSourceJar( jar );
		packArchiver.setDestFile( outputPack );
		packArchiver.createArchive();
		
		GZipCompressor gzCompressor = new GZipCompressor();
		gzCompressor.setDestFile(new File(jar.getParentFile(), jar.getName() + ".pack.gz"));
		gzCompressor.setSourceFile(outputPack);
		gzCompressor.execute();
		
		outputPack.delete();
	}

	private void signJar(File outputJar) throws MojoExecutionException {
		if(keystore == null) {
			keystore = new File(System.getProperty("user.home"), ".keystore");
		}
		
		if(!keystore.exists()) {
			getLog().warn("Unable to sign, keystore file not found: "+keystore.getAbsolutePath());
			return;
		}
		if(storepass == null) {
			getLog().warn("Unable to sign, keystore password must be specifyed.");
			return;
		}
		
		if(alias == null) {
			getLog().warn("Unable to sign, alias must be specifyed.");
			return;
		}
		if(keypass == null) {
			getLog().warn("Unable to sign, keypass must be specifyed.");
			return;
		}
		
		JarSignMojo sign = new JarSignMojo();
		sign.setKeystore(keystore.getAbsolutePath());
		sign.setStorepass(storepass);
		sign.setAlias(alias);
		sign.setKeypass(keypass);
		sign.setJarPath(outputJar);
		sign.setBasedir(basedir);
		sign.setWorkingDir(basedir);
		sign.setLog(getLog());
		sign.setVerbose(false);
		sign.setVerify(false);
		sign.execute();
	}

	public void contextualize(Context ctx) throws ContextException {
		plexus = (PlexusContainer) ctx.get( PlexusConstants.PLEXUS_KEY );
	}

}
