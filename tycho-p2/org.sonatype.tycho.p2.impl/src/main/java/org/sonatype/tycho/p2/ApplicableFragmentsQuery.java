package org.sonatype.tycho.p2;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.equinox.internal.p2.director.DirectorActivator;
import org.eclipse.equinox.internal.p2.director.TwoTierMap;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnitFragment;
import org.eclipse.equinox.internal.provisional.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.query.MatchQuery;
import org.osgi.framework.InvalidSyntaxException;

@SuppressWarnings( "restriction" )
public class ApplicableFragmentsQuery
    extends MatchQuery
{
    // see BundlesAction#CAPABILITY_NS_OSGI_BUNDLE
    private static final String CAPABILITY_NS_OSGI_BUNDLE = "osgi.bundle"; //$NON-NLS-1$

    // see BundlesAction#CAPABILITY_NS_OSGI_FRAGMENT
    private static final String CAPABILITY_NS_OSGI_FRAGMENT = "osgi.fragment"; //$NON-NLS-1$

    // (namespace,name) => Set<Version>
    private TwoTierMap capabilities = new TwoTierMap();

    private Dictionary selectionContext;

    public ApplicableFragmentsQuery( Collection<IInstallableUnit> units, Dictionary selectionContext )
    {
        this.selectionContext = selectionContext;
        
        for ( IInstallableUnit unit : units )
        {
            for ( IProvidedCapability provided : unit.getProvidedCapabilities() )
            {
                Set<Version> versions = (Set<Version>) capabilities.get( provided.getNamespace(), provided.getName() );
                if ( versions == null )
                {
                    versions = new HashSet<Version>();
                    capabilities.put( provided.getNamespace(), provided.getName(), versions );
                }
                versions.add( provided.getVersion() );
            }
        }
    }

    @Override
    public boolean isMatch( Object object )
    {
        if ( !( object instanceof IInstallableUnit ) )
        {
            return false;
        }

        IInstallableUnit candidate = (IInstallableUnit) object;

        if ( !isApplicable( candidate.getFilter() ) )
        {
            return false;
        }

        if ( candidate instanceof IInstallableUnitFragment )
        {
            IInstallableUnitFragment fragment = (IInstallableUnitFragment) candidate;

            for ( IRequiredCapability host : fragment.getHost() )
            {
                if ( isProvided( host ) )
                {
                    return true;
                }
            }
        }

        for ( IProvidedCapability provided : candidate.getProvidedCapabilities() )
        {
            if ( CAPABILITY_NS_OSGI_FRAGMENT.equals( provided.getNamespace() ) )
            {
                // find corresponding required capability
                for ( IRequiredCapability required : candidate.getRequiredCapabilities() )
                {
                    if ( CAPABILITY_NS_OSGI_BUNDLE.equals( required.getNamespace() )
                        && provided.getName().equals( required.getName() ) )
                    {
                        if ( isProvided( required ) )
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean isProvided( IRequiredCapability required )
    {
        Set<Version> versions = (Set<Version>) capabilities.get( required.getNamespace(), required.getName() );
        if ( versions == null )
        {
            return false;
        }

        for ( Version version : versions )
        {
            // TODO what is host filter?
            if ( version != null && required.getRange().isIncluded( version ) && isApplicable( required.getFilter() ) )
            {
                return true;
            }
        }

        return false;
    }

    protected boolean isApplicable( String filter )
    {
        if ( filter == null )
        {
            return true;
        }

        try
        {
            return DirectorActivator.context.createFilter( filter ).match( selectionContext );
        }
        catch ( InvalidSyntaxException e )
        {
            return false;
        }
    }

}
