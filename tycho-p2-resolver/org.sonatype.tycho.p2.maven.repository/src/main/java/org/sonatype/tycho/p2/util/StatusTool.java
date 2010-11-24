package org.sonatype.tycho.p2.util;

import org.eclipse.core.runtime.IStatus;

public class StatusTool
{
    /**
     * Composes a status message out of the messages of the given status and its recursive children. Children with
     * status severity {@link IStatus#OK} don't add to the constructed message.
     */
    public static String collectProblems( IStatus status )
    {
        StringBuilder result = new StringBuilder();
        collectProblems( result, status );
        return result.toString();
    }

    private static void collectProblems( StringBuilder result, IStatus status )
    {
        result.append( '"' );
        result.append( status.getMessage() );
        result.append( '"' );
        IStatus[] children = status.getChildren();
        if ( children != null && children.length > 0 )
        {
            result.append( ": [" );
            for ( IStatus childStatus : children )
            {
                if ( !childStatus.isOK() )
                {
                    collectProblems( result, childStatus );
                }
                result.append( ", " );
            }
            result.setLength( result.length() - 2 );
            result.append( "]" );
        }
    }
}
