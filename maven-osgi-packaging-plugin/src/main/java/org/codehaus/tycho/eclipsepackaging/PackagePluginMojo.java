package org.codehaus.tycho.eclipsepackaging;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.builder.ProjectUri.Build;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;

/**
 * Creates a jar-based plugin and attaches it as an artifact
 * 
 * @goal package-plugin
 */
public class PackagePluginMojo extends AbstractMojo {
	private static final String OUTPUT = "output.";
	private static final String SOURCE = "source.";

	/**
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	protected File buildDirectory;

	/**
	 * @parameter expression="${project.build.outputDirectory}"
	 * @required
	 */
	protected File outputDirectory;

	/**
	 * @parameter expression="${project}"
	 * @required
	 */
	protected MavenProject project;

	/**
	 * The Jar archiver.
	 * 
	 * parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
	 * required
	 */
	private JarArchiver jarArchiver = new JarArchiver();

	/**
	 * Name of the generated JAR.
	 * 
	 * @parameter alias="jarName" expression="${project.build.finalName}"
	 * @required
	 */
	protected String finalName;

	/**
	 * The maven archiver to use.
	 * 
	 * @parameter
	 */
	private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

	/**
	 * @parameter expression="${component.org.apache.maven.project.MavenProjectHelper}
	 */
	protected MavenProjectHelper projectHelper;

	/**
	 * @parameter expression="${buildNumber}"
	 */
	protected String qualifier;

	private Properties buildProperties;

	public void execute() throws MojoExecutionException {
		createPlugin();
	}

	private void createPlugin() throws MojoExecutionException {
		try {
			buildProperties = new Properties();

			File file = new File(project.getBasedir(), "build.properties");
			if (file.canRead()) {
				buildProperties.load(new FileInputStream(file));
			}

			createSubJars();

			File pluginFile = createPluginJar();

			project.getArtifact().setFile(pluginFile);

		} catch (Exception e) {
			throw new MojoExecutionException("", e);
		}
	}

	private void createSubJars() throws MojoExecutionException {
		try {
			for (Iterator iterator = buildProperties.keySet().iterator(); iterator.hasNext();) {
				String key = (String) iterator.next();
				if (key.startsWith(OUTPUT) && !key.equals("output..")) {
					String fileName = key.substring(OUTPUT.length());
					String classesDir[] = buildProperties.getProperty(key)
							.split(",");
					makeJar(fileName, classesDir);
				} else if (key.startsWith(SOURCE) && !key.equals("source..")) {
					String fileName = key.substring(SOURCE.length());
					String classesDir[] = new String[]{buildDirectory.getName() + "/" + fileName.substring(0, fileName.length() - 4) + "-classes"};
					makeJar(fileName, classesDir);
				}
			}
		} catch (Exception e) {
			throw new MojoExecutionException("", e);
		}

	}

	private File makeJar(String fileName, String[] classesDir)
			throws MojoExecutionException {
		File jarFile = new File(project.getBasedir(), fileName);
		Util.makeJar(project.getBasedir(), jarFile, classesDir,
				new JarArchiver(), null);
		return jarFile;
	}

	private File createPluginJar() throws MojoExecutionException {
		try {
			MavenArchiver archiver = new MavenArchiver();
			archiver.setArchiver(jarArchiver);

			File pluginFile = new File(buildDirectory, finalName + ".jar");
			if (pluginFile.exists()) {
				pluginFile.delete();
			}

			String output = buildProperties.getProperty(OUTPUT + ".");
			if (output != null) {
				String[] includes = output.split(",");
				addToArchiver(archiver, includes, false);
			}

			if (outputDirectory.exists()) {
				archiver.getArchiver().addDirectory(outputDirectory);
			}
			
			if (buildProperties.containsKey("bin.includes")) {
				String[] binIncludes = buildProperties.getProperty("bin.includes")
						.split(",");
				addToArchiver(archiver, binIncludes, true);
			}

			File manifest = expandVersion(new File(project.getBasedir(), "META-INF/MANIFEST.MF"));
			if (manifest.exists()) {
				archive.setManifestFile(manifest);
			}

			archiver.setOutputFile(pluginFile);

			archiver.createArchive(project, archive);

			return pluginFile;
		} catch (Exception e) {
			throw new MojoExecutionException("Error assembling JAR", e);
		}
	}

	private File expandVersion(File mfile) throws FileNotFoundException, IOException 
	{
		FileInputStream is = new FileInputStream(mfile);
		Manifest mf;
		try {
			mf = new Manifest(is);
		} finally {
			is.close();
		}

		if (expandVersion(mf)) {
			mfile = new File(project.getBuild().getDirectory(), "MANIFEST.MF");
			mfile.getParentFile().mkdirs();
			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(mfile));
			try {
				mf.write(os);
			} finally {
				os.close();
			}
		}
		return mfile;
	}

	private boolean expandVersion(Manifest mf) {
		Attributes attributes = mf.getMainAttributes();

		String version = attributes.getValue("Bundle-Version");
		if (qualifier != null && version.endsWith(".qualifier")) {
			version = version.substring(0, version.lastIndexOf('.') + 1);
			version = version + qualifier;
			attributes.putValue("Bundle-Version", version);
			
			return true;
		}

		return false;
	}

	private void addToArchiver(MavenArchiver archiver, String[] includes,
			boolean includeBase) throws ArchiverException {
		for (int i = 0; i < includes.length; i++) {
			String file = includes[i];
			if (!file.equals(".")) {
				File f = new File(project.getBasedir(), file);
				if (f.exists()) {
					if (f.isDirectory()) {
						archiver.getArchiver().addDirectory(f,
								includeBase ? file : "", Util.DEFAULT_INCLUDES,
								Util.DEFAULT_EXCLUDES);
					} else {
						archiver.getArchiver().addFile(f, file);
					}
				}
			}
		}
	}

}
