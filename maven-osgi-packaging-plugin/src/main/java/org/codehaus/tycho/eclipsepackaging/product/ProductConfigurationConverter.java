package org.codehaus.tycho.eclipsepackaging.product;

import java.lang.reflect.Field;
import java.util.Iterator;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.maven.plugin.logging.Log;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class ProductConfigurationConverter implements Converter {

	private Log log;

	public ProductConfigurationConverter(Log log) {
		this();
		this.log = log;
	}

	public ProductConfigurationConverter() {
		super();
	}

	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		context.convertAnother(source);
	}

	@SuppressWarnings("unchecked")
	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		ProductConfiguration cfg = new ProductConfiguration();
		Field[] fields = ProductConfiguration.class.getDeclaredFields();

		Iterator attributeNames = reader.getAttributeNames();
		while (attributeNames.hasNext()) {
			String name = (String) attributeNames.next();
			Field field = getField(name, fields);
			if (field == null) {
				continue;
			}
			// Object value = context.convertAnother(name, field.getType());
			String value = reader.getAttribute(name);
			try {
				BeanUtils.setProperty(cfg, name, value);
				// field.set(cfg, value);
			} catch (Exception e) {
				throw new ConversionException(e);
			}
		}

		while (reader.hasMoreChildren()) {
			try {
				reader.moveDown();
				String name = reader.getNodeName();
				Field field = getField(name, fields);
				if (field == null) {
					continue;
				}

				Object value = context.convertAnother(name, field.getType());
				try {
					field.set(cfg, value);
				} catch (Exception e) {
					throw new ConversionException(e);
				}
			} finally {
				reader.moveUp();
			}
		}

		return cfg;
	}

	private Field getField(String name, Field[] fields) {
		Field field = null;
		for (Field f : fields) {
			if (f.getName().equals(name)) {
				field = f;
				break;
			}
		}
		if (field == null) {
			if (log != null) {
				log.warn("Tycho doesn't handle '" + name + "' property.");
			}
			return null;
		}

		field.setAccessible(true);
		return field;
	}

	@SuppressWarnings("unchecked")
	public boolean canConvert(Class type) {
		return type == ProductConfiguration.class;
	}

}
