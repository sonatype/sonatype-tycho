package org.sonatype.tycho.launching;

import java.util.List;

import org.sonatype.tycho.ReactorProject;

public interface LaunchConfigurationFactory
{
    public LaunchConfiguration createLaunchConfiguration( List<ReactorProject> reactorProjects );
}
