package org.sonatype.tycho.p2.impl.publisher.rootfiles;

final class SegmentHelper
{

    static boolean segmentEquals( String[] segments, int segmentIndex, String string )
    {
        if ( segmentIndex < segments.length )
        {
            return string.equals( segments[segmentIndex] );
        }
        else
        {
            return false;
        }
    }

    static String segmentsToString( String[] keySegments, char separator )
    {
        if ( keySegments.length == 0 )
        {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for ( String segment : keySegments )
        {
            result.append( segment );
            result.append( separator );
        }
        return result.substring( 0, result.length() - 1 );
    }

}
