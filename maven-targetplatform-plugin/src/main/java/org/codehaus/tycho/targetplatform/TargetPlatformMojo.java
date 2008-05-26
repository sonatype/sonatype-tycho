package org.codehaus.tycho.targetplatform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.tycho.plugin.artifactfilter.FilterArtifacts;
import org.codehaus.tycho.plugin.artifactfilter.TransitivityFilter;
import org.codehaus.tycho.plugin.artifactfilter.TypeFilter;

/**
 * Downloads the dependencies in the pom, to create a target platform that is
 * usable in PDE. Also validates the target platform, and downloads source jars
 * if available.
 * 
 * @goal create-target-platform
 * @requiresDependencyResolution compile
 * 
 */
public class TargetPlatformMojo extends AbstractMojo {

	/**
	 * @parameter expression="${project}"
	 */
	private MavenProject project;

	/**
	 * To look up Archiver/UnArchiver implementations
	 * 
	 * @parameter expression="${component.org.codehaus.plexus.archiver.manager.ArchiverManager}"
	 * @required
	 */
	protected ArchiverManager archiverManager;

	/**
	 * Remote repositories which will be searched for source attachments.
	 * 
	 * @parameter expression="${project.remoteArtifactRepositories}"
	 * @required
	 * @readonly
	 */
	protected List remoteRepositories;

	/**
	 * Local maven repository.
	 * 
	 * @parameter expression="${localRepository}"
	 * @required
	 * @readonly
	 */
	protected ArtifactRepository localRepository;

	/**
	 * Artifact factory, needed to download source jars for inclusion in
	 * classpath.
	 * 
	 * @component role="org.apache.maven.artifact.factory.ArtifactFactory"
	 * @required
	 * @readonly
	 */
	protected ArtifactFactory artifactFactory;

	/**
	 * Artifact resolver, needed to download source jars for inclusion in
	 * classpath.
	 * 
	 * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
	 * @required
	 * @readonly
	 */
	protected ArtifactResolver artifactResolver;

	/**
	 * @parameter default-value="${project.build.directory}/plugins"
	 */
	private File pluginsDir;

	/**
	 * @parameter default-value="${project.build.directory}/plugins/source"
	 */
	private File sourcePlugin;

	/**
	 * @component
	 */
	private MavenProjectBuilder projectBuilder;

	/**
	 * @parameter default-value="${project.build.directory}/markers"
	 */
	private File markersDirectory;

	private Set markers = new HashSet();

	public void execute() throws MojoExecutionException, MojoFailureException {
		markersDirectory.mkdirs();
		markers.addAll(Arrays.asList(markersDirectory.listFiles()));

		if (!sourcePlugin.exists()) {
			createSourcePlugin();
		}

		Set artifacts = getArtifacts();

		for (Iterator iterator = artifacts.iterator(); iterator.hasNext();) {
			Artifact a = (Artifact) iterator.next();
			if (a.getType().equals("eclipse-feature")) {
				processFeature(a);
			} else {
				processPlugin(a);
			}
		}

		for (Iterator iter = markers.iterator(); iter.hasNext();) {
			File marker = (File) iter.next();
			try {
				File f = readMarker(marker);
				getLog().info("Deleting " + f);
				if (f.isDirectory()) {
					FileUtils.deleteDirectory(f);
				} else {
					f.delete();
				}
				if (!marker.delete()) {
					getLog().error("Could not delete marker");
				}
			} catch (IOException e) {
				getLog().error("Could not delete " + marker);
			}
		}

	}

	private File readMarker(File marker) throws IOException {
		BufferedReader r = new BufferedReader(new FileReader(marker));
		try {
			String fileName = r.readLine();
			return new File(fileName);
		} finally {
			r.close();
		}
	}

	private void processFeature(Artifact a) {
		try {
			MavenProject project = projectBuilder.buildFromRepository(a,
					remoteRepositories, localRepository);

			Set artifacts = project
					.createArtifacts(artifactFactory, null, null);
			Iterator it = artifacts.iterator();
			while (it.hasNext()) {
				Artifact artifact = (Artifact) it.next();
				processPlugin(artifact);
			}

		} catch (ProjectBuildingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MojoExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Set getArtifacts() throws MojoExecutionException,
			MojoFailureException {
		FilterArtifacts fa = new FilterArtifacts();
		fa.addFilter(new TypeFilter("jar,eclipse-feature", null));
		fa.addFilter(new TransitivityFilter(project.getDependencyArtifacts(),
				true));
		Set artifacts = fa.filter(project.getArtifacts(), getLog());

		return artifacts;
	}

	private void processPlugin(Artifact artifact) throws MojoExecutionException {
		markers.remove(getMarker(artifact));
		try {
			artifactResolver.resolve(artifact, remoteRepositories,
					localRepository);

			if (isUpToDate(artifact)) {
				getLog().info(artifact + " is up to date");
				return;
			}

			File f = artifact.getFile();
			JarFile jar = new JarFile(f);
			Manifest mft = jar.getManifest();
			if (mft == null
					|| mft.getMainAttributes().getValue("Bundle-SymbolicName") == null) {
				getLog().debug("Not a bundle. Skipping " + artifact);
				return;
			}
			getLog().info("Processing " + artifact);

			boolean extract = !Util.isSimpleJar(artifact, jar);
			jar.close();

			String name = Util.getName(artifact.getFile());
			File target;
			if (extract) {
				target = new File(pluginsDir, name);
				target.mkdirs();
				Util.unpack(archiverManager, f, target);
			} else {
				target = new File(pluginsDir, name + ".jar");
				FileUtils.copyFile(f, target);
			}

			File sourceFile = getSourceFile(artifact);
			if (sourceFile != null) {
				File d = new File(sourcePlugin, name);
				d.mkdir();
				FileUtils.copyFile(sourceFile, new File(d, "src.zip"));
			}

			updateMarker(artifact, target);

		} catch (IOException e) {
			throw new MojoExecutionException("Error processing artifact "
					+ artifact, e);
		} catch (ArtifactResolutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ArtifactNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private boolean isUpToDate(Artifact artifact) {
		File marker = getMarker(artifact);
		if (!marker.exists()) {
			return false;
		}

		File file = artifact.getFile();
		if (file == null) {
			return false;
		}

		return marker.lastModified() > file.lastModified();
	}

	private void updateMarker(Artifact artifact, File file) throws IOException {
		markersDirectory.mkdirs();
		File marker = getMarker(artifact);
		if (file != null) {
			PrintWriter w = new PrintWriter(new FileWriter(marker));
			try {
				w.println(file.getCanonicalPath());
			} finally {
				w.close();
			}
		} else {
			marker.createNewFile();
		}
	}

	private File getMarker(Artifact artifact) {
		File marker = new File(markersDirectory, artifact.getGroupId() + "-"
				+ artifact.getArtifactId() + "-" + artifact.getVersion());
		return marker;
	}

	private File createSourcePlugin() throws MojoExecutionException {
		if (!sourcePlugin.exists()) {
			sourcePlugin.mkdirs();
		}

		File manifestDir = new File(sourcePlugin, "META-INF");
		manifestDir.mkdirs();

		Manifest mft = new Manifest();
		Attributes a = mft.getMainAttributes();
		a.putValue("Manifest-Version", "1.0");
		a.putValue("Bundle-SymbolicName", "source");
		a.putValue("Bundle-Name", "source");
		a.putValue("Bundle-Version", "not.important");
		try {
			FileOutputStream fos = new FileOutputStream(new File(manifestDir,
					"MANIFEST.MF"));
			mft.write(fos);
			fos.close();

			File pluginXML = new File(sourcePlugin, "plugin.xml");
			PrintWriter pw = new PrintWriter(new FileWriter(pluginXML));
			pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			pw.println("<?eclipse version=\"3.0\"?>");
			pw.println("<plugin>");
			pw.println("<extension point = \"org.eclipse.pde.core.source\">");
			pw.println("\t<location path=\".\" />");
			pw.println("</extension>");
			pw.println("</plugin>");

			pw.close();
		} catch (IOException e) {
			throw new MojoExecutionException("Creating plugin.xml", e);
		}

		return sourcePlugin;
	}

	public File getSourceFile(Artifact artifact) {
		Artifact resolvedArtifact = artifactFactory
				.createArtifactWithClassifier(artifact.getGroupId(), artifact
						.getArtifactId(), artifact.getVersion(), "jar",
						"sources");

		try {
			artifactResolver.resolve(resolvedArtifact, remoteRepositories,
					localRepository);
		} catch (ArtifactNotFoundException e) {
			getLog().debug("No source artifact found for " + artifact);
		} catch (ArtifactResolutionException e) {
			getLog().warn(
					"Error resolving " + resolvedArtifact.getId() + " - "
							+ "sources", e);
		}

		File result = resolvedArtifact.getFile();
		return result != null && result.exists() ? result : null;
	}

}
