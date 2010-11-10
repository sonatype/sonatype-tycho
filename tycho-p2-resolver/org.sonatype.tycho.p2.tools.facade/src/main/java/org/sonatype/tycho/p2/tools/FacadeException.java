package org.sonatype.tycho.p2.tools;

/**
 * Wrapper for checked exceptions from the OSGi world.
 */
public class FacadeException
    extends Exception
{
    private static final long serialVersionUID = 1864994424422146579L;

    public FacadeException( Throwable cause )
    {
        super( cause.getClass().getSimpleName() + " in OSGi bundle code", cause );
    }

    public FacadeException( String message, Throwable exception )
    {
        super( message, exception );
    }
}
