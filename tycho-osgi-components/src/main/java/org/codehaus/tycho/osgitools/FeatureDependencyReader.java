package org.codehaus.tycho.osgitools;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.tycho.FeatureResolutionState;
import org.codehaus.tycho.ProjectType;
import org.codehaus.tycho.maven.DependenciesReader;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.osgitools.features.FeatureDescription;

@Component( role = DependenciesReader.class, hint = ProjectType.ECLIPSE_FEATURE )
public class FeatureDependencyReader
    extends AbstractDependenciesReader
{

    public List<Dependency> getDependencies( MavenSession session, MavenProject project )
        throws MavenExecutionException
    {
        FeatureResolutionState state = getFeatureResolutionState( session, project );

        FeatureDescription description = state.getFeatureByLocation( project.getBasedir() );

        if ( description == null )
        {
            return NO_DEPENDENCIES;
        }

        ArrayList<Dependency> result = new ArrayList<Dependency>();

        Feature feature = description.getFeature();

        result.addAll( getPluginsDependencies( project, feature.getPlugins(), session ) );
        result.addAll( getFeaturesDependencies( project, feature.getIncludedFeatures(), session ) );

        return result;
    }
}
