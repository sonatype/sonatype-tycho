package org.sonatype.tycho.p2.facade;

import java.io.InputStream;

public interface P2ResolutionResultCollector
{

    void setItemContent( String path, InputStream content, String mimeType ) throws Exception;

    void createLinkItem( String path, String targetRepositoryId, String targetPath ) throws Exception;
}
