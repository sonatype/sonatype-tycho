package org.codehaus.tycho.osgitools.targetplatform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.ArtifactDescription;
import org.codehaus.tycho.ArtifactKey;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.osgitools.DefaultArtifactDescription;
import org.osgi.framework.Version;

public class DefaultTargetPlatform
    implements TargetPlatform
{
    private static final Version VERSION_0_0_0 = new Version( "0.0.0" );

    private static final WeakHashMap<ArtifactKey, ArtifactKey> KEY_CACHE = new WeakHashMap<ArtifactKey, ArtifactKey>();

    private static final WeakHashMap<ArtifactKey, ArtifactDescription> ARTIFACT_CACHE =
        new WeakHashMap<ArtifactKey, ArtifactDescription>();

    protected Map<ArtifactKey, ArtifactDescription> artifacts = new LinkedHashMap<ArtifactKey, ArtifactDescription>();

    protected Map<File, ArtifactDescription> locations = new LinkedHashMap<File, ArtifactDescription>();

    public List<ArtifactDescription> getArtifacts( String type )
    {
        ArrayList<ArtifactDescription> result = new ArrayList<ArtifactDescription>();
        for ( Map.Entry<ArtifactKey, ArtifactDescription> entry : artifacts.entrySet() )
        {
            if ( type.equals( entry.getKey().getType() ) )
            {
                result.add( entry.getValue() );
            }
        }

        return result;
    }

    public void addArtifactFile( ArtifactKey key, File location )
    {
        addArtifact( new DefaultArtifactDescription( key, location, null ) );
    }

    public void addArtifact( ArtifactDescription artifact )
    {
        ArtifactKey key = normalizeKey( artifact.getKey() );

        ArtifactKey cachedKey = KEY_CACHE.get( key );
        if ( cachedKey != null )
        {
            key = cachedKey;
        }
        else
        {
            KEY_CACHE.put( key, key );
        }

        artifact = normalizeArtifact( artifact );

        ArtifactDescription cachedArtifact = ARTIFACT_CACHE.get( key );
        if ( cachedArtifact != null && eq( cachedArtifact.getLocation(), artifact.getLocation() )
            && eq( cachedArtifact.getMavenProject(), artifact.getMavenProject() ) )
        {
            artifact = cachedArtifact;
        }
        else
        {
            ARTIFACT_CACHE.put( key, artifact );
        }

        artifacts.put( key, artifact );
        locations.put( artifact.getLocation(), artifact );
    }

    private ArtifactDescription normalizeArtifact( ArtifactDescription artifact )
    {
        try
        {
            File location = artifact.getLocation().getCanonicalFile();
            if ( !location.equals( artifact.getLocation() ) )
            {
                return new DefaultArtifactDescription( artifact.getKey(), location, artifact.getMavenProject() );
            }
            return artifact;
        }
        catch ( IOException e )
        {
            // not sure what good this will do to the caller
            return artifact;
        }
    }

    protected ArtifactKey normalizeKey( ArtifactKey key )
    {
        if ( TychoProject.ECLIPSE_TEST_PLUGIN.equals( key.getType() ) )
        {
            // normalize eclipse-test-plugin... after all, a bundle is a bundle.
            key = new ArtifactKey( TychoProject.ECLIPSE_PLUGIN, key.getId(), key.getVersion() );
        }
        return key;
    }

    private static <T> boolean eq( T a, T b )
    {
        return a != null ? a.equals( b ) : b == null;
    }

    /**
     * @deprecated get rid of me, I am not used for anything
     */
    public void addSite( File location )
    {
    }

    public void dump()
    {
        for ( Map.Entry<ArtifactKey, ArtifactDescription> entry : artifacts.entrySet() )
        {
            System.out.println( entry.getKey() + "\t" + entry.getValue() );
        }
    }

    public boolean isEmpty()
    {
        return artifacts.isEmpty();
    }

    public ArtifactDescription getArtifact( String type, String id, String version )
    {
        if ( type == null || id == null )
        {
            // TODO should we throw something instead?
            return null;
        }

        // features with matching id, sorted by version, highest version first
        SortedMap<Version, ArtifactDescription> relevantArtifacts =
            new TreeMap<Version, ArtifactDescription>( new Comparator<Version>()
            {
                public int compare( Version o1, Version o2 )
                {
                    return -o1.compareTo( o2 );
                };
            } );

        for ( Map.Entry<ArtifactKey, ArtifactDescription> entry : this.artifacts.entrySet() )
        {
            ArtifactKey key = entry.getKey();
            if ( type.equals( key.getType() ) && id.equals( key.getId() ) )
            {
                relevantArtifacts.put( Version.parseVersion( key.getVersion() ), entry.getValue() );
            }
        }

        if ( relevantArtifacts.isEmpty() )
        {
            return null;
        }

        if ( version == null )
        {
            return relevantArtifacts.get( relevantArtifacts.firstKey() ); // latest version
        }

        Version parsedVersion = new Version( version );
        if ( VERSION_0_0_0.equals( parsedVersion ) )
        {
            return relevantArtifacts.get( relevantArtifacts.firstKey() ); // latest version
        }

        String qualifier = parsedVersion.getQualifier();

        if ( qualifier == null || "".equals( qualifier ) || ANY_QUALIFIER.equals( qualifier ) )
        {
            // latest qualifier
            for ( Map.Entry<Version, ArtifactDescription> entry : relevantArtifacts.entrySet() )
            {
                if ( baseVersionEquals( parsedVersion, entry.getKey() ) )
                {
                    return entry.getValue();
                }
            }
        }

        // perfect match or nothing
        return relevantArtifacts.get( parsedVersion );
    }

    private static boolean baseVersionEquals( Version v1, Version v2 )
    {
        return v1.getMajor() == v2.getMajor() && v1.getMinor() == v2.getMinor() && v1.getMicro() == v2.getMicro();
    }

    public void addMavenProject( ArtifactKey key, MavenProject project )
    {
        DefaultArtifactDescription artifact = new DefaultArtifactDescription( key, project.getBasedir(), project );
        addArtifact( artifact );
    }

    public MavenProject getMavenProject( File location )
    {
        ArtifactDescription artifact = getArtifact( location );
        return artifact != null ? artifact.getMavenProject() : null;
    }

    public ArtifactDescription getArtifact( File location )
    {
        try
        {
            location = location.getCanonicalFile();
            return locations.get( location );
        }
        catch ( IOException e )
        {
            return null;
        }
    }

    public ArtifactDescription getArtifact( ArtifactKey key )
    {
        return artifacts.get( normalizeKey( key ) );
    }

    public void removeAll( String type, String id )
    {
        Iterator<Entry<ArtifactKey, ArtifactDescription>> iter = artifacts.entrySet().iterator();
        while ( iter.hasNext() )
        {
            ArtifactKey key = iter.next().getKey();
            if ( key.getType().equals( type ) && key.getId().equals( id ) )
            {
                iter.remove();
            }
        }
    }

    public void toDebugString( StringBuilder sb, String linePrefix )
    {
        for ( ArtifactDescription artifact : artifacts.values() )
        {
            sb.append( linePrefix );
            sb.append( artifact.getKey().toString() );
            sb.append( ": " );
            MavenProject project = artifact.getMavenProject();
            if ( project != null )
            {
                sb.append( project.toString() );
            }
            else
            {
                sb.append( artifact.getLocation() );
            }
            sb.append( "\n" );
        }
    }
}
