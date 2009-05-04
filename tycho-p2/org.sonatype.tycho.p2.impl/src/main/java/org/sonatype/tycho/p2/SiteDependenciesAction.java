package org.sonatype.tycho.p2;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.updatesite.Activator;
import org.eclipse.equinox.internal.p2.updatesite.SiteFeature;
import org.eclipse.equinox.internal.p2.updatesite.UpdateSite;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.PublisherResult;

@SuppressWarnings( "restriction" )
public class SiteDependenciesAction
    extends AbstractPublisherAction
{
    private final File location;

    private final String id;

    private final String version;

    public SiteDependenciesAction( File location, String id, String version )
    {
        this.location = location;
        this.id = id;
        this.version = version;
    }

    @Override
    public IStatus perform( IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor )
    {
        try
        {
            UpdateSite updateSite = UpdateSite.load( location.toURI(), monitor );

            InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
            iud.setId( id );
            iud.setVersion( new Version( version ) );

            Set<IProvidedCapability> provided = new LinkedHashSet<IProvidedCapability>();
            provided.add( MetadataFactory.createProvidedCapability( IInstallableUnit.NAMESPACE_IU_ID, iud.getId(),
                                                                    iud.getVersion() ) );
            iud.addProvidedCapabilities( provided );

            Set<IRequiredCapability> required = new LinkedHashSet<IRequiredCapability>();
            for ( SiteFeature feature : updateSite.getSite().getFeatures() )
            {
                String id = feature.getFeatureIdentifier() + ".feature.group"; //$NON-NLS-1$
                String versionString = feature.getFeatureVersion();

                VersionRange range;
                if ( versionString == null || versionString.length() <= 0 || "0.0.0".equals( versionString ) )
                {
                    range = VersionRange.emptyRange;
                }
                else
                {
                    Version version = new Version( versionString );
                    range = new VersionRange( version, true, version, true );
                }

                required.add( MetadataFactory.createRequiredCapability( IInstallableUnit.NAMESPACE_IU_ID, id, range,
                                                                        null, false, false ) );
            }

            iud.addRequiredCapabilities( required );

            results.addIU( MetadataFactory.createInstallableUnit( iud ), PublisherResult.ROOT );
        }
        catch ( ProvisionException e )
        {
            return new Status( IStatus.ERROR, Activator.ID, "Error generating site xml action.", e );
        }
        catch ( OperationCanceledException e )
        {
            return Status.CANCEL_STATUS;
        }

        return Status.OK_STATUS;
    }
}
