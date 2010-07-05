package org.sonatype.tycho.test.util;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;

public class InstallableUnitUtil
{

    public static IInstallableUnit createIU( String id, String version )
    {
        InstallableUnitDescription description = new InstallableUnitDescription();
        description.setId( id );
        description.setVersion( Version.create( version ) );
        return MetadataFactory.createInstallableUnit( description );
    }
}
