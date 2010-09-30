package org.sonatype.tycho.p2.impl.resolver;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.query.IQueryable;

public abstract class ResolutionStrategy
{
    protected IQueryable<IInstallableUnit> availableIUs;

    protected Set<IInstallableUnit> rootIUs;

    protected List<IRequirement> additionalRequirements;

    public void setAvailableInstallableUnits( IQueryable<IInstallableUnit> availableIUs )
    {
        this.availableIUs = availableIUs;
    }

    public void setRootInstallableUnits( Set<IInstallableUnit> rootIUs )
    {
        this.rootIUs = rootIUs;
    }

    public void setAdditionalRequirements( List<IRequirement> additionalRequirements )
    {
        this.additionalRequirements = additionalRequirements;
    }

    public abstract Collection<IInstallableUnit> resolve( IProgressMonitor monitor );
}
