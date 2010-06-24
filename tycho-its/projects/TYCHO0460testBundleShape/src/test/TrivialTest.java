package test;

import junit.framework.TestCase;

public class TrivialTest
    extends TestCase
{
    public void test()
    {
        assertNotNull( Activator.context.getBundle().getEntry( "test-resource.txt" ) );
    }
}
