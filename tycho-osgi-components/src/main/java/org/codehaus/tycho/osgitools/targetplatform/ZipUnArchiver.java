package org.codehaus.tycho.osgitools.targetplatform;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;

public class ZipUnArchiver {

	private File sourceFile;
	private File destDirectory;

	public void setSourceFile(File file) {
		this.sourceFile = file;
	}

	public void setDestDirectory(File dstDir) {
		this.destDirectory = dstDir;
	}

	public void extract() throws IOException {
		ZipFile zip = new JarFile(sourceFile);
		Enumeration<? extends ZipEntry> entries = zip.entries();
		while (entries.hasMoreElements()) {
			extract(zip, entries.nextElement());
		}
	}

	private void extract(ZipFile zip, ZipEntry entry) throws IOException {
		File dst = new File(destDirectory, entry.getName());
		if (entry.isDirectory()) {
			dst.mkdirs();
		} else {
			dst.getParentFile().mkdirs();
			InputStream is = zip.getInputStream(entry);
			FileUtils.copyStreamToFile(new RawInputStreamFacade(is), dst);
		}
	}

}
