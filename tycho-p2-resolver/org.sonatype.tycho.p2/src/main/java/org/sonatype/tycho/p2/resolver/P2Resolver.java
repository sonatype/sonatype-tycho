package org.sonatype.tycho.p2.resolver;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.sonatype.tycho.p2.IArtifactFacade;
import org.sonatype.tycho.p2.IReactorArtifactFacade;
import org.sonatype.tycho.p2.repository.RepositoryReader;
import org.sonatype.tycho.p2.repository.TychoRepositoryIndex;

public interface P2Resolver
{
    /** @see org.sonatype.tycho.ArtifactKey */
    public static final String TYPE_ECLIPSE_PLUGIN = "eclipse-plugin";

    /** @see org.sonatype.tycho.ArtifactKey */
    public static final String TYPE_ECLIPSE_FEATURE = "eclipse-feature";

    /** @see org.sonatype.tycho.ArtifactKey */
    public static final String TYPE_ECLIPSE_TEST_PLUGIN = "eclipse-test-plugin";

    /** @see org.sonatype.tycho.ArtifactKey */
    public static final String TYPE_ECLIPSE_APPLICATION = "eclipse-application";

    /** @see org.sonatype.tycho.ArtifactKey */
    public static final String TYPE_ECLIPSE_UPDATE_SITE = "eclipse-update-site";

    /** @see org.sonatype.tycho.ArtifactKey */
    public static final String TYPE_ECLIPSE_REPOSITORY = "eclipse-repository";

    /**
     * Pseudo artifact type used to denote P2 installable unit dependencies
     */
    public static final String TYPE_INSTALLABLE_UNIT = "p2-installable-unit";

    public static final String ANY_QUALIFIER = "qualifier";

    public void addReactorArtifact( IReactorArtifactFacade project );

    public void addMavenArtifact( IArtifactFacade artifact );

    public void addP2Repository( URI location );

    public void addMavenRepository( URI location, TychoRepositoryIndex projectIndex, RepositoryReader contentLocator );

    public void setLocalRepositoryLocation( File location );

    public void setEnvironments( List<Map<String, String>> properties );

    public void addDependency( String type, String id, String version );

    public List<P2ResolutionResult> resolveProject( File location );

    public P2ResolutionResult collectProjectDependencies( File projectLocation );

    public void setLogger( P2Logger logger );

    public void setRepositoryCache( P2RepositoryCache repositoryCache );

    public void setCredentials( URI location, String username, String password );

    public void setOffline( boolean offline );

    /**
     * Releases all resources used by the resolver instance
     */
    public void stop();

    public P2ResolutionResult resolveMetadata( Map<String, String> properties );
}
