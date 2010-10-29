package org.sonatype.tycho.equinox.launching;

import org.sonatype.tycho.launching.LaunchConfiguration;

public interface EquinoxLauncher
{
    public int execute( LaunchConfiguration configuration, int forkedProcessTimeoutInSeconds )
        throws EquinoxLaunchingException;
}
