package org.sonatype.tycho.p2.maven.repository.xstream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepDescriptor;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.CompositeClassLoader;
import com.thoughtworks.xstream.io.xml.XppDriver;

@SuppressWarnings( "restriction" )
public class ArtifactsIO
{
    private final XStream xs;

    public ArtifactsIO()
    {
        CompositeClassLoader cl = new CompositeClassLoader();
        cl.add( ArtifactDescriptor.class.getClassLoader() );

        xs = new XStream( null, new XppDriver(), cl );
        xs.setMode( XStream.NO_REFERENCES );

        xs.registerConverter( new VersionConverter() );

        xs.alias( "artifact", ArtifactDescriptor.class );
        xs.registerLocalConverter( ArtifactDescriptor.class, "properties", new PropertiesConverter() );
        xs.registerLocalConverter( ArtifactDescriptor.class, "repositoryProperties", new PropertiesConverter() );

        xs.alias( "key", ArtifactKey.class );
        xs.useAttributeFor( ArtifactKey.class, "id" );
        xs.useAttributeFor( ArtifactKey.class, "classifier" );
        xs.useAttributeFor( ArtifactKey.class, "version" );

        xs.aliasField( "processing", ArtifactDescriptor.class, "processingSteps" );
        xs.alias( "step", ProcessingStepDescriptor.class );
        xs.useAttributeFor( ProcessingStepDescriptor.class, "processorId" );
        xs.aliasAttribute( ProcessingStepDescriptor.class, "processorId", "id" );
        xs.useAttributeFor( ProcessingStepDescriptor.class, "required" );
    }

    @SuppressWarnings( "unchecked" )
    public Set<IArtifactDescriptor> readXML( InputStream is )
    {
        return (Set<IArtifactDescriptor>) xs.fromXML( is );
    }

    public void writeXML( Set<IArtifactDescriptor> descriptors, OutputStream os )
    {
        xs.toXML( descriptors, os );
    }

    public void writeXML( LinkedHashSet<IArtifactDescriptor> descriptors, File file )
        throws IOException
    {
        OutputStream os = new BufferedOutputStream( new FileOutputStream( file ) );
        try
        {
            writeXML( descriptors, os );
        }
        finally
        {
            os.close();
        }
    }
}
