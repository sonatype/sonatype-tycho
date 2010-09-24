package org.sonatype.tycho.equinox.embedder;

import java.io.File;
import java.util.List;

public interface EquinoxRuntimeLocator
{
    public List<File> getRuntimeLocations();

    /**
     * Location can either be a file or directory. Files will be included in equinox runtime without any further
     * processing. For directories, equinox runtime will include all files from plugins/ subdirectory.
     */
    public void addRuntimeLocation( File runtimeLocation );
}
