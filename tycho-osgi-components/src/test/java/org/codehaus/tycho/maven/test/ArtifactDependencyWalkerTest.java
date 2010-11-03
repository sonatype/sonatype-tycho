package org.codehaus.tycho.maven.test;

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
import org.codehaus.tycho.ArtifactDependencyVisitor;
import org.codehaus.tycho.ArtifactDependencyWalker;
import org.codehaus.tycho.FeatureDescription;
import org.codehaus.tycho.PluginDescription;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.model.ProductConfiguration;
import org.codehaus.tycho.osgitools.AbstractArtifactDependencyWalker;
import org.codehaus.tycho.osgitools.DefaultReactorProject;
import org.codehaus.tycho.osgitools.targetplatform.LocalTargetPlatformResolver;
import org.codehaus.tycho.testing.AbstractTychoMojoTestCase;
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
