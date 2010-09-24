package org.sonatype.tycho.equinox.embedder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;

@Component( role = EquinoxRuntimeLocator.class )
public class DefaultEquinoxRuntimeLocator
    implements EquinoxRuntimeLocator
{

    private final List<File> runtimeLocations = new ArrayList<File>();

    public List<File> getRuntimeLocations()
    {
        return runtimeLocations;
    }

    public void addRuntimeLocation( File runtimeLocation )
    {
        this.runtimeLocations.add( runtimeLocation );
    }

}
