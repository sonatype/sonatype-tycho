package org.codehaus.tycho;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultTargetPlatform
    implements TargetPlatform
{

    private Map<File, String> artifacts = new LinkedHashMap<File, String>();
    private Set<File> sites = new LinkedHashSet<File>();

    public List<File> getArtifactFiles( String... artifactTypes )
    {
        ArrayList<File> result = new ArrayList<File>();
        for ( Map.Entry<File, String> entry : artifacts.entrySet() )
        {
            for ( String type : artifactTypes )
            {
                if ( type.equals( entry.getValue() ) )
                {
                    result.add( entry.getKey() );
                    break;
                }
            }
        }

        return result;
    }

    public void addArtifactFile( String artifactType, File artifactFile )
    {
        artifacts.put( artifactFile, artifactType );
    }

    public void addSite( File location )
    {
        sites.add( location );
    }

    public List<File> getSites()
    {
        return new ArrayList<File>( sites );
    }
}
