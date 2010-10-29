package org.sonatype.tycho.launching;

import java.io.File;
import java.util.Map;

public interface LaunchConfiguration
{
    public Map<String, String> getEnvironment();

    public File getWorkingDirectory();

    public String[] getProgramArguments();

    public String[] getVMArguments();

    public File getLauncherJar();
}
