package org.sonatype.tycho.p2.facade.internal;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Implementation of RepositoryReader interface that delegates to Maven
 * repository subsystem to retrieve artifacts from remote repository.
 */
@Component( role = MavenRepositoryReader.class, instantiationStrategy = "per-lookup" )
public class MavenRepositoryReader
    implements RepositoryReader
{

    @Requirement
    private ArtifactFactory artifactFactory;

    @Requirement
    private ArtifactResolver artifactResolver;

    private List<ArtifactRepository> repositories;

    private ArtifactRepository localRepository;

    @SuppressWarnings( "deprecation" )
    public InputStream getContents( GAV gav, String classifier, String extension )
        throws IOException
    {
        Artifact a = artifactFactory.createArtifactWithClassifier(
            gav.getGroupId(),
            gav.getArtifactId(),
            gav.getVersion(),
            extension,
            classifier );

        try
        {
            artifactResolver.resolve( a, repositories, localRepository );
        }
        catch ( AbstractArtifactResolutionException e )
        {
            IOException exception = new IOException( "Could not resolve artifact" );
            exception.initCause( e );
            throw exception;
        }

        return new FileInputStream( a.getFile() );
    }

    public void setArtifactRepository( ArtifactRepository repository )
    {
        this.repositories = new ArrayList<ArtifactRepository>();
        this.repositories.add( repository );
    }

    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }
}
