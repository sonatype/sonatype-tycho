package org.sonatype.tycho.p2.maven.repository;

import java.net.URI;

import org.sonatype.tycho.p2.repository.RepositoryReader;
import org.sonatype.tycho.p2.repository.TychoRepositoryIndex;

public class MavenMetadataRepository
    extends AbstractMavenMetadataRepository
{

    public MavenMetadataRepository( URI location, TychoRepositoryIndex projectIndex, RepositoryReader contentLocator )
    {
        super( location, projectIndex, contentLocator );
    }
}
