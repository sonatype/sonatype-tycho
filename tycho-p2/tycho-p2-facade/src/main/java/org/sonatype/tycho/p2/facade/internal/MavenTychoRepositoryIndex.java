package org.sonatype.tycho.p2.facade.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonException;

public class MavenTychoRepositoryIndex
    extends DefaultTychoRepositoryIndex
{

    public MavenTychoRepositoryIndex( Wagon wagon )
        throws WagonException,
            IOException
    {
        File file = File.createTempFile( "tycho-index", ".properties" );

        try
        {
            wagon.get( INDEX_RELPATH, file );

            this.gavs = read( new FileInputStream( file ) );
        }
        finally
        {
            file.delete();
        }
    }

}
