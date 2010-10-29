package org.codehaus.tycho;

import java.util.List;

import org.codehaus.tycho.model.FeatureRef;
import org.codehaus.tycho.model.PluginRef;
import org.sonatype.tycho.ArtifactDescriptor;

public abstract class ArtifactDependencyVisitor
{
    public boolean visitFeature( FeatureDescription feature )
    {
        return true; // keep visiting
    }

    public void visitPlugin( PluginDescription plugin )
    {

    }

    public void missingFeature( FeatureRef ref, List<ArtifactDescriptor> walkback )
    {
        throw newRuntimeException( "Could not resolve feature", ref.toString(), walkback );
    }

    public void missingPlugin( PluginRef ref, List<ArtifactDescriptor> walkback )
    {
        throw newRuntimeException( "Could not resolve plugin", ref.toString(), walkback );
    }

    protected RuntimeException newRuntimeException( String message, String missing, List<ArtifactDescriptor> walkback )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( message ).append( " " ).append( missing ).append( "; Path to dependency:\n" );
        for ( ArtifactDescriptor artifact : walkback )
        {
            sb.append( "  " ).append( artifact.toString() ).append( "\n" );
        }
        return new RuntimeException( sb.toString() );
    }
}
