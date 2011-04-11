package org.eclipse.tycho.plugins.p2;


/**
 * @goal update-site-p2-metadata
 */
public class UpdateSiteP2MetadataMojo
    extends AbstractP2MetadataMojo
{
    protected String getPublisherApplication()
    {
        return "org.eclipse.equinox.p2.publisher.UpdateSitePublisher";
    }
}
