package org.codehaus.tycho.osgitools;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reactor.MavenExecutionException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.ProjectType;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.TychoSession;
import org.codehaus.tycho.maven.DependenciesReader;
import org.codehaus.tycho.osgitools.DependencyComputer.DependencyEntry;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.BundleException;

@Component( role = DependenciesReader.class, hint = ProjectType.OSGI_BUNDLE )
public class OsgiBundleDependenciesReader
    extends AbstractDependenciesReader
{

    @Requirement
    private DependencyComputer dependencyComputer;

    public List<Dependency> getDependencies( MavenProject project, TychoSession session )
        throws MavenExecutionException
    {
        BundleResolutionState state = session.getBundleResolutionState( project );

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

            MavenProject otherProject = session.getMavenProject( supplier.getLocation() );

            Dependency dependency;
            if ( otherProject != null )
            {
                dependency = newProjectDependency( otherProject );
            }
            else
            {
                String artifactId = supplier.getSymbolicName();
                String version = supplier.getVersion().toString();

                dependency = newExternalDependency( supplier.getLocation(), artifactId, version );
            }

            if ( dependency != null )
            {
                result.add( dependency );
            }
        }

        return result;
    }
}
