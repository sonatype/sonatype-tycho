package org.sonatype.tycho.p2.tools.publisher;

import java.io.File;

public class BuildContext
{
    private final String qualifier;

    private final String[] configurations;

    private final File targetDirectory;

    /**
     * Creates a new BuildContext instance.
     * 
     * @param qualifier
     * @param configurations List of platform configurations; the entries must be of the form
     *            "ws.os.arch".
     * @param targetDirectory
     */
    public BuildContext( String qualifier, String[] configurations, File targetDirectory )
    {
        this.qualifier = qualifier;
        this.configurations = configurations;
        this.targetDirectory = targetDirectory;
    }

    public String getQualifier()
    {
        return qualifier;
    }

    public String[] getConfigurations()
    {
        return configurations;
    }

    public File getTargetDirectory()
    {
        return targetDirectory;
    }
}
