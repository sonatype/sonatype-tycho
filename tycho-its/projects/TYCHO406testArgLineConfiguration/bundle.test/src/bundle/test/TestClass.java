package bundle.test;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestClass {

    @Test
    public void simpleTest() {
        ClassUnderTest cut = new ClassUnderTest();
        assertEquals(42, cut.add(39,3));
    }
}
