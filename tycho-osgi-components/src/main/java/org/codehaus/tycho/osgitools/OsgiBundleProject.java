package org.codehaus.tycho.osgitools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.tycho.ArtifactDependencyVisitor;
import org.codehaus.tycho.ArtifactDependencyWalker;
import org.codehaus.tycho.BundleProject;
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
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.sonatype.tycho.ArtifactDescriptor;
import org.sonatype.tycho.ArtifactKey;
import org.sonatype.tycho.ReactorProject;
import org.sonatype.tycho.classpath.ClasspathEntry;

@Component( role = TychoProject.class, hint = org.sonatype.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN )
public class OsgiBundleProject
    extends AbstractTychoProject
    implements BundleProject
{

    private static final String CTX_ARTIFACT_KEY = TychoConstants.CTX_BASENAME + "/osgiBundle/artifactKey";

    @Requirement
    private BundleReader bundleReader;

    @Requirement
    private EquinoxResolver resolver;

    @Requirement
    private DependencyComputer dependencyComputer;

    public ArtifactDependencyWalker getDependencyWalker( MavenProject project, TargetEnvironment environment )
    {
        return getDependencyWalker( project );
    }

    public ArtifactDependencyWalker getDependencyWalker( MavenProject project )
    {
        final TargetPlatform platform = getTargetPlatform( project );

        final List<ClasspathEntry> cp = getClasspath( project );

        return new ArtifactDependencyWalker()
        {
            public void walk( ArtifactDependencyVisitor visitor )
            {
                for ( ClasspathEntry entry : cp )
                {
                    ArtifactDescriptor artifact = platform.getArtifact( entry.getArtifactKey() );

                    ArtifactKey key = artifact.getKey();
                    File location = artifact.getLocation();
                    ReactorProject project = artifact.getMavenProject();
                    String classifier = artifact.getClassifier();
                    Set<Object> installableUnits = artifact.getInstallableUnits();

                    PluginDescription plugin =
                        new DefaultPluginDescription( key, location, project, classifier, null, installableUnits );

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

    public ArtifactKey getArtifactKey( ReactorProject project )
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
        ArtifactKey key = readArtifactKey( project.getBasedir() );

        if ( key == null )
        {
            throw new IllegalArgumentException( "Missing bundle symbolic name or version for project "
                + project.toString() );
        }
        
        project.setContextValue( CTX_ARTIFACT_KEY, key );
    }

    public ArtifactKey readArtifactKey( File location )
    {
        Manifest mf = bundleReader.loadManifest( location );

        ManifestElement[] id = bundleReader.parseHeader( Constants.BUNDLE_SYMBOLICNAME, mf );
        ManifestElement[] version = bundleReader.parseHeader( Constants.BUNDLE_VERSION, mf );

        if ( id == null || version == null )
        {
            return null;
        }

        return new DefaultArtifactKey( org.sonatype.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN, id[0].getValue(),
                                version[0].getValue() );
    }

    public String getManifestValue( String key, MavenProject project )
    {
        Manifest mf = bundleReader.loadManifest( project.getBasedir() );
        return mf.getMainAttributes().getValue( key );
    }

    @Override
    public void resolve( MavenSession session, MavenProject project )
    {
        TargetPlatform platform = getTargetPlatform( project );

        State state = getResolverState( project, platform );

        if ( getLogger().isDebugEnabled() && DebugUtils.isDebugEnabled( session, project ) )
        {
            getLogger().debug( resolver.toDebugString( state ) );
        }

        BundleDescription bundleDescription = state.getBundleByLocation( project.getBasedir().getAbsolutePath() );

        List<ClasspathEntry> classpath = new ArrayList<ClasspathEntry>();

        // project itself
        ArtifactDescriptor artifact = platform.getArtifact( project.getBasedir() );
        ReactorProject projectProxy = DefaultReactorProject.adapt( project );
        List<File> projectClasspath = getProjectClasspath( artifact, projectProxy, null );
        classpath.add( new DefaultClasspathEntry( projectProxy, artifact.getKey(), projectClasspath, null ) );

        // build.properties/jars.extra.classpath
        addExtraClasspathEntries( classpath, projectProxy, platform );

        // dependencies
        for ( DependencyEntry entry : dependencyComputer.computeDependencies( state.getStateHelper(), bundleDescription ) )
        {
            File location = new File( entry.desc.getLocation() );
            ArtifactDescriptor otherArtifact = platform.getArtifact( location );
            ReactorProject otherProject = otherArtifact.getMavenProject();
            List<File> locations;
            if ( otherProject != null )
            {
                locations = getProjectClasspath( otherArtifact, otherProject, null );
            }
            else
            {
                locations = getBundleClasspath( otherArtifact, null );
            }

            classpath.add( new DefaultClasspathEntry( otherProject, otherArtifact.getKey(), locations, entry.rules ) );
        }
        project.setContextValue( TychoConstants.CTX_ECLIPSE_PLUGIN_CLASSPATH, classpath );

    }

    public State getResolverState( MavenProject project )
    {
        TargetPlatform platform = getTargetPlatform( project );

        return getResolverState( project, platform );
    }

    protected State getResolverState( MavenProject project, TargetPlatform platform )
    {
        try
        {
            return resolver.newResolvedState( project, platform );
        }
        catch ( BundleException e )
        {
            throw new RuntimeException( e );
        }
    }

//    public EclipsePluginProjectImpl getEclipsePluginProject( MavenProject otherProject )
//    {
//        
//    }
    
    public EclipsePluginProjectImpl getEclipsePluginProject( ReactorProject otherProject )
    {
        EclipsePluginProjectImpl pdeProject =
            (EclipsePluginProjectImpl) otherProject.getContextValue( TychoConstants.CTX_ECLIPSE_PLUGIN_PROJECT );
        if ( pdeProject == null )
        {
            try
            {
                pdeProject = new EclipsePluginProjectImpl( otherProject );
                otherProject.setContextValue( TychoConstants.CTX_ECLIPSE_PLUGIN_PROJECT, pdeProject );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
        return pdeProject;
    }

    public List<ClasspathEntry> getClasspath( MavenProject project )
    {
        List<ClasspathEntry> classpath =
            (List<ClasspathEntry>) project.getContextValue( TychoConstants.CTX_ECLIPSE_PLUGIN_CLASSPATH );
        if ( classpath == null )
        {
            throw new IllegalStateException();
        }
        return classpath;
    }

    private List<File> getProjectClasspath( ArtifactDescriptor bundle, ReactorProject otherProject, String nestedPath )
    {
        LinkedHashSet<File> classpath = new LinkedHashSet<File>();

        EclipsePluginProject pdeProject = getEclipsePluginProject( otherProject );

        Map<String, BuildOutputJar> outputJars = pdeProject.getOutputJarMap();
        if ( nestedPath == null )
        {
            for ( String cp : parseBundleClasspath( bundle ) )
            {
                if ( outputJars.containsKey( cp ) )
                {
                    // add output folder even if it does not exist (yet)
                    classpath.add( outputJars.get( cp ).getOutputDirectory() );
                }
                else
                {
                    File jar = new File( otherProject.getBasedir(), cp );
                    if ( jar.exists() )
                    {
                        classpath.add( jar );
                    }
                    else
                    {
                        getLogger().warn( "Missing classpath entry " + cp + " " + otherProject.toString() );
                    }
                }
            }
        }
        else
        /* nestedPath != null */
        {
            File jar = new File( otherProject.getBasedir(), nestedPath );

            // TODO ideally, we need to honour build.properties/bin.includes
            // but for now lets just assume nestedPath is included
            if ( jar.exists() || outputJars.containsKey( nestedPath ) )
            {
                classpath.add( jar );
            }
            else
            {
                getLogger().warn( "Missing classpath entry " + nestedPath + " " + otherProject.toString() );
            }
        }

        return new ArrayList<File>( classpath );
    }

    private void addExtraClasspathEntries( List<ClasspathEntry> classpath, ReactorProject project, TargetPlatform platform )
    {
        EclipsePluginProject pdeProject = getEclipsePluginProject( project );
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

                    if ( path != null && path.length() <= 0 )
                    {
                        path = null;
                    }
                }
                else
                {
                    // Log and
                    continue;
                }
                ArtifactDescriptor matchingBundle =
                    platform.getArtifact( org.sonatype.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN, bundleId, null );
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
                    classpath.add( new DefaultClasspathEntry( matchingBundle.getMavenProject(),
                                                              matchingBundle.getKey(), locations, null ) );
                }
                else
                {
                    getLogger().warn( "Missing extra classpath entry " + entry.trim() );
                }
            }
        }
    }

    private List<File> getBundleClasspath( ArtifactDescriptor bundle, String nestedPath )
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
                    entry = getNestedJarOrDir( bundle, cp );
                }

                if ( entry != null )
                {
                    classpath.add( entry );
                }
            }
        }

        return new ArrayList<File>( classpath );
    }

    private String[] parseBundleClasspath( ArtifactDescriptor bundle )
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

    private File getNestedJarOrDir( ArtifactDescriptor bundle, String cp )
    {
        return bundleReader.getEntry( bundle.getLocation(), cp );
    }

}
