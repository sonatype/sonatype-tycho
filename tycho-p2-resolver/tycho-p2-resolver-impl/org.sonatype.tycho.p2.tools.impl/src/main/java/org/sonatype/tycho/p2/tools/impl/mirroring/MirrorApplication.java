package org.sonatype.tycho.p2.tools.impl.mirroring;

import org.eclipse.equinox.p2.core.IProvisioningAgent;

/**
 * {@link org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication} that uses a custom
 * {@link IProvisioningAgent}.
 */
@SuppressWarnings( "restriction" )
public class MirrorApplication
    extends org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication
{
    public MirrorApplication( IProvisioningAgent agent )
    {
        super();
        this.agent = agent;
        this.removeAddedRepositories = false;
    }
}
