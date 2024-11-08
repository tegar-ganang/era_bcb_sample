package es.ua.qallme;

import java.io.InputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.ws.WebServiceContext;
import net.sf.qallme.WebServiceTools;
import net.sf.qallme.gen.ws.InternalServiceFault;
import net.sf.qallme.gen.ws.RDFGraph;
import net.sf.qallme.gen.ws.answerpool.AnswerPool;
import net.sf.qallme.gen.ws.answerpool.MalformedQueryFault;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Provides a web service implementation for the {@link AnswerPool} web service
 * interface.
 * 
  * @author Alicante Team
 * @version SVN $Rev$ by $Author$
 */
@WebService(name = "AnswerPool", serviceName = "AnswerPoolWS", portName = "AnswerPoolPort", targetNamespace = "http://qallme.sf.net/wsdl/answerpool.wsdl")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public class RDFAnswerPoolWSProvider implements AnswerPool {

    /** the web service context is injected by the application server */
    @Resource
    private WebServiceContext wsContext = null;

    /**
	 * Default constructor as needed by classes that are annotated with the
	 * {@link WebService} annotation.
	 */
    public RDFAnswerPoolWSProvider() {
        super();
    }

    /**
	 * Converts iso format into utf-8 format
	 * 
	 * @param s
	 *            Input string
	 * @return Modified string, now in utf-8 format
	 */
    private String convertFromUTF8(String s) {
        String out = null;
        try {
            out = new String(s.getBytes("UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            return null;
        }
        return out;
    }

    /**
	 * Converts the given RDF {@link Model} to an RDF/XML root {@link Element}
	 * which is returned.
	 * 
	 * @param model
	 *            the model to convert
	 * @return the RDF/XML root {@link Element} for the given model
	 * @throws InternalServiceFault
	 */
    private Element convertModelToDomElement(final Model model) throws InternalServiceFault, ParserConfigurationException, TransformerConfigurationException, TransformerException {
        Document doc = null;
        try {
            PipedInputStream pIn = new PipedInputStream();
            final PipedOutputStream pOut = new PipedOutputStream();
            pOut.connect(pIn);
            Thread writerThread = new Thread(new Runnable() {

                public void run() {
                    model.write(pOut, "RDF/XML-ABBREV");
                    try {
                        pOut.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            writerThread.start();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder domBuilder = factory.newDocumentBuilder();
            doc = domBuilder.parse(pIn);
            pIn.close();
            writerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            throw new InternalServiceFault("The constructed RDF/XML " + "graph doesn't appear to be well-formed.", e.getMessage(), e);
        } catch (IOException e) {
            throw new InternalServiceFault("Unexpected error.", e.getMessage(), e);
        }
        return doc.getDocumentElement();
    }

    /**
	 * Implementation of the {@link AnswerPool#getAnswers(String)} web method
	 * which for a SPARQL {@code CONSTRUCT} query returns an RDF answer graph.
	 * 
	 * @param sparqlQuery
	 *            the SPARQL query to use
	 * @return a (possibly empty) RDF answer graph
	 * @throws InternalServiceFault
	 *             if the service encounters an internal problem
	 * @throws MalformedQueryFault
	 *             if the given query string is not a valid SPARQL {@code
	 *             CONSTRUCT} query
	 * @see AnswerPool#getAnswers(String)
	 */
    @Override
    @WebMethod
    @WebResult(name = "RDFGraph", targetNamespace = "http://qallme.sf.net/xsd/qallmeshared.xsd", partName = "rdfGraph")
    public RDFGraph getAnswers(@WebParam(name = "string", targetNamespace = "http://qallme.sf.net/wsdl/qallmeshared.wsdl", partName = "str") String sparqlQuery) throws InternalServiceFault, MalformedQueryFault {
        sparqlQuery = convertFromUTF8(sparqlQuery);
        RDFGraph grafo = new RDFGraph();
        try {
            Query query = QueryFactory.create(sparqlQuery);
            InputStream in = null;
            InputStream inOnt = null;
            Model model = null;
            try {
                String fichero = "/res/instancias.rdf";
                String ficheroOnt = "/res/qallme-tourism.owl";
                in = WebServiceTools.getServletContext(this.wsContext).getResourceAsStream(fichero);
                inOnt = WebServiceTools.getServletContext(this.wsContext).getResourceAsStream(ficheroOnt);
            } catch (Exception e) {
                System.err.println("error lectura: " + e.toString());
                return null;
            }
            model = ModelFactory.createDefaultModel();
            model.read(in, "");
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            model.read(inOnt, "");
            try {
                inOnt.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            QueryExecution qe = QueryExecutionFactory.create(query, model);
            Model m = qe.execConstruct();
            qe.close();
            Element elemento = null;
            try {
                elemento = convertModelToDomElement(m);
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            }
            m.close();
            if (elemento != null) {
                grafo.setAny(elemento);
            } else {
                grafo = null;
            }
        } catch (QueryException e) {
            System.err.println(e.toString());
            throw new MalformedQueryFault("Malformed query input string", "MalformedQueryFault");
        }
        return grafo;
    }
}
