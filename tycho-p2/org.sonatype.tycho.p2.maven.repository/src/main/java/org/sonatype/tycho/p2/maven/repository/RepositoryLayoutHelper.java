package org.sonatype.tycho.p2.maven.repository;

import java.util.Map;

public class RepositoryLayoutHelper
{
    public static final String PROP_GROUP_ID = "maven-groupId";

    public static final String PROP_ARTIFACT_ID = "maven-artifactId";

    public static final String PROP_VERSION = "maven-version";

    public static final String XXX_CLASSIFIER = "p2meta";

    public static final String XXX_EXTENSION = "xml";

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
        sb.append( groupId ).append( '/' ).append( artifactId ).append( '/' ).append( version ).append( '/' );

        // filename
        sb.append( artifactId ).append( '-' ).append( version );
        if ( classifier != null )
        {
            sb.append( '-' ).append( classifier );
        }
        sb.append( '.' ).append( extension != null ? extension : DEFAULT_EXTERNSION );

        return sb.toString();
    }

    public static String getGAV( String groupId, String artifactId, String version )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( groupId ).append( ':' ).append( artifactId ).append( ':' ).append( version );

        return sb.toString();
    }

    public static GAV getGAV( Map properties )
    {
        String groupId = (String) properties.get( PROP_GROUP_ID );
        String artifactId = (String) properties.get( PROP_ARTIFACT_ID );
        String version = (String) properties.get( PROP_VERSION );

        if ( groupId != null && artifactId != null && version != null )
        {
            return new GAV( groupId, artifactId, version );
        }

        return null;
    }

}
