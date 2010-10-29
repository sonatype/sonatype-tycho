package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.sonatype.tycho.ArtifactKey;
import org.sonatype.tycho.classpath.ClasspathEntry;

public class DefaultClasspathEntry
    implements ClasspathEntry
{
    private final MavenProject project;

    private final ArtifactKey key;

    private final List<File> locations;

    private final List<AccessRule> rules;

    public static class DefaultAccessRule
        implements AccessRule
    {
        private final String pattern;

        private final boolean discouraged;

        public DefaultAccessRule( String path, boolean discouraged )
        {
            this.pattern = path;
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
            return isDiscouraged() == other.isDiscouraged() && getPattern().equals( other.getPattern() );
        }

        public String getPattern()
        {
            return pattern;
        }

        public boolean isDiscouraged()
        {
            return discouraged;
        }

    }

    public DefaultClasspathEntry( MavenProject project, ArtifactKey key, List<File> locations, List<AccessRule> rules )
    {
        this.project = project;
        this.key = key;
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

    public ArtifactKey getArtifactKey()
    {
        return key;
    }

    public MavenProject getMavenProject()
    {
        return project;
    }
}
