package org.codehaus.tycho;

import java.io.File;
import java.util.List;

import org.apache.maven.project.MavenProject;

public interface TargetPlatform
{
    /**
     * Conventional qualifier used to denote "ANY QUALIFIER" in feature.xml and .product files. See TYCHO-383.
     */
    public static final String ANY_QUALIFIER = "qualifier";

    /**
     * Returns all artifacts of the given type.
     */
    public List<ArtifactDescription> getArtifacts( String type );

    /**
     * Returns artifact of the given type and id and best matching version or null if no such artifact is found.
     * <p>
     * This method uses the following version selection rules
     * <ul>
     * <li>0.0.0 or null matches the latest version
     * <li>1.2.3, i.e. withput a qualifier, is equivalent to [1.2.3,1.2.4) and matches 1.2.3 with the latest qualifier.
     * <li>1.2.3.qualifier, i.e. literal "qualifier", is equivalent to [1.2.3,1.2.4) and matches 1.2.3 with the latest
     * qualifier.
     * <li>all other versions match artifact with that exact version, 1.2.3.foo is equivalent to [1.2.3.foo]
     * </ul>
     */
    public ArtifactDescription getArtifact( String type, String id, String version );

    public MavenProject getMavenProject( File location );

    public ArtifactDescription getArtifact( File location );
}
