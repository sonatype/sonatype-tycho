package org.codehaus.tycho;

import java.io.File;
import java.util.List;

public interface TargetPlatform
{
    /**
     * Returns all artifact files of given types. 
     * 
     * Implementation must insure artifacts corresponding to MavenProjects are 
     * at the end of the list. 
     * 
     * TODO Probably not the best API spec.
     */
    public List<File> getArtifactFiles( String... types );

    public List<File> getSites();
}
