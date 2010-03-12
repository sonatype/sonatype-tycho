package bundle;

import org.junit.Test;

public class BrokenTest
{
    @Test
    public void test()
    {
        throw new RuntimeException();
    }
}
