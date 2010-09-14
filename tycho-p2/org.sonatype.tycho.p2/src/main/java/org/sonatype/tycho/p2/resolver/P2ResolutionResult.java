package org.sonatype.tycho.p2.resolver;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class P2ResolutionResult
{

    public static class Entry
    {
        private final String type;

        private final String id;

        private final String version;

        private final File location;

        public Entry( String type, String id, String version, File location )
        {
            this.type = type;
            this.id = id;
            this.version = version;
            this.location = location;
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
    }

    private final List<Entry> artifacts = new ArrayList<Entry>();

    private final Set<Object/* IInstallableUnit */> installableUnits = new LinkedHashSet<Object>();

    public void addArtifact( String type, String id, String version, File location )
    {
        artifacts.add( new Entry( type, id, version, location ) );
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
