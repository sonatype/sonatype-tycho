package org.codehaus.tycho.testing;

import java.util.Collection;

public class CompoundRuntimeException
    extends RuntimeException
{
    private static final long serialVersionUID = 4566763905270086193L;

    private final Collection<Exception> causes;

    public CompoundRuntimeException( Collection<Exception> causes )
    {
        this.causes = causes;

        if ( causes != null && causes.size() > 0 )
        {
            initCause( causes.iterator().next() );
        }
    }
    
    @Override
    public String getMessage()
    {
        if ( causes == null )
        {
            return super.getMessage();
        }

        StringBuilder sb = new StringBuilder();

        for ( Throwable t : causes )
        {
            sb.append( t.getMessage() ).append( "\n" );
        }

        return sb.toString();
    }
}
