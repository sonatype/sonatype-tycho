package org.codehaus.tycho;

import java.io.File;
import java.util.List;
import java.util.Properties;

public interface TargetPlatform
{
    public String getProperty( String key );

    public List<File> getArtifactFiles( String... types );

    public Properties getProperties();
    
    public List<File> getSites();
}
