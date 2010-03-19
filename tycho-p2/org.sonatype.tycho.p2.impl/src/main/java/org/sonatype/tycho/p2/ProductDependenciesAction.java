package org.sonatype.tycho.p2;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionedName;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.osgi.service.environment.Constants;
import org.sonatype.tycho.p2.model.VersionedName2;

@SuppressWarnings( "restriction" )
public class ProductDependenciesAction
    extends AbstractDependenciesAction
{
    private final IProductDescriptor product;

    private final List<Properties> environments;

    public ProductDependenciesAction( IProductDescriptor product, List<Properties> environments )
    {
        this.product = product;
        this.environments = environments;
    }

    @Override
    protected Version getVersion()
    {
        return Version.create( product.getVersion() );
    }

    @Override
    protected String getId()
    {
        return product.getId();
    }

    @SuppressWarnings( "unchecked" )
    @Override
    protected Set<IRequiredCapability> getRequiredCapabilities()
    {
        Set<IRequiredCapability> required = new LinkedHashSet<IRequiredCapability>();

        if ( product.useFeatures() )
        {
            for ( VersionedName feature : (List<VersionedName>) product.getFeatures() )
            {
                String id = feature.getId() + FEATURE_GROUP_IU_SUFFIX; //$NON-NLS-1$
                Version version = feature.getVersion();

                addRequiredCapability( required, id, version, null );
            }
        }
        else
        {
            for ( VersionedName plugin : (List<VersionedName>) product.getBundles( true ) )
            {
                addRequiredCapability( required, plugin.getId(), plugin.getVersion(), getFilter( plugin ) );
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
        return required;
    }

    private String getFilter( VersionedName name )
    {
        if ( !( name instanceof VersionedName2 ) )
        {
            return null;
        }

        VersionedName2 name2 = (VersionedName2) name;
        return getFilter( name2.getOs(), name2.getWs(), name2.getArch() );
    }

    private void addNativeRequirements( Set<IRequiredCapability> required, String os, String ws, String arch )
    {
        String filter = getFilter( os, ws, arch );

        if ( Constants.OS_MACOSX.equals( os ) )
        {
            // macosx is twisted
            if ( Constants.ARCH_X86.equals( arch ) )
            {
                addRequiredCapability( required, "org.eclipse.equinox.launcher." + ws + "." + os, null, filter );
                return;
            }
        }

        addRequiredCapability( required, "org.eclipse.equinox.launcher." + ws + "." + os + "." + arch, null, filter );
    }

}
