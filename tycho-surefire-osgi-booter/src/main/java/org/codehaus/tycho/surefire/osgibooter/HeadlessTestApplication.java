package org.codehaus.tycho.surefire.osgibooter;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class HeadlessTestApplication implements IApplication {

	public Object start(IApplicationContext context) throws Exception {
		String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
        return new Integer(OsgiSurefireBooter.run(args));
	}

	public void stop() {
		// TODO Auto-generated method stub

	}
	
}
