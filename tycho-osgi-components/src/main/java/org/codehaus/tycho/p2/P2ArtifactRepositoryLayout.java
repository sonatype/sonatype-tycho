package org.codehaus.tycho.p2;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout2;
import org.codehaus.plexus.component.annotations.Component;

@Component( role = ArtifactRepositoryLayout.class, hint = P2ArtifactRepositoryLayout.ID )
public class P2ArtifactRepositoryLayout
    implements ArtifactRepositoryLayout, ArtifactRepositoryLayout2
{
    public static final String ID = "p2";
    
    private static final ArtifactRepositoryPolicy DISABLED_POLICY =
        new ArtifactRepositoryPolicy( false, ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER,
                                      ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE );

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

    public ArtifactRepository newMavenArtifactRepository( String id, String url, ArtifactRepositoryPolicy snapshots,
                                                          ArtifactRepositoryPolicy releases )
    {
        return new MavenArtifactRepository( id, url, this, DISABLED_POLICY, DISABLED_POLICY );
    }

}
