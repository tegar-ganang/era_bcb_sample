package uk.ac.ebi.ontocat.examples;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.ebi.ontocat.OntologyService;
import uk.ac.ebi.ontocat.OntologyServiceException;
import uk.ac.ebi.ontocat.OntologyTerm;
import uk.ac.ebi.ontocat.OntologyService.SearchOptions;
import uk.ac.ebi.ontocat.bioportal.BioportalOntologyService;
import uk.ac.ebi.ontocat.ols.OlsOntologyService;
import uk.ac.ebi.ontocat.virtual.CompositeDecorator;
import uk.ac.ebi.ook.web.services.Query;
import uk.ac.ebi.ook.web.services.QueryServiceLocator;

public class ServiceComparison {

    private static final Logger log = Logger.getLogger(ServiceComparison.class);

    public static void main(String[] args) throws OntologyServiceException, XPathExpressionException, IOException, ParserConfigurationException, SAXException, ServiceException {
        queryOLS();
        queryBioportal();
        queryOntoCAT();
    }

    private static void queryBioportal() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        String query = "thymus";
        URL urlQuery = new URL("http://rest.bioontology.org/bioportal/search/" + query + "/?isexactmatch=0" + "&includeproperties=1" + "&maxnumhits=10000000" + "&email=ontocat-svn@lists.sourceforge.net");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(urlQuery.openStream());
        XPathFactory XPfactory = XPathFactory.newInstance();
        XPath xpath = XPfactory.newXPath();
        XPathExpression expr = xpath.compile("//searchResultList/searchBean");
        NodeList terms = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < terms.getLength(); i++) {
            String ontologyAccession = (String) xpath.evaluate("ontologyId", terms.item(i), XPathConstants.STRING);
            String termAccession = (String) xpath.evaluate("conceptIdShort", terms.item(i), XPathConstants.STRING);
            String label = (String) xpath.evaluate("preferredName", terms.item(i), XPathConstants.STRING);
        }
        log.info(terms.getLength() + " BP terms");
    }

    private static void queryOLS() throws RemoteException, ServiceException {
        Query qs = null;
        qs = new QueryServiceLocator().getOntologyQuery();
        String query = "thymus";
        Set<Map.Entry<String, String>> terms = qs.getPrefixedTermsByName(query, false).entrySet();
        for (Map.Entry<String, String> entry : terms) {
            String termAccession = entry.getKey();
            String ontologyAccession = entry.getValue().split(":")[0];
            String label = entry.getValue().split(":")[1];
        }
        log.info(terms.size() + " OLS terms");
    }

    private static void queryOntoCAT() throws OntologyServiceException {
        String query = "thymus";
        List<OntologyService> lOntologies = new ArrayList<OntologyService>();
        lOntologies.add(new BioportalOntologyService());
        lOntologies.add(new OlsOntologyService());
        OntologyService os = CompositeDecorator.getService(lOntologies);
        List<OntologyTerm> terms = os.searchAll(query, SearchOptions.INCLUDE_PROPERTIES);
        for (OntologyTerm ot : terms) {
        }
        log.info(terms.size() + " CAT terms");
    }
}
