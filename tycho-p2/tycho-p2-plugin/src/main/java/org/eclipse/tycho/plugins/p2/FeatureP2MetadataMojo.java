package org.eclipse.tycho.plugins.p2;

/**
 * @goal feature-p2-metadata
 */
public class FeatureP2MetadataMojo
    extends AbstractP2MetadataMojo
{
    @Override
    protected String getPublisherApplication()
    {
        return "org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher";
    }
}
