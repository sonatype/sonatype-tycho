package bundle.tests;

import junit.framework.TestCase;

public class SystemPropertyTest extends TestCase {

	public void test() {
		assertEquals("test-value", System.getProperty("test.property"));
	}

}
