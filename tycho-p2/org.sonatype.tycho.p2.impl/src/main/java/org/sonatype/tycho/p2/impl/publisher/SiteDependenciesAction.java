package org.sonatype.tycho.p2.impl.publisher;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.updatesite.Activator;
import org.eclipse.equinox.internal.p2.updatesite.SiteFeature;
import org.eclipse.equinox.internal.p2.updatesite.UpdateSite;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;

@SuppressWarnings( "restriction" )
public class SiteDependenciesAction
    extends AbstractDependenciesAction
{
    private final File location;

    private final String id;

    private final String version;

    private UpdateSite updateSite;

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
            updateSite = UpdateSite.load( location.toURI(), monitor );
        }
        catch ( ProvisionException e )
        {
            return new Status( IStatus.ERROR, Activator.ID, "Error generating site xml action.", e );
        }

        return super.perform( publisherInfo, results, monitor );
    }

    @Override
    protected Set<IRequirement> getRequiredCapabilities()
    {
        Set<IRequirement> required = new LinkedHashSet<IRequirement>();

        for ( SiteFeature feature : updateSite.getSite().getFeatures() )
        {
            String id = feature.getFeatureIdentifier() + FEATURE_GROUP_IU_SUFFIX; //$NON-NLS-1$

            VersionRange range = getVersionRange( createVersion( feature.getFeatureVersion() ) );

            required.add( MetadataFactory.createRequirement( IInstallableUnit.NAMESPACE_IU_ID, id, range, null,
                                                                    false, false ) );
        }
        return required;
    }

    @Override
    protected String getId()
    {
        return id;
    }

    @Override
    protected Version getVersion()
    {
        return createSiteVersion( version );
    }

    public static Version createSiteVersion( String version )
    {
        try
        {
            // try default (OSGi?) format first
            return Version.create( version );
        }
        catch ( IllegalArgumentException e )
        {
            // treat as raw otherwise
            return Version.create( "format(n[.n=0;[.n=0;['-'S]]]):" + version );
        }
    }
}
