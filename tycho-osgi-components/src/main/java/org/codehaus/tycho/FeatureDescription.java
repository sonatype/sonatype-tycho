package org.codehaus.tycho;

import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.FeatureRef;

public interface FeatureDescription
    extends ArtifactDescription
{

    FeatureRef getFeatureRef();

    Feature getFeature();

}
