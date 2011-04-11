package org.eclipse.tycho.launching;

import java.util.List;

import org.eclipse.tycho.ReactorProject;

public interface LaunchConfigurationFactory
{
    public LaunchConfiguration createLaunchConfiguration( List<ReactorProject> reactorProjects );
}
