package org.sonatype.tycho.p2.tools;

import java.io.File;
import java.util.List;

public class BuildContext
{
    private final String qualifier;

    private final List<TargetEnvironment> environments;

    private final File targetDirectory;

    /**
     * Creates a new <code>BuildContext</code> instance.
     * 
     * @param qualifier The build qualifier
     * @param configurations The list of selected environments targeted by the build
     * @param targetDirectory The directory for temporary files of the current module
     */
    public BuildContext( String qualifier, List<TargetEnvironment> environments, File targetDirectory )
    {
        this.qualifier = qualifier;
        this.environments = environments;
        this.targetDirectory = targetDirectory;
    }

    public String getQualifier()
    {
        return qualifier;
    }

    public List<TargetEnvironment> getEnvironments()
    {
        return environments;
    }

    public File getTargetDirectory()
    {
        return targetDirectory;
    }
}