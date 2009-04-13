package org.sonatype.tycho.p2.lockdown.test;

import java.io.File;
import java.io.FileInputStream;

import junit.framework.TestCase;

import org.codehaus.plexus.util.IOUtil;
import org.sonatype.tycho.p2.lockdown.io.xpp3.LockdownDescriptorXpp3Reader;

public class IOTest
    extends TestCase
{

    public void testIO() throws Exception
    {
        LockdownDescriptorXpp3Reader reader = new LockdownDescriptorXpp3Reader();
        FileInputStream is = new FileInputStream( new File( "src/test/resources/lockdown.xml" ) );
        try
        {
            reader.read( is );
        }
        finally
        {
            IOUtil.close( is );
        }
    }
}
