package org.eclipse.tycho.testing;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

public class TestUtil
{

    public static File getBasedir( String name )
        throws IOException
    {
        File src = new File( PlexusTestCase.getBasedir(), "src/test/resources/" + name );
        File dst = new File( PlexusTestCase.getBasedir(), "target/" + name );

        if ( dst.isDirectory() )
        {
            FileUtils.deleteDirectory( dst );
        }
        else if ( dst.isFile() )
        {
            if ( !dst.delete() )
            {
                throw new IOException( "Can't delete file " + dst.toString() );
            }
        }

        FileUtils.copyDirectoryStructure( src, dst );

        return dst;
    }
}
