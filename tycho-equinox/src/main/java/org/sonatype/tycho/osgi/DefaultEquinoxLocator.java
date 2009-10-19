package org.sonatype.tycho.osgi;

import java.io.File;

import org.codehaus.plexus.component.annotations.Component;

@Component( role = EquinoxLocator.class )
public class DefaultEquinoxLocator
    implements EquinoxLocator
{

    private File runtimeLocation;

    public File getRuntimeLocation()
    {
        return runtimeLocation;
    }

    public void setRuntimeLocation( File runtimeLocation )
    {
        this.runtimeLocation = runtimeLocation;
    }

}
