package org.sonatype.tycho.p2.facade.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.codehaus.plexus.util.FileUtils;
import org.sonatype.tycho.p2.facade.RepositoryLayoutHelper;

public class LocalRepositoryReader
    implements RepositoryReader
{

    private final File basedir;

    public LocalRepositoryReader( File basedir )
    {
        this.basedir = basedir;
    }

    public InputStream getContents( GAV gav, String classifier, String extension )
        throws IOException
    {
        return getContents( RepositoryLayoutHelper.getRelativePath( gav, classifier, extension ) );
    }
    
    public InputStream getContents( String remoteRelpath )
        throws IOException
    {
        return new FileInputStream( new File( basedir, remoteRelpath ) );
    }

    public void retrieve( String remoteRelpath, File localDestination )
        throws IOException
    {
        FileUtils.copyFile( new File( basedir, remoteRelpath), localDestination );
    }

}
