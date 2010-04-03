package org.sonatype.tycho.p2.facade.internal;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Properties;

import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TychoProject;

public interface P2Resolver
{
    public static final String TYPE_ECLIPSE_PLUGIN = TychoProject.ECLIPSE_PLUGIN;

    public static final String TYPE_ECLIPSE_FEATURE = TychoProject.ECLIPSE_FEATURE;

    public static final String TYPE_ECLIPSE_TEST_PLUGIN = TychoProject.ECLIPSE_TEST_PLUGIN;

    public static final String TYPE_ECLIPSE_APPLICATION = TychoProject.ECLIPSE_APPLICATION;

    public static final String TYPE_ECLIPSE_UPDATE_SITE = TychoProject.ECLIPSE_UPDATE_SITE;

    public static final String ANY_QUALIFIER = TargetPlatform.ANY_QUALIFIER;

    /**
     * Pseudo artifact type used to denote P2 installable unit dependencies
     */
    public static final String TYPE_INSTALLABLE_UNIT = "p2-installable-unit";

    public void addMavenProject( File location, String type, String groupId, String artifactId, String version );

    public void addMavenArtifact( File location, String type, String groupId, String artifactId, String version );

    public void addP2Repository( URI location );

    public void addMavenRepository( URI location, TychoRepositoryIndex projectIndex, RepositoryReader contentLocator );

    public void setLocalRepositoryLocation( File location );

    public void setEnvironments( List<Properties> properties );

    public List<P2ResolutionResult> resolveProject( File location );

    public void addDependency( String type, String id, String version );

    public void setLogger( P2Logger logger );

    void setRepositoryCache( P2RepositoryCache repositoryCache );

    void setCredentials( URI location, String username, String password );
}
