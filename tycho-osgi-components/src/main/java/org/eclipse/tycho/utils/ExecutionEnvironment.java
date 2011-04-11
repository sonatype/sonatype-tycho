package org.eclipse.tycho.utils;

import java.util.Properties;

/**
 * Represents an OSGi execution environment a.k.a. profile. Execution
 * environments are referenced in MANIFEST.MF using the header
 * "Bundle-RequiredExecutionEnvironment".
 * 
 * See the list of known OSGi profiles in bundle org.eclipse.osgi, file
 * profile.list.
 * 
 * @author jan.sievers@sap.com
 * 
 */
public class ExecutionEnvironment {

	private String profileName;
	private String compilerSourceLevel;
	private String compilerTargetLevel;

	/**
	 * Do no instantiate. Use factory method instead
	 * {@link ExecutionEnvironmentUtils#getExecutionEnvironment(String)}.
	 */
	/* package */ExecutionEnvironment(Properties profileProperties) {
		this.profileName = profileProperties
				.getProperty("osgi.java.profile.name");
		this.compilerSourceLevel = profileProperties
				.getProperty("org.eclipse.jdt.core.compiler.source");
		this.compilerTargetLevel = profileProperties
				.getProperty("org.eclipse.jdt.core.compiler.codegen.targetPlatform");
	}

	public String getProfileName() {
		return profileName;
	}

	public String getCompilerSourceLevel() {
		return compilerSourceLevel;
	}

	public String getCompilerTargetLevel() {
		return compilerTargetLevel;
	}

	/*
	 * for debug purposes
	 */
	@Override
	public String toString() {
		return "OSGi profile '" + getProfileName() + "' { source level: "
				+ compilerSourceLevel + ", target level: "
				+ compilerTargetLevel + "}";
	}

}
