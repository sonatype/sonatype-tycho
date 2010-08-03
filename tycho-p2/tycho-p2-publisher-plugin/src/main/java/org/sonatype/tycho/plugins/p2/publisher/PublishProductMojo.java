package org.sonatype.tycho.plugins.p2.publisher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;
import org.codehaus.tycho.ArtifactDescription;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.buildversion.VersioningHelper;
import org.codehaus.tycho.model.FeatureRef;
import org.codehaus.tycho.model.PluginRef;
import org.codehaus.tycho.model.ProductConfiguration;
import org.codehaus.tycho.p2.MetadataSerializable;
import org.sonatype.tycho.osgi.EquinoxEmbedder;

/**
 * This goal invokes the product publisher for each product file found.
 *
 * @see http://wiki.eclipse.org/Equinox/p2/Publisher
 * @goal publish-products
 */
public final class PublishProductMojo
    extends AbstractP2Mojo
{

    private static String PUBLISHER_BUNDLE_ID = "org.sonatype.tycho.p2.publisher";

    private static String PRODUCT_PUBLISHER_APP_NAME = PUBLISHER_BUNDLE_ID + ".ProductPublisher";

    /**
     * @parameter default-value="true"
     */
    private boolean compress;

    /**
     * @parameter default-value="tooling"
     */
    private String flavor;

    /**
     * @component role="org.codehaus.plexus.archiver.UnArchiver" role-hint="zip"
     */
    private UnArchiver deflater;

    /** @component */
    private EquinoxEmbedder p2;

    /**
     * Kill the forked process after a certain number of seconds. If set to 0, wait forever for the
     * process, never timing out.
     *
     * @parameter expression="${p2.timeout}" default-value="0"
     */
    private int forkedProcessTimeoutInSeconds;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        publishProducts();
    }

    private void publishProducts()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {

            for ( Product product : getProducts() )
            {
                final Product buildProduct =
                    prepareBuildProduct( product, new File( getProject().getBuild().getDirectory() ), getQualifier() );
                // see http://wiki.eclipse.org/Equinox/p2/Publisher#Product_Publisher
                Commandline cli = createOSGiCommandline( PRODUCT_PUBLISHER_APP_NAME, getEquinoxLauncher( p2 ) );
                cli.setWorkingDirectory( getProject().getBasedir() );

                /*
                 * Restore the p2 view on the Tycho build target platform that was calculated
                 * earlier (see org.sonatype.tycho.p2.resolver.P2ResolverImpl.toResolutionResult).
                 * We cannot access the computation logic from here because it is contained in the
                 * OSGi bundle "org.sonatype.tycho.p2.impl" in Tycho's "OSGi layer" that cannot be
                 * accessed from current (lower) Mojo-Layer.
                 */
                String contextRepositoryUrl =
                    materializeRepository( getTargetPlatform().getP2MetadataSerializable(),
                                           new File( getProject().getBuild().getDirectory() ) );
                cli.addArguments( new String[] { "-artifactRepository", getRepositoryUrl(), //
                    "-metadataRepository", getRepositoryUrl(), //
                    "-productFile", buildProduct.productFile.getCanonicalPath(), //
                    "-contextMetadata", contextRepositoryUrl, //
                    "-append", //
                    "-executables", getEquinoxExecutableFeature(), //
                    "-publishArtifacts", //
                    "-flavor", flavor } );
                cli.addArguments( getConfigsParameter( getEnvironments() ) );
                cli.addArguments( getCompressFlag() );

                try
                {
                    int result = executeCommandline( cli, forkedProcessTimeoutInSeconds );
                    if ( result != 0 )
                    {
                        throw new MojoFailureException( "P2 publisher return code was " + result );
                    }
                }
                catch ( CommandLineException cle )
                {
                    throw new MojoExecutionException( "P2 publisher failed to be executed ", cle );
                }
            }
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "Unable to execute the publisher", ioe );
        }
    }

    static Commandline createOSGiCommandline( String applicationId, File equinoxLauncher )
    {
        Commandline cli = new Commandline();

        String executable = System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java";
        cli.setExecutable( executable );
        cli.addArguments( new String[] { "-jar", equinoxLauncher.getAbsolutePath(), } );
        cli.addArguments( new String[] { "-application", applicationId } );
        cli.addArguments( new String[] { "-nosplash" } );
        cli.addArguments( new String[] { "-consoleLog" } );
        return cli;
    }

    private static File getEquinoxLauncher( EquinoxEmbedder equinoxEmbedder )
    {
        File p2location = equinoxEmbedder.getRuntimeLocation();
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir( p2location );
        ds.setIncludes( new String[] { "plugins/org.eclipse.equinox.launcher_*.jar" } );
        ds.scan();
        String[] includedFiles = ds.getIncludedFiles();
        if ( includedFiles == null || includedFiles.length != 1 )
        {
            throw new IllegalStateException( "Can't locate org.eclipse.equinox.launcher bundle in " + p2location );
        }
        return new File( p2location, includedFiles[0] );
    }

    /**
     * Prepare the product file for the Eclipse publisher application.
     * <p>
     * Copies the product file and, if present, corresponding p2 advice file to a working directory.
     * The folder is named after the product ID (stored in the 'uid' attribute!), and the p2 advice
     * file is renamed to "p2.inf" so that the publisher application finds it.
     * </p>
     */
    static Product prepareBuildProduct( Product product, File targetDir, String qualifier )
        throws IOException
    {

        ProductConfiguration productConfiguration = ProductConfiguration.read( product.productFile );

        qualifyVersions( productConfiguration, qualifier );

        File buildProductDir = new File( targetDir, "products/" + productConfiguration.getId() );
        buildProductDir.mkdirs();
        final Product buildProduct =
            new Product( new File( buildProductDir, product.getProductFile().getName() ), new File( buildProductDir,
                                                                                                    "p2.inf" ) );
        ProductConfiguration.write( productConfiguration, buildProduct.productFile );
        copyP2Inf( product.p2infFile, buildProduct.p2infFile );

        return buildProduct;
    }

    static void copyP2Inf( final File sourceP2Inf, final File buildP2Inf )
        throws IOException
    {
        if ( sourceP2Inf.exists() )
        {
            FileUtils.copyFile( sourceP2Inf, buildP2Inf );
        }
    }

    private int executeCommandline( Commandline cli, int timeout )
        throws CommandLineException
    {
        getLog().info( "Command line:\n\t" + cli.toString() );
        return CommandLineUtils.executeCommandLine( cli, new DefaultConsumer(),
                                                    new WriterStreamConsumer( new OutputStreamWriter( System.err ) ),
                                                    timeout );
    }

    /**
     * Value class identifying a product file (and optionally an associated p2.inf file) for the
     * {@link PublishProductMojo}.
     */
    static class Product
    {
        private final File productFile;

        private final File p2infFile;

        public Product( File productFile )
        {
            this( productFile, getSourceP2InfFile( productFile ) );
        }

        public Product( File productFile, File p2infFile )
        {
            this.productFile = productFile;
            this.p2infFile = p2infFile;
        }

        public File getProductFile()
        {
            return productFile;
        }

        public File getP2infFile()
        {
            return p2infFile;
        }

        /**
         * We expect an p2 advice file called "xx.p2.inf" next to a product file "xx.product".
         */
        static File getSourceP2InfFile( File productFile )
        {
            final int indexOfExtension = productFile.getName().indexOf( ".product" );
            final String p2infFilename = productFile.getName().substring( 0, indexOfExtension ) + ".p2.inf";
            return new File( productFile.getParentFile(), p2infFilename );
        }

    }

    static void qualifyVersions( ProductConfiguration productConfiguration, String buildQualifier )
    {
        // we need to expand the version otherwise the published artifact still has the '.qualifier'
        String productVersion = productConfiguration.getVersion();
        if ( productVersion != null )
        {
            productVersion = productVersion.replace( VersioningHelper.QUALIFIER, buildQualifier );
            productConfiguration.setVersion( productVersion );
        }

        // now same for the features and bundles that version would be something else than "0.0.0"
        for ( FeatureRef featRef : productConfiguration.getFeatures() )
        {
            if ( featRef.getVersion() != null && featRef.getVersion().indexOf( VersioningHelper.QUALIFIER ) != -1 )
            {
                String newVersion = featRef.getVersion().replace( VersioningHelper.QUALIFIER, buildQualifier );
                featRef.setVersion( newVersion );
            }
        }
        for ( PluginRef plugRef : productConfiguration.getPlugins() )
        {
            if ( plugRef.getVersion() != null && plugRef.getVersion().indexOf( VersioningHelper.QUALIFIER ) != -1 )
            {
                String newVersion = plugRef.getVersion().replace( VersioningHelper.QUALIFIER, buildQualifier );
                plugRef.setVersion( newVersion );
            }
        }
    }

    String materializeRepository( MetadataSerializable metadataRepositorySerializable, File targetDirectory )
        throws IOException
    {
        File repositoryLocation = new File( targetDirectory, "targetMetadataRepository" );
        repositoryLocation.mkdirs();
        FileOutputStream stream = new FileOutputStream( new File( repositoryLocation, "content.xml" ) );
        try
        {
            metadataRepositorySerializable.serialize( stream );
        }
        finally
        {
            stream.close();
        }
        return repositoryLocation.toURI().toURL().toExternalForm();
    }

    /**
     * @return the value of the -configs argument: a list of config identifiers separated by a
     *         comma.
     */
    String[] getConfigsParameter( List<TargetEnvironment> envs )
    {
        if ( envs.isEmpty() )
        {
            return new String[0];
        }
        StringBuilder sb = new StringBuilder();
        for ( TargetEnvironment env : envs )
        {
            if ( sb.length() > 0 )
            {
                sb.append( "," );
            }
            sb.append( env.getWs() + "." + env.getOs() + "." + env.getArch() );
        }
        return new String[] { "-configs", sb.toString() };
    }

    /**
     * @return The value of the -metadataRepository and -artifactRepository (always the same for us
     *         so far)
     */
    private String getRepositoryUrl()
    {
        return getTargetRepositoryLocation().toURI().toString();
    }

    /**
     * @return The '-compress' flag or empty if we don't want to compress.
     */
    private String[] getCompressFlag()
    {
        return compress ? new String[] { "-compress" } : new String[0];
    }

    /**
     * Same code than in the ProductExportMojo. Needed to get the launcher binaries.
     */
    private String getEquinoxExecutableFeature()
        throws MojoExecutionException, MojoFailureException
    {
        ArtifactDescription artifact =
            getTargetPlatform().getArtifact( TychoProject.ECLIPSE_FEATURE, "org.eclipse.equinox.executable", null );

        if ( artifact == null )
        {
            throw new MojoExecutionException( "Unable to locate the equinox launcher feature (aka delta-pack)" );
        }

        File equinoxExecFeature = artifact.getLocation();
        if ( equinoxExecFeature.isDirectory() )
        {
            return equinoxExecFeature.getAbsolutePath();
        }
        else
        {
            File unzipped =
                new File( getProject().getBuild().getOutputDirectory(), artifact.getKey().getId() + "-"
                    + artifact.getKey().getVersion() );
            if ( unzipped.exists() )
            {
                return unzipped.getAbsolutePath();
            }
            try
            {
                // unzip now then:
                unzipped.mkdirs();
                deflater.setSourceFile( equinoxExecFeature );
                deflater.setDestDirectory( unzipped );
                deflater.extract();
                return unzipped.getAbsolutePath();
            }
            catch ( ArchiverException e )
            {
                throw new MojoFailureException( "Unable to unzip the eqiuinox executable feature", e );
            }
        }
    }

    private List<Product> getProducts()
    {
        List<Product> result = new ArrayList<Product>();
        for ( File productFile : getEclipseRepositoryProject().getProductFiles( getProject() ) )
        {
            result.add( new Product( productFile ) );
        }
        return result;
    }

}
