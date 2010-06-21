package org.codehaus.tycho.surefire.osgibooter;

import org.eclipse.core.runtime.IPlatformRunnable;
import org.eclipse.core.runtime.Platform;

@SuppressWarnings("deprecation")
public class HeadlessTestApplication implements IPlatformRunnable {

	public Object run(Object object) throws Exception {
		String[] args = Platform.getCommandLineArgs();
        return new Integer(OsgiSurefireBooter.run(args));
	}
	
}
