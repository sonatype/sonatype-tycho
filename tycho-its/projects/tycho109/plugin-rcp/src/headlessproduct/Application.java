package headlessproduct;

import org.eclipse.core.runtime.IPlatformRunnable;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IPlatformRunnable {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.IPlatformRunnable#run(java.lang.Object)
	 */
	public Object run(Object args) throws Exception {
		System.out.println("Hello RCP World!");
		System.exit(911);
		return IPlatformRunnable.EXIT_OK;
	}
}
