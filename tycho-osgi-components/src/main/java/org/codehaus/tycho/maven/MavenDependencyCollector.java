package org.codehaus.tycho.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.tycho.ArtifactDependencyVisitor;
import org.codehaus.tycho.FeatureDescription;
import org.codehaus.tycho.PluginDescription;
import org.codehaus.tycho.model.PluginRef;
import org.sonatype.tycho.ArtifactDescriptor;
import org.sonatype.tycho.ArtifactKey;
import org.sonatype.tycho.ReactorProject;

/**
 * Generates list of Maven dependencies from project OSGi/Eclipse dependencies
 */
public class MavenDependencyCollector
    extends ArtifactDependencyVisitor
{
    private final MavenProject project;

    private final Logger logger;

    public MavenDependencyCollector( MavenProject project, Logger logger )
    {
        this.project = project;
        this.logger = logger;
    }

    @Override
    public boolean visitFeature( FeatureDescription feature )
    {
        addDependency( feature );
        return true; // keep visiting
    }

    @Override
    public void visitPlugin( PluginDescription plugin )
    {
        addDependency( plugin );
    }

    @Override
    public void missingPlugin( PluginRef ref, List<ArtifactDescriptor> walkback )
    {
        // we don't handle multi-environment target platforms well, so
        // missing environment specific bundles should not fail the build

        if ( ref.getOs() == null && ref.getWs() == null && ref.getArch() == null )
        {
            super.missingPlugin( ref, walkback );
        }
        else
        {
            logger.warn( "Missing environment specific bundle " + ref.toString() );
        }
    }

    protected void addDependency( ArtifactDescriptor artifact )
    {
        Dependency dependency = null;
        if ( artifact.getMavenProject() != null )
        {
            if ( !artifact.getMavenProject().sameProject( project ) )
            {
                dependency = newProjectDependency( artifact.getMavenProject() );
            }
        }
        else
        {
            ArtifactKey key = artifact.getKey();
            dependency = newExternalDependency( artifact.getLocation(), key.getType(), key.getId(), key.getVersion() );
        }
        // can be null for directory-based features/bundles
        if ( dependency != null )
        {
            project.getModel().addDependency( dependency );
        }
    }

    public static final String P2_CLASSIFIER_BUNDLE = "osgi.bundle";

    public static final String P2_CLASSIFIER_FEATURE = "org.eclipse.update.feature";

    protected static final List<Dependency> NO_DEPENDENCIES = new ArrayList<Dependency>();

    protected Dependency newExternalDependency( File location, String p2Classifier, String artifactId, String version )
    {
        if ( !location.exists() || !location.isFile() || !location.canRead() )
        {
            logger.warn( "Dependency at location " + location
                + " can not be represented in Maven model and will not be visible to non-OSGi aware Maven plugins" );
            return null;
        }

        Dependency dependency = new Dependency();
        dependency.setArtifactId( artifactId );
        dependency.setGroupId( "p2." + p2Classifier ); // See also RepositoryLayoutHelper#getP2Gav
        dependency.setVersion( version );
        dependency.setScope( Artifact.SCOPE_SYSTEM );
        dependency.setSystemPath( location.getAbsolutePath() );
        return dependency;
    }

    protected Dependency newProjectDependency( ReactorProject dependentMavenProjectProxy )
    {
        if ( dependentMavenProjectProxy == null )
        {
            return null;
        }

        Dependency dependency = new Dependency();
        dependency.setArtifactId( dependentMavenProjectProxy.getArtifactId() );
        dependency.setGroupId( dependentMavenProjectProxy.getGroupId() );
        dependency.setVersion( dependentMavenProjectProxy.getVersion() );
        dependency.setType( dependentMavenProjectProxy.getPackaging() );
        dependency.setScope( Artifact.SCOPE_PROVIDED );
        return dependency;
    }
}
