package org.codehaus.tycho;

import java.io.File;
import java.util.List;
import java.util.Properties;

public interface TargetPlatform
{
    public String getProperty( String key );

    /**
     * Returns all artifact files of given types. 
     * 
     * Implementation must insure artifacts corresponding to MavenProjects are 
     * at the end of the list. 
     * 
     * TODO Probably not the best API spec.
     */
    public List<File> getArtifactFiles( String... types );

    public Properties getProperties();
    
    public List<File> getSites();
}
