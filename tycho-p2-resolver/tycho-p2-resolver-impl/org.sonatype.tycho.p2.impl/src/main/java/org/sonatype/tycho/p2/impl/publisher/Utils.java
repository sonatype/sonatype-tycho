package org.sonatype.tycho.p2.impl.publisher;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class Utils
{

    public static Properties loadBuildProperties( File projectRootDir )
    {
        File file = new File( projectRootDir, "build.properties" );

        Properties buildProperties = new Properties();
        if ( file.canRead() )
        {
            InputStream is = null;
            try
            {
                try
                {
                    is = new FileInputStream( file );
                    buildProperties.load( is );
                }
                finally
                {
                    if ( is != null )
                        is.close();
                }
            }
            catch ( Exception e )
            {
                // ignore
            }
        }

        return buildProperties;
    }

}
