package org.sonatype.tycho.p2.facade.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;


/**
 * Simplistic local Maven repository index to allow efficient lookup 
 * of all installed Tycho projects.
 */
public class LocalTychoRepositoryIndex
    extends DefaultTychoRepositoryIndex
{
    private final File indexFile;

    public LocalTychoRepositoryIndex( File basedir )
    {
        this.indexFile = new File( basedir, INDEX_RELPATH );
        try
        {
            this.gavs = read( new FileInputStream( indexFile ) );
        }
        catch ( IOException e )
        {
            // lets assume index does not exist yet
            this.gavs = new LinkedHashSet<GAV>();
        }
    }

    public static void addProject( File basedir, String groupId, String artifactId, String version )
        throws IOException
    {
        lock( basedir );

        try
        {
            LocalTychoRepositoryIndex index = new LocalTychoRepositoryIndex( basedir );
            index.addProject( groupId, artifactId, version );
            index.save();
        }
        finally
        {
            unlock( basedir );
        }
    }

    public void save()
        throws IOException
    {
        indexFile.getParentFile().mkdirs();

        write( new FileOutputStream( indexFile ) );
    }

    public static void unlock( File basedir )
    {
        // TODO Auto-generated method stub

    }

    public static void lock( File basedir )
    {
        // TODO Auto-generated method stub

    }

}
