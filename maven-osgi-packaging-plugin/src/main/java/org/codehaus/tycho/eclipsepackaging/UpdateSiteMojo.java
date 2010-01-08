package org.codehaus.tycho.eclipsepackaging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.gzip.GZipCompressor;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.tycho.MavenSessionUtils;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.buildversion.VersioningHelper;
import org.codehaus.tycho.eclipsepackaging.pack200.Pack200Archiver;
import org.codehaus.tycho.model.IFeatureRef;
import org.codehaus.tycho.model.PluginRef;
import org.codehaus.tycho.model.UpdateSite;
import org.codehaus.tycho.osgitools.features.FeatureDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;

/**
 * @goal update-site
 */
public class UpdateSiteMojo extends AbstractTychoPackagingMojo {

	/** @component */
	private RepositorySystem repositorySystem;

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

	/** @parameter */
	private boolean inlineArchives;
	
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
	    initializeProjectContext();
	    
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
		String featureId = featureRef.getId();
		String featureVersion = featureRef.getVersion();

		if ("0.0.0".equals(featureVersion)) {
			featureVersion = TychoConstants.HIGHEST_VERSION;
		}

		FeatureDescription feature = featureResolutionState.getFeature(featureId, featureVersion);

		if (feature == null) { 
			throw new ArtifactResolutionException("Feature " + featureId + " not found", "", featureId, featureVersion, "eclipse-feature", null, null)  ;
		}

		String artifactId = feature.getId();
		String version = VersioningHelper.getExpandedVersion(session, feature);

		String url = "features/" + artifactId + "_" + version + ".jar";
		File outputJar = new File(target, url);

		if (!outputJar.canRead()) {

			MavenProject featureProject = MavenSessionUtils.getMavenProject(session, feature.getLocation());
			Properties props = new Properties();
			if (featureProject != null) {
				props.load(new FileInputStream(new File(featureProject.getBasedir(), "build.properties")));

				packageIncludedArtifacts(feature, props, archives, isPack200);
	
				FileUtils.copyFile(featureProject.getArtifact().getFile(), outputJar);
				
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
		BundleDescription bundle = bundleResolutionState.getBundle(bundleId, bundleVersion);
		if (bundle == null) {
			throw new MojoExecutionException("Can't find bundle " + bundleId);
		}
		MavenProject bundleProject = MavenSessionUtils.getMavenProject(session, bundle.getLocation());

		//resolve source artifact
		Artifact bundleSourceArtifact = repositorySystem.createArtifactWithClassifier(bundleProject.getGroupId(), bundleProject.getArtifactId(), bundleProject.getVersion(), "jar", "sources");
		ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        repositorySystem.resolve(request);
		return bundleSourceArtifact;
	}

	private String getPluginVersion(PluginRef pluginRef) throws MojoExecutionException {
		String bundleVersion = pluginRef.getVersion();
		
		if ("0.0.0".equals(bundleVersion)) {
			bundleVersion = TychoConstants.HIGHEST_VERSION;
		}
		
		BundleDescription bundle = bundleResolutionState.getBundle(pluginRef.getId(), bundleVersion);
		if (bundle == null) {
			throw new MojoExecutionException("Can't find bundle " + pluginRef.getId());
		}
		MavenProject bundleProject = MavenSessionUtils.getMavenProject(session, bundle.getLocation());
		
		return bundleProject.getVersion();
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
			version = TychoConstants.HIGHEST_VERSION;
		}

		BundleDescription bundle = bundleResolutionState.getBundle(bundleId, version);
		if (bundle == null) {
			throw new MojoExecutionException("Can't find bundle " + bundleId);
		}

		File file;
		MavenProject bundleProject = MavenSessionUtils.getMavenProject(session, bundle.getLocation());
		if (bundleProject != null) {
			file = bundleProject.getArtifact().getFile();
			if (file.isDirectory()) {
				throw new MojoExecutionException("Bundle project " + bundleProject.getId() + " artifact is a directory. The build should at least run ``package'' phase.");
			}
		} else {
			file = new File(bundle.getLocation());
		}

		String bundleVersion = VersioningHelper.getExpandedVersion(session, bundle);

		String url = "plugins/" + bundleId + "_" + bundleVersion + ".jar";
		File outputJar = new File(target, url);
		outputJar.getParentFile().mkdirs();
		if (file.isDirectory()) {
			packDir(file, outputJar);
		} else {
			FileUtils.copyFile(file, outputJar);
		}

		if(sign) {
			signJar(outputJar);
		}
		if(isPack200) {
	        shipPack200(outputJar, url);
		}
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
		
//		JarSignMojo sign = new JarSignMojo();
//		sign.setKeystore(keystore.getAbsolutePath());
//		sign.setStorepass(storepass);
//		sign.setAlias(alias);
//		sign.setKeypass(keypass);
//		sign.setJarPath(outputJar);
//		sign.setBasedir(basedir);
//		sign.setWorkingDir(basedir);
//		sign.setLog(getLog());
//		sign.setVerbose(false);
//		sign.setVerify(false);
//		sign.execute();
	}

}
