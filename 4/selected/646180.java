package de.dfki.qallme;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.ws.WebServiceContext;
import net.sf.qallme.WebServiceTools;
import net.sf.qallme.gen.ws.InternalServiceFault;
import net.sf.qallme.gen.ws.ObjectFactory;
import net.sf.qallme.gen.ws.RDFGraph;
import net.sf.qallme.gen.ws.answerpool.AnswerPool;
import net.sf.qallme.gen.ws.answerpool.MalformedQueryFault;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
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
 * @author Christian Spurk (cspurk@dfki.de)
 * @version SVN $Rev$ by $Author$
 */
@WebService(name = "AnswerPool", serviceName = "AnswerPoolWS", portName = "AnswerPoolPort", targetNamespace = "http://qallme.sf.net/wsdl/answerpool.wsdl")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public class RDFAnswerPoolWSProvider implements AnswerPool {

    /** relative server paths to the RDF files to retrieve answers from */
    private static final String[] RDF_REPOSITORY_PATHS = new String[] { "/res/20070318_cinema_data_v4.1format.rdf" };

    /** relative server path to the QALL-ME ontology file */
    private static final String ONTOLOGY_PATH = "/res/qallme-tourism.owl";

    /** relative server path to the QALL-ME answers ontology */
    private static final String ANSWERS_ONTOLOGY_PATH = "/res/qallme-answers.owl";

    /** a factory for creating {@link RDFGraph} objects */
    private static final ObjectFactory OBJ_FACTORY = new ObjectFactory();

    /** the web service context is injected by the server */
    @Resource
    private WebServiceContext wsContext = null;

    /** the RDF model from which we return answer graphs */
    private Model answerPoolModel = null;

    /** a DOM builder which we need to create proper return results */
    private DocumentBuilder domBuilder = null;

    /**
	 * the path prefix to apply to internal resource file paths; if {@code null}
	 * , then resource file paths are resolved using a servlet context
	 */
    private String resourcePathPrefix = null;

    /**
	 * Constructs an {@link RDFAnswerPoolWSProvider} for offline usage.
	 * 
	 * @param resourcePathPrefix
	 *            the path prefix to apply to internal resource file paths
	 */
    public RDFAnswerPoolWSProvider(String resourcePathPrefix) {
        this.resourcePathPrefix = resourcePathPrefix;
    }

    /**
	 * Default constructor as needed by classes that are annotated with the
	 * {@link WebService} annotation.
	 */
    public RDFAnswerPoolWSProvider() {
        super();
    }

    /**
	 * (Re-)Initializes the class or rather its member variables.
	 * 
	 * @throws InternalServiceFault
	 *             if there is a problem initializing the web service
	 */
    private synchronized void initialize() throws InternalServiceFault {
        if (this.answerPoolModel != null) return;
        DocumentBuilderFactory docBuilderFact = DocumentBuilderFactory.newInstance();
        docBuilderFact.setIgnoringComments(true);
        docBuilderFact.setNamespaceAware(true);
        try {
            this.domBuilder = docBuilderFact.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new InternalServiceFault("The web service could " + "not be initialized.", e.getMessage(), e);
        }
        this.answerPoolModel = ModelFactory.createDefaultModel();
        try {
            ServletContext sContext = this.resourcePathPrefix == null ? WebServiceTools.getServletContext(wsContext) : null;
            addRDFResourceToModel(sContext, ONTOLOGY_PATH, this.answerPoolModel);
            for (String path : RDF_REPOSITORY_PATHS) addRDFResourceToModel(sContext, path, this.answerPoolModel);
            addRDFResourceToModel(sContext, ANSWERS_ONTOLOGY_PATH, this.answerPoolModel);
        } catch (IllegalStateException e) {
            this.answerPoolModel = null;
            throw new InternalServiceFault("The web service " + "could not be initialized.", e.getMessage(), e);
        } catch (InternalServiceFault e) {
            this.answerPoolModel = null;
            throw e;
        }
    }

    /**
	 * Adds an RDF resource file to the given model. The resource file is
	 * specified by its path name in the given servlet context.
	 * 
	 * @param sContext
	 *            the servlet context in which to interpret the given path name;
	 *            if {@code null}, then the {@link #resourcePathPrefix} is used
	 *            to resolve the given path name
	 * @param rdfResourcePath
	 *            the path name of the RDF resource file to add
	 * @param model
	 *            the model to which to add the specified resource
	 * @throws InternalServiceFault
	 *             if there was a problem reading the resource
	 */
    private void addRDFResourceToModel(ServletContext sContext, String rdfResourcePath, Model model) throws InternalServiceFault {
        InputStream resInputStream = null;
        try {
            resInputStream = sContext == null ? new BufferedInputStream(new FileInputStream(this.resourcePathPrefix + rdfResourcePath)) : sContext.getResourceAsStream(rdfResourcePath);
        } catch (FileNotFoundException e) {
            throw new InternalServiceFault("The web service could not be " + "initialized.", e.getMessage(), e);
        }
        if (resInputStream == null) throw new InternalServiceFault("The web service could not be " + "initialized.", "InputStream mustn't be null (for \"" + rdfResourcePath + "\")");
        model.read(resInputStream, "");
        try {
            resInputStream.close();
        } catch (IOException e) {
            throw new InternalServiceFault("The web service could not be " + "initialized.", e.getMessage(), e);
        }
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
        initialize();
        Model resultModel = null;
        try {
            QueryExecution queryExec = QueryExecutionFactory.create(QueryFactory.create(sparqlQuery), this.answerPoolModel);
            resultModel = queryExec.execConstruct();
            queryExec.close();
        } catch (QueryException e) {
            throw new MalformedQueryFault("The given query string is not " + "a valid SPARQL 'CONSTRUCT' query.", e.getMessage(), e);
        }
        RDFGraph result = OBJ_FACTORY.createRDFGraph();
        result.setAny(convertModelToDomElement(resultModel));
        return result;
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
    private Element convertModelToDomElement(final Model model) throws InternalServiceFault {
        Document doc = null;
        try {
            PipedInputStream pIn = new PipedInputStream();
            final PipedOutputStream pOut = new PipedOutputStream();
            pOut.connect(pIn);
            Thread writerThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    model.write(pOut, "RDF/XML-ABBREV");
                    try {
                        pOut.close();
                    } catch (IOException e) {
                    }
                }
            });
            writerThread.start();
            doc = this.domBuilder.parse(pIn);
            pIn.close();
            writerThread.join();
        } catch (InterruptedException e) {
        } catch (SAXException e) {
            throw new InternalServiceFault("The constructed RDF/XML " + "graph doesn't appear to be well-formed.", e.getMessage(), e);
        } catch (IOException e) {
            throw new InternalServiceFault("Unexpected error.", e.getMessage(), e);
        }
        return doc.getDocumentElement();
    }
}
