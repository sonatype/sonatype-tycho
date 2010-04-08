package org.codehaus.tycho.osgitools.targetplatform;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

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
        ArtifactKey key = artifact.getKey();
        if ( TychoProject.ECLIPSE_TEST_PLUGIN.equals( key.getType() ) )
        {
            // normalize eclipse-test-plugin... after all, a bundle is a bundle.
            key = new ArtifactKey( TychoProject.ECLIPSE_PLUGIN, key.getId(), key.getVersion() );
        }
        artifacts.put( key, artifact );
        locations.put( artifact.getLocation(), artifact );
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
        return artifact != null? artifact.getMavenProject(): null;
    }

    public ArtifactDescription getArtifact( File location )
    {
        return locations.get( location );
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
}
