package org.eclipse.tycho.surefire.osgibooter;

import org.eclipse.core.runtime.IPlatformRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

@SuppressWarnings("deprecation")
public class UITestApplication extends AbstractUITestApplication implements IApplication {

	private IApplicationContext fContext;

	public void stop() {
	}

	@Override
	protected void runApplication(Object application, String[] args) throws Exception {
		if (application instanceof IPlatformRunnable) {
			((IPlatformRunnable) application).run(args);
		} else if (application instanceof IApplication) {
			((IApplication) application).start(fContext);
		}
	}

	public Object start(IApplicationContext context) throws Exception {
		this.fContext = context;
		return run(Platform.getApplicationArgs());
	}

}
