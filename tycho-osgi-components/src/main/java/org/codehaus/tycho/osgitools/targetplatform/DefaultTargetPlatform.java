package org.codehaus.tycho.osgitools.targetplatform;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.ArtifactKey;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TychoConstants;
import org.osgi.framework.Version;

public class DefaultTargetPlatform
    implements TargetPlatform
{
    private static final Version VERSION_0_0_0 = new Version( "0.0.0" );

    private Map<ArtifactKey, File> artifacts = new LinkedHashMap<ArtifactKey, File>();

    private Map<File, MavenProject> projects = new LinkedHashMap<File, MavenProject>();

    private Set<File> sites = new LinkedHashSet<File>();

    public List<File> getArtifactFiles( String... artifactTypes )
    {
        ArrayList<File> result = new ArrayList<File>();
        for ( Map.Entry<ArtifactKey, File> entry : artifacts.entrySet() )
        {
            for ( String type : artifactTypes )
            {
                if ( type.equals( entry.getKey().getType() ) )
                {
                    result.add( entry.getValue() );
                    break;
                }
            }
        }

        return result;
    }

    public void addArtifactFile( ArtifactKey key, File artifactFile )
    {
        artifacts.put( key, artifactFile );
    }

    public void addSite( File location )
    {
        sites.add( location );
    }

    public List<File> getSites()
    {
        return new ArrayList<File>( sites );
    }

    public void dump()
    {
        for ( Map.Entry<ArtifactKey, File> entry : artifacts.entrySet() )
        {
            System.out.println( entry.getKey() + "\t" + entry.getValue() );
        }
    }

    public boolean isEmpty()
    {
        return artifacts.isEmpty();
    }

    public File getArtifact( ArtifactKey key )
    {
        return getArtifact( key.getType(), key.getId(), key.getVersion() );
    }

    public ArtifactKey getArtifactKey( String type, String id, String version )
    {
        if ( type == null || id == null )
        {
            // TODO should we throw something instead?
            return null;
        }

        // features with matching id, sorted by version, highest version first
        SortedMap<Version, ArtifactKey> relevantArtifacts =
            new TreeMap<Version, ArtifactKey>( new Comparator<Version>()
            {
                public int compare( Version o1, Version o2 )
                {
                    return -o1.compareTo( o2 );
                };
            } );

        for ( Map.Entry<ArtifactKey, File> entry : this.artifacts.entrySet() )
        {
            ArtifactKey key = entry.getKey();
            if ( type.equals( key.getType() ) && id.equals( key.getId() ) )
            {
                relevantArtifacts.put( Version.parseVersion( key.getVersion() ), key );
            }
        }

        if ( relevantArtifacts.isEmpty() )
        {
            return null;
        }

        if ( version == null || version == TychoConstants.HIGHEST_VERSION ) // == match is intentional
        {
            return relevantArtifacts.get( relevantArtifacts.firstKey() ); // latest version
        }

        Version parsedVersion = new Version( version );
        if ( VERSION_0_0_0.equals( parsedVersion ) )
        {
            return relevantArtifacts.get( relevantArtifacts.firstKey() ); // latest version
        }

        ArtifactKey perfectMatch = relevantArtifacts.get( parsedVersion );

        if ( perfectMatch != null )
        {
            return perfectMatch; // perfect match
        }

        boolean qualified = !"".equals( parsedVersion.getQualifier() );

        if ( qualified )
        {
            return null; // must be perfect match for qualified versions
        }

        for ( Map.Entry<Version, ArtifactKey> entry : relevantArtifacts.entrySet() )
        {
            if ( baseVersionEquals( parsedVersion, entry.getKey() ) )
            {
                return entry.getValue();
            }
        }

        return null;
    }

    private static boolean baseVersionEquals( Version v1, Version v2 )
    {
        return v1.getMajor() == v2.getMajor() && v1.getMinor() == v2.getMinor() && v1.getMicro() == v2.getMicro();
    }

    public void addMavenProject( ArtifactKey key, MavenProject project )
    {
        addArtifactFile( key, project.getBasedir() );
        projects.put( project.getBasedir(), project );
    }

    public MavenProject getMavenProject( File location )
    {
        return projects.get( location );
    }

    public File getArtifact( String type, String id, String version )
    {
        ArtifactKey key = getArtifactKey( type, id, version );
        if ( key == null )
        {
            return null;
        }
        return artifacts.get( key );
    }
}
