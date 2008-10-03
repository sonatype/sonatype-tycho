package bundle.test;

import junit.framework.TestCase;
import bundle.UTF8Source;

public class UTF8SourceTest extends TestCase {

	public void test() {
		UTF8Source utf8 = new UTF8Source();

		assertEquals("\u041F\u043E-\u0440\u0443\u0441\u0441\u043A\u0438", utf8.getText());
	}

}
