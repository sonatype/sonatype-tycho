package org.codehaus.tycho.targetplatform;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

public class Util {

	/**
	 * Unpacks the archive file.
	 * 
	 * @param archive
	 *            File to be unpacked.
	 * @param target
	 *            Location where to put the unpacked files.
	 */
	protected static void unpack(ArchiverManager archiverManager, File archive, File target)
			throws MojoExecutionException {
		try {
			UnArchiver unArchiver = archiverManager.getUnArchiver("jar");
			unArchiver.setSourceFile(archive);
			unArchiver.setDestDirectory(target);

			unArchiver.extract();
		} catch (ArchiverException e) {
			throw new MojoExecutionException("Error unpacking file: " + archive
					+ "to: " + target, e);
		} catch (NoSuchArchiverException e) {
			throw new MojoExecutionException("Nu unarchiver");
		} catch (IOException e) {
			throw new MojoExecutionException("Error unpacking file: " + archive
					+ "to: " + target, e);
		}
	}
	
	public static String getName(File bundle) throws IOException {
		JarFile jar = new JarFile(bundle);
		Attributes attributes = jar.getManifest().getMainAttributes();
		jar.close();
		String name = attributes.getValue("Bundle-SymbolicName");
		if (name == null) {
			return null;
		}
		name = name.split(";")[0];
		name += "_" + attributes.getValue("Bundle-Version");
		return name;
	}
	
	public static boolean isSimpleJar(Artifact a, JarFile jar) throws IOException {
		Manifest mft = jar.getManifest();
		String bundleClassPath = mft != null ? mft.getMainAttributes()
				.getValue("Bundle-Classpath") : null;
				
		if (bundleClassPath != null) {
			String[] libs = bundleClassPath.split(",");
			for (int i = 0; i < libs.length; i++) {
				String lib = libs[i];
				lib = lib.trim();
				if (!".".equals(lib) && jar.getEntry(lib) != null) {
					return false;
				}
			}
		}
				
		return true;
	}
}
