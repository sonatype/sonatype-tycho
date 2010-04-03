package explicit.start.test;

import org.junit.Assert;
import org.junit.Test;

import explicit.start.Activator;

public class ExplicitStartTest
{
    @Test
    public void test()
    {
        Assert.assertNotNull( Activator.context );
    }
}
