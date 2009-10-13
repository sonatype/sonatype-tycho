package tycho.its.app.args;

import java.util.Arrays;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Platform;

public class Test
    extends TestCase
{

    public void test()
        throws Exception
    {
        assertTrue( Arrays.asList( Platform.getApplicationArgs() ).contains( "-foo" ) );
    }
}
