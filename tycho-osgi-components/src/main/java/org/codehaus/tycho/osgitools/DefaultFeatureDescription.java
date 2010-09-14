package org.codehaus.tycho.osgitools;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.ArtifactKey;
import org.codehaus.tycho.FeatureDescription;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.FeatureRef;

public class DefaultFeatureDescription
    extends DefaultArtifactDescription
    implements FeatureDescription
{
    private Feature feature;

    private FeatureRef featureRef;

    public DefaultFeatureDescription( ArtifactKey key, File location, MavenProject project, Feature feature,
                                      FeatureRef featureRef )
    {
        super( key, location, project );
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
