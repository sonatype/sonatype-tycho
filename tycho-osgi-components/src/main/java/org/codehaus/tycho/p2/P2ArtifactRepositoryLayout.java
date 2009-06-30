package org.codehaus.tycho.p2;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.component.annotations.Component;

@Component( role = ArtifactRepositoryLayout.class, hint = P2ArtifactRepositoryLayout.ID )
public class P2ArtifactRepositoryLayout
    implements ArtifactRepositoryLayout
{
    public static final String ID = "p2";

    public String pathOf( Artifact artifact )
    {
        return ".p2-ignore";
    }

    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
    {
        return ".p2-ignore";
    }

    public String pathOfRemoteRepositoryMetadata( ArtifactMetadata metadata )
    {
        return ".p2-ignore";
    }

    public String getId()
    {
        return ID;
    }

}
