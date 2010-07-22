package org.codehaus.tycho.maven;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;

/**
 * Workaround for {@see MavenExecutionRequest#setExecutionListener(ExecutionListener)} which does not allow to register
 * multiple execution listeners. All {@link ExecutionEvent}s will be forwarded to the list of registered listeners. Use
 * this class to "piggy-back" additional listeners on top of the original listener.
 * 
 * @author jan.sievers@sap.com
 */
public class MultiplexExecutionListener
    implements ExecutionListener
{


    private List<ExecutionListener> listeners = new ArrayList<ExecutionListener>();

    public MultiplexExecutionListener( ExecutionListener originalListener )
    {
        if (originalListener != null) {
            addListener( originalListener );
        }
    }

    public void projectDiscoveryStarted( ExecutionEvent event )
    {
        for ( ExecutionListener listener : listeners )
        {
            listener.projectDiscoveryStarted( event );
        }
    }

    public void sessionStarted( ExecutionEvent event )
    {
        for ( ExecutionListener listener : listeners )
        {
            listener.sessionStarted( event );
        }
    }

    public void sessionEnded( ExecutionEvent event )
    {
        for ( ExecutionListener listener : listeners )
        {
            listener.sessionEnded( event );
        }
    }

    public void projectSkipped( ExecutionEvent event )
    {
        for ( ExecutionListener listener : listeners )
        {
            listener.projectSkipped( event );
        }
    }

    public void projectStarted( ExecutionEvent event )
    {
        for ( ExecutionListener listener : listeners )
        {
            listener.projectStarted( event );
        }
    }

    public void projectSucceeded( ExecutionEvent event )
    {
        for ( ExecutionListener listener : listeners )
        {
            listener.projectSucceeded( event );
        }
    }

    public void projectFailed( ExecutionEvent event )
    {
        for ( ExecutionListener listener : listeners )
        {
            listener.projectFailed( event );
        }
    }

    public void mojoSkipped( ExecutionEvent event )
    {
        for ( ExecutionListener listener : listeners )
        {
            listener.mojoSkipped( event );
        }
    }

    public void mojoStarted( ExecutionEvent event )
    {
        for ( ExecutionListener listener : listeners )
        {
            listener.mojoStarted( event );
        }
    }

    public void mojoSucceeded( ExecutionEvent event )
    {
        for ( ExecutionListener listener : listeners )
        {
            listener.mojoSucceeded( event );
        }
    }

    public void mojoFailed( ExecutionEvent event )
    {
        for ( ExecutionListener listener : listeners )
        {
            listener.mojoFailed( event );
        }
    }

    public void forkStarted( ExecutionEvent event )
    {
        for ( ExecutionListener listener : listeners )
        {
            listener.forkStarted( event );
        }
    }

    public void forkSucceeded( ExecutionEvent event )
    {
        for ( ExecutionListener listener : listeners )
        {
            listener.forkSucceeded( event );
        }
    }

    public void forkFailed( ExecutionEvent event )
    {
        for ( ExecutionListener listener : listeners )
        {
            listener.forkFailed( event );
        }
    }

    public void forkedProjectStarted( ExecutionEvent event )
    {
        for ( ExecutionListener listener : listeners )
        {
            listener.forkedProjectStarted( event );
        }
    }

    public void forkedProjectSucceeded( ExecutionEvent event )
    {
        for ( ExecutionListener listener : listeners )
        {
            listener.forkedProjectSucceeded( event );
        }
    }

    public void forkedProjectFailed( ExecutionEvent event )
    {
        for ( ExecutionListener listener : listeners )
        {
            listener.forkedProjectFailed( event );
        }
    }

    /**
     * Add the specified listener to the list of listeners which will be notified by this
     * {@link MultiplexExecutionListener}.
     * 
     * @param listener
     */
    public void addListener( ExecutionListener listener )
    {
        listeners.add( listener );
    }

    /**
     * Remove the specified listener from the list of listeners which will be notified by this
     * {@link MultiplexExecutionListener}.
     * 
     * @param listener must not be <code>null</code>
     * @return <code>true</code> if listener was found and removed
     */
    public boolean removeListener( ExecutionListener listener )
    {
        for ( Iterator<ExecutionListener> iterator = listeners.iterator(); iterator.hasNext(); )
        {
            if ( listener.equals( iterator.next() ) )
            {
                iterator.remove();
                return true;
            }

        }
        return false;
    }
}
