package org.codehaus.tycho.osgitest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.Arg;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.tycho.ArtifactDescription;
import org.codehaus.tycho.BundleProject;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.maven.TychoMavenLifecycleParticipant;
import org.codehaus.tycho.utils.PlatformPropertiesUtils;
import org.osgi.framework.Version;

/**
 * @phase integration-test
 * @goal test
 * @requiresProject true
 * @requiresDependencyResolution runtime
 */
public class TestMojo
    extends AbstractMojo
{

    private static final Version VERSION_3_3_0 = Version.parseVersion( "3.3.0" );

    private static final String EQUINOX_LAUNCHER = "org.eclipse.equinox.launcher";

    /**
     * @parameter default-value="${project.build.directory}/work"
     */
    private File work;

    /**
     * @parameter expression="${project}"
     */
    private MavenProject project;

    /**
     * @parameter expression="${debugPort}"
     */
    private int debugPort;

    /**
     * List of patterns (separated by commas) used to specify the tests that should be included in testing. When not
     * specified and whent the <code>test</code> parameter is not specified, the default includes will be
     * <code>**&#47;Test*.java   **&#47;*Test.java   **&#47;*TestCase.java</code>
     * 
     * @parameter
     */
    private List<String> includes;

    /**
     * List of patterns (separated by commas) used to specify the tests that should be excluded in testing. When not
     * specified and whent the <code>test</code> parameter is not specified, the default excludes will be
     * <code>**&#47;Abstract*Test.java  **&#47;Abstract*TestCase.java **&#47;*$*</code>
     * 
     * @parameter
     */
    private List<String> excludes;

    /**
     * Specify this parameter if you want to use the test pattern matching notation, Ant pattern matching, to select
     * tests to run. The Ant pattern will be used to create an include pattern formatted like
     * <code>**&#47;${test}.java</code> When used, the <code>includes</code> and <code>excludes</code> patterns
     * parameters are ignored
     * 
     * @parameter expression="${test}"
     */
    private String test;

    /**
     * @parameter expression="${maven.test.skipExec}" default-value="false"
     */
    private boolean skipExec;

    /**
     * @parameter expression="${maven.test.skip}" default-value="false"
     */
    private boolean skip;

    /**
     * The directory containing generated test classes of the project being tested.
     * 
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File testClassesDirectory;

    /**
     * Enables -debug -consolelog for the test OSGi runtime
     * 
     * @parameter expression="${tycho.showEclipseLog}" default-value="false"
     */
    private boolean showEclipseLog;

    /**
     * Base directory where all reports are written to.
     * 
     * @parameter expression="${project.build.directory}/surefire-reports"
     */
    private File reportsDirectory;

    /** @parameter expression="${project.build.directory}/surefire.properties" */
    private File surefireProperties;

    /** @parameter expression="${project.build.directory}/dev.properties" */
    private File devProperties;

    /**
     * Additional test target platform dependencies.
     * 
     * @parameter
     */
    private Dependency[] dependencies;

    /**
     * Eclipse application to be run. If not specified, default application org.eclipse.ui.ide.workbench will be used.
     * Application runnable will be invoked from test harness, not directly from Eclipse.
     * 
     * @parameter
     */
    private String application;

    /**
     * Eclipse product to be run, i.e. -product parameter passed to test Eclipse runtime.
     * 
     * @parameter
     */
    private String product;

    /**
     * @parameter expression="${session}"
     * @readonly
     * @required
     */
    private MavenSession session;

    /**
     * Run tests using UI (true) or headless (false) test harness.
     * 
     * @parameter default-value="false"
     */
    private boolean useUIHarness;

    /**
     * Run tests in UI (true) or background (false) thread. Only applies to UI test harness.
     * 
     * @parameter default-value="true"
     */
    private boolean useUIThread;

    /**
     * @parameter expression="${plugin.artifacts}"
     */
    private List<Artifact> pluginArtifacts;

    /**
     * Arbitrary JVM options to set on the command line.
     * 
     * @parameter expression="${tycho.testArgLine}"
     */
    private String argLine;

    /**
     * Arbitrary applications arguments to set on the command line.
     * 
     * @parameter
     */
    private String appArgLine;

    /**
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for the process, never
     * timing out.
     * 
     * @parameter expression="${surefire.timeout}"
     */
    private int forkedProcessTimeoutInSeconds;

    /**
     * Bundle-SymbolicName of the test suite, a special bundle that knows how to locate and execute all relevant tests.
     * testSuite and testClass identify single test class to run. All other tests will be ignored if both testSuite and
     * testClass are provided. It is an error if provide one of the two parameters but not the other.
     * 
     * @parameter expression="${testSuite}"
     */
    private String testSuite;

    /**
     * See testSuite
     * 
     * @parameter expression="${testClass}"
     */
    private String testClass;

    /**
     * Additional environments to set for the forked test JVM.
     * 
     * @parameter
     */
    private Map<String, String> environmentVariables;

    /**
     * Additional system properties to set for the forked test JVM.
     * 
     * @parameter
     */
    private Map<String, String> systemProperties;

    /**
     * List of bundles that must be expanded in order to execute the tests
     * 
     * @parameter
     */
    private String[] explodedBundles;

    /**
     * List of framework extension bundles to add.
     * 
     * @parameter
     */
    private Dependency[] frameworkExtensions;

    /**
     * Bundle start level and auto start configuration used by the test runtime.
     * 
     * @parameter
     */
    private BundleStartLevel[] bundleStartLevel;

    /**
     * @component
     */
    private RepositorySystem repositorySystem;

    /**
     * @component
     */
    private ResolutionErrorHandler resolutionErrorHandler;

    /** @component */
    private PlexusContainer plexus;

    /** @component */
    private Logger logger;

    /**
     * @component role="org.codehaus.tycho.TychoProject"
     */
    private Map<String, TychoProject> projectTypes;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skip || skipExec )
        {
            getLog().info( "Skipping tests" );
            return;
        }

        if ( !"eclipse-test-plugin".equals( project.getPackaging() ) )
        {
            getLog().warn( "Unsupported packaging type " + project.getPackaging() );
            return;
        }

        if ( testSuite != null || testClass != null )
        {
            if ( testSuite == null || testClass == null )
            {
                throw new MojoExecutionException(
                                                  "Both testSuite and testClass must be provided or both should be null" );
            }

            MavenProject suite = getTestSuite( testSuite );

            if ( suite == null )
            {
                throw new MojoExecutionException( "Cannot find test suite project with Bundle-SymbolicName "
                    + testSuite );
            }

            if ( !suite.equals( project ) )
            {
                getLog().info( "Not executing tests, testSuite=" + testSuite + " and project is not the testSuite" );
                return;
            }
        }

        TargetPlatformResolver platformResolver =
            TychoMavenLifecycleParticipant.lookupPlatformResolver( plexus, project );

        ArrayList<Dependency> dependencies = new ArrayList<Dependency>();

        if ( this.dependencies != null )
        {
            dependencies.addAll( Arrays.asList( this.dependencies ) );
        }

        dependencies.addAll( getTestDependencies() );

        TargetPlatform testTargetPlatform = platformResolver.resolvePlatform( session, project, dependencies );

        if ( testTargetPlatform == null )
        {
            throw new MojoExecutionException(
                                              "Cannot determinate build target platform location -- not executing tests" );
        }

        work.mkdirs();

        TestEclipseRuntime testRuntime = new TestEclipseRuntime();
        testRuntime.enableLogging( logger );
        testRuntime.setLocation( work );
        testRuntime.setPlexusContainer( plexus );
        testRuntime.setBundlesToExplode( getBundlesToExplode() );
        testRuntime.addFrameworkExtensions( getFrameworkExtensions() );
        if ( bundleStartLevel != null )
        {
            for ( BundleStartLevel level : bundleStartLevel )
            {
                testRuntime.addBundleStartLevel( level );
            }
        }

        BundleProject projectType = (BundleProject) projectTypes.get( project.getPackaging() );
        String testFramework = new TestFramework().getTestFramework( projectType.getClasspath( project ) );
        if ( testFramework == null )
        {
            throw new MojoExecutionException( "Could not determine test framework used by test bundle "
                + project.toString() );
        }
        getLog().debug( "Using test framework " + testFramework );

        for ( ArtifactDescription artifact : testTargetPlatform.getArtifacts( TychoProject.ECLIPSE_PLUGIN ) )
        {
            // note that this project is added as directory structure rooted at project basedir.
            // project classes and test-classes are added via dev.properties file (see #createDevProperties())
            // all other projects are added as bundle jars.
            MavenProject otherProject = artifact.getMavenProject();
            if ( otherProject == project )
            {
                testRuntime.addBundle( artifact.getKey(), project.getBasedir() );
                continue;
            }
            else if ( otherProject != null )
            {
                File file = otherProject.getArtifact().getFile();
                if ( file != null )
                {
                    testRuntime.addBundle( artifact.getKey(), file );
                    continue;
                }
            }
            testRuntime.addBundle( artifact );
        }

        Set<File> surefireBundles = getSurefirePlugins( testFramework );
        for ( File file : surefireBundles )
        {
            testRuntime.addBundle( file, true );
        }

        testRuntime.create();

        createDevProperties();
        createSurefireProperties( projectType.getArtifactKey( project ).getId(), testFramework );

        reportsDirectory.mkdirs();

        String testBundle = null;
        boolean succeeded = runTest( testRuntime, testBundle );

        if ( succeeded )
        {
            getLog().info( "All tests passed!" );
        }
        else
        {
            throw new MojoFailureException( "There are test failures.\n\nPlease refer to " + reportsDirectory
                + " for the individual test results." );
        }
    }

    private MavenProject getTestSuite( String symbolicName )
    {
        for ( MavenProject otherProject : session.getProjects() )
        {
            TychoProject projectType = projectTypes.get( otherProject.getPackaging() );
            if ( projectType != null && projectType.getArtifactKey( otherProject ).getId().equals( symbolicName ) )
            {
                return otherProject;
            }
        }
        return null;
    }

    private List<Dependency> getTestDependencies()
    {
        ArrayList<Dependency> result = new ArrayList<Dependency>();

        result.add( newBundleDependency( "org.eclipse.osgi" ) );
        result.add( newBundleDependency( EQUINOX_LAUNCHER ) );
        if ( useUIHarness )
        {
            result.add( newBundleDependency( "org.eclipse.ui.ide.application" ) );
        }
        else
        {
            result.add( newBundleDependency( "org.eclipse.core.runtime" ) );
        }

        return result;
    }

    protected Dependency newBundleDependency( String bundleId )
    {
        Dependency ideapp = new Dependency();
        ideapp.setArtifactId( bundleId );
        ideapp.setType( TychoProject.ECLIPSE_PLUGIN );
        return ideapp;
    }

    private void createSurefireProperties( String symbolicName, String testFramework )
        throws MojoExecutionException
    {
        Properties p = new Properties();

        p.put( "testpluginname", symbolicName );
        p.put( "testclassesdirectory", testClassesDirectory.getAbsolutePath() );
        p.put( "reportsdirectory", reportsDirectory.getAbsolutePath() );
        p.put( "testrunner", getTestRunner( testFramework ) );

        if ( test != null )
        {
            String test = this.test;
            test = test.replace( '.', '/' );
            test = test.endsWith( ".class" ) ? test : test + ".class";
            test = test.startsWith( "**/" ) ? test : "**/" + test;
            p.put( "includes", test );
        }
        else
        {
            if ( testClass != null )
            {
                p.put( "includes", testClass.replace( '.', '/' ) + ".class" );
            }
            else
            {
                p.put( "includes", includes != null ? getIncludesExcludes( includes )
                                : "**/Test*.class,**/*Test.class,**/*TestCase.class" );
                p.put( "excludes", excludes != null ? getIncludesExcludes( excludes )
                                : "**/Abstract*Test.class,**/Abstract*TestCase.class,**/*$*" );
            }
        }

        try
        {
            BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream( surefireProperties ) );
            try
            {
                p.store( out, null );
            }
            finally
            {
                out.close();
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Can't write test launcher properties file", e );
        }
    }

    private String getTestRunner( String testFramework )
    {
        if ( TestFramework.TEST_JUNIT.equals( testFramework ) )
        {
            return "org.codehaus.tycho.surefire.junit.JUnitDirectoryTestSuite";
        }
        else if ( TestFramework.TEST_JUNIT4.equals( testFramework ) )
        {
            return "org.apache.maven.surefire.junit4.JUnit4DirectoryTestSuite";
        }
        throw new IllegalArgumentException(); // can't happen
    }

    private String getIncludesExcludes( List<String> patterns )
    {
        StringBuilder sb = new StringBuilder();
        for ( String pattern : patterns )
        {
            if ( sb.length() > 0 )
            {
                sb.append( ',' );
            }
            sb.append( pattern );
        }
        return sb.toString();
    }

    private boolean runTest( TestEclipseRuntime testRuntime, String testBundle )
        throws MojoExecutionException
    {
        int result;

        try
        {
            String workspace = new File( work, "data" ).getAbsolutePath();

            FileUtils.deleteDirectory( workspace );

            Commandline cli = new Commandline();

            cli.setWorkingDirectory( project.getBasedir() );

            String executable = System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java";
            if ( File.separatorChar == '\\' )
            {
                executable = executable + ".exe";
            }
            cli.setExecutable( executable );

            if ( debugPort > 0 )
            {
                cli.addArguments( new String[] { "-Xdebug",
                    "-Xrunjdwp:transport=dt_socket,address=" + debugPort + ",server=y,suspend=y" } );
            }
            cli.addArguments( new String[] { "-Dosgi.noShutdown=false", } );

            Properties properties = (Properties) project.getContextValue( TychoConstants.CTX_MERGED_PROPERTIES );
            cli.addArguments( new String[] { "-Dosgi.os=" + PlatformPropertiesUtils.getOS( properties ),
                "-Dosgi.ws=" + PlatformPropertiesUtils.getWS( properties ),
                "-Dosgi.arch=" + PlatformPropertiesUtils.getArch( properties ), } );

            if ( argLine != null )
            {
                Arg arg = cli.createArg();
                arg.setLine( argLine );
            }

            if ( systemProperties != null )
            {
                for ( Map.Entry<String, String> entry : systemProperties.entrySet() )
                {
                    cli.createArg().setValue( "-D" + entry.getKey() + "=" + entry.getValue() );
                }
            }

            cli.addArguments( new String[] { "-jar", getEclipseLauncher( testRuntime ).getAbsolutePath(), } );

            if ( getLog().isDebugEnabled() || showEclipseLog )
            {
                cli.addArguments( new String[] { "-debug", "-consolelog", } );
            }
            cli.addArguments( new String[] { "-data", workspace, "-dev",
                devProperties.toURI().toURL().toExternalForm(), "-install",
                testRuntime.getLocation().getAbsolutePath(), "-configuration",
                new File( work, "configuration" ).getAbsolutePath(), "-application", getTestApplication( testRuntime ),
                "-testproperties", surefireProperties.getAbsolutePath(), } );
            if ( application != null )
            {
                cli.addArguments( new String[] { "-testApplication", application, } );
            }
            if ( product != null )
            {
                cli.addArguments( new String[] { "-product", product, } );
            }
            if ( useUIHarness && !useUIThread )
            {
                cli.addArguments( new String[] { "-nouithread", } );
            }
            if ( appArgLine != null )
            {
                Arg appArg = cli.createArg();
                appArg.setLine( appArgLine );
            }
            if ( environmentVariables != null )
            {
                for ( Map.Entry<String, String> entry : environmentVariables.entrySet() )
                {
                    cli.addEnvironment( entry.getKey(), entry.getValue() );
                }
            }

            getLog().info( "Expected eclipse log file: " + new File( workspace, ".metadata/.log" ).getCanonicalPath() );
            getLog().info( "Command line:\n\t" + cli.toString() );

            StreamConsumer out = new StreamConsumer()
            {
                public void consumeLine( String line )
                {
                    System.out.println( line );
                }
            };
            StreamConsumer err = new StreamConsumer()
            {
                public void consumeLine( String line )
                {
                    System.err.println( line );
                }
            };
            result = CommandLineUtils.executeCommandLine( cli, out, err, forkedProcessTimeoutInSeconds );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error while executing platform", e );
        }

        return result == 0;
    }

    private String getTestApplication( TestEclipseRuntime testRuntime )
    {
        if ( useUIHarness )
        {
            ArtifactDescription systemBundle = testRuntime.getSystemBundle();
            Version osgiVersion = Version.parseVersion( systemBundle.getKey().getVersion() );
            if ( osgiVersion.compareTo( VERSION_3_3_0 ) < 0 )
            {
                return "org.sonatype.tycho.surefire.osgibooter.uitest32";
            }
            else
            {
                return "org.sonatype.tycho.surefire.osgibooter.uitest";
            }
        }
        else
        {
            return "org.sonatype.tycho.surefire.osgibooter.headlesstest";
        }
    }

    private File getEclipseLauncher( TestEclipseRuntime testRuntime )
        throws IOException
    {
        ArtifactDescription systemBundle = testRuntime.getSystemBundle();
        Version osgiVersion = Version.parseVersion( systemBundle.getKey().getVersion() );
        if ( osgiVersion.compareTo( VERSION_3_3_0 ) < 0 )
        {
            throw new IllegalArgumentException( "Eclipse 3.2 and earlier are not supported." );
            // return new File(state.getTargetPlaform(), "startup.jar").getCanonicalFile();
        }
        else
        {
            // assume eclipse 3.3 or 3.4
            ArtifactDescription launcher = testRuntime.getBundle( EQUINOX_LAUNCHER, null );
            if ( launcher == null )
            {
                throw new IllegalArgumentException( "Could not find " + EQUINOX_LAUNCHER
                    + " bundle in the test runtime." );
            }
            return launcher.getLocation().getCanonicalFile();
        }
    }

    private Set<File> getSurefirePlugins( String testFramework )
        throws MojoExecutionException
    {
        Set<File> result = new LinkedHashSet<File>();

        String fragment;
        if ( TestFramework.TEST_JUNIT.equals( testFramework ) )
        {
            fragment = "org.sonatype.tycho.surefire.junit";
        }
        else if ( TestFramework.TEST_JUNIT4.equals( testFramework ) )
        {
            fragment = "org.sonatype.tycho.surefire.junit4";
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported test framework " + testFramework );
        }

        for ( Artifact artifact : pluginArtifacts )
        {
            if ( "org.sonatype.tycho".equals( artifact.getGroupId() ) )
            {
                if ( "org.sonatype.tycho.surefire.osgibooter".equals( artifact.getArtifactId() )
                    || fragment.equals( artifact.getArtifactId() ) )
                {
                    result.add( artifact.getFile() );
                }
            }
        }

        if ( result.size() != 2 )
        {
            StringBuilder sb =
                new StringBuilder(
                                   "Unable to locate org.sonatype.tycho:org.sonatype.tycho.surefire.osgibooter and/or its fragments\n" );
            sb.append( "Test framework: " + testFramework );
            sb.append( "All plugin artifacts: " );
            for ( Artifact artifact : pluginArtifacts )
            {
                sb.append( "\n\t" ).append( artifact.toString() );
            }
            sb.append( "\nMatched OSGi test booter artifacts: " );
            for ( File file : result )
            {
                sb.append( "\n\t" ).append( file.getAbsolutePath() );
            }

            throw new MojoExecutionException( sb.toString() );
        }

        return result;
    }

    private void createDevProperties()
        throws MojoExecutionException
    {
        Properties dev = new Properties();
        // dev.put("@ignoredot@", "true");
        // for (MavenProject otherProject : session.getProjects()) {
        // if ("eclipse-test-plugin".equals(otherProject.getPackaging())) {
        // TychoProject projectType = projectTypes.get(otherProject.getPackaging());
        // dev.put(projectType.getArtifactKey(otherProject).getId(), getBuildOutputDirectories(otherProject));
        // } else if ("eclipse-plugin".equals(otherProject.getPackaging())) {
        // TychoProject projectType = projectTypes.get(otherProject.getPackaging());
        // File file = otherProject.getArtifact().getFile();
        // if (file == null || file.isDirectory()) {
        // dev.put(projectType.getArtifactKey(otherProject).getId(), getBuildOutputDirectories(otherProject));
        // }
        // }
        // }

        TychoProject projectType = projectTypes.get( project.getPackaging() );
        dev.put( projectType.getArtifactKey( project ).getId(), getBuildOutputDirectories( project ) );

        try
        {
            OutputStream os = new BufferedOutputStream( new FileOutputStream( devProperties ) );
            try
            {
                dev.store( os, null );
            }
            finally
            {
                os.close();
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Can't create osgi dev properties file", e );
        }
    }

    private String getBuildOutputDirectories( MavenProject project )
    {
        StringBuilder sb = new StringBuilder();

        Build build = project.getBuild();
        sb.append( build.getOutputDirectory() );
        sb.append( ',' ).append( build.getTestOutputDirectory() );

        Properties buildProperties = new Properties();
        File file = new File( project.getBasedir(), "build.properties" );
        try
        {
            FileInputStream is = new FileInputStream( file );
            try
            {
                buildProperties.load( is );
            }
            finally
            {
                is.close();
            }

            // TODO plugin package mojo has this same logic, move to a helper
            final String OUTPUT = "output.";
            final String SOURCE = "source.";

            for ( Iterator<Object> iterator = buildProperties.keySet().iterator(); iterator.hasNext(); )
            {
                String key = (String) iterator.next();
                String[] classesDir = null;
                if ( key.startsWith( OUTPUT ) && !key.equals( "output.." ) )
                {
                    classesDir = buildProperties.getProperty( key ).split( "," );
                }
                else if ( key.startsWith( SOURCE ) && !key.equals( "source.." ) )
                {
                    String fileName = key.substring( SOURCE.length() );
                    classesDir =
                        new String[] { new File( project.getBuild().getDirectory() ).getName() + "/"
                            + fileName.substring( 0, fileName.length() - 4 ) + "-classes" };
                }
                if ( classesDir != null )
                {
                    for ( String dir : classesDir )
                    {
                        if ( sb.length() > 0 )
                            sb.append( ',' );
                        sb.append( dir );
                    }
                }
            }

        }
        catch ( IOException e )
        {
            getLog().debug( "Exception reading build.properties of " + project.getId(), e );
        }

        return sb.toString();
    }

    private List<String> getBundlesToExplode()
    {
        List<String> bundles = new ArrayList<String>();

        if ( explodedBundles != null )
        {
            bundles.addAll( Arrays.asList( explodedBundles ) );
        }

        return bundles;
    }

    private List<File> getFrameworkExtensions()
        throws MojoExecutionException
    {
        List<File> files = new ArrayList<File>();

        if ( frameworkExtensions != null )
        {
            for ( Dependency frameworkExtension : frameworkExtensions )
            {
                Artifact artifact = repositorySystem.createDependencyArtifact( frameworkExtension );
                ArtifactResolutionRequest request = new ArtifactResolutionRequest();
                request.setArtifact( artifact );
                request.setResolveRoot( true ).setResolveTransitively( false );
                request.setLocalRepository( session.getLocalRepository() );
                request.setRemoteRepositories( project.getPluginArtifactRepositories() );
                request.setOffline( session.isOffline() );
                request.setForceUpdate( session.getRequest().isUpdateSnapshots() );
                ArtifactResolutionResult result = repositorySystem.resolve( request );
                try
                {
                    resolutionErrorHandler.throwErrors( request, result );
                }
                catch ( ArtifactResolutionException e )
                {
                    throw new MojoExecutionException( "Failed to resolve framework extension "
                        + frameworkExtension.getManagementKey(), e );
                }
                files.add( artifact.getFile() );
            }
        }

        return files;
    }

}
