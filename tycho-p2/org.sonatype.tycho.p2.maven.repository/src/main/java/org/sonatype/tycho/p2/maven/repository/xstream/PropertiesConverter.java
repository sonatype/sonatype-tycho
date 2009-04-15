package org.sonatype.tycho.p2.maven.repository.xstream;

import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

@SuppressWarnings( "restriction" )
public class PropertiesConverter
    implements Converter
{
    public void marshal( Object value, HierarchicalStreamWriter writer, MarshallingContext context )
    {
        OrderedProperties properties = (OrderedProperties) value;

        for ( Map.Entry entry : (Set<Map.Entry>) properties.entrySet() )
        {
            writer.startNode( "property" );
            writer.addAttribute( "key", (String) entry.getKey() );
            writer.addAttribute( "value", (String) entry.getValue() );
            writer.endNode();
        }
    }

    public Object unmarshal( HierarchicalStreamReader reader, UnmarshallingContext context )
    {
        Class requiredType = context.getRequiredType();
        try
        {
            Map properties = (Map) requiredType.newInstance();
            while ( reader.hasMoreChildren() )
            {
                reader.moveDown();
                String name = reader.getAttribute( "key" );
                String value = reader.getAttribute( "value" );
                properties.put( name, value );
                reader.moveUp();
            }
            return properties;
        }
        catch ( InstantiationException e )
        {
            throw new ConversionException( "Cannot instantiate " + requiredType.getName(), e );
        }
        catch ( IllegalAccessException e )
        {
            throw new ConversionException( "Cannot instantiate " + requiredType.getName(), e );
        }
    }

    public boolean canConvert( Class clazz )
    {
        return Map.class.isAssignableFrom( clazz );
    }

}
