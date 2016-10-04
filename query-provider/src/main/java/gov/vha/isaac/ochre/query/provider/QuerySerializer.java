package gov.vha.isaac.ochre.query.provider;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Created by kec on 10/30/14.
 */
public class QuerySerializer {
    public static String marshall(Query q) throws JAXBException, IOException {
        //JAXBContext ctx = JaxbForQuery.get();
        q.setup();
        Marshaller marshaller = JaxbForQuery.get().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter builder = new StringWriter();
        marshaller.marshal(q, builder);
        return builder.toString();
    }

    public static Query unmarshall(Reader xmlData) throws JAXBException, ParserConfigurationException, Exception, Throwable {
        JAXBContext ctx = JaxbForQuery.get();

        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        //Query query = (Query) unmarshaller.unmarshal(xmlData);
        
        //To avoid XXE injection do not use unmarshal methods that process 
        //an XML source directly as java.io.File, java.io.Reader or java.io.InputStream. 
        //Parse the document with a securely configured parser and use an unmarshal method 
        //that takes the secure parser as the XML source
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        domFactory.setExpandEntityReferences(false);
        
        DocumentBuilder db = domFactory.newDocumentBuilder();
        InputSource source = new InputSource(xmlData);
        Document document = db.parse(source);
        Query query = (Query) unmarshaller.unmarshal(document);
        
        return query;
    }
}
