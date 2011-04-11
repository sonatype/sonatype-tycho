package org.eclipse.tycho.surefire.osgibooter;

import java.util.StringTokenizer;

/**
 * Copy&paste from  org.codehaus.plexus.util.StringUtils
 * 
 * @author igor
 */
public class StringUtils {

	/**
     * <p>Joins the elements of the provided array into a single String
     * containing the provided list of elements.</p>
     *
     * <p>No delimiter is added before or after the list. A
     * <code>null</code> separator is the same as a blank String.</p>
     *
     * @param array the array of values to join together
     * @param separator the separator character to use
     * @return the joined String
     */
    public static String join( Object[] array, String separator )
    {
        if ( separator == null )
        {
            separator = "";
        }
        int arraySize = array.length;
        int bufSize = ( arraySize == 0 ? 0 : ( array[0].toString().length() +
            separator.length() ) * arraySize );
        StringBuffer buf = new StringBuffer( bufSize );

        for ( int i = 0; i < arraySize; i++ )
        {
            if ( i > 0 )
            {
                buf.append( separator );
            }
            buf.append( array[i] );
        }
        return buf.toString();
    }

    /**
     * @see #split(String, String, int)
     */
    public static String[] split( String text, String separator )
    {
        return split( text, separator, -1 );
    }

    /**
     * <p>Splits the provided text into a array, based on a given separator.</p>
     *
     * <p>The separator is not included in the returned String array. The
     * maximum number of splits to perfom can be controlled. A <code>null</code>
     * separator will cause parsing to be on whitespace.</p>
     *
     * <p>This is useful for quickly splitting a String directly into
     * an array of tokens, instead of an enumeration of tokens (as
     * <code>StringTokenizer</code> does).</p>
     *
     * @param str The string to parse.
     * @param separator Characters used as the delimiters. If
     *  <code>null</code>, splits on whitespace.
     * @param max The maximum number of elements to include in the
     *  array.  A zero or negative value implies no limit.
     * @return an array of parsed Strings
     */
    public static String[] split( String str, String separator, int max )
    {
        StringTokenizer tok = null;
        if ( separator == null )
        {
            // Null separator means we're using StringTokenizer's default
            // delimiter, which comprises all whitespace characters.
            tok = new StringTokenizer( str );
        }
        else
        {
            tok = new StringTokenizer( str, separator );
        }

        int listSize = tok.countTokens();
        if ( ( max > 0 ) && ( listSize > max ) )
        {
            listSize = max;
        }

        String[] list = new String[listSize];
        int i = 0;
        int lastTokenBegin = 0;
        int lastTokenEnd = 0;
        while ( tok.hasMoreTokens() )
        {
            if ( ( max > 0 ) && ( i == listSize - 1 ) )
            {
                // In the situation where we hit the max yet have
                // tokens left over in our input, the last list
                // element gets all remaining text.
                String endToken = tok.nextToken();
                lastTokenBegin = str.indexOf( endToken, lastTokenEnd );
                list[i] = str.substring( lastTokenBegin );
                break;
            }
            else
            {
                list[i] = tok.nextToken();
                lastTokenBegin = str.indexOf( list[i], lastTokenEnd );
                lastTokenEnd = lastTokenBegin + list[i].length();
            }
            i++;
        }
        return list;
    }

    /**
     * <p>Replace all occurances of a String within another String.</p>
     *
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     *
     * @see #replace(String text, String repl, String with, int max)
     * @param text text to search and replace in
     * @param repl String to search for
     * @param with String to replace with
     * @return the text with any replacements processed
     */
    public static String replace( String text, String repl, String with )
    {
        return replace( text, repl, with, -1 );
    }
    

    /**
     * <p>Replace a String with another String inside a larger String,
     * for the first <code>max</code> values of the search String.</p>
     *
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     *
     * @param text text to search and replace in
     * @param repl String to search for
     * @param with String to replace with
     * @param max maximum number of values to replace, or <code>-1</code> if no maximum
     * @return the text with any replacements processed
     */
    public static String replace( String text, String repl, String with, int max )
    {
        if ( ( text == null ) || ( repl == null ) || ( with == null ) || ( repl.length() == 0 ) )
        {
            return text;
        }

        StringBuffer buf = new StringBuffer( text.length() );
        int start = 0, end = 0;
        while ( ( end = text.indexOf( repl, start ) ) != -1 )
        {
            buf.append( text.substring( start, end ) ).append( with );
            start = end + repl.length();

            if ( --max == 0 )
            {
                break;
            }
        }
        buf.append( text.substring( start ) );
        return buf.toString();
    }
}
