package org.sonatype.tycho.p2.impl.test;

import java.io.File;
import java.util.Set;

import org.sonatype.tycho.p2.IArtifactFacade;
import org.sonatype.tycho.p2.IReactorArtifactFacade;

public class ArtifactMock
    implements IArtifactFacade, IReactorArtifactFacade
{
    private File location;

    private String groupId;

    private String artifactId;

    private String version;

    private String packagingType;

    private final String classifier;

    private Set<Object> dependencyMetadata;

    public ArtifactMock( File location, String groupId, String artifactId, String version, String packagingType,
                         String classifier )
    {
        this.location = location;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packagingType = packagingType;
        this.classifier = classifier;
    }

    public ArtifactMock( File location, String groupId, String artifactId, String version, String packagingType )
    {
        this( location, groupId, artifactId, version, packagingType, null );
    }

    public File getLocation()
    {
        return location;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public String getPackagingType()
    {
        return packagingType;
    }

    public String getClassidier()
    {
        return classifier;
    }

    public Set<Object> getDependencyMetadata()
    {
        return dependencyMetadata;
    }

    public void setDependencyMetadata( Set<Object> dependencyMetadata )
    {
        this.dependencyMetadata = dependencyMetadata;
    }
}
