package org.codehaus.tycho.maven;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.DefaultPluginManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.tycho.osgitools.OsgiState;

public class EclipsePluginManager extends DefaultPluginManager {

	private OsgiState osgiState;

	@Override
	protected void resolveTransitiveDependencies(MavenSession context,
			ArtifactResolver artifactResolver, String scope,
			ArtifactFactory artifactFactory, MavenProject project,
			boolean isAggregator) throws ArtifactResolutionException,
			ArtifactNotFoundException, InvalidDependencyVersionException 
	{
		File mf = new File(project.getFile().getParentFile(), "META-INF/MANIFEST.MF");
		// TODO detect "our" projects
		if (mf.canRead()) {
			/*
			 * at this point we already injected resolved OSGi dependencies
			 * into project.model.dependencies, so we only need to resolve individual artifacts.
			 */
			
			// <copy-and-paste>

			// TODO: such a call in MavenMetadataSource too - packaging not really the intention of type
	        Artifact artifact = artifactFactory.createBuildArtifact( project.getGroupId(),
	                                                                 project.getArtifactId(),
	                                                                 project.getVersion(),
	                                                                 project.getPackaging() );

	        // TODO: we don't need to resolve over and over again, as long as we are sure that the parameters are the same
	        // check this with yourkit as a hot spot.
	        // Don't recreate if already created - for effeciency, and because clover plugin adds to it
	        if ( project.getDependencyArtifacts() == null )
	        {
	            // NOTE: Don't worry about covering this case with the error-reporter bindings...it's already handled by the project error reporter.
	            project.setDependencyArtifacts( project.createArtifacts( artifactFactory, null, null ) );
	        }

	        // </copy-and-paste>

	        Set artifacts = new LinkedHashSet(project.createArtifacts( artifactFactory, null, null ));
	        for (Iterator it = artifacts.iterator(); it.hasNext(); ) {
	        	Artifact a = (Artifact) it.next();
				artifactResolver.resolve(a, project.getRemoteArtifactRepositories(), context.getLocalRepository());
	        }
			project.setArtifacts(artifacts);
		} else {
			super.resolveTransitiveDependencies(context, artifactResolver, scope,
					artifactFactory, project, isAggregator);
		}
	}
}
