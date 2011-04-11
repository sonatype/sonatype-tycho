package org.eclipse.tycho;

import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;

public interface FeatureDescription
    extends ArtifactDescriptor
{

    FeatureRef getFeatureRef();

    Feature getFeature();

}
