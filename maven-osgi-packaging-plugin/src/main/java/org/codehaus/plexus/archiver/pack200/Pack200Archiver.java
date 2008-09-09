package org.codehaus.plexus.archiver.pack200;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Packer;

import org.codehaus.plexus.archiver.AbstractArchiver;
import org.codehaus.plexus.archiver.ArchiverException;

public class Pack200Archiver
    extends AbstractArchiver
{

    private Map<? extends String, ? extends String> properties;

    private File sourceJar;

    public Map<? extends String, ? extends String> getProperties()
    {
        if ( properties == null )
        {
            return new LinkedHashMap<String, String>();
        }
        return properties;
    }

    public void setProperties( Map<? extends String, ? extends String> properties )
    {
        this.properties = properties;
    }

    public void createArchive()
        throws ArchiverException, IOException
    {
        // Create the Packer object
        Packer packer = Pack200.newPacker();

        packer.properties().putAll( getProperties() );
        try
        {
            JarFile jarFile = new JarFile( getSourceJar() );
            FileOutputStream fos = new FileOutputStream( getDestFile() );
            // Call the packer
            packer.pack( jarFile, fos );
            jarFile.close();
            fos.close();
        }
        catch ( IOException ioe )
        {
            ioe.printStackTrace();
        }

    }

    public File getSourceJar()
    {
        return this.sourceJar;
    }

    public void setSourceJar( File sourceJar )
    {
        this.sourceJar = sourceJar;
    }

}
