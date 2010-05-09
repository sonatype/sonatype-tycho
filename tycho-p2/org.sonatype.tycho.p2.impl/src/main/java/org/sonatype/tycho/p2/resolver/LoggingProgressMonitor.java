package org.sonatype.tycho.p2.resolver;

import org.eclipse.core.runtime.IProgressMonitor;
import org.sonatype.tycho.p2.facade.internal.P2Logger;

public class LoggingProgressMonitor
    implements IProgressMonitor
{

    private final P2Logger logger;

    public LoggingProgressMonitor( P2Logger logger )
    {
        this.logger = logger;
    }

    public void beginTask( String name, int totalWork )
    {
        logger.info( name );
    }

    public void done()
    {
    }

    public void internalWorked( double work )
    {
    }

    public boolean isCanceled()
    {
        return false;
    }

    public void setCanceled( boolean value )
    {
    }

    public void setTaskName( String name )
    {
    }

    public void subTask( String name )
    {
        logger.debug( name );
    }

    public void worked( int work )
    {
    }

}
