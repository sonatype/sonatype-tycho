package org.codehaus.tycho.eclipsepackaging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;


public class Util {

    public final static String[] DEFAULT_INCLUDES = new String[] { "**/*" };

    public static String[] getIncludedFiles(File basedir, String[] includes) {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(basedir);
        ds.setIncludes(includes);
        ds.setExcludes(DEFAULT_EXCLUDES);
        ds.scan();
        String[] files = ds.getIncludedFiles();

        return files;
    }

    public final static String[] DEFAULT_EXCLUDES = new String[] { "**/CVS/**",
            "**/.svn/**", };

    public static void makeJar(File basedir, File outputFile,
            String[] includes, JarArchiver archiver, File manifest)
            throws MojoExecutionException {

        try {
            if (manifest != null && manifest.exists()) {
                archiver.setManifest(manifest);
            }
            
            for (int i = 0; i < includes.length; i++) {
				String file= includes[i];
                File f = new File(basedir, file);
                if (f.exists()) {
                    if (f.isDirectory()) {
                        archiver.addDirectory(f, DEFAULT_INCLUDES,
                                DEFAULT_EXCLUDES);
                    } else {
                        archiver.addFile(f, file);
                    }
                }
            }
            archiver.setDestFile(outputFile);
            archiver.createArchive();
        } catch (Exception e) {
            throw new MojoExecutionException("", e);
        }
    }

    public static void extractJar(File jarFile, File targetDirectory) throws MojoExecutionException {
    	extractJar(jarFile, targetDirectory, false);
    }

    public static void extractJar(File jarFile, File targetDirectory, boolean excludeManifest)
            throws MojoExecutionException {
        try {
            ZipFile file = new ZipFile(jarFile);
            try {
			    for (Enumeration e = file.entries(); e.hasMoreElements();) {
			        ZipEntry entry = (ZipEntry) e.nextElement();
			        String name = entry.getName().toUpperCase();
			        if (excludeManifest) {
			        	if (name.equals(JarFile.MANIFEST_NAME)) {
			        		continue;
			        	} 
			        	
			        	if (name.startsWith("META-INF") && (name.endsWith(".DSA") || name.endsWith(".SF"))) {
			        		continue;
			        	}
			        }
			
			        // if (!entry.getName().startsWith("META-INF/")) {
			        File destFile = new File(targetDirectory, entry.getName());
			        if (!entry.isDirectory()) {
			            destFile.getParentFile().mkdirs();
			
			            FileOutputStream fos = new FileOutputStream(destFile);
			
			            try {
			                IOUtil.copy(file.getInputStream(entry), fos);
			            } finally {
			                IOUtil.close(fos);
			            }
			        } else {
			            destFile.mkdirs();
			        }
			    }
			    // }
			} finally {
			    file.close();
			}
        } catch (IOException e) {
            throw new MojoExecutionException("Error extracting zip: " + jarFile, e);
        }
    }
    
	public static int exec(final String executable, String[] arguments) throws MojoExecutionException {
		StreamConsumer outConsumer = new StreamConsumer() {
			public void consumeLine(String line) {
				System.out.println(executable + " > " + line);
			}
		};
		StreamConsumer errConsumer = new StreamConsumer() {
			public void consumeLine(String line) {
				System.err.println(executable + " > " + line);
			}
		};
		
		Commandline cl = new Commandline();
		cl.setExecutable(executable);
		cl.addArguments(arguments);
		try {
			return CommandLineUtils.executeCommandLine(cl, outConsumer, errConsumer);
		} catch (CommandLineException e) {
			throw new MojoExecutionException("", e);
		}

	}

}