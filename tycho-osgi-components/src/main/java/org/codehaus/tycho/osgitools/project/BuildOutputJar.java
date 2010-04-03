package org.codehaus.tycho.osgitools.project;

import java.io.File;
import java.util.List;

public class BuildOutputJar {

	private final String name;
	private final List<File> sourceFolders;
	private final File outputDirectory;
	private List<String> extraClasspathEntries;
	
	public BuildOutputJar(String name, File outputDirectory, List<File> sourceFolders, List<String> extraClasspathEntries) {
		this.name = name;
		this.outputDirectory = outputDirectory;
		this.sourceFolders = sourceFolders;
		this.extraClasspathEntries  = extraClasspathEntries;
	}

	public String getName() {
		return name;
	}
	
	public File getOutputDirectory() {
		return outputDirectory;
	}
	
	public List<File> getSourceFolders() {
		return sourceFolders;
	}

	public List<String> getExtraClasspathEntries() {
		return extraClasspathEntries; 
	}
}
