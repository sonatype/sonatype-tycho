package tycho.its;

import junit.framework.TestCase;

public class Test
    extends TestCase
{

    public void test()
        throws Exception
    {
        assertEquals( "PASSED", System.getProperty( "tycho.353" ) );
        assertNotNull( getClass().getResource( "/tycho353/tycho353.properties" ) );
    }

}
