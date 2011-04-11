package org.eclipse.tycho.maven.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.tycho.ArtifactDependencyVisitor;
import org.eclipse.tycho.ArtifactDependencyWalker;
import org.eclipse.tycho.FeatureDescription;
import org.eclipse.tycho.PluginDescription;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.TargetPlatformResolver;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.osgitools.AbstractArtifactDependencyWalker;
import org.eclipse.tycho.osgitools.DefaultReactorProject;
import org.eclipse.tycho.osgitools.targetplatform.LocalTargetPlatformResolver;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.sonatype.aether.util.DefaultRepositorySystemSession;

public class ArtifactDependencyWalkerTest
    extends AbstractTychoMojoTestCase
{
    public void testProductDepdendencies()
        throws Exception
    {
        final ArrayList<PluginDescription> plugins = new ArrayList<PluginDescription>();
        final ArrayList<FeatureDescription> features = new ArrayList<FeatureDescription>();
        walkProduct( "src/test/resources/dependencywalker/plugin_based.product", plugins, features );

        assertEquals( 0, features.size() );

        assertEquals( 2, plugins.size() );
        assertEquals( "bundle01", plugins.get( 0 ).getKey().getId() );
        assertEquals( "0.0.1", plugins.get( 0 ).getKey().getVersion() );

        assertEquals( AbstractArtifactDependencyWalker.EQUINOX_LAUNCHER, plugins.get( 1 ).getKey().getId() );

        plugins.clear();
        features.clear();

        walkProduct( "src/test/resources/dependencywalker/feature_based.product", plugins, features );
        assertEquals( 1, features.size() );
        assertEquals( "feature01", features.get( 0 ).getKey().getId() );
        assertEquals( "1.0.0", features.get( 0 ).getKey().getVersion() );

        assertEquals( 2, plugins.size() );
        assertEquals( "bundle01", plugins.get( 0 ).getKey().getId() );
        assertEquals( "0.0.1", plugins.get( 0 ).getKey().getVersion() );

        assertEquals( AbstractArtifactDependencyWalker.EQUINOX_LAUNCHER, plugins.get( 1 ).getKey().getId() );
    }

    protected void walkProduct( String productFile, final ArrayList<PluginDescription> plugins,
                                final ArrayList<FeatureDescription> features )
        throws Exception, IOException, XmlPullParserException
    {
        TargetPlatform platform = getTargetPlatform();

        final ProductConfiguration product = ProductConfiguration.read( new File( productFile ) );

        ArtifactDependencyWalker walker = new AbstractArtifactDependencyWalker( platform )
        {
            public void walk( ArtifactDependencyVisitor visitor )
            {
                traverseProduct( product, visitor );
            }
        };

        walker.walk( new ArtifactDependencyVisitor()
        {
            @Override
            public void visitPlugin( PluginDescription plugin )
            {
                plugins.add( plugin );
            };

            @Override
            public boolean visitFeature( FeatureDescription feature )
            {
                features.add( feature );
                return true;
            };
        } );
    }

    protected TargetPlatform getTargetPlatform()
        throws Exception
    {
        LocalTargetPlatformResolver resolver =
            (LocalTargetPlatformResolver) lookup( TargetPlatformResolver.class, LocalTargetPlatformResolver.ROLE_HINT );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        DefaultRepositorySystemSession repositorySession = new DefaultRepositorySystemSession();
        MavenSession session = new MavenSession( getContainer(), repositorySession, request, result );
        session.setProjects( new ArrayList<MavenProject>() );

        MavenProject project = new MavenProject();

        resolver.setLocation( new File( "src/test/resources/targetplatforms/basic" ) );

        TargetPlatform platform =
            resolver.resolvePlatform( session, project, DefaultReactorProject.adapt( session ), null );
        return platform;
    }
}
