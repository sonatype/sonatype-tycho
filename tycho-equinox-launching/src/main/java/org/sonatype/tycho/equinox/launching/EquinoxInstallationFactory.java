package org.sonatype.tycho.equinox.launching;

import java.io.File;

public interface EquinoxInstallationFactory
{
    public EquinoxInstallation createInstallation( EquinoxInstallationDescription description, File location );
}
