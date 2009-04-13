package org.sonatype.tycho.p2.facade;

import java.io.File;
import java.util.List;

/**
 * This interface bridges   
 */
public interface P2Facade
{
    static final String ENCODING = "UTF8";

    static final String PROP_REPOSITORY_ID = "tycho.p2.repositoryId";

    ItemMetadata getBundleMetadata( File file );

    ItemMetadata getFeatureMetadata( File file );

    void publish( File location, List<File> bundles, List<File> features )
        throws Exception;

    void resolve( P2ResolutionRequest request, P2ResolutionResultCollector result )
        throws Exception;

    public void getRepositoryContent( String url, File destination );

    public void getRepositoryArtifacts( String url, File destination );

    public String getP2RuntimeLocation();
}
