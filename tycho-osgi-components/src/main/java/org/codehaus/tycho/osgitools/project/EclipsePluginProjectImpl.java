package org.codehaus.tycho.osgitools.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.eclipse.osgi.service.resolver.BundleDescription;

public class EclipsePluginProjectImpl implements EclipsePluginProject {

	private final MavenProject project;
	private final BundleDescription bundleDescription;
	private final Properties buildProperties;

	private final LinkedHashMap<String, BuildOutputJar> outputJars = new LinkedHashMap<String, BuildOutputJar>();
	private final BuildOutputJar dotOutputJar;

	public EclipsePluginProjectImpl(MavenProject project, BundleDescription desc) throws IOException {
		this.project = project;
		this.bundleDescription = desc;
		this.buildProperties = loadProperties(project);

		//
		LinkedHashMap<String, BuildOutputJar> jars = new LinkedHashMap<String, BuildOutputJar>();
		String jarsOrder = buildProperties.getProperty("jars.compile.order");
		if (jarsOrder != null) {
			for (String jarName : jarsOrder.split(",")) {
				jars.put(jarName, null);
			}
		}

		List<String> globalExtraClasspath = new ArrayList<String>();
		if (buildProperties.getProperty("jars.extra.classpath") != null)
			globalExtraClasspath.addAll(Arrays.asList(buildProperties.getProperty("jars.extra.classpath").split(",")));
		
		for (Map.Entry<Object,Object> entry : buildProperties.entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			if(!key.startsWith("source.")) {
				continue;
			}
			String jarName = key.substring(7);
			File outputDirectory = ".".equals(jarName)
					? new File(project.getBuild().getOutputDirectory())
					: new File(project.getBuild().getDirectory(), jarName + "-classes");
			List<File> sourceFolders = toFileList(project.getBasedir(), value.split(","));
			
			List<String> extraEntries = new ArrayList<String>();
			if (buildProperties.getProperty("extra." + jarName) != null) {
				extraEntries.addAll(Arrays.asList(buildProperties.getProperty("extra." + jarName).split(",")));
				extraEntries.addAll(globalExtraClasspath);
			}
			jars.put(jarName, new BuildOutputJar(jarName, outputDirectory, sourceFolders, extraEntries.size() == 0 ? globalExtraClasspath : extraEntries));
		}

		this.dotOutputJar = jars.get(".");

		for (BuildOutputJar jar : jars.values()) {
			if (jar != null) {
				this.outputJars.put(jar.getName(), jar);
			}
		}
	}

	private List<File> toFileList(File parent, String[] names) throws IOException {
		ArrayList<File> result = new ArrayList<File>();
		for (String name : names) {
			result.add(new File(parent, name.trim()).getCanonicalFile());
		}
		return result;
	}

	private static Properties loadProperties(MavenProject project) throws IOException {
		File file = new File(project.getBasedir(), "build.properties");

		Properties buildProperties = new Properties();
		if (file.canRead()) {
			InputStream is = new FileInputStream(file);
			try {
				buildProperties.load(is);
			} finally {
				is.close();
			}
		}

//		throw new IllegalArgumentException("Unable to read build.properties file");


		return buildProperties;
	}

	public Properties getBuildProperties() {
		return buildProperties;
	}

	public BundleDescription getBundleDescription() {
		return bundleDescription;
	}

	public MavenProject getMavenProject() {
		return project;
	}

	public List<BuildOutputJar> getOutputJars() {
		return new ArrayList<BuildOutputJar>(outputJars.values());
	}

	public BuildOutputJar getDotOutputJar() {
		return dotOutputJar;
	}

	public Map<String, BuildOutputJar> getOutputJarMap() {
		return outputJars;
	}

}
