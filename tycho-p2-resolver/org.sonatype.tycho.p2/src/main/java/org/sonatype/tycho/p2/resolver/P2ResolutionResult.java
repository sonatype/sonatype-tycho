package org.sonatype.tycho.p2.resolver;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class P2ResolutionResult
{

    public static class Entry
    {
        private final String type;

        private final String id;

        private final String version;

        private final File location;

        private Set<Object> installableUnits;

        public Entry( String type, String id, String version, File location, Set<Object> installableUnits )
        {
            this.type = type;
            this.id = id;
            this.version = version;
            this.location = location;
            this.installableUnits = installableUnits;
        }

        public String getType()
        {
            return type;
        }

        public String getId()
        {
            return id;
        }

        public String getVersion()
        {
            return version;
        }

        public File getLocation()
        {
            return location;
        }

        public Set<Object> getInstallableUnits()
        {
            return installableUnits;
        }

        void setInstallableUnits( Set<Object> installableUnits )
        {
            this.installableUnits = installableUnits;
        }
    }

    private final List<Entry> artifacts = new ArrayList<Entry>();

    private final Map<File, Entry> locations = new HashMap<File, Entry>();

    private final Set<Object/* IInstallableUnit */> installableUnits = new LinkedHashSet<Object>();

    public void addArtifact( String type, String id, String version, File location, Object installableUnit )
    {
        Entry entry = null;
        if ( location != null )
        {
            entry = locations.get( location );
        }

        if ( entry != null )
        {
            if ( !entry.getType().equals( type ) || !entry.getId().equals( id ) || !entry.getVersion().equals( version ) )
            {
                throw new IllegalArgumentException( "Conflicting results for artifact at location " + location );
            }

            // merge installable units
            Set<Object> installableUnits = new HashSet<Object>();
            installableUnits.addAll( entry.getInstallableUnits() );
            installableUnits.add( installableUnit );
            entry.setInstallableUnits( installableUnits );
        }
        else
        {
            entry = new Entry( type, id, version, location, Collections.singleton( installableUnit ) );
            artifacts.add( entry );
            if ( location != null )
            {
                locations.put( location, entry );
            }
        }
    }

    public Collection<Entry> getArtifacts()
    {
        return artifacts;
    }

    public Set<Object> getInstallableUnits()
    {
        return installableUnits;
    }

    public void addInstallableUnits( Set<Object/* IInstallableUnit */> installableUnits )
    {
        this.installableUnits.addAll( installableUnits );
    }
}
