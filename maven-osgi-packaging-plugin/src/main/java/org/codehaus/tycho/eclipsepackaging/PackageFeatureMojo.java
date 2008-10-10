package org.codehaus.tycho.eclipsepackaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.tycho.eclipsepackaging.product.Plugin;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.osgitools.OsgiState;

/**
 * @phase package
 * @goal package-feature
 * @requiresProject
 * @requiresDependencyResolution runtime
 * 
 */
public class PackageFeatureMojo extends AbstractMojo implements
		Contextualizable {

	/** @component */
	private OsgiState state;

	private PlexusContainer plexus;

	private static final String GENERATE_FEATURE = "generate.feature@";

	/**
	 * The maven archiver to use.
	 * 
	 * @parameter
	 */
	private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

	/**
	 * @parameter expression="${project}"
	 * @required
	 */
	protected MavenProject project;

	/**
	 * @parameter expression="${project.build.directory}"
	 */
	private File outputDirectory;

	/**
	 * @parameter expression="${project.basedir}"
	 */
	private File basedir;

	/**
	 * Name of the generated JAR.
	 * 
	 * @parameter alias="jarName" expression="${project.build.finalName}"
	 * @required
	 */
	private String finalName;

	public void execute() throws MojoExecutionException, MojoFailureException {
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(
					new File(basedir, "build.properties")));
		} catch (IOException e) {
			throw new MojoExecutionException("Error reading build properties",
					e);
		}

		// update version at feature.xml
		// XXX is that really necessary?
		// File featureXml = new File(outputDirectory, Feature.FEATURE_XML);
		// try {
		// Feature feature = Feature.read(new File(basedir,
		// Feature.FEATURE_XML));
		// feature.setVersion(project.getVersion());
		// Feature.write(feature, featureXml);
		// } catch (Exception e) {
		// throw new MojoExecutionException("Error updating feature version",
		// e);
		// }

		File outputJar = new File(outputDirectory, finalName + ".jar");
		outputJar.getParentFile().mkdirs();
		String[] binIncludes = props.getProperty("bin.includes").split(",");
		String files[] = Util.getIncludedFiles(basedir, binIncludes);

		MavenArchiver archiver = new MavenArchiver();
		JarArchiver jarArchiver = getJarArchiver();
		archiver.setArchiver(jarArchiver);
		archiver.setOutputFile(outputJar);
		jarArchiver.setDestFile(outputJar);

		try {
			for (int i = 0; i < files.length; i++) {
				String fileName = files[i];
				File f = /*
						 * "feature.xml".equals(fileName) ? featureXml :
						 */new File(basedir, fileName);
				if (!f.isDirectory()) {
					jarArchiver.addFile(f, fileName);
				}
			}
			archiver.createArchive(project, archive);

		} catch (Exception e) {
			throw new MojoExecutionException("Error creating feature package",
					e);
		}

		project.getArtifact().setFile(outputJar);

		if(false) {
			//TODO generate source not supported yet
			generateSources(props);
		}
	}

	private void generateSources(Properties props)
			throws MojoExecutionException {
		boolean individualSourceBundle = Boolean.parseBoolean(props
				.getProperty("individualSourceBundles"));

		Set<Object> keys = props.keySet();
		for (Object keyObj : keys) {
			String key = keyObj.toString();
			if (key.toString().startsWith(GENERATE_FEATURE)) {
				String baseFeature = props.getProperty(key);
				String sourceFeature = key.substring(GENERATE_FEATURE.length());

				if (individualSourceBundle) {

				} else {
					generateSourceFeature(baseFeature, sourceFeature);
				}
			}
		}

	}

	private JarArchiver getJarArchiver() throws MojoExecutionException {
		try {
			JarArchiver jarArchiver = (JarArchiver) plexus.lookup(
					JarArchiver.ROLE, "jar");
			return jarArchiver;
		} catch (ComponentLookupException e) {
			throw new MojoExecutionException("Unable to get JarArchiver", e);
		}
	}

	private void generateSourceFeature(String baseFeatureId,
			String sourceFeature) throws MojoExecutionException {
		Feature baseFeature = state.getFeature(baseFeatureId,
				OsgiState.HIGHEST_VERSION);
		if (baseFeature == null) {
			getLog().warn("Base feature not found: " + baseFeatureId);
			return;
		}

		List<Plugin> plugins = new ArrayList<Plugin>();
		plugins.add(new Plugin(sourceFeature, baseFeature.getVersion()));
		generateSourceFeature(sourceFeature, baseFeature.getVersion(), plugins);
	}

	private void generateSourceFeature(String featureId, String featureVersion,
			List<Plugin> plugins) throws MojoExecutionException {
		File target = new File(outputDirectory, featureId);
		target.mkdirs();

		File featureFile = new File(target, Feature.FEATURE_XML);

		// TODO check if tycho already has something to write a new feature
		try {
			FileWriter fw = new FileWriter(featureFile);
			fw.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
					.append('\n');
			fw.append(
					"<feature id=\"" + featureId + "\" version=\""
							+ featureVersion + "\" primary=\"false\" >")
					.append('\n');
			for (Plugin plugin : plugins) {
				fw.append(
						"\t<plugin id=\"" + plugin.getId() + "\" version=\""
								+ plugin.getVersion() + "\" />").append('\n');
			}
			fw.append("</feature>").append('\n');
			fw.flush();
			fw.close();
		} catch (IOException e) {
			throw new MojoExecutionException("Error creating feature.xml for "
					+ featureId + "_" + featureVersion, e);
		}

		File outputJar = new File(outputDirectory, featureId + "-"
				+ featureVersion + ".jar");

		JarArchiver jarArchiver = getJarArchiver();
		jarArchiver.setDestFile(outputJar);
		try {
			jarArchiver.addFile(featureFile, "feature.xml");
			jarArchiver.createArchive();
		} catch (Exception e) {
			throw new MojoExecutionException("Error packing source feature "
					+ featureId + "_" + featureVersion, e);
		}
	}

	public void contextualize(Context ctx) throws ContextException {
		plexus = (PlexusContainer) ctx.get(PlexusConstants.PLEXUS_KEY);
	}

}