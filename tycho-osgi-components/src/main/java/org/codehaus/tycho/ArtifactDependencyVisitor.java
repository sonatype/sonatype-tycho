package org.codehaus.tycho;

import org.codehaus.tycho.model.FeatureRef;
import org.codehaus.tycho.model.PluginRef;


public abstract class ArtifactDependencyVisitor
{
    public boolean visitFeature( FeatureDescription feature )
    {
        return true; // keep visiting
    }

    public void visitPlugin( PluginDescription plugin )
    {

    }

    public void missingFeature( FeatureRef ref )
    {
        throw new RuntimeException( "Could not resolve feature " + ref.toString() );
    }

    public void missingPlugin( PluginRef ref )
    {
        throw new RuntimeException( "Could not resolve plugin " + ref.toString() );
    }
}
