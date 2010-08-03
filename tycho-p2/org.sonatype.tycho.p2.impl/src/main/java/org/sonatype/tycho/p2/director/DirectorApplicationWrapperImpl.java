package org.sonatype.tycho.p2.director;

import org.codehaus.tycho.p2.DirectorApplicationWrapper;
import org.eclipse.equinox.internal.p2.director.app.DirectorApplication;

@SuppressWarnings( "restriction" )
public final class DirectorApplicationWrapperImpl
    implements DirectorApplicationWrapper
{

    public Object run( String[] args )
    {
        return new DirectorApplication().run( args );
    }

}
