package org.eclipse.tycho.p2.impl.publisher;

import org.eclipse.equinox.internal.p2.updatesite.SiteModel;

@SuppressWarnings( "restriction" )
public class CategoryDependenciesAction
    extends AbstractSiteDependenciesAction
{
    private final SiteModel siteModel;

    public CategoryDependenciesAction( SiteModel siteModel, String id, String version )
    {
        super( id, version );
        this.siteModel = siteModel;
    }

    @Override
    SiteModel getSiteModel()
    {
        return this.siteModel;
    }

}
