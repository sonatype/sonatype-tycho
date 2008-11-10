package org.codehaus.tycho.osgitools.project;

import java.io.File;
import java.util.List;

public class BuildOutputJar {

	private final String name;
	private final List<File> sourceFolders;
	private final File outputDirectory;

	public BuildOutputJar(String name, File outputDirectory, List<File> sourceFolders) {
		this.name = name;
		this.outputDirectory = outputDirectory;
		this.sourceFolders = sourceFolders;
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

}
