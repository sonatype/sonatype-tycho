package org.sonatype.tycho.p2.facade;

import java.io.IOException;
import java.io.OutputStream;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.tycho.TargetPlatform;
import org.sonatype.tycho.equinox.EquinoxServiceFactory;
import org.sonatype.tycho.p2.MetadataSerializable;

@Component( role = P2MetadataRepositoryWriter.class )
public class P2MetadataRepositoryWriter
{

    @Requirement
    private EquinoxServiceFactory equinox;

    public void write( OutputStream out, TargetPlatform platform, String qualifier )
        throws IOException
    {
        MetadataSerializable serializer = equinox.getService( MetadataSerializable.class );
        serializer.serialize( out, platform.getInstallableUnits(), qualifier );
    }
}
