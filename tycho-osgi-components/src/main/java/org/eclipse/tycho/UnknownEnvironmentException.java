package org.eclipse.tycho;

/**
 * Indicates that an OSGi execution environment is unknown if thrown.
 * 
 * @author jan.sievers@sap.com
 * 
 */
public class UnknownEnvironmentException extends Exception {

	private final String environmentName;

	public UnknownEnvironmentException(String environmentName) {
		this.environmentName = environmentName;
	}

	public String getEnvironmentName() {
		return environmentName;
	}
}