package org.codehaus.tycho.eclipsepackaging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.osgitools.project.BuildOutputJar;
import org.codehaus.tycho.osgitools.project.EclipsePluginProject;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.Version;

/**
 * Creates a jar-based plugin and attaches it as an artifact
 * 
 * @goal package-plugin
 */
public class PackagePluginMojo extends AbstractTychoPackagingMojo {

	/**
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	protected File buildDirectory;

	protected EclipsePluginProject pdeProject;

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
	 * Build qualifier. Recommended way to set this parameter is using
	 * build-qualifier goal. 
	 * 
	 * @parameter expression="${buildQualifier}"
	 */
	protected String qualifier;

	public void execute() throws MojoExecutionException {
	    initializeProjectContext();

		pdeProject = (EclipsePluginProject) project.getContextValue( TychoConstants.CTX_ECLIPSE_PLUGIN_PROJECT );

		createSubJars();

		File pluginFile = createPluginJar();

		project.getArtifact().setFile(pluginFile);
		
		attachP2Metadata();
	}

	private void createSubJars() throws MojoExecutionException {
		for (BuildOutputJar jar : pdeProject.getOutputJars()) {
			if (!".".equals(jar.getName())) {
				makeJar(jar.getName(), jar.getOutputDirectory());
			}
		}
	}

	private File makeJar(String jarName, File classesFolder) throws MojoExecutionException {
		try {
			File jarFile = new File(project.getBasedir(), jarName);
			JarArchiver archiver = new JarArchiver();
			archiver.setDestFile(jarFile);
			archiver.addDirectory(classesFolder);
	        archiver.createArchive();
			return jarFile;
		} catch (Exception e) {
			throw new MojoExecutionException("Could not create jar " + jarName, e);
		}
	}

	private File createPluginJar() throws MojoExecutionException {
		try {
			MavenArchiver archiver = new MavenArchiver();
			archiver.setArchiver(jarArchiver);

			File pluginFile = new File(buildDirectory, finalName + ".jar");
			if (pluginFile.exists()) {
				pluginFile.delete();
			}

			BuildOutputJar dotOutputJar = pdeProject.getDotOutputJar();
			if (dotOutputJar != null) {
				archiver.getArchiver().addDirectory(dotOutputJar.getOutputDirectory());
			}

			String binIncludes = pdeProject.getBuildProperties().getProperty("bin.includes");
			String binExcludes = pdeProject.getBuildProperties().getProperty("bin.excludes");
			if (binIncludes != null) {
				archiver.getArchiver().addFileSet(getFileSet(project.getBasedir(), toFilePattern(binIncludes), toFilePattern(binExcludes)));
			}

			File manifest = updateManifest();
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

	private File updateManifest() throws FileNotFoundException, IOException, MojoExecutionException 
	{
		BundleDescription bundle = bundleResolutionState.getBundleByLocation( project.getBasedir() );
		Version version = bundle.getVersion();

		File mfile = new File(project.getBasedir(), "META-INF/MANIFEST.MF");

		InputStream is = new FileInputStream(mfile);
		Manifest mf;
		try {
			mf = new Manifest(is);
		} finally {
			is.close();
		}
		Attributes attributes = mf.getMainAttributes();

		if (VersioningHelper.isSnapshotVersion(version)) {
			String expandedVersion = VersioningHelper.expandVersion(version, qualifier).toString();

			attributes.putValue("Bundle-Version", expandedVersion);
			VersioningHelper.setExpandedVersion(session, bundle.getLocation(), expandedVersion);
		}

		attributes.putValue(TychoConstants.ATTR_GROUP_ID, project.getGroupId());
		attributes.putValue(TychoConstants.ATTR_BASE_VERSION, project.getVersion());

		mfile = new File(project.getBuild().getDirectory(), "MANIFEST.MF");
		mfile.getParentFile().mkdirs();
		BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(mfile));
		try {
			mf.write(os);
		} finally {
			os.close();
		}

		return mfile;
	}

}
