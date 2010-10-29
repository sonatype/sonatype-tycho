package org.sonatype.tycho.equinox.launching;

public class EquinoxLaunchingException
    extends RuntimeException
{
    private static final long serialVersionUID = -2582656444738672521L;

    public EquinoxLaunchingException( Exception cause )
    {
        super( cause );
    }
}
