package org.sonatype.tycho.test.util;

import java.util.Arrays;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.ProvidedCapability;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;

@SuppressWarnings( { "restriction", "nls" } )
public class InstallableUnitUtil
{

    public static IInstallableUnit createIU( String id, String version )
    {
        InstallableUnitDescription description = createIuDescription( id, version );
        return MetadataFactory.createInstallableUnit( description );
    }

    public static IInstallableUnit createIU( String id, String version, String capabilityId, String capabilityVersion )
    {
        InstallableUnitDescription description = createIuDescription( id, version );
        description.addProvidedCapabilities( Arrays.<IProvidedCapability> asList( new ProvidedCapability(
                                                                                                          IInstallableUnit.NAMESPACE_IU_ID,
                                                                                                          capabilityId,
                                                                                                          Version.create( capabilityVersion ) ) ) );
        return MetadataFactory.createInstallableUnit( description );
    }

    public static IInstallableUnit createIUArtifact( String id, String version, String artifactId,
                                                     String artifactVersion )
    {
        InstallableUnitDescription description = createIuDescription( id, version );
        description.setArtifacts( new IArtifactKey[] { new ArtifactKey( "type", artifactId,
                                                                        Version.create( artifactVersion ) ) } );
        return MetadataFactory.createInstallableUnit( description );
    }

    private static InstallableUnitDescription createIuDescription( String id, String version )
    {
        InstallableUnitDescription description = new InstallableUnitDescription();
        description.setId( id );
        description.setVersion( Version.create( version ) );
        return description;
    }
}
