package org.codehaus.tycho.maven;

import org.apache.maven.classrealm.ClassRealmManagerDelegate;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component( role = ClassRealmManagerDelegate.class )
public class TychoClassRealmManagerDelegate
    implements ClassRealmManagerDelegate
{
    @Requirement
    private PlexusContainer plexus;

    public void setupRealm( ClassRealm realm )
    {
        ClassRealm coreRealm = plexus.getContainerRealm();

        realm.importFrom( coreRealm, "org.codehaus.tycho" );
        realm.importFrom( coreRealm, "org.sonatype.tycho" );

        realm.importFrom( coreRealm, "org.osgi.framework" );
        realm.importFrom( coreRealm, "org.eclipse.osgi" );
    }

}
