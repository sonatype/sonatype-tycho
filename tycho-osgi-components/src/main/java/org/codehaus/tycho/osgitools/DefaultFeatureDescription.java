package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.Set;

import org.codehaus.tycho.FeatureDescription;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.FeatureRef;
import org.sonatype.tycho.ArtifactKey;
import org.sonatype.tycho.ReactorProject;

public class DefaultFeatureDescription
    extends DefaultArtifactDescriptor
    implements FeatureDescription
{
    private Feature feature;

    private FeatureRef featureRef;

    public DefaultFeatureDescription( ArtifactKey key, File location, ReactorProject project, String classifier,
                                      Feature feature, FeatureRef featureRef, Set<Object> installableUnits )
    {
        super( key, location, project, classifier, installableUnits );
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
