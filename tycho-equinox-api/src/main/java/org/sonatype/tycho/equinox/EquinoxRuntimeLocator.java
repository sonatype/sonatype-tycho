package org.sonatype.tycho.equinox;

import java.io.File;
import java.util.List;

public interface EquinoxRuntimeLocator
{
    // TODO do we need more specific exception type here?
    public List<File> getRuntimeLocations()
        throws Exception;

    /**
     * Packages exported by embedding application. This allows embedded runtime import API classes from embedding
     * application with Import-Package.
     */
    public List<String> getSystemPackagesExtra();
}
