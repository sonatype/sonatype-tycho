package org.eclipse.tycho.equinox.launching;

import org.eclipse.tycho.launching.LaunchConfiguration;

public interface EquinoxLauncher
{
    public int execute( LaunchConfiguration configuration, int forkedProcessTimeoutInSeconds )
        throws EquinoxLaunchingException;
}
