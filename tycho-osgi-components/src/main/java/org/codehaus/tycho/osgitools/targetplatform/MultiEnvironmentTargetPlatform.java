package org.codehaus.tycho.osgitools.targetplatform;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.p2.MetadataSerializable;
import org.codehaus.tycho.p2.MetadataSerializableMerger;

public class MultiEnvironmentTargetPlatform
    extends DefaultTargetPlatform
{
    public Map<TargetEnvironment, TargetPlatform> platforms = new LinkedHashMap<TargetEnvironment, TargetPlatform>();
    private final MetadataSerializableMerger<MetadataSerializable> metadataRepositorySerializableMerger;

    public MultiEnvironmentTargetPlatform( MetadataSerializableMerger<MetadataSerializable> metadataRepositorySerializableMerger )
    {
        this.metadataRepositorySerializableMerger = metadataRepositorySerializableMerger;
    }

    public void addPlatform( TargetEnvironment environment, DefaultTargetPlatform platform )
    {
        platforms.put( environment, platform );

        artifacts.putAll( platform.artifacts );
        locations.putAll( platform.locations );
    }

    public TargetPlatform getPlatform( TargetEnvironment environment )
    {
        return platforms.get( environment );
    }
    
    @Override
    public MetadataSerializable getP2MetadataSerializable()
    {
        Set<MetadataSerializable> serializables = new HashSet<MetadataSerializable>();
        for ( TargetPlatform platform : platforms.values() )
        {
            serializables.add(platform.getP2MetadataSerializable());
        }
        return metadataRepositorySerializableMerger.merge( serializables );
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
