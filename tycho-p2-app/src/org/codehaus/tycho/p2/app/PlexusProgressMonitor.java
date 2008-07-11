package org.codehaus.tycho.p2.app;

import java.lang.reflect.Method;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Redirects IProgressMonitor to plexus logger. The logger is accessed via
 * reflection.
 * 
 * @author igor
 *
 */
public class PlexusProgressMonitor implements IProgressMonitor {

	private final Object logger;
	private final Method info;

	public PlexusProgressMonitor(Object logger) throws SecurityException, NoSuchMethodException {
		this.logger = logger;
		info = logger.getClass().getMethod("info", String.class);
	}

	public void beginTask(String name, int totalWork) {
		try {
			info.invoke(logger, name);
		} catch (Exception e) {
			// too bad
		}
	}

	public void done() {
	}

	public void internalWorked(double work) {
	}

	public boolean isCanceled() {
		return false;
	}

	public void setCanceled(boolean value) {
	}

	public void setTaskName(String name) {
	}

	public void subTask(String name) {
		try {
			info.invoke(logger, name);
		} catch (Exception e) {
			// too bad
		}
	}

	public void worked(int work) {
	}

}
