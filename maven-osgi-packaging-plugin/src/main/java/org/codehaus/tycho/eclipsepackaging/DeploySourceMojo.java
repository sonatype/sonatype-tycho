package org.codehaus.tycho.eclipsepackaging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;

/**
 * This goal is designed to run on a target platform or feature mojo. It will
 * iterate over all dependencies, extract any source ZIPs and deploy them as
 * Maven artifacts.
 * 
 * @goal deploy-source
 * @requiresProject
 * @requiresDependencyResolution compile
 * @author tom
 * 
 */
public class DeploySourceMojo extends AbstractMojo {

	/**
	 * @parameter expression="${project}"
	 */
	private MavenProject project;

	/**
	 * @parameter expression="${component.org.apache.maven.artifact.installer.ArtifactInstaller}"
	 * @required
	 * @readonly
	 */
	protected ArtifactInstaller installer;

	/**
	 * Remote repositories which will be searched for source attachments.
	 * 
	 * @parameter expression="${project.remoteArtifactRepositories}"
	 * @required
	 * @readonly
	 */
	protected List remoteArtifactRepositories;

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
	 * @parameter expression="${project.build.directory}/work"
	 */
	private File work;

	public void execute() throws MojoExecutionException, MojoFailureException {
		for (Iterator iterator = project.getDependencyArtifacts().iterator(); iterator.hasNext();) {
			Artifact a = (Artifact) iterator.next();
			try {
				processArtifact(a);
			} catch (ArtifactResolutionException e) {
				throw new MojoExecutionException("Could not resolve: " + a);
			} catch (ArtifactNotFoundException e) {
				throw new MojoExecutionException("Could not find: " + a);
			} catch (IOException e) {
				throw new MojoExecutionException("Error processing: " + a);
			} catch (ArtifactInstallationException e) {
				throw new MojoExecutionException("Error installing: " + a);
			} catch (ArchiverException e) {
				throw new MojoExecutionException("Error creating jar: " + a);
			}
		}
	}

	private void processArtifact(Artifact a) throws IOException,
			ArtifactResolutionException, ArtifactNotFoundException,
			ArtifactInstallationException, ArchiverException {
		if (!a.isResolved()) {
			artifactResolver.resolve(a, remoteArtifactRepositories,
					localRepository);
		}

		JarFile jar = new JarFile(a.getFile());
		Manifest mft = jar.getManifest();
		String classPath = mft.getMainAttributes().getValue("Bundle-ClassPath");
		if (classPath == null) {
			classPath = ".";
		}

		if (classPath.equals(".")) {
			JarEntry je = jar.getJarEntry("src.zip");
			if (je != null) {
				File target = File.createTempFile("deploysource", "jar");
				extractEntry(jar.getInputStream(je), target);
				deploySourceArtifact(a, target);
			}
		} else {
			String[] libs = classPath.split(",");
			File targetDir = new File(work, a.getArtifactId());
			targetDir.mkdirs();

			for (int i = 0; i < libs.length; i++) {
				String lib = libs[i];
				String srcName;
				if (lib.endsWith(".jar")) {
					srcName = lib.substring(0, lib.length() - 4) + "src.zip";
				} else {
					srcName = "src.zip";
				}
				JarEntry je = jar.getJarEntry(srcName);
				if (je != null) {
					File target = new File(targetDir, srcName);
					extractEntry(jar.getInputStream(je), target);
				}
			}
			jar.close();

			JarArchiver jarArchiver = new JarArchiver();
			jarArchiver.addDirectory(targetDir, new String[] { "**/*" },
					new String[0]);
			File f = File.createTempFile("deploysource", ".jar");
			jarArchiver.setDestFile(f);
			jarArchiver.createArchive();

			deploySourceArtifact(a, f);
		}

	}

	private void deploySourceArtifact(Artifact artifact, File sourceFile)
			throws ArtifactInstallationException {
		Artifact sourceArtifact = artifactFactory.createArtifactWithClassifier(
				artifact.getGroupId(), artifact.getArtifactId(), artifact
						.getVersion(), "jar", "sources");
		installer.install(sourceFile, sourceArtifact, localRepository);
		getLog().info("Artifact source installed: " + artifact);
	}

	private void extractEntry(InputStream is, File target) throws IOException {
		byte[] jbuf = new byte[8192];
		FileOutputStream os = new FileOutputStream(target);
		int n = 0;
		while ((n = is.read(jbuf)) > 0) {
			os.write(jbuf, 0, n);
		}
		is.close();
		os.close();
	}
}
