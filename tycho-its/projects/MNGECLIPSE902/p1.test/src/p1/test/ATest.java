package p1.test;

import junit.framework.TestCase;
import p1.Activator;

public class ATest extends TestCase {

	public void testSuccess() {
		assertEquals("p1.Activator", Activator.class.getName());
	}

}
