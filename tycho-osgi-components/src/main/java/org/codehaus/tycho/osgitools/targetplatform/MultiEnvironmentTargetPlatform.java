package org.codehaus.tycho.osgitools.targetplatform;

import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TargetPlatform;

public class MultiEnvironmentTargetPlatform
    extends DefaultTargetPlatform
{
    public Map<TargetEnvironment, TargetPlatform> platforms = new LinkedHashMap<TargetEnvironment, TargetPlatform>();

    public void addPlatform( TargetEnvironment environment, DefaultTargetPlatform platform )
    {
        platforms.put( environment, platform );

        artifacts.putAll( platform.artifacts );
        projects.putAll( platform.projects );
        sites.addAll( platform.sites );
    }

    public TargetPlatform getPlatform( TargetEnvironment environment )
    {
        return platforms.get( environment );
    }
}
