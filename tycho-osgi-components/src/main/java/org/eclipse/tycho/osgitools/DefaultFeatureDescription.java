package org.eclipse.tycho.osgitools;

import java.io.File;
import java.util.Set;

import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.FeatureDescription;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;

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
