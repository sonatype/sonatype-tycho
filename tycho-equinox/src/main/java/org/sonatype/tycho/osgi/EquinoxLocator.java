package org.sonatype.tycho.osgi;

import java.io.File;

public interface EquinoxLocator
{

    File getRuntimeLocation();

    void setRuntimeLocation( File runtimeLocation );

}
