package org.sonatype.tycho.classpath;

import java.io.File;
import java.util.List;

public interface SourcepathEntry
{
    public File getOutputDirectory();

    public File getSourcesRoot();

    /**
     * null means "everything included"
     */
    public List<String> getIncludes();

    /**
     * null means "nothing excluded"
     */
    public List<String> getExcludes();
}
