package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.FeatureDescription;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.FeatureRef;
import org.sonatype.tycho.ArtifactKey;

public class DefaultFeatureDescription
    extends DefaultArtifactDescriptor
    implements FeatureDescription
{
    private Feature feature;

    private FeatureRef featureRef;

    public DefaultFeatureDescription( ArtifactKey key, File location, MavenProject project, Feature feature,
                                      FeatureRef featureRef, Set<Object> installableUnits )
    {
        super( key, location, project, installableUnits );
        this.feature = feature;
        this.featureRef = featureRef;
    }

    public FeatureRef getFeatureRef()
    {
        return featureRef;
    }

    public Feature getFeature()
    {
        return feature;
    }
}
