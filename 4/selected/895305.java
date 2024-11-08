package tr.edu.metu.srdc.utils.xslt;

import java.io.FileWriter;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 *
 * @author yildiray
 */
public class SaxonXSLTEngine {

    /**
     * 
     * @param xslFilePath The path of the xsl document
     * @param xmlContent The xml document in String
     * @return 
     */
    public static String transformString(String xslFilePath, String xmlContent) {
        try {
            System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer(new StreamSource(xslFilePath));
            StringWriter swriter = new StringWriter();
            StringReader sreader = new StringReader(xmlContent);
            transformer.transform(new StreamSource(sreader), new StreamResult(swriter));
            return swriter.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * 
     * @param xslFilePath The path of the xsl document
     * @param xmlURL The url of the xml document
     * @param owlFilePath The path of the owl file to be generated
     */
    public static void transformURL(String xslFilePath, String xmlURL, String owlFilePath) {
        try {
            System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer(new StreamSource(xslFilePath));
            FileWriter fwriter = new FileWriter(owlFilePath);
            StreamSource sourceXML = new StreamSource(xmlURL);
            transformer.transform(sourceXML, new StreamResult(fwriter));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String argv[]) {
        SaxonXSLTEngine.transformURL(argv[0], argv[1], argv[2]);
    }
}
