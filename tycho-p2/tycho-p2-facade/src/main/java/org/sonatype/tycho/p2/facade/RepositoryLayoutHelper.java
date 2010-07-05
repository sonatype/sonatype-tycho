package org.sonatype.tycho.p2.facade;

import java.util.Map;
import java.util.StringTokenizer;

import org.sonatype.tycho.p2.facade.internal.GAV;

public class RepositoryLayoutHelper
{
    public static final String PROP_GROUP_ID = "maven-groupId";

    public static final String PROP_ARTIFACT_ID = "maven-artifactId";

    public static final String PROP_VERSION = "maven-version";

    public static final String PROP_CLASSIFIER = "maven-classifier";
    
    public static final String PROP_PATH = "path";

    public static final String CLASSIFIER_P2_METADATA = "p2metadata";

    public static final String EXTENSION_P2_METADATA = "xml";

    public static final String CLASSIFIER_P2_ARTIFACTS = "p2artifacts";

    public static final String EXTENSION_P2_ARTIFACTS = "xml";

    public static final String DEFAULT_EXTERNSION = "jar";

    public static String getRelativePath( GAV gav, String classifier, String extension )
    {
        return getRelativePath( gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), classifier, extension );
    }

    public static String getRelativePath( String groupId, String artifactId, String version, String classifier,
        String extension )
    {
        StringBuilder sb = new StringBuilder();

        // basedir
        StringTokenizer st = new StringTokenizer( groupId, "." );
        while ( st.hasMoreTokens() )
        {
            sb.append( st.nextToken() ).append( '/' );
        }
        sb.append( artifactId ).append( '/' ).append( version ).append( '/' );

        // filename
        sb.append( artifactId ).append( '-' ).append( version );
        if ( classifier != null )
        {
            sb.append( '-' ).append( classifier );
        }
        sb.append( '.' ).append( extension != null ? extension : DEFAULT_EXTERNSION );

        return sb.toString();
    }

    public static GAV getGAV( Map properties )
    {
        String groupId = (String) properties.get( PROP_GROUP_ID );
        String artifactId = (String) properties.get( PROP_ARTIFACT_ID );
        String version = (String) properties.get( PROP_VERSION );

        return getGAV( groupId, artifactId, version );
    }

    public static GAV getGAV( String groupId, String artifactId, String version )
    {
        if ( groupId != null && artifactId != null && version != null )
        {
            return new GAV( groupId, artifactId, version );
        }

        return null;
    }

    public static GAV getP2Gav( String classifier, String id, String version )
    {
        // Should match AbstractDependenciesReader#newExternalDependency
        return new GAV( "p2." + classifier, id, version );
    }
}
