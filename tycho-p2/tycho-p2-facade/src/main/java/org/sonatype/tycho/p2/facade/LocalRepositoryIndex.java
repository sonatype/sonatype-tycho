package org.sonatype.tycho.p2.facade;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Simplistic local Maven repository index to allow efficient lookup of all installed Tycho projects.
 */
public class LocalRepositoryIndex
{
    private static final String ENCODING = "UTF8";

    private static final String EOL = "\n";

    // must match extension point filter in plugin.xml
    public static final String INDEX_RELPATH = ".p2/metadata.properties";

    private final Set<GAV> gavs;

    private final File indexFile;

    public LocalRepositoryIndex( File basedir )
    {
        this.indexFile = new File( basedir, INDEX_RELPATH );
        gavs = load();
    }

    private Set<GAV> load()
    {
        LinkedHashSet<GAV> result = new LinkedHashSet<GAV>();

        try
        {
            BufferedReader br = new BufferedReader( new InputStreamReader( new FileInputStream( indexFile ), ENCODING ) );
            try
            {
                String str;
                while ( ( str = br.readLine() ) != null )
                {
                    result.add( GAV.parse( str ) );
                }
            }
            finally
            {
                br.close();
            }
        }
        catch ( IOException e )
        {
            // lets assume index does not exist
        }

        return result;
    }

    public static void addProject( File basedir, String groupId, String artifactId, String version )
        throws IOException
    {
        lock( basedir );

        try
        {
            LocalRepositoryIndex index = new LocalRepositoryIndex( basedir );
            index.addProject( groupId, artifactId, version );
            index.save();
        }
        finally
        {
            unlock( basedir );
        }

    }

    public static void unlock( File basedir )
    {
        // TODO Auto-generated method stub

    }

    public static void lock( File basedir )
    {
        // TODO Auto-generated method stub

    }

    public void save()
        throws IOException
    {
        indexFile.getParentFile().mkdirs();

        Writer out = new OutputStreamWriter( new BufferedOutputStream( new FileOutputStream( indexFile ) ), ENCODING );
        try
        {
            for ( GAV gav : gavs )
            {
                out.write( gav.toExternalForm() );
                out.write( EOL );
            }
            out.flush();
        }
        finally
        {
            out.close();
        }
    }

    public void addProject( String groupId, String artifactId, String version )
    {
        addProject( new GAV( groupId, artifactId, version ) );
    }

    public void addProject( GAV gav )
    {
        gavs.add( gav );
    }

    public List<GAV> getProjectGAVs()
    {
        return new ArrayList<GAV>( gavs );
    }

}
