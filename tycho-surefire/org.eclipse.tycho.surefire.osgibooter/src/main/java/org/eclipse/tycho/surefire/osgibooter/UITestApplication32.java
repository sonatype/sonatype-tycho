package org.eclipse.tycho.surefire.osgibooter;

import org.eclipse.core.runtime.IPlatformRunnable;
import org.eclipse.core.runtime.Platform;

@SuppressWarnings("deprecation")
public class UITestApplication32 extends AbstractUITestApplication implements IPlatformRunnable {

	public Object run(Object args) throws Exception {
		return run(Platform.getApplicationArgs());
	}

	@Override
	protected void runApplication(Object application, String[] args) throws Exception {
		((IPlatformRunnable) application).run(args);
	}

}
