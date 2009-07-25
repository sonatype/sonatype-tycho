package org.sonatype.tycho.p2.facade.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;

public class MavenTychoRepositoryIndex
    extends DefaultTychoRepositoryIndex
{

    public MavenTychoRepositoryIndex( RepositorySystem repositorySystem, ArtifactRepository repository )
        throws IOException,
            TransferFailedException,
            ResourceDoesNotExistException
    {
        File file = File.createTempFile( "tycho-index", ".properties" );

        try
        {
            repositorySystem.retrieve( repository, file, INDEX_RELPATH, null );

            this.gavs = read( new FileInputStream( file ) );
        }
        finally
        {
            file.delete();
        }
    }

}
