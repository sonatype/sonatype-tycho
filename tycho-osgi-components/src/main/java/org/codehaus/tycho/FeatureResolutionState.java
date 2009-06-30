package org.codehaus.tycho;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.osgitools.features.FeatureDescription;
import org.codehaus.tycho.osgitools.features.FeatureDescriptionImpl;
import org.osgi.framework.Version;

@Component( role = FeatureResolutionState.class )
public class FeatureResolutionState
    extends AbstractLogEnabled
{
    private Map<String, FeatureDescription> features = new LinkedHashMap<String, FeatureDescription>();

    private static final Version VERSION_0_0_0 = new Version( "0.0.0" );

    public FeatureResolutionState( Logger logger, MavenSession session, TargetPlatform platform )
    {
        enableLogging( logger );

        for ( File location : platform.getArtifactFiles( ProjectType.ECLIPSE_FEATURE ) )
        {
            try
            {
                Feature feature;
                if ( location.isDirectory() )
                {
                    feature = Feature.read( new File( location, Feature.FEATURE_XML ) );
                }
                else
                {
                    // eclipse does NOT support packed features
                    feature = Feature.readJar( location );
                }

                FeatureDescription description = new FeatureDescriptionImpl( feature, location );
                description.setMavenProject( MavenSessionUtils.getMavenProject( session, location ) );

                String key = description.getId() + "_" + description.getVersion().toString();

                features.put( key, description );
            }
            catch ( IOException e )
            {
                getLogger().warn( "Could not read feature " + location, e );
            }
            catch ( XmlPullParserException e )
            {
                getLogger().warn( "Could not parse feature " + location, e );
            }
        }
    }

    public FeatureDescription getFeatureByLocation( File location )
    {
        for ( FeatureDescription feature : features.values() )
        {
            if ( feature.getLocation().equals( location ) )
            {
                return feature;
            }
        }
        return null;
    }

    public FeatureDescription getFeature( String id, String version )
    {
        if ( id == null )
        {
            return null;
        }

        // features with matching id, sorted by version, highest version first
        SortedMap<Version, FeatureDescription> features =
            new TreeMap<Version, FeatureDescription>( new Comparator<Version>()
            {
                public int compare( Version o1, Version o2 )
                {
                    return -o1.compareTo( o2 );
                };
            } );

        for ( FeatureDescription desc : this.features.values() )
        {
            if ( id.equals( desc.getId() ) )
            {
                features.put( desc.getVersion(), desc );
            }
        }

        if ( features.isEmpty() )
        {
            return null;
        }

        if ( version == null || version == TychoConstants.HIGHEST_VERSION )
        {
            return features.get( features.firstKey() ); // latest version
        }

        Version parsedVersion = new Version( version );
        if ( VERSION_0_0_0.equals( parsedVersion ) )
        {
            return features.get( features.firstKey() ); // latest version
        }

        FeatureDescription perfectMatch = features.get( parsedVersion );

        if ( perfectMatch != null )
        {
            return perfectMatch; // perfect match
        }

        boolean qualified = !"".equals( parsedVersion.getQualifier() );

        if ( qualified )
        {
            return null; // must be perfect match for qualified versions
        }

        for ( FeatureDescription desc : features.values() )
        {
            if ( baseVersionEquals( parsedVersion, desc.getVersion() ) )
            {
                return desc;
            }
        }

        return null;
    }

    private static boolean baseVersionEquals( Version v1, Version v2 )
    {
        return v1.getMajor() == v2.getMajor() && v1.getMinor() == v2.getMinor() && v1.getMicro() == v2.getMicro();
    }

}
