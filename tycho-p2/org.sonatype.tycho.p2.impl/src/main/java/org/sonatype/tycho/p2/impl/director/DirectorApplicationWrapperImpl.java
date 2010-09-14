package org.sonatype.tycho.p2.impl.director;

import org.eclipse.equinox.internal.p2.director.app.DirectorApplication;
import org.sonatype.tycho.p2.DirectorApplicationWrapper;

@SuppressWarnings( "restriction" )
public final class DirectorApplicationWrapperImpl
    implements DirectorApplicationWrapper
{

    public Object run( String[] args )
    {
        return new DirectorApplication().run( args );
    }

}
