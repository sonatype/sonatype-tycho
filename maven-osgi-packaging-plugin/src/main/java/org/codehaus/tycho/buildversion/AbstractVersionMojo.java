package org.codehaus.tycho.buildversion;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.BundleResolutionState;
import org.codehaus.tycho.FeatureResolutionState;
import org.codehaus.tycho.ProjectType;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.osgitools.features.FeatureDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;

public abstract class AbstractVersionMojo
    extends AbstractMojo
{

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter default-value="${project.packaging}"
     * @required
     * @readonly
     */
    protected String packaging;

    /**
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;

    protected String getOSGiVersion()
    {
        String version = null;

        if ( ProjectType.ECLIPSE_PLUGIN.equals( packaging ) || ProjectType.ECLIPSE_TEST_PLUGIN.endsWith( packaging ) )
        {
            BundleResolutionState bundleResolutionState =
                (BundleResolutionState) project.getContextValue( TychoConstants.CTX_BUNDLE_RESOLUTION_STATE );
            BundleDescription bundle = bundleResolutionState.getBundleByLocation( project.getBasedir() );

            version = VersioningHelper.getExpandedVersion( session, bundle );

        }
        else if ( ProjectType.ECLIPSE_FEATURE.equals( packaging ) )
        {
            FeatureResolutionState featureResolutionState =
                (FeatureResolutionState) project.getContextValue( TychoConstants.CTX_FEATURE_RESOLUTION_STATE );
            FeatureDescription featureDesc = featureResolutionState.getFeatureByLocation( project.getBasedir() );

            version = VersioningHelper.getExpandedVersion( session, featureDesc );
        }
        return version;
    }
    
}
