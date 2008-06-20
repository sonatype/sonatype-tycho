/*
 * 	Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.tycho.osgicompiler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.osgi.service.resolver.BundleDescription;

/**
 * This class is responsible for extracting bundles with embedded jars to a
 * temporary directory, so they are usable by compilers
 * 
 * TODO: set up some kind of caching strategy over multiple executions
 * (compile and testCompile and/or over multiple modules)
 *
 */
public class BundleStorageManager {

	private File storage;

	private class BundleInfo {
		String original;
		String extracted;
		List libraries;
	}

	private List/* <BundleInfo> */bundleInfos = new ArrayList();

	public BundleStorageManager(File storage) {
		this.storage = storage;

		if (storage.exists() && !storage.isDirectory()) {
			throw new IllegalArgumentException(storage + " is not a directory");
		}

		if (!storage.exists()) {
			storage.mkdirs();
		}
	}

	public void clean() throws MojoExecutionException {
		try {
			FileUtils.forceDelete(storage);
			storage.mkdirs();
		} catch (IOException e) {
			throw new MojoExecutionException(
					"Could not delete storage directory", e);
		}
	}

	/**
	 * returns canonical path of original file or extracted directory
	 * 
	 * @param f
	 * @return
	 */
	public String addBundleFile(File f) throws MojoExecutionException {

		JarFile jarFile = null;
		try {
			String canonicalOriginal = f.getCanonicalPath();
			BundleInfo bundleInfo = getBundleInfoByOriginal(canonicalOriginal);

			if (bundleInfo != null) {
				return bundleInfo.extracted;
			}

			bundleInfo = new BundleInfo();
			bundleInfo.original = canonicalOriginal;
			jarFile = new JarFile(f, false);
			Manifest mft = jarFile.getManifest();
			String bundleClassPath = getClassPath(mft);
			if (bundleClassPath == null || bundleClassPath.equals(".")) {
				bundleInfo.extracted = canonicalOriginal;
				bundleInfo.libraries = Collections
						.singletonList(canonicalOriginal);
				bundleInfos.add(bundleInfo);
				return canonicalOriginal;
			} else {

				File target = new File(storage, getUniqueName(mft));

				String canonicalTarget = target.getCanonicalPath();
				bundleInfo.extracted = canonicalTarget;

				String[] elements = bundleClassPath.trim().split(",");
				List libs = new ArrayList();
				for (int i = 0; i < elements.length; i++) {
					String element = elements[i];
					element = element.trim();
					if (element.equals(".")) {
						libs.add(canonicalOriginal);
					} else {
						File library = extract(jarFile, element, target);
						if (library != null) {
							libs.add(library.getCanonicalPath());
						}
					}
				}
				bundleInfo.libraries = libs;
				bundleInfos.add(bundleInfo);

				writeManifest(mft, target);

				return canonicalTarget;
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Error while processing file: "
					+ f, e);
		} finally {
			if (jarFile != null) {
				try {
					jarFile.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private void writeManifest(Manifest mft, File target)
			throws FileNotFoundException, IOException {
		File metainf = new File(target, "META-INF");
		metainf.mkdirs();
		Attributes attributes = mft.getMainAttributes();
		if (!attributes.containsKey(java.util.jar.Attributes.Name.MANIFEST_VERSION)) {
			attributes.put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0");
		}
		OutputStream fos = new FileOutputStream(new File(metainf,
				"MANIFEST.MF"));
		mft.write(fos);
		fos.close();
	}

	private String getClassPath(Manifest mft) {
		String classPath = mft.getMainAttributes().getValue("Bundle-Classpath");
		if (classPath == null) {
			return ".";
		} else {
			return classPath.trim();
		}
	}

	private BundleInfo getBundleInfoByOriginal(String canonicalOriginal) {
		for (Iterator iterator = bundleInfos.iterator(); iterator.hasNext();) {
			BundleInfo info = (BundleInfo) iterator.next();
			if (info.original.equals(canonicalOriginal)) {
				return info;
			}
		}
		return null;
	}

	/**
	 * returns original file where output directory was extracted from canonical
	 * paths !
	 * 
	 * @param outputDirectory
	 * @return
	 */
	public String getOriginalFile(String outputDirectory) {
		for (Iterator iterator = bundleInfos.iterator(); iterator.hasNext();) {
			BundleInfo info = (BundleInfo) iterator.next();
			if (info.extracted.equals(outputDirectory)) {
				return info.original;
			}
		}
		throw new IllegalArgumentException("No such bundle");
	}

	/**
	 * returns symbolicName_version
	 * 
	 * @param manifest
	 * @return
	 */
	private static String getUniqueName(Manifest manifest) {
		String symbolicName = manifest.getMainAttributes().getValue(
				"Bundle-SymbolicName");
		String version = manifest.getMainAttributes()
				.getValue("Bundle-Version");

		int semicolon = symbolicName.indexOf(";");
		if (semicolon != -1) {
			symbolicName = symbolicName.substring(0, semicolon);
		}

		return symbolicName + "_" + version;
	}

	public File extract(JarFile jarFile, String element, File tempDirectory)
			throws IOException {

		ZipEntry entry = jarFile.getEntry(element);
		if (entry == null) {
			// log.warn("No entry found for " + element);
			return null;
		}
		File efile = new File(tempDirectory, entry.getName());
		if (!efile.getParentFile().exists()) {
			efile.getParentFile().mkdirs();
		}

		InputStream in = new BufferedInputStream(jarFile.getInputStream(entry));
		OutputStream out = new BufferedOutputStream(new FileOutputStream(efile));
		byte[] buffer = new byte[2048];
		for (;;) {
			int nBytes = in.read(buffer);
			if (nBytes <= 0)
				break;
			out.write(buffer, 0, nBytes);
		}
		out.flush();
		out.close();
		in.close();

		return efile;
	}

	public String addBundleDirectory(File f) throws MojoExecutionException {
		try {
			String canonicalOriginal = f.getCanonicalPath();
			BundleInfo bundleInfo = getBundleInfoByOriginal(canonicalOriginal);
	
			if (bundleInfo != null) {
				return bundleInfo.extracted;
			}
	
			bundleInfo = new BundleInfo();
			bundleInfo.original = canonicalOriginal;
			bundleInfo.extracted = canonicalOriginal;
	
			Manifest mft = new Manifest();
			InputStream is = new FileInputStream(new File(f, "META-INF/MANIFEST.MF"));
			try {
				mft.read(is);
			} finally {
				is.close();
			}

			String bundleClassPath = getClassPath(mft);
	
			String[] elements = bundleClassPath.trim().split(",");
			List libs = new ArrayList();
			for (int i = 0; i < elements.length; i++) {
				String element = elements[i];
				element = element.trim();
				File library = new File(f, element);
				if (library.exists()) {
					libs.add(library.getCanonicalPath());
				}
			}
			bundleInfo.libraries = libs;
			bundleInfos.add(bundleInfo);
			
			return canonicalOriginal;
		} catch (IOException ex) {
			throw new MojoExecutionException("Error while processing file: "
					+ f);
		}

	}

	public File getLibraryFile(BundleDescription desc, String basePath, File artifact, String libraryName) throws IOException {
		File base = new File(basePath);

		if (".".equals(libraryName)) {
			return artifact != null? artifact: base;
		}

		if (base.isDirectory()) {
			File library = new File(base, libraryName);
			return library;
		}

		JarFile jarFile = new JarFile(base);
		try {
			File target = new File(storage, desc.getSymbolicName() + "_" + desc.getVersion());
			return extract(jarFile, libraryName, target);
		} finally {
			jarFile.close();
		}

//		if (base.isFile()) { // &&"jar".equalsIgnoreCase(basePath.getFileExtension())) { //$NON-NLS-1$
//			path = base.getCanonicalPath();
//		} else {
//			if (libraryName.equals(".") && model != theBundle) {
//				path = epm.getOriginalFile(basePath);
//			} else {
//				path = new File(base, libraryName).getCanonicalPath();//basePath.append(libraryName).toString();
//			}
//		}

	}
}
