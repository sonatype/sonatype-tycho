package org.eclipse.tycho.versions.engine;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProjectMetadata
{
    private final File basedir;

    @SuppressWarnings( "rawtypes" )
    private Map metadata = new LinkedHashMap();

    public ProjectMetadata( File basedir )
    {
        this.basedir = basedir;
    }

    public <T> T getMetadata( Class<T> type )
    {
        return type.cast( metadata.get( type ) );
    }

    @SuppressWarnings( { "unchecked" } )
    public <T> void putMetadata( T metadata )
    {
        this.metadata.put( metadata.getClass(), metadata );
    }

    public File getBasedir()
    {
        return basedir;
    }

}
