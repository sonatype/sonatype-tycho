package org.sonatype.tycho.p2.impl.test;

import org.eclipse.core.runtime.IProgressMonitor;

public class ConsoleProgressMonitor
    implements IProgressMonitor
{

    public void beginTask( String name, int totalWork )
    {
        System.out.println( name );
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
        System.out.println( name );
    }

    public void subTask( String name )
    {
        System.out.println( name );
    }

    public void worked( int work )
    {
    }

}
