package org.codehaus.tycho.osgitools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.ProjectType;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.maven.DependenciesReader;
import org.codehaus.tycho.osgitools.DependencyComputer.DependencyEntry;
import org.codehaus.tycho.osgitools.project.EclipsePluginProjectImpl;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.BundleException;

@Component( role = DependenciesReader.class, hint = ProjectType.OSGI_BUNDLE )
public class OsgiBundleDependenciesReader
    extends AbstractDependenciesReader
{

    @Requirement
    private DependencyComputer dependencyComputer;

    public List<Dependency> getDependencies( MavenSession session, MavenProject project )
        throws MavenExecutionException
    {
        BundleResolutionState state = getBundleResolutionState( session, project );

        BundleDescription bundleDescription = state.getBundleByLocation( project.getBasedir() );
        if ( bundleDescription == null )
        {
            return NO_DEPENDENCIES;
        }

        try
        {
            state.assertResolved( bundleDescription );
        }
        catch ( BundleException e )
        {
            throw new MavenExecutionException( e.getMessage(), project.getFile() );
        }

        ArrayList<Dependency> result = new ArrayList<Dependency>();

        for ( DependencyEntry entry : dependencyComputer.computeDependencies( state, bundleDescription ) )
        {
            BundleDescription supplier = entry.desc;

            Dependency dependency = newBundleDependency( session, supplier );

            if ( dependency != null )
            {
                result.add( dependency );
            }
        }

        // TODO not the best place for this code...
        try
        {
            project.setContextValue( TychoConstants.CTX_ECLIPSE_PLUGIN_PROJECT, new EclipsePluginProjectImpl( project, bundleDescription ) );
        }
        catch ( IOException e )
        {
            throw new MavenExecutionException( "Could not read build.properties", e );
        }
        
        return result;
    }
}
