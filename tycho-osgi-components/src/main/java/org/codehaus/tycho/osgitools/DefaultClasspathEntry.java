package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.List;

import org.codehaus.tycho.ClasspathEntry;

public class DefaultClasspathEntry
    implements ClasspathEntry
{
    private final List<File> locations;

    private final List<AccessRule> rules;

    public DefaultClasspathEntry( List<File> locations, List<AccessRule> rules )
    {
        this.locations = locations;
        this.rules = rules;
    }

    public List<File> getLocations()
    {
        return locations;
    }

    public List<AccessRule> getAccessRules()
    {
        return rules;
    }

}
