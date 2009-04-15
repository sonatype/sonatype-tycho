package org.sonatype.tycho.p2.maven.repository.xstream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.ProvidedCapability;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.TouchpointType;
import org.eclipse.equinox.internal.p2.metadata.UpdateDescriptor;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.CompositeClassLoader;
import com.thoughtworks.xstream.io.xml.XppDriver;

@SuppressWarnings( "restriction" )
public class MetadataIO
{
    private final XStream xs;

    public MetadataIO()
    {
        CompositeClassLoader cl = new CompositeClassLoader();
        cl.add( InstallableUnit.class.getClassLoader() );

        xs = new XStream( null, new XppDriver(), cl );
        xs.setMode( XStream.NO_REFERENCES );

        xs.alias( "unit", InstallableUnit.class );
        xs.useAttributeFor( InstallableUnit.class, "id" );
        xs.useAttributeFor( InstallableUnit.class, "singleton" );
        xs.useAttributeFor( InstallableUnit.class, "version" );

        xs.aliasField( "provides", InstallableUnit.class, "providedCapabilities" );
        xs.alias( "provided", ProvidedCapability.class );
        xs.useAttributeFor( ProvidedCapability.class, "name" );
        xs.useAttributeFor( ProvidedCapability.class, "namespace" );
        xs.useAttributeFor( ProvidedCapability.class, "version" );

        xs.alias( "required", RequiredCapability.class );
        xs.useAttributeFor( RequiredCapability.class, "name" );
        xs.useAttributeFor( RequiredCapability.class, "namespace" );
        xs.useAttributeFor( RequiredCapability.class, "range" );
        xs.useAttributeFor( RequiredCapability.class, "multiple" );
        xs.useAttributeFor( RequiredCapability.class, "optional" );
        xs.useAttributeFor( RequiredCapability.class, "greedy" );

        xs.alias( "update", UpdateDescriptor.class );
        xs.useAttributeFor( UpdateDescriptor.class, "id" );
        xs.useAttributeFor( UpdateDescriptor.class, "range" );
        xs.useAttributeFor( UpdateDescriptor.class, "severity" );

        xs.alias( "artifact", ArtifactKey.class );
        xs.useAttributeFor( ArtifactKey.class, "id" );
        xs.useAttributeFor( ArtifactKey.class, "classifier" );
        xs.useAttributeFor( ArtifactKey.class, "version" );

        xs.alias( "touchpoint", TouchpointType.class );
        xs.useAttributeFor( TouchpointType.class, "id" );
        xs.useAttributeFor( TouchpointType.class, "version" );

        xs.registerConverter( new VersionConverter() );
        xs.registerConverter( new VersionRangeConverter() );
        xs.registerLocalConverter( InstallableUnit.class, "properties", new PropertiesConverter() );
    }

    @SuppressWarnings( "unchecked" )
    public Set<IInstallableUnit> readXML( InputStream is )
    {
        return (Set<IInstallableUnit>) xs.fromXML( is );
    }

    public void writeXML( Set<IInstallableUnit> units, OutputStream os )
    {
        xs.toXML( units, os );
    }

    public void writeXML( Set<IInstallableUnit> units, File file )
        throws IOException
    {
        OutputStream os = new BufferedOutputStream( new FileOutputStream( file ) );
        try
        {
            writeXML( units, os );
        }
        finally
        {
            os.close();
        }
    }
}
