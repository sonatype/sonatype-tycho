package org.codehaus.tycho;

import java.io.File;
import java.util.List;

public interface ClasspathEntry
{
    public static class AccessRule
    {
        public final String path;

        public final boolean discouraged;

        public AccessRule( String path, boolean discouraged )
        {
            this.path = path;
            this.discouraged = discouraged;
        }

        public boolean equals( Object obj )
        {
            if ( this == obj )
            {
                return true;
            }
            if ( !( obj instanceof AccessRule ) )
            {
                return false;
            }
            AccessRule other = (AccessRule) obj;
            return discouraged == other.discouraged && path.equals( other.path );
        }
    }

    public List<File> getLocations();

    /**
     * <code>null</code> means "no access restrictions"
     */
    public List<AccessRule> getAccessRules();
}
