package org.codehaus.tycho.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class Target
{
    private final Xpp3Dom dom;

    public static class Location
    {
        private final Xpp3Dom dom;

        public Location( Xpp3Dom dom )
        {
            this.dom = dom;
        }

        public List<Unit> getUnits()
        {
            ArrayList<Unit> units = new ArrayList<Unit>();
            for ( Xpp3Dom unitDom : dom.getChildren( "unit" ) )
            {
                units.add( new Unit( unitDom ) );
            }
            return Collections.unmodifiableList( units );
        }

        public String getRepositoryLocation()
        {
            Xpp3Dom repositoryDom = dom.getChild( "repository" );
            if ( repositoryDom == null )
            {
                return null;
            }

            return repositoryDom.getAttribute( "location" );
        }

        public void setRepositoryLocation( String locationURI )
        {
            Xpp3Dom repositoryDom = dom.getChild( "repository" );
            if ( repositoryDom != null )
            {
                repositoryDom.setAttribute( "location", locationURI );
            }
        }

        public String getRepositoryId()
        {
            Xpp3Dom repositoryDom = dom.getChild( "repository" );
            if ( repositoryDom == null )
            {
                return null;
            }

            // this is Maven specific, used to match credentials and mirrors
            return repositoryDom.getAttribute( "id" );
        }
    }

    public static class Unit
    {
        private final Xpp3Dom dom;

        public Unit( Xpp3Dom dom )
        {
            this.dom = dom;
        }

        public String getId()
        {
            return dom.getAttribute( "id" );
        }

        public String getVersion()
        {
            return dom.getAttribute( "version" );
        }
    }

    public Target( Xpp3Dom dom )
    {
        this.dom = dom;
    }

    public List<Location> getLocations()
    {
        ArrayList<Location> locations = new ArrayList<Location>();
        Xpp3Dom locationsDom = dom.getChild( "locations" );
        if ( locationsDom != null )
        {
            for ( Xpp3Dom locationDom : locationsDom.getChildren( "location" ) )
            {
                locations.add( new Location( locationDom ) );
            }
        }
        return Collections.unmodifiableList( locations );
    }

    @SuppressWarnings( "deprecation" )
    public static Target read( File file )
        throws IOException, XmlPullParserException
    {
        XmlStreamReader reader = ReaderFactory.newXmlReader( file );
        try
        {
            return new Target( Xpp3DomBuilder.build( reader ) );
        }
        finally
        {
            reader.close();
        }
    }

    public static void write( Target target, File file )
        throws IOException
    {
        Writer writer = new OutputStreamWriter( new FileOutputStream( file ), "UTF-8" );
        try
        {
            Xpp3DomWriter.write( writer, target.dom );
        }
        finally
        {
            writer.close();
        }
    }

}
