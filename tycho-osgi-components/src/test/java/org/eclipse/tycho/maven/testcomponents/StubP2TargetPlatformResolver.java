package org.eclipse.tycho.maven.testcomponents;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.TargetPlatformResolver;
import org.eclipse.tycho.osgitools.targetplatform.LocalTargetPlatformResolver;

// TODO romove me as part of TYCHO-527
@Component( role = TargetPlatformResolver.class, hint = "p2", instantiationStrategy = "per-lookup" )
public class StubP2TargetPlatformResolver
    extends LocalTargetPlatformResolver
{

}
