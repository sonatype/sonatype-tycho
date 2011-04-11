package org.eclipse.tycho.p2.maven.repository;

import java.net.URI;

import org.eclipse.tycho.p2.repository.RepositoryReader;
import org.eclipse.tycho.p2.repository.TychoRepositoryIndex;

public class MavenMetadataRepository
    extends AbstractMavenMetadataRepository
{

    public MavenMetadataRepository( URI location, TychoRepositoryIndex projectIndex, RepositoryReader contentLocator )
    {
        super( location, projectIndex, contentLocator );
    }
}
