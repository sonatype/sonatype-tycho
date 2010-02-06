package tycho.its;

import junit.framework.TestCase;

public class Test
    extends TestCase
{

    public void test()
        throws Exception
    {
        assertEquals( "spacy value", System.getProperty( "tycho.351" ) );
    }

}
