package org.codehaus.tycho;

import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.FeatureRef;
import org.sonatype.tycho.ArtifactDescriptor;

public interface FeatureDescription
    extends ArtifactDescriptor
{

    FeatureRef getFeatureRef();

    Feature getFeature();

}
