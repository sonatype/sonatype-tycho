package org.eclipse.tycho.equinox.launching;

import java.io.File;

public interface EquinoxInstallation
{
    public File getLauncherJar();

    public File getLocation();

    public EquinoxInstallationDescription getInstallationDescription();
}
