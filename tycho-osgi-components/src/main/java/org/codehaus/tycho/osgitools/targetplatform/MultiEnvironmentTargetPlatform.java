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
        locations.putAll( platform.locations );
        nonReactorUnits.addAll( platform.nonReactorUnits );
    }

    public TargetPlatform getPlatform( TargetEnvironment environment )
    {
        return platforms.get( environment );
    }

    @Override
    public void toDebugString( StringBuilder sb, String linePrefix )
    {
        for ( Map.Entry<TargetEnvironment, TargetPlatform> entry : platforms.entrySet() )
        {
            sb.append( linePrefix );
            sb.append( "Target environment: " ).append( entry.getKey().toString() ).append( "\n" );
            entry.getValue().toDebugString( sb, linePrefix );
        }
    }
}
