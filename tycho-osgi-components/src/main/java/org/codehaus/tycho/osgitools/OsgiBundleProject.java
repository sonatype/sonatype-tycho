package org.codehaus.tycho.osgitools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.tycho.ArtifactDependencyVisitor;
import org.codehaus.tycho.ArtifactDependencyWalker;
import org.codehaus.tycho.ArtifactDescription;
import org.codehaus.tycho.ArtifactKey;
import org.codehaus.tycho.BundleProject;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.ClasspathEntry;
import org.codehaus.tycho.PluginDescription;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.ProductConfiguration;
import org.codehaus.tycho.model.UpdateSite;
import org.codehaus.tycho.osgitools.DependencyComputer.DependencyEntry;
import org.codehaus.tycho.osgitools.project.BuildOutputJar;
import org.codehaus.tycho.osgitools.project.EclipsePluginProject;
import org.codehaus.tycho.osgitools.project.EclipsePluginProjectImpl;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

@Component( role = TychoProject.class, hint = TychoProject.ECLIPSE_PLUGIN )
public class OsgiBundleProject
    extends AbstractTychoProject
    implements BundleProject
{

    private static final String CTX_ARTIFACT_KEY = TychoConstants.CTX_BASENAME + "/osgiBundle/artifactKey";

    @Requirement
    private BundleReader bundleReader;

    @Requirement
    private DependencyComputer dependencyComputer;

    public ArtifactDependencyWalker getDependencyWalker( MavenProject project, TargetEnvironment environment )
    {
        return getDependencyWalker( project );
    }

    public ArtifactDependencyWalker getDependencyWalker( MavenProject project )
    {
        final TargetPlatform platform = getTargetPlatform( project );
        final BundleResolutionState state = getBundleResolutionState( project );
        final BundleDescription bundleDescription = state.getBundleByLocation( project.getBasedir() );

        return new ArtifactDependencyWalker()
        {
            public void walk( ArtifactDependencyVisitor visitor )
            {
                for ( DependencyEntry entry : dependencyComputer.computeDependencies( state, bundleDescription ) )
                {
                    BundleDescription supplier = entry.desc;

                    String artifactId = supplier.getSymbolicName();
                    String version = supplier.getVersion().toString();
                    File location = new File( supplier.getLocation() );
                    MavenProject project = platform.getMavenProject( location );

                    String type = project != null ? project.getPackaging() : TychoProject.ECLIPSE_PLUGIN;
                    ArtifactKey key = new ArtifactKey( type, artifactId, version );

                    PluginDescription plugin = new DefaultPluginDescription( key, location, project, null );

                    visitor.visitPlugin( plugin );
                }
            }

            public void traverseFeature( File location, Feature feature, ArtifactDependencyVisitor visitor )
            {
            }

            public void traverseUpdateSite( UpdateSite site, ArtifactDependencyVisitor artifactDependencyVisitor )
            {
            }

            public void traverseProduct( ProductConfiguration productConfiguration, ArtifactDependencyVisitor visitor )
            {
            }
        };
    }

    @Override
    public void setTargetPlatform( MavenSession session, MavenProject project, TargetPlatform targetPlatform )
    {
        super.setTargetPlatform( session, project, targetPlatform );

        EquinoxBundleResolutionState resolver =
            EquinoxBundleResolutionState.newInstance( session.getContainer(), session, project );

        project.setContextValue( TychoConstants.CTX_BUNDLE_RESOLUTION_STATE, resolver );
    }

    protected EquinoxBundleResolutionState getBundleResolutionState( MavenProject project )
    {
        EquinoxBundleResolutionState resolver =
            (EquinoxBundleResolutionState) project.getContextValue( TychoConstants.CTX_BUNDLE_RESOLUTION_STATE );
        return resolver;
    }

    public ArtifactKey getArtifactKey( MavenProject project )
    {
        ArtifactKey key = (ArtifactKey) project.getContextValue( CTX_ARTIFACT_KEY );
        if ( key == null )
        {
            throw new IllegalStateException( "Project has not been setup yet " + project.toString() );
        }

        return key;
    }

    @Override
    public void setupProject( MavenSession session, MavenProject project )
    {
        Manifest mf = bundleReader.loadManifest( project.getBasedir() );

        ManifestElement[] id = bundleReader.parseHeader( Constants.BUNDLE_SYMBOLICNAME, mf );
        ManifestElement[] version = bundleReader.parseHeader( Constants.BUNDLE_VERSION, mf );

        if ( id == null || version == null )
        {
            throw new IllegalArgumentException( "Missing bundle symbolic name or version for project "
                + project.toString() );
        }

        ArtifactKey key = new ArtifactKey( TychoProject.ECLIPSE_PLUGIN, id[0].getValue(), version[0].getValue() );
        project.setContextValue( CTX_ARTIFACT_KEY, key );
    }

    @Override
    public void resolve( MavenProject project )
    {
        EquinoxBundleResolutionState state = getBundleResolutionState( project );
        BundleDescription bundle = state.resolve( project );
        try
        {
            state.assertResolved( bundle );
            project.setContextValue( TychoConstants.CTX_ECLIPSE_PLUGIN_PROJECT, new EclipsePluginProjectImpl( project,
                                                                                                              bundle ) );
        }
        catch ( BundleException e )
        {
            throw new RuntimeException( e );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    public List<ClasspathEntry> getClasspath( MavenProject project )
    {
        TargetPlatform platform = getTargetPlatform( project );

        BundleResolutionState state = getBundleResolutionState( project );
        BundleDescription bundleDescription = state.getBundleByLocation( project.getBasedir() );

        List<ClasspathEntry> cp = new ArrayList<ClasspathEntry>();

        // project itself
        ArtifactDescription artifact = platform.getArtifact( project.getBasedir() );
        cp.add( new DefaultClasspathEntry( getProjectClasspath( artifact, project, null ), null ) );

        // build.properties/jars.extra.classpath
        addExtraClasspathEntries( cp, project, platform );

        // dependencies
        for ( DependencyEntry entry : dependencyComputer.computeDependencies( state, bundleDescription ) )
        {
            File location = new File( entry.desc.getLocation() );
            ArtifactDescription otherArtifact = platform.getArtifact( location );
            MavenProject otherProject = platform.getMavenProject( location );
            List<File> locations;
            if ( otherProject != null )
            {
                locations = getProjectClasspath( otherArtifact, otherProject, null );
            }
            else
            {
                locations = getBundleClasspath( otherArtifact, null );
            }

            cp.add( new DefaultClasspathEntry( locations, entry.rules ) );
        }

        return cp;
    }

    private List<File> getProjectClasspath( ArtifactDescription bundle, MavenProject project, String nestedPath )
    {
        LinkedHashSet<File> classpath = new LinkedHashSet<File>();

        EclipsePluginProject pdeProject =
            (EclipsePluginProject) project.getContextValue( TychoConstants.CTX_ECLIPSE_PLUGIN_PROJECT );

        Map<String, BuildOutputJar> outputJars = pdeProject.getOutputJarMap();
        for ( String cp : parseBundleClasspath( bundle ) )
        {
            if ( nestedPath == null || nestedPath.equals( cp ) )
            {
                if ( outputJars.containsKey( cp ) )
                {
                    // add output folder even if it does not exist (yet)
                    classpath.add( outputJars.get( cp ).getOutputDirectory() );
                }
                else
                {
                    File jar = new File( project.getBasedir(), cp );
                    if ( jar.exists() )
                    {
                        classpath.add( jar );
                    }
                }
            }
        }

        if ( nestedPath != null && classpath.isEmpty() )
        {
            // TODO ideally, we need to honour build.properties/bin.includes
            // but for now lets just assume nestedPath is included
            
            File jar = new File( project.getBasedir(), nestedPath );
            if ( jar.exists() )
            {
                classpath.add( jar );
            }
        }

        return new ArrayList<File>( classpath );
    }

    private void addExtraClasspathEntries( List<ClasspathEntry> classpath, MavenProject project, TargetPlatform platform )
    {
        EclipsePluginProject pdeProject =
            (EclipsePluginProject) project.getContextValue( TychoConstants.CTX_ECLIPSE_PLUGIN_PROJECT );
        Collection<BuildOutputJar> outputJars = pdeProject.getOutputJarMap().values();
        for ( BuildOutputJar buildOutputJar : outputJars )
        {
            List<String> entries = buildOutputJar.getExtraClasspathEntries();
            for ( String entry : entries )
            {
                Pattern platformURL = Pattern.compile( "platform:/(plugin|fragment)/([^/]*)/*(.*)" );
                Matcher m = platformURL.matcher( entry.trim() );
                String bundleId = null;
                String path = null;
                if ( m.matches() )
                {
                    bundleId = m.group( 2 ).trim();
                    path = m.group( 3 ).trim();

                    if ( path.isEmpty() )
                    {
                        path = null;
                    }
                }
                else
                {
                    // Log and
                    continue;
                }
                ArtifactDescription matchingBundle = platform.getArtifact( ECLIPSE_PLUGIN, bundleId, null );
                if ( matchingBundle != null )
                {
                    List<File> locations;
                    if ( matchingBundle.getMavenProject() != null )
                    {
                        locations = getProjectClasspath( matchingBundle, matchingBundle.getMavenProject(), path );
                    }
                    else
                    {
                        locations = getBundleClasspath( matchingBundle, path );
                    }
                    classpath.add( new DefaultClasspathEntry( locations, null ) );
                }
                else
                {
                    getLogger().warn( "Missing extra classpath entry " + entry.trim() );
                }
            }
        }
    }

    private List<File> getBundleClasspath( ArtifactDescription bundle, String nestedPath )
    {
        LinkedHashSet<File> classpath = new LinkedHashSet<File>();

        for ( String cp : parseBundleClasspath( bundle ) )
        {
            File entry;
            if ( nestedPath == null || nestedPath.equals( cp ) )
            {
                if ( ".".equals( cp ) )
                {
                    entry = bundle.getLocation();
                }
                else
                {
                    entry = getNestedJar( bundle, cp );
                }
    
                if ( entry != null )
                {
                    classpath.add( entry );
                }
            }
        }

        return new ArrayList<File>( classpath );
    }

    private String[] parseBundleClasspath( ArtifactDescription bundle )
    {
        String[] result = new String[] { "." };
        Manifest mf = bundleReader.loadManifest( bundle.getLocation() );
        ManifestElement[] classpathEntries = bundleReader.parseHeader( Constants.BUNDLE_CLASSPATH, mf );
        if ( classpathEntries != null )
        {
            result = new String[classpathEntries.length];
            for ( int i = 0; i < classpathEntries.length; i++ )
            {
                result[i] = classpathEntries[i].getValue();
            }
        }
        return result;
    }

    private File getNestedJar( ArtifactDescription bundle, String cp )
    {
        return bundleReader.getEntry( bundle.getLocation(), cp );
    }

}
