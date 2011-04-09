package org.codehaus.tycho.osgitest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
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
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.apache.maven.toolchain.java.DefaultJavaToolChain;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.tycho.BundleProject;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.osgitools.DefaultReactorProject;
import org.codehaus.tycho.osgitools.OsgiBundleProject;
import org.codehaus.tycho.resolver.DefaultTargetPlatformResolverFactory;
import org.codehaus.tycho.utils.PlatformPropertiesUtils;
import org.osgi.framework.Version;
import org.sonatype.tycho.ArtifactDescriptor;
import org.sonatype.tycho.ArtifactKey;
import org.sonatype.tycho.ReactorProject;
import org.sonatype.tycho.equinox.launching.BundleStartLevel;
import org.sonatype.tycho.equinox.launching.DefaultEquinoxInstallationDescription;
import org.sonatype.tycho.equinox.launching.EquinoxInstallation;
import org.sonatype.tycho.equinox.launching.EquinoxInstallationDescription;
import org.sonatype.tycho.equinox.launching.EquinoxInstallationFactory;
import org.sonatype.tycho.equinox.launching.EquinoxLauncher;
import org.sonatype.tycho.equinox.launching.internal.EquinoxLaunchConfiguration;
import org.sonatype.tycho.launching.LaunchConfiguration;
import org.sonatype.tycho.launching.LaunchConfigurationFactory;

/**
 * @phase integration-test
 * @goal test
 * @requiresProject true
 * @requiresDependencyResolution runtime
 */
public class TestMojo extends AbstractMojo implements LaunchConfigurationFactory {

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
	 * List of patterns (separated by commas) used to specify the tests that
	 * should be included in testing. When not specified and whent the
	 * <code>test</code> parameter is not specified, the default includes will
	 * be
	 * <code>**&#47;Test*.java   **&#47;*Test.java   **&#47;*TestCase.java</code>
	 * 
	 * @parameter
	 */
	private List<String> includes;

	/**
	 * List of patterns (separated by commas) used to specify the tests that
	 * should be excluded in testing. When not specified and whent the
	 * <code>test</code> parameter is not specified, the default excludes will
	 * be
	 * <code>**&#47;Abstract*Test.java  **&#47;Abstract*TestCase.java **&#47;*$*</code>
	 * 
	 * @parameter
	 */
	private List<String> excludes;

	/**
	 * Specify this parameter if you want to use the test pattern matching
	 * notation, Ant pattern matching, to select tests to run. The Ant pattern
	 * will be used to create an include pattern formatted like
	 * <code>**&#47;${test}.java</code> When used, the <code>includes</code>
	 * and <code>excludes</code> patterns parameters are ignored
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
	 * If set to "false" the test execution will not fail in case there are 
	 * no tests found.
	 * 
	 * @parameter expression="${failIfNoTests}" default-value="true"
	 */
	private boolean failIfNoTests;

	/**
	 * The directory containing generated test classes of the project being
	 * tested.
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
	 * Eclipse application to be run. If not specified, default application
	 * org.eclipse.ui.ide.workbench will be used. Application runnable
	 * will be invoked from test harness, not directly from Eclipse. 
	 * 
	 * @parameter 
	 */
	private String application;

	/**
	 * Eclipse product to be run, i.e. -product parameter
	 * passed to test Eclipse runtime.
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
	 * Run tests in UI (true) or background (false) thread. Only applies to
	 * UI test harness.
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
     * Kill the forked test process after a certain number of seconds.  If set to 0,
     * wait forever for the process, never timing out.
     * 
     * @parameter expression="${surefire.timeout}"
     */
    private int forkedProcessTimeoutInSeconds;

    /**
	 * Bundle-SymbolicName of the test suite, a special bundle that knows
	 * how to locate and execute all relevant tests. 
	 * 
	 * testSuite and testClass identify single test class to run. All other
	 * tests will be ignored if both testSuite and testClass are provided.
	 * It is an error if provide one of the two parameters but not the other.
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
    private DefaultTargetPlatformResolverFactory targetPlatformResolverLocator;
    
    /**
     * @component role="org.codehaus.tycho.TychoProject"
     */
    private Map<String, TychoProject> projectTypes;

    /** @component */
    private EquinoxInstallationFactory installationFactory;

    /** @component */
    private EquinoxLauncher launcher;

    /**
     * @component role="org.codehaus.tycho.TychoProject" role-hint="eclipse-plugin"
     */
    private OsgiBundleProject osgiBundle;

    /** @component */
    private ToolchainManager toolchainManager;

    public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip || skipExec) {
			getLog().info("Skipping tests");
			return;
		}

		if (!"eclipse-test-plugin".equals(project.getPackaging())) {
			getLog().warn("Unsupported packaging type " + project.getPackaging());
			return;
		}

		if (testSuite != null || testClass != null) {
			if (testSuite == null || testClass == null) {
				throw new MojoExecutionException("Both testSuite and testClass must be provided or both should be null");
			}

			MavenProject suite = getTestSuite(testSuite);

			if (suite == null) {
				throw new MojoExecutionException("Cannot find test suite project with Bundle-SymbolicName " + testSuite);
			}

			if (!suite.equals(project)) {
				getLog().info("Not executing tests, testSuite=" + testSuite + " and project is not the testSuite");
				return;
			}
		}

		EquinoxInstallation testRuntime = createEclipseInstallation(false, DefaultReactorProject.adapt(session));

		// Hack to use JavaToolChain
		String oldJavaHome = System.getProperty( "java.home" );
		Toolchain tc = getToolchain();
		if ( tc != null && tc instanceof DefaultJavaToolChain )
		{
			getLog().info( "Toolchain: " + tc );
			String jvm = tc.findTool( "java" );
			String javaHome = ((DefaultJavaToolChain) tc).getJavaHome();
			getLog().info( "java.home: " + javaHome );
			System.setProperty( "java.home", javaHome );
		}

		String testBundle = null;
		boolean succeeded = runTest(testRuntime, testBundle);

		System.setProperty( "java.home", oldJavaHome );
		
		if (succeeded) {
			getLog().info("All tests passed!");
		} else {
	        throw new MojoFailureException("There are test failures.\n\nPlease refer to " + reportsDirectory + " for the individual test results.");
		}
	}

    private Toolchain getToolchain()
    {
        Toolchain tc = null;

        if ( toolchainManager != null )
        {
            tc = toolchainManager.getToolchainFromBuildContext( "jdk", session );
        }

        return tc;
    }

    private EquinoxInstallation createEclipseInstallation(boolean includeReactorProjects, List<ReactorProject> reactorProjects)
        throws MojoExecutionException
    {
        TargetPlatformResolver platformResolver = targetPlatformResolverLocator.lookupPlatformResolver( project );

		ArrayList<Dependency> dependencies = new ArrayList<Dependency>(); 

		if ( this.dependencies != null )
		{
		    dependencies.addAll( Arrays.asList( this.dependencies ) );
		}

		dependencies.addAll( getTestDependencies() );

		TargetPlatform testTargetPlatform = platformResolver.resolvePlatform( session, project, reactorProjects, dependencies );

		if (testTargetPlatform == null) {
			throw new MojoExecutionException("Cannot determinate build target platform location -- not executing tests");
		}

		work.mkdirs();

		EquinoxInstallationDescription testRuntime = new DefaultEquinoxInstallationDescription();
		testRuntime.addBundlesToExplode(getBundlesToExplode());
		testRuntime.addFrameworkExtensions(getFrameworkExtensions());
        if (bundleStartLevel != null) {
            for (BundleStartLevel level : bundleStartLevel) {
                testRuntime.addBundleStartLevel(level);
            }
        }

        BundleProject projectType = (BundleProject) projectTypes.get(project.getPackaging());
		String testFramework = new TestFramework().getTestFramework(projectType.getClasspath(project));
		if (testFramework == null) {
		    throw new MojoExecutionException("Could not determine test framework used by test bundle " + project.toString());
		}
		getLog().debug("Using test framework " + testFramework);

		for (ArtifactDescriptor artifact : testTargetPlatform.getArtifacts(ArtifactKey.TYPE_ECLIPSE_PLUGIN)) {
		    // note that this project is added as directory structure rooted at project basedir.
		    // project classes and test-classes are added via dev.properties file (see #createDevProperties())
		    // all other projects are added as bundle jars.
		    ReactorProject otherProject = artifact.getMavenProject();
		    if (otherProject != null) {
		        if (otherProject.sameProject(project)) {
	                testRuntime.addBundle(artifact.getKey(), project.getBasedir());
	                continue;
	            }
                File file = otherProject.getArtifact();
                if (file != null) {
                    testRuntime.addBundle(artifact.getKey(), file);
                    continue;
                }
		    }
            testRuntime.addBundle(artifact);
		}

		Set<File> surefireBundles = getSurefirePlugins(testFramework);
		for (File file : surefireBundles) {
		    testRuntime.addBundle(getBundleArtifacyKey(file), file, true);
		}

		createDevProperties(includeReactorProjects, reactorProjects);
		createSurefireProperties(projectType.getArtifactKey(DefaultReactorProject.adapt(project)).getId(), testFramework);

		reportsDirectory.mkdirs();
        return installationFactory.createInstallation(testRuntime, work);
    }

    private ArtifactKey getBundleArtifacyKey( File file )
        throws MojoExecutionException
    {
        ArtifactKey key = osgiBundle.readArtifactKey( file );
        if ( key == null )
        {
            throw new MojoExecutionException( "Not an OSGi bundle " + file.getAbsolutePath() );
        }
        return key;
    }

    private MavenProject getTestSuite( String symbolicName )
    {
        for ( MavenProject otherProject : session.getProjects() )
        {
            TychoProject projectType = projectTypes.get( otherProject.getPackaging() );
            if ( projectType != null
                && projectType.getArtifactKey( DefaultReactorProject.adapt( otherProject ) ).getId().equals( symbolicName ) )
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
        result.add( newBundleDependency( EquinoxInstallationDescription.EQUINOX_LAUNCHER ) );
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
        ideapp.setType( ArtifactKey.TYPE_ECLIPSE_PLUGIN );
        return ideapp;
    }

	private void createSurefireProperties(String symbolicName, String testFramework) throws MojoExecutionException {
		Properties p = new Properties();

		p.put("testpluginname", symbolicName);
		p.put("testclassesdirectory", testClassesDirectory.getAbsolutePath());
		p.put("reportsdirectory", reportsDirectory.getAbsolutePath());
		p.put("testrunner", getTestRunner(testFramework));

		if (test != null) {
            String test = this.test;
            test = test.replace('.', '/');
            test = test.endsWith(".class") ? test : test + ".class";
            test = test.startsWith("**/") ? test : "**/" + test;
            p.put("includes", test);
        } else {
            if (testClass != null) {
                p.put("includes", testClass.replace('.', '/') + ".class");
            } else {
                p.put("includes", includes != null ? getIncludesExcludes(includes): "**/Test*.class,**/*Test.class,**/*TestCase.class");
                p.put("excludes", excludes != null ? getIncludesExcludes(excludes): "**/Abstract*Test.class,**/Abstract*TestCase.class,**/*$*");
            }
        }

		p.put("failifnotests", String.valueOf(failIfNoTests));

		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(surefireProperties));
			try {
				p.store(out, null);
			} finally {
				out.close();
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Can't write test launcher properties file", e);
		}
	}

	private String getTestRunner(String testFramework) {
		if (TestFramework.TEST_JUNIT.equals(testFramework)) {
			return "org.codehaus.tycho.surefire.junit.JUnitDirectoryTestSuite";
		} else if (TestFramework.TEST_JUNIT4.equals(testFramework)) {
			return "org.apache.maven.surefire.junit4.JUnit4DirectoryTestSuite";
		}
		throw new IllegalArgumentException(); // can't happen
	}

	private String getIncludesExcludes(List<String> patterns) {
		StringBuilder sb = new StringBuilder();
		for (String pattern : patterns) {
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(pattern);
		}
		return sb.toString();
	}

    private boolean runTest( EquinoxInstallation testRuntime, String testBundle )
        throws MojoExecutionException
    {
        int result;

        try
        {
            File workspace = new File( work, "data" ).getAbsoluteFile();

            FileUtils.deleteDirectory( workspace );

            LaunchConfiguration cli = createCommandLine( testRuntime, workspace );

            getLog().info( "Expected eclipse log file: " + new File( workspace, ".metadata/.log" ).getCanonicalPath() );

            result = launcher.execute( cli, forkedProcessTimeoutInSeconds );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error while executing platform", e );
        }

        return result == 0;
    }

    LaunchConfiguration createCommandLine( EquinoxInstallation testRuntime, File workspace )
        throws MalformedURLException
    {
        EquinoxLaunchConfiguration cli = new EquinoxLaunchConfiguration( testRuntime );

        cli.setWorkingDirectory( project.getBasedir() );

        if ( debugPort > 0 )
        {
            cli.addVMArguments( "-Xdebug", "-Xrunjdwp:transport=dt_socket,address=" + debugPort + ",server=y,suspend=y" );
        }

        cli.addVMArguments( "-Dosgi.noShutdown=false" );

        Properties properties = (Properties) project.getContextValue( TychoConstants.CTX_MERGED_PROPERTIES );
        cli.addVMArguments( "-Dosgi.os=" + PlatformPropertiesUtils.getOS( properties ), //
                            "-Dosgi.ws=" + PlatformPropertiesUtils.getWS( properties ), //
                            "-Dosgi.arch=" + PlatformPropertiesUtils.getArch( properties ) );

        addVMArgs( cli , argLine );

        if ( systemProperties != null )
        {
            for ( Map.Entry<String, String> entry : systemProperties.entrySet() )
            {
                cli.addVMArguments( true, "-D" + entry.getKey() + "=" + entry.getValue() );
            }
        }

        if ( getLog().isDebugEnabled() || showEclipseLog )
        {
            cli.addProgramArguments( "-debug", "-consolelog" );
        }

        addProgramArgs( true, cli, "-data", workspace.getAbsolutePath(), //
                                 "-dev", devProperties.toURI().toURL().toExternalForm(), //
                                 "-install", testRuntime.getLocation().getAbsolutePath(), //
                                 "-configuration", new File( work, "configuration" ).getAbsolutePath(), //
                                 "-application", getTestApplication( testRuntime.getInstallationDescription() ), //
                                 "-testproperties", surefireProperties.getAbsolutePath() );
        if ( application != null )
        {
            cli.addProgramArguments( "-testApplication", application );
        }
        if ( product != null )
        {
            cli.addProgramArguments( "-product", product );
        }
        if ( useUIHarness && !useUIThread )
        {
            cli.addProgramArguments( "-nouithread" );
        }
        addProgramArgs( false , cli, appArgLine );
        if ( environmentVariables != null )
        {
            cli.addEnvironmentVariables( environmentVariables );
        }
        return cli;
    }

    void addProgramArgs( boolean escape, EquinoxLaunchConfiguration cli, String... arguments)
    {
        if ( arguments != null )
        {
            for ( String argument : arguments )
            {
                if ( argument != null )
                {
                    cli.addProgramArguments( escape, argument );
                }
            }
        }
    }

    void addVMArgs( EquinoxLaunchConfiguration cli , String argLine)
    {
        if ( argLine != null )
        {
            cli.addVMArguments( false, argLine );
        }
    }

	private String getTestApplication(EquinoxInstallationDescription testRuntime) {
		if (useUIHarness) {
		    ArtifactDescriptor systemBundle = testRuntime.getSystemBundle();
		    Version osgiVersion = Version.parseVersion(systemBundle.getKey().getVersion());
			if (osgiVersion.compareTo(EquinoxInstallationDescription.EQUINOX_VERSION_3_3_0) < 0) {
				return "org.sonatype.tycho.surefire.osgibooter.uitest32";
			} else {
				return "org.sonatype.tycho.surefire.osgibooter.uitest";
			}
		} else {
			return "org.sonatype.tycho.surefire.osgibooter.headlesstest";
		}
	}

	private Set<File> getSurefirePlugins(String testFramework) throws MojoExecutionException {
		Set<File> result = new LinkedHashSet<File>();
		
		String fragment;
		if (TestFramework.TEST_JUNIT.equals(testFramework)) {
			fragment = "org.sonatype.tycho.surefire.junit";
		} else if (TestFramework.TEST_JUNIT4.equals(testFramework)) {
			fragment = "org.sonatype.tycho.surefire.junit4";
		} else {
			throw new IllegalArgumentException("Unsupported test framework " + testFramework);
		}

		for (Artifact artifact : pluginArtifacts) {
			if ("org.sonatype.tycho".equals(artifact.getGroupId())) {
				if ("org.sonatype.tycho.surefire.osgibooter".equals(artifact.getArtifactId()) || fragment.equals(artifact.getArtifactId())) {
					result.add(artifact.getFile());
				}
			}
		}

		if (result.size() != 2) {
		    StringBuilder sb = new StringBuilder("Unable to locate org.sonatype.tycho:org.sonatype.tycho.surefire.osgibooter and/or its fragments\n");
		    sb.append("Test framework: " + testFramework);
		    sb.append("All plugin artifacts: ");
            for (Artifact artifact : pluginArtifacts) {
                sb.append("\n\t").append(artifact.toString());
            }
            sb.append("\nMatched OSGi test booter artifacts: ");
            for (File file : result) {
                sb.append("\n\t").append(file.getAbsolutePath());
            }

			throw new MojoExecutionException(sb.toString());
		}

		return result;
	}

	private void createDevProperties( boolean includeReactorProjects, List<ReactorProject> reactorProjects ) throws MojoExecutionException {
		Properties dev = new Properties();

        if ( includeReactorProjects )
        {
            // this is needed for IDE integration, where we want to use reactor project output folders
            dev.put( "@ignoredot@", "true" );
            for ( ReactorProject otherProject : reactorProjects )
            {
                if ( "eclipse-test-plugin".equals( otherProject.getPackaging() )
                    || "eclipse-plugin".equals( otherProject.getPackaging() ) )
                {
                    TychoProject projectType = projectTypes.get( otherProject.getPackaging() );
                    dev.put( projectType.getArtifactKey( otherProject ).getId(),
                             getBuildOutputDirectories( otherProject ) );
                }
            }
        }
        
        ReactorProject reactorProject = DefaultReactorProject.adapt( project );

		TychoProject projectType = projectTypes.get(project.getPackaging());
		dev.put(projectType.getArtifactKey(reactorProject).getId(), getBuildOutputDirectories(reactorProject));

		try {
			OutputStream os = new BufferedOutputStream(new FileOutputStream(devProperties));
			try {
				dev.store(os, null);
			} finally {
				os.close();
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Can't create osgi dev properties file", e);
		}
	}

	private String getBuildOutputDirectories(ReactorProject otherProject) {
		StringBuilder sb = new StringBuilder();
		
		sb.append(otherProject.getOutputDirectory());
		sb.append(',').append(otherProject.getTestOutputDirectory());

		Properties buildProperties = new Properties();
		File file = new File(otherProject.getBasedir(), "build.properties");
		try {
			FileInputStream is = new FileInputStream(file);
			try {
				buildProperties.load(is);
			} finally {
				is.close();
			}

			// TODO plugin package mojo has this same logic, move to a helper
			final String OUTPUT = "output.";
			final String SOURCE = "source.";
			
			for (Iterator<Object> iterator = buildProperties.keySet().iterator(); iterator.hasNext();) {
				String key = (String) iterator.next();
				String[] classesDir = null;
				if (key.startsWith(OUTPUT) && !key.equals("output..")) {
					classesDir = buildProperties.getProperty(key).split(",");
				} else if (key.startsWith(SOURCE) && !key.equals("source..")) {
					String fileName = key.substring(SOURCE.length());
					classesDir = new String[] {otherProject.getBuildDirectory().getName() + "/" + fileName.substring(0, fileName.length() - 4) + "-classes"};
				}
				if (classesDir != null) {
					for (String dir : classesDir) {
						if (sb.length() > 0) sb.append(',');
						sb.append(dir);
					}
				}
			}

		} catch (IOException e) {
			getLog().debug("Exception reading build.properties of " + otherProject.getId(), e);
		}

		return sb.toString();
	}

	private List<String> getBundlesToExplode() {
		List<String> bundles = new ArrayList<String>();

		if(explodedBundles != null) {
			bundles.addAll(Arrays.asList(explodedBundles));
		}

		return bundles;
	}

    private List<File> getFrameworkExtensions() throws MojoExecutionException {
        List<File> files = new ArrayList<File>();

        if (frameworkExtensions != null) {
            for (Dependency frameworkExtension : frameworkExtensions) {
                Artifact artifact = repositorySystem.createDependencyArtifact(frameworkExtension);
                ArtifactResolutionRequest request = new ArtifactResolutionRequest();
                request.setArtifact(artifact);
                request.setResolveRoot(true).setResolveTransitively(false);
                request.setLocalRepository(session.getLocalRepository());
                request.setRemoteRepositories(project.getPluginArtifactRepositories());
                request.setOffline(session.isOffline());
                request.setForceUpdate(session.getRequest().isUpdateSnapshots());
                ArtifactResolutionResult result = repositorySystem.resolve(request);
                try {
                    resolutionErrorHandler.throwErrors(request, result);
                } catch (ArtifactResolutionException e) {
                    throw new MojoExecutionException("Failed to resolve framework extension "
                            + frameworkExtension.getManagementKey(), e);
                }
                files.add(artifact.getFile());
            }
        }

        return files;
    }

    public LaunchConfiguration createLaunchConfiguration(List<ReactorProject> reactorProjects)
    {
        try
        {
            EquinoxInstallation testRuntime = createEclipseInstallation(true, reactorProjects);
    
            return createCommandLine( testRuntime, work );
        }
        catch ( MalformedURLException e )
        {
            getLog().error( e );
        }
        catch ( MojoExecutionException e )
        {
            getLog().error( e );
        }

        return null;
    }
}
