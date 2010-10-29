package org.codehaus.tycho.testing;

import java.io.File;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.tycho.equinox.EquinoxRuntimeLocator;

@Component( role = EquinoxRuntimeLocator.class, hint = "stub" )
public class StubEquinoxRuntimeLocator
    implements EquinoxRuntimeLocator
{

    public List<File> getRuntimeLocations()
        throws Exception
    {
        throw new UnsupportedOperationException();
    }

    public List<String> getSystemPackagesExtra()
    {
        throw new UnsupportedOperationException();
    }
}
