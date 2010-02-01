package org.codehaus.tycho;

import org.codehaus.tycho.model.PluginRef;

/**
 * Describes Eclipse plugin jar in context of aggregator project like eclipse-feature
 * or eclipse-application.
 * 
 * @author igor
 */
public interface PluginDescription
    extends ArtifactDescription
{

    PluginRef getPluginRef();

}
