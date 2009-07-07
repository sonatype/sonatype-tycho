package org.sonatype.tycho.p2.facade.internal;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of TychoRepositoryIndex defines tycho repository index format and provides generic index
 * read/write methods.
 */
public class DefaultTychoRepositoryIndex
    implements TychoRepositoryIndex
{
    protected static final String ENCODING = "UTF8";

    protected static final String EOL = "\n";

    // must match extension point filter in plugin.xml
    public static final String INDEX_RELPATH = ".meta/p2-metadata.properties";

    protected Set<GAV> gavs = new LinkedHashSet<GAV>();

    protected static Set<GAV> read( InputStream is )
        throws IOException
    {
        LinkedHashSet<GAV> result = new LinkedHashSet<GAV>();

        BufferedReader br = new BufferedReader( new InputStreamReader( is, ENCODING ) );
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

        return result;
    }

    public List<GAV> getProjectGAVs()
    {
        return new ArrayList<GAV>( gavs );
    }

    public void addProject( String groupId, String artifactId, String version )
    {
        addProject( new GAV( groupId, artifactId, version ) );
    }

    public void addProject( GAV gav )
    {
        gavs.add( gav );
    }

    public void write( OutputStream os )
        throws IOException
    {
        Writer out = new OutputStreamWriter( new BufferedOutputStream( os ), ENCODING );
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

}
