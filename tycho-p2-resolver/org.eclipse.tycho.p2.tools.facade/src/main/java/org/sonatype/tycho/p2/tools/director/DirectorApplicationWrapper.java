package org.sonatype.tycho.p2.tools.director;

public interface DirectorApplicationWrapper
{
    /**
     * @see org.eclipse.equinox.app.IApplication#EXIT_OK
     */
    public static final Integer EXIT_OK = Integer.valueOf( 0 );

    /**
     * @see org.eclipse.equinox.internal.p2.director.app.DirectorApplication#run(String[] )
     */
    Object run( String[] args );
}
