package org.codehaus.tycho;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class DefaultTargetPlatform
    implements TargetPlatform
{

    private Map<File, String> artifacts = new LinkedHashMap<File, String>();
    private Properties properties;
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

    public String getProperty( String key )
    {
        return properties.getProperty( key );
    }

    public void addArtifactFile( String artifactType, File artifactFile )
    {
        artifacts.put( artifactFile, artifactType );
    }

    public void setProperties( Properties properties )
    {
        this.properties = new Properties();
        this.properties.putAll( properties );

        this.properties.put(PlatformPropertiesUtils.OSGI_OS, PlatformPropertiesUtils.getOS(this.properties));
        this.properties.put(PlatformPropertiesUtils.OSGI_WS, PlatformPropertiesUtils.getWS(this.properties));
        this.properties.put(PlatformPropertiesUtils.OSGI_ARCH, PlatformPropertiesUtils.getArch(this.properties));
        ExecutionEnvironmentUtils.loadVMProfile(this.properties);
    }

    public Properties getProperties()
    {
        return properties;
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
