package org.sonatype.tycho.p2;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.core.VersionedName;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.osgi.service.environment.Constants;

@SuppressWarnings( "restriction" )
public class ProductDependenciesAction
    extends AbstractPublisherAction
{
    static final Version OSGi_versionMin = Version.createOSGi( 0, 0, 0 );

    // copy&paste from e3.5.1 org.eclipse.osgi.internal.resolver.StateImpl
    private static final String OSGI_OS = "osgi.os"; //$NON-NLS-1$

    private static final String OSGI_WS = "osgi.ws"; //$NON-NLS-1$

    private static final String OSGI_ARCH = "osgi.arch"; //$NON-NLS-1$

    private final IProductDescriptor product;

    private final List<Properties> environments;

    public ProductDependenciesAction( IProductDescriptor product, List<Properties> environments )
    {
        this.product = product;
        this.environments = environments;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public IStatus perform( IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor )
    {
        InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
        iud.setId( product.getId() );
        iud.setVersion( Version.create( product.getVersion() ) );

        Set<IProvidedCapability> provided = new LinkedHashSet<IProvidedCapability>();
        provided.add( MetadataFactory.createProvidedCapability( IInstallableUnit.NAMESPACE_IU_ID, iud.getId(),
                                                                iud.getVersion() ) );
        iud.addProvidedCapabilities( provided );

        Set<IRequiredCapability> required = new LinkedHashSet<IRequiredCapability>();

        if ( product.useFeatures() )
        {
            for ( VersionedName feature : (List<VersionedName>) product.getFeatures() )
            {
                String id = feature.getId() + ".feature.group"; //$NON-NLS-1$
                Version version = feature.getVersion();

                addRequiredCapability( required, id, version, null );
            }
        }
        else
        {
            for ( VersionedName plugin : (List<VersionedName>) product.getBundles( true ) )
            {
                addRequiredCapability( required, plugin.getId(), plugin.getVersion(), null );
            }
        }

        // TODO include include when includeLaunchers=true (includeLaunchers is not exposed by IProductDescriptor)
        addRequiredCapability( required, "org.eclipse.equinox.executable.feature.group", null, null );

        // these are implicitly required, see
        // See also org.codehaus.tycho.osgitools.AbstractArtifactDependencyWalker.traverseProduct
        addRequiredCapability( required, "org.eclipse.equinox.launcher", null, null );
        if ( environments != null )
        {
            for ( Properties env : environments )
            {
                addNativeRequirements( required, env.getProperty( OSGI_OS ), env.getProperty( OSGI_WS ),
                                       env.getProperty( OSGI_ARCH ) );
            }
        }

        iud.addRequiredCapabilities( required );

        results.addIU( MetadataFactory.createInstallableUnit( iud ), PublisherResult.ROOT );

        return Status.OK_STATUS;
    }

    private void addNativeRequirements( Set<IRequiredCapability> required, String os, String ws, String arch )
    {
        String filter = getFilter( os, ws, arch );

        if ( Constants.OS_MACOSX.equals( os ) && Constants.WS_CARBON.equals( ws ) )
        {
            addRequiredCapability( required, "org.eclipse.equinox.launcher." + ws + "." + os, null, filter );
        }
        else
        {
            addRequiredCapability( required, "org.eclipse.equinox.launcher." + ws + "." + os + "." + arch, null, filter );
        }
    }

    protected String getFilter( String os, String ws, String arch )
    {
        ArrayList<String> conditions = new ArrayList<String>();

        if ( os != null )
        {
            conditions.add( OSGI_OS + "=" + os );
        }
        if ( ws != null )
        {
            conditions.add( OSGI_WS + "=" + ws );
        }
        if ( arch != null )
        {
            conditions.add( OSGI_ARCH + "=" + arch );
        }

        if ( conditions.isEmpty() )
        {
            return null;
        }

        if ( conditions.size() == 1 )
        {
            return conditions.get( 0 );
        }

        StringBuilder filter = new StringBuilder( "(&" );
        for ( String condition : conditions )
        {
            filter.append( " (" ).append( condition ).append( ")" );
        }
        filter.append( " )" );

        return filter.toString();
    }

    protected void addRequiredCapability( Set<IRequiredCapability> required, String id, Version version, String filter )
    {
        VersionRange range;
        if ( version == null || OSGi_versionMin.equals( version ) )
        {
            range = VersionRange.emptyRange;
        }
        else
        {
            range = new VersionRange( version, true, version, true );
        }

        required.add( MetadataFactory.createRequiredCapability( IInstallableUnit.NAMESPACE_IU_ID, id, range, filter,
                                                                false, false ) );
    }

}
