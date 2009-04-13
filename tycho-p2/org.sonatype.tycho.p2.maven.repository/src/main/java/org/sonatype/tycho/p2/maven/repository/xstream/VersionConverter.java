package org.sonatype.tycho.p2.maven.repository.xstream;

import org.eclipse.equinox.internal.provisional.p2.core.Version;

import com.thoughtworks.xstream.converters.SingleValueConverter;

@SuppressWarnings( "restriction" )
public class VersionConverter
    implements SingleValueConverter
{

    public boolean canConvert( Class clazz )
    {
        return Version.class.equals( clazz );
    }

    public Object fromString( String string )
    {
        return Version.parseVersion( string );
    }

    public String toString( Object value )
    {
        Version version = (Version) value;
        StringBuffer sb = new StringBuffer();
        version.toString( sb );
        return sb.toString();
    }

}
