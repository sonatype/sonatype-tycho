package org.sonatype.tycho.p2.tools.impl.mirroring;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.mirroring.IArtifactMirrorLog;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;
import org.eclipse.equinox.p2.internal.repository.tools.SlicingOptions;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.sonatype.tycho.p2.tools.BuildContext;
import org.sonatype.tycho.p2.tools.FacadeException;
import org.sonatype.tycho.p2.tools.RepositoryReferences;
import org.sonatype.tycho.p2.tools.TargetEnvironment;
import org.sonatype.tycho.p2.tools.impl.Activator;
import org.sonatype.tycho.p2.tools.mirroring.MirrorApplicationService;
import org.sonatype.tycho.p2.util.StatusTool;

@SuppressWarnings( "restriction" )
public class MirrorApplicationServiceImpl
    implements MirrorApplicationService
{

    private static final String MIRROR_FAILURE_MESSAGE = "Copying p2 repository content failed";

    public void mirror( RepositoryReferences sources, File destination, Collection<?> rootUnits, BuildContext context,
                        int flags )
        throws FacadeException
    {
        IProvisioningAgent agent = Activator.createProvisioningAgent( context.getTargetDirectory() );
        try
        {
            final MirrorApplication mirrorApp = new MirrorApplication( agent );

            setSourceRepositories( mirrorApp, sources );

            final RepositoryDescriptor destinationDescriptor = new RepositoryDescriptor();
            destinationDescriptor.setLocation( destination.toURI() );
            destinationDescriptor.setAppend( true );
            destinationDescriptor.setCompressed( ( flags & REPOSITORY_COMPRESS ) != 0 );
            mirrorApp.addDestination( destinationDescriptor );

            mirrorApp.setSourceIUs( toInstallableUnitList( rootUnits ) );

            final SlicingOptions options = new SlicingOptions();
            options.considerStrictDependencyOnly( true );

            if ( context.getEnvironments().size() == 0 )
            {
                // don't mirror any environment specific units
                // TODO is this what we want?
                Map<String, String> filter = new HashMap<String, String>();
                addFilterForFeatureJARs( filter );

                // PermissiveSlicer only considers filters if there is _more_ than one property
                // TODO Proper fix at Eclipse?
                filter.put( "dummy.abc", "false" );
                options.setFilter( filter );
                options.forceFilterTo( false );
                executeMirroring( mirrorApp, options );
            }
            else
            {
                for ( TargetEnvironment environment : context.getEnvironments() )
                {
                    Map<String, String> filter = environment.toFilter();
                    addFilterForFeatureJARs( filter );
                    options.setFilter( filter );

                    executeMirroring( mirrorApp, options );
                }
            }
        }
        finally
        {
            agent.stop();
        }
    }

    /**
     * Set filter value so that the feature JAR units and artifacts are included when mirroring.
     */
    private static void addFilterForFeatureJARs( Map<String, String> filter )
    {
        filter.put( "org.eclipse.update.install.features", "true" );
    }

    private void executeMirroring( MirrorApplication mirrorApp, SlicingOptions options )
        throws FacadeException
    {
        try
        {
            LogListener logListener = new LogListener();
            mirrorApp.setLog( logListener );
            // mirrorApp.setValidate( true ); // TODO Broken; fix at Eclipse

            mirrorApp.setSlicingOptions( options );

            IStatus returnStatus = mirrorApp.run( null );
            checkStatus( returnStatus );
            /*
             * Treat the slicer warnings (typically "unable to satisfy dependency") as errors
             * because some expected content is missing.
             */
            for ( IStatus logStatus : logListener.getSlicerProblems() )
            {
                checkStatus( logStatus );
            }
        }
        catch ( ProvisionException e )
        {
            throw new FacadeException( MIRROR_FAILURE_MESSAGE + ": " + StatusTool.collectProblems( e.getStatus() ), e );
        }
    }

    private static void setSourceRepositories( MirrorApplication mirrorApp, RepositoryReferences sources )
    {
        setSourceRepositories( mirrorApp, sources.getMetadataRepositories(), RepositoryDescriptor.KIND_METADATA );
        setSourceRepositories( mirrorApp, sources.getArtifactRepositories(), RepositoryDescriptor.KIND_ARTIFACT );
    }

    private static void setSourceRepositories( MirrorApplication mirrorApp, Collection<URI> repositoryLocations,
                                               String repositoryKind )
    {
        for ( URI repositoryLocation : repositoryLocations )
        {
            RepositoryDescriptor repository = new RepositoryDescriptor();
            repository.setKind( repositoryKind );
            repository.setLocation( repositoryLocation );
            mirrorApp.addSource( repository );
        }
    }

    private static List<IInstallableUnit> toInstallableUnitList( Collection<?> units )
    {
        List<IInstallableUnit> result = new ArrayList<IInstallableUnit>( units.size() );
        for ( Object unit : units )
        {
            result.add( (IInstallableUnit) unit );
        }
        return result;
    }

    private static void checkStatus( IStatus status )
        throws FacadeException
    {
        if ( !status.isOK() )
        {
            throw new FacadeException( MIRROR_FAILURE_MESSAGE + ": " + StatusTool.collectProblems( status ),
                                       status.getException() );
        }
    }

    static class LogListener
        implements IArtifactMirrorLog
    {
        List<IStatus> entries;

        public void log( IArtifactDescriptor descriptor, IStatus status )
        {
            // artifact comparator result -> ignore
        }

        public void log( IStatus status )
        {
            if ( entries == null )
                entries = new ArrayList<IStatus>();
            entries.add( status );
        }

        public void close()
        {
        }

        List<IStatus> getSlicerProblems()
        {
            // TODO request from Eclipse that they identify the slicer warnings with a dedicated code
            return entries;
        }
    }
}
