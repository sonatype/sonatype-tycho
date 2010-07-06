package org.sonatype.tycho.test.TYCHO406testArgLineConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlUtil
{

    public static Document readFrom( File f )
        throws IOException, SAXException, ParserConfigurationException
    {
        FileInputStream fis = new FileInputStream( f );
        try
        {
            InputSource in = new InputSource( fis );
            in.setSystemId( "" );
            return readFrom( in );
        }
        finally
        {
            fis.close();
        }
    }

    private static Document readFrom( InputSource in )
        throws ParserConfigurationException, SAXException, IOException
    {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating( false );
        factory.setNamespaceAware( true );
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        docBuilder.setEntityResolver( new EntityResolver()
        {
            public InputSource resolveEntity( String publicId, String systemId )
            {
                return new InputSource( new StringReader( "<?xml version='1.0' encoding='UTF-8'?>" ) );
            }
        } );
        return docBuilder.parse( in );
    }
}
