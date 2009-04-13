package org.sonatype.tycho.p2.maven.repository.xstream;

import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;

import com.thoughtworks.xstream.converters.SingleValueConverter;

@SuppressWarnings("restriction")
public class VersionRangeConverter
    implements SingleValueConverter
{

    public Object fromString( String str )
    {
        return new VersionRange( str );
    }

    public String toString( Object obj )
    {
        VersionRange value = (VersionRange) obj;
        StringBuffer sb = new StringBuffer();
        value.toString( sb );
        return sb.toString();
    }

    public boolean canConvert( Class type )
    {
        return VersionRange.class.equals( type );
    }

}
