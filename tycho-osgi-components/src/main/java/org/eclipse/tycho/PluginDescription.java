package org.eclipse.tycho;

import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.model.PluginRef;

/**
 * Describes Eclipse plugin jar in context of aggregator project like eclipse-feature
 * or eclipse-application.
 * 
 * @author igor
 */
public interface PluginDescription
    extends ArtifactDescriptor
{

    PluginRef getPluginRef();

}
