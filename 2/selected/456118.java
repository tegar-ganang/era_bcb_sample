package jp.ac.keio.ae.comp.yamaguti.doddle.utils;

import java.io.*;
import java.net.*;
import java.util.*;
import jp.ac.keio.ae.comp.yamaguti.doddle.*;
import jp.ac.keio.ae.comp.yamaguti.doddle.data.*;
import jp.ac.keio.ae.comp.yamaguti.doddle.ui.*;
import org.apache.log4j.*;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.*;
import com.hp.hpl.jena.vocabulary.*;

/**
 * @author takeshi morita
 */
public class SwoogleWebServiceWrapper {

    private static final int MAX_CNT = 5;

    private static Set<Resource> literalResourceSet = new HashSet<Resource>();

    static {
        literalResourceSet.add(ResourceFactory.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#Literal"));
        literalResourceSet.add(ResourceFactory.createResource("http://www.w3.org/2001/XMLSchema#string"));
        literalResourceSet.add(ResourceFactory.createResource("http://www.w3.org/2001/XMLSchema#date"));
        literalResourceSet.add(ResourceFactory.createResource("http://www.w3.org/2001/XMLSchema#dateTime"));
        literalResourceSet.add(ResourceFactory.createResource("http://www.w3.org/2001/XMLSchema#double"));
        literalResourceSet.add(ResourceFactory.createResource("http://www.w3.org/2001/XMLSchema#float"));
        literalResourceSet.add(ResourceFactory.createResource("http://www.w3.org/2001/XMLSchema#int"));
    }

    private static List<String> owlOntologyList;

    private static List<String> swoogleQueryList;

    private static NameSpaceTable nsTable;

    private static SwoogleWebServiceData swoogleWebServiceData = new SwoogleWebServiceData();

    private static final String SWOOGLE_WEB_SERVICE_URI = "http://logos.cs.umbc.edu:8080/swoogle31/q?";

    private static final String SWOOGLE_WEB_SERVICE_KEY = "&key=demo";

    public static String SWOOGLE_QUERY_RESULTS_DIR = "C:/DODDLE-OWL/swoogle_query_results_tmp";

    private static String SWOOGLE_QUERY_RESULT_LIST_FILE = "swoogle_query_files.txt";

    public static String OWL_ONTOLOGIES_DIR = "C:/DODDLE-OWL/owl_ontologies";

    private static String OWL_ONTOLOGIY_RESULT_LIST_FILE = "owl_files.txt";

    private static final String ONTOLOGY_URL = "ontology_url";

    private static final String ONTOLOGY_RANK = "ontoRank";

    private static final String SWT_URI = "swt_uri";

    private static final String TERM_RANK = "termRank";

    private static final String PROPERTY = "property";

    private static final String CLASS = "class";

    private static final String ENCODING = "encoding";

    private static final String RDF_TYPE = "rdf_type";

    private static final String FILE_TYPE = "file_type";

    private static final String REFERENCE_TYPE = "reference_type";

    public static final String RESOURCE_DIR = "jp/ac/keio/ae/comp/yamaguti/doddle/resources/";

    public static SwoogleWebServiceData getSwoogleWebServiceData() {
        return swoogleWebServiceData;
    }

    private static void initOWLOntologyList() {
        File file = new File(OWL_ONTOLOGIES_DIR + File.separator + OWL_ONTOLOGIY_RESULT_LIST_FILE);
        System.out.println(file.getAbsolutePath());
        if (!file.exists()) {
            return;
        }
        owlOntologyList = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            while (reader.ready()) {
                String line = reader.readLine();
                owlOntologyList.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initSwoogleQueryList() {
        File file = new File(SWOOGLE_QUERY_RESULTS_DIR + File.separator + SWOOGLE_QUERY_RESULT_LIST_FILE);
        System.out.println(file.getAbsolutePath());
        if (!file.exists()) {
            return;
        }
        swoogleQueryList = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            while (reader.ready()) {
                String line = reader.readLine();
                swoogleQueryList.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存されているSwoogleクエリー結果からswoogleWebServiceDataのuriSwoogleOWLMetaDataMapにOWLメタデータを格納する
     */
    private static void initSwoogleOWLMetaData() {
        if (swoogleQueryList == null) {
            return;
        }
        QueryExecution qexec = null;
        try {
            for (int i = 0; i < swoogleQueryList.size(); i++) {
                String queryTypeAndSearchString = swoogleQueryList.get(i);
                int index = swoogleQueryList.lastIndexOf(queryTypeAndSearchString);
                if (queryTypeAndSearchString.indexOf("queryType=search_swd_ontology") != -1 || queryTypeAndSearchString.indexOf("queryType=digest_swd") != -1) {
                    File file = new File(SWOOGLE_QUERY_RESULTS_DIR + File.separator + "query_" + (index + 1));
                    Model model = getModel(new FileInputStream(file), DODDLEConstants.BASE_URI);
                    String sparqlQueryString = SPARQLQueryUtil.getQueryString(getSearchOntologyQuery());
                    Query query = QueryFactory.create(sparqlQueryString);
                    qexec = QueryExecutionFactory.create(query, model);
                    ResultSet results = qexec.execSelect();
                    while (results.hasNext()) {
                        QuerySolution qs = results.nextSolution();
                        loadOWLMetaData(qs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (qexec != null) {
                qexec.close();
            }
        }
    }

    public static void setNameSpaceTable(NameSpaceTable nstbl) {
        nsTable = nstbl;
    }

    public static void deleteOWLOntologies() {
        File ontologyDir = new File(OWL_ONTOLOGIES_DIR);
        File[] files = ontologyDir.listFiles();
        for (int i = 0; i < files.length; i++) {
        }
    }

    public static void deleteSwoogleQueryResults() {
        File ontologyDir = new File(SWOOGLE_QUERY_RESULTS_DIR);
        File[] files = ontologyDir.listFiles();
        for (int i = 0; i < files.length; i++) {
        }
    }

    private static InputStream getSearchOntologyQuery() {
        return DODDLE.class.getClassLoader().getResourceAsStream(RESOURCE_DIR + "swoogle_queries/SearchOntology.rq");
    }

    private static InputStream getSearchTermQuery() {
        return DODDLE.class.getClassLoader().getResourceAsStream(RESOURCE_DIR + "swoogle_queries/SearchTerm.rq");
    }

    private static InputStream getListDocumentsUsingTermQuery() {
        return DODDLE.class.getClassLoader().getResourceAsStream(RESOURCE_DIR + "swoogle_queries/listDocumentsUsingTerm.rq");
    }

    private static InputStream getListPropertiesOfaRegionClassQuery() {
        return DODDLE.class.getClassLoader().getResourceAsStream(RESOURCE_DIR + "swoogle_queries/listPropertiesOfaRegionClass.rq");
    }

    private static InputStream getListRegionClassesOfaPropertyQuery() {
        return DODDLE.class.getClassLoader().getResourceAsStream(RESOURCE_DIR + "swoogle_queries/listRegionClassesOfaProperty.rq");
    }

    private static Model getModel(InputStream inputStream, String baseURI) {
        Model model = ModelFactory.createDefaultModel();
        try {
            model.read(inputStream, baseURI, "RDF/XML");
        } catch (Exception e) {
            System.out.println("RDF Parse Exception");
        }
        return model;
    }

    private static void saveFile(File file, InputStream inputStream, String encoding) {
        BufferedWriter writer = null;
        BufferedReader reader = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));
            reader = new BufferedReader(new InputStreamReader(inputStream, encoding));
            String line = "";
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.write("\n");
            }
        } catch (FileNotFoundException fne) {
            System.out.println("fileName error: " + file);
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private static void appendURI(String uri, File file) {
        if (!file.exists()) {
            return;
        }
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
            writer.append(uri);
            writer.append("\n");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void appendOntology(String url) {
        appendURI(url, new File(OWL_ONTOLOGIES_DIR + File.separator + OWL_ONTOLOGIY_RESULT_LIST_FILE));
        owlOntologyList.add(url);
    }

    private static void appendQuery(String url) {
        appendURI(url, new File(SWOOGLE_QUERY_RESULTS_DIR + File.separator + SWOOGLE_QUERY_RESULT_LIST_FILE));
        swoogleQueryList.add(url);
    }

    private static void saveOntology(String url, File file, InputStream inputStream, String encoding) {
        saveFile(file, inputStream, encoding);
        appendOntology(url);
    }

    private static void saveQueryResult(String queryTypeAndSearchString, File file, InputStream inputStream) {
        saveFile(file, inputStream, "UTF-8");
        try {
            DODDLE.getLogger().log(Level.DEBUG, "sleep 2 sec");
            Thread.sleep(2000);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        appendQuery(queryTypeAndSearchString);
    }

    private static Model getSwoogleQueryResultModel(String queryTypeAndSearchString) {
        String restQuery = SWOOGLE_WEB_SERVICE_URI + queryTypeAndSearchString + SWOOGLE_WEB_SERVICE_KEY;
        Model model = null;
        try {
            System.out.println(queryTypeAndSearchString);
            int index = swoogleQueryList.lastIndexOf(queryTypeAndSearchString);
            index += 1;
            File queryCachFile = new File(SWOOGLE_QUERY_RESULTS_DIR + File.separator + "query_" + index);
            if (queryCachFile.exists()) {
                DODDLE.getLogger().log(Level.DEBUG, "Using Cashed Data");
                model = getModel(new FileInputStream(queryCachFile), DODDLEConstants.BASE_URI);
            } else {
                queryCachFile = new File(SWOOGLE_QUERY_RESULTS_DIR + File.separator + "query_" + (swoogleQueryList.size() + 1));
                URL url = new URL(restQuery);
                saveQueryResult(queryTypeAndSearchString, queryCachFile, url.openStream());
                model = getModel(url.openStream(), DODDLEConstants.BASE_URI);
            }
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        } catch (FileNotFoundException fne) {
            fne.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        if (model == null) {
            model = ModelFactory.createDefaultModel();
        }
        return model;
    }

    private static void loadOWLMetaData(QuerySolution qs) {
        Resource ontologyURL = (Resource) qs.get(ONTOLOGY_URL);
        Literal ontoRank = (Literal) qs.get(ONTOLOGY_RANK);
        Literal encoding = (Literal) qs.get(ENCODING);
        Literal fileType = (Literal) qs.get(FILE_TYPE);
        Resource rdfType = (Resource) qs.get(RDF_TYPE);
        SwoogleOWLMetaData owlMetaData = new SwoogleOWLMetaData(ontologyURL, encoding, fileType, rdfType, ontoRank);
        swoogleWebServiceData.putSwoogleOWLMetaData(ontologyURL.getURI(), owlMetaData);
    }

    private static void saveOntology(QuerySolution qs) {
        File ontFile = null;
        URL ontURL = null;
        try {
            Resource ontologyURL = (Resource) qs.get(ONTOLOGY_URL);
            Literal ontoRank = (Literal) qs.get(ONTOLOGY_RANK);
            Literal encoding = (Literal) qs.get(ENCODING);
            Literal fileType = (Literal) qs.get(FILE_TYPE);
            Resource rdfType = (Resource) qs.get(RDF_TYPE);
            DODDLE.getLogger().log(Level.INFO, "Try to save: " + ontologyURL.getURI());
            if (ontologyURL.getURI().equals("http://c703-deri03.uibk.ac.at:8080/people/mappings.owl")) {
                System.out.println("skip: http://c703-deri03.uibk.ac.at:8080/people/mappings.owl");
                return;
            } else if (ontologyURL.getURI().equals("http://www.daml.org/2004/05/unspsc/unspsc.owl")) {
                System.out.println("skip: http://www.daml.org/2004/05/unspsc/unspsc.owl");
                return;
            } else if (ontologyURL.getURI().equals("http://www.daml.org/2004/05/unspsc/unspsc")) {
                System.out.println("skip: http://www.daml.org/2004/05/unspsc/unspsc");
                return;
            } else if (ontologyURL.getURI().equals("http://dmag.upf.edu/ontologies/2004/10/ODRL-DD-11.owl")) {
                System.out.println("skip: http://dmag.upf.edu/ontologies/2004/10/ODRL-DD-11.owl");
                return;
            }
            SwoogleOWLMetaData owlMetaData = new SwoogleOWLMetaData(ontologyURL, encoding, fileType, rdfType, ontoRank);
            swoogleWebServiceData.putSwoogleOWLMetaData(ontologyURL.getURI(), owlMetaData);
            ontURL = new URL(ontologyURL.getURI());
            int index = owlOntologyList.lastIndexOf(owlMetaData.getURL());
            index += 1;
            ontFile = new File(OWL_ONTOLOGIES_DIR + File.separator + "onto_" + index);
            if (!ontFile.exists()) {
                DODDLE.getLogger().log(Level.DEBUG, "Save Ontology: " + ontologyURL);
                try {
                    ontFile = new File(OWL_ONTOLOGIES_DIR + File.separator + "onto_" + (owlOntologyList.size() + 1));
                    saveOntology(ontologyURL.getURI(), ontFile, ontURL.openStream(), owlMetaData.getFileEncoding());
                } catch (Exception e) {
                    DODDLE.getLogger().log(Level.DEBUG, "ignore exception !!");
                }
            } else {
                DODDLE.getLogger().log(Level.DEBUG, "Using Cashed Data");
            }
            if (swoogleWebServiceData.getRefOntology(ontologyURL.getURI()) == null) {
                Model ontModel = Utils.getOntModel(new FileInputStream(ontFile), fileType.getString(), Utils.getRDFType(rdfType), Utils.getNameSpace(ontologyURL));
                ReferenceOWLOntology refOnto = new ReferenceOWLOntology(ontModel, ontologyURL.getURI(), nsTable);
                refOnto.getOntologyRank().setSwoogleOntoRank(owlMetaData.getOntoRank());
                swoogleWebServiceData.putRefOntology(ontologyURL.getURI(), refOnto);
            }
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        } catch (FileNotFoundException fne) {
            DODDLE.getLogger().log(Level.INFO, "Save Ontology Exception: File => " + ontFile.getAbsolutePath() + " URL => " + ontURL);
        }
    }

    /**
     * 獲得したオントロジーの中から入力単語に関連するクラス及びプロパティを抽出する
     */
    public static void setSWTSet(Set<String> inputWordSet) {
        for (String uri : swoogleWebServiceData.getRefOntologyURISet()) {
            ReferenceOWLOntology refOnt = swoogleWebServiceData.getRefOntology(uri);
            for (String inputWord : inputWordSet) {
                Set<String> conceptURISet = refOnt.getURISet(inputWord);
                if (conceptURISet == null) {
                    continue;
                }
                for (String conceptURI : conceptURISet) {
                    Resource conceptResource = ResourceFactory.createResource(conceptURI);
                    if (refOnt.getPropertySet().contains(conceptURI)) {
                        swoogleWebServiceData.addProperty(conceptResource);
                    } else {
                        swoogleWebServiceData.addClass(inputWord, conceptResource);
                    }
                }
            }
        }
    }

    /**
     * 入力単語に関連するオントロジーを獲得
     *
     * @param inputWord
     */
    public static void searchOntology(String inputWord) {
        for (int i = 1; i < 10; i += 10) {
            QueryExecution qexec = null;
            try {
                String queryTypeAndSearchString = "queryType=search_swd_ontology&searchString=def:" + inputWord + "&searchStart=" + i;
                DODDLE.getLogger().log(Level.DEBUG, "Search Ontology: " + inputWord);
                DODDLE.STATUS_BAR.setText("Search Ontology: " + inputWord);
                Model model = getSwoogleQueryResultModel(queryTypeAndSearchString);
                String sparqlQueryString = SPARQLQueryUtil.getQueryString(getSearchOntologyQuery());
                Query query = QueryFactory.create(sparqlQueryString);
                if (model != null) {
                    qexec = QueryExecutionFactory.create(query, model);
                    ResultSet results = qexec.execSelect();
                    while (results.hasNext()) {
                        QuerySolution qs = results.nextSolution();
                        saveOntology(qs);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (qexec != null) {
                    qexec.close();
                }
            }
        }
    }

    private static Set<String> classWordSet = new HashSet<String>();

    private static Set<String> propertyWordSet = new HashSet<String>();

    /**
     * 入力単語に関連するSWTを獲得
     *
     * @param maxCnt
     * @param inputWord
     * @param type
     */
    public static void searchTerms(int maxCnt, boolean isSearchLabel, String inputWord, String type) {
        QueryExecution qexec = null;
        try {
            String searchString = "";
            if (isSearchLabel) {
                searchString = "(localname:" + inputWord + " OR label:" + inputWord + ") ";
            } else {
                searchString = "(localname:" + inputWord + ") ";
            }
            searchString += "(type:owl." + type + " OR type:rdfs." + type + " OR type:daml." + type + ")";
            String queryTypeAndSearchString = "queryType=search_swt&searchString=" + URLEncoder.encode(searchString, "UTF-8");
            DODDLE.getLogger().log(Level.DEBUG, "Search Terms: " + inputWord);
            DODDLE.STATUS_BAR.setText("Search Terms: " + inputWord);
            Model model = getSwoogleQueryResultModel(queryTypeAndSearchString);
            if (model == null) {
                System.out.println("model == null");
                return;
            }
            String sparqlQueryString = SPARQLQueryUtil.getQueryString(getSearchTermQuery());
            Query query = QueryFactory.create(sparqlQueryString);
            qexec = QueryExecutionFactory.create(query, model);
            ResultSet results = qexec.execSelect();
            int cnt = 0;
            while (results.hasNext()) {
                QuerySolution qs = results.nextSolution();
                if (cnt == maxCnt) {
                    break;
                }
                cnt++;
                Resource swtURI = (Resource) qs.get(SWT_URI);
                Literal termRank = (Literal) qs.get(TERM_RANK);
                if (type.equals(CLASS)) {
                    classWordSet.add(inputWord);
                    swoogleWebServiceData.addClass(inputWord, swtURI);
                } else if (type.equals(PROPERTY)) {
                    propertyWordSet.add(inputWord);
                    swoogleWebServiceData.addProperty(swtURI);
                }
                swoogleWebServiceData.putTermRank(swtURI.getURI(), termRank.getDouble());
            }
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        } finally {
            if (qexec != null) {
                qexec.close();
            }
        }
    }

    private static void searchListPropertiesOfaRegionClass(String queryTypeAndSearchString, String regionURI, Property regionType) {
        Resource region = ResourceFactory.createResource(regionURI);
        QueryExecution qexec = null;
        try {
            Model model = getSwoogleQueryResultModel(queryTypeAndSearchString);
            String sparqlQueryString = SPARQLQueryUtil.getQueryString(getListPropertiesOfaRegionClassQuery());
            Query query = QueryFactory.create(sparqlQueryString);
            qexec = QueryExecutionFactory.create(query, model);
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution qs = results.nextSolution();
                Resource property = (Resource) qs.get(PROPERTY);
                swoogleWebServiceData.addRelatedProperty(property);
                if (regionType == RDFS.domain) {
                    swoogleWebServiceData.addPropertyDomain(property, region);
                } else if (regionType == RDFS.range) {
                    swoogleWebServiceData.addPropertyRange(property, region);
                }
            }
        } finally {
            if (qexec != null) {
                qexec.close();
            }
        }
    }

    /**
     * 入力したクラスを定義域とするプロパティを獲得
     */
    public static void searchListPropertiesOfaDomainClass(String domainURI) {
        try {
            String queryTypeAndSearchString = "queryType=rel_swd_instance_domain_c2p&searchString=" + URLEncoder.encode(domainURI, "UTF-8");
            DODDLE.getLogger().log(Level.DEBUG, "Search List Properties Of a Domain Class: " + domainURI);
            DODDLE.STATUS_BAR.setText("Search List Properties Of a Domain Class: " + domainURI);
            searchListPropertiesOfaRegionClass(queryTypeAndSearchString, domainURI, RDFS.domain);
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        }
    }

    /**
     * 入力したクラスを値域とするプロパティを獲得
     */
    public static void searchListPropertiesOfaRangeClass(String rangeURI) {
        try {
            String queryTypeAndSearchString = "queryType=rel_swd_instance_range_c2p&searchString=" + URLEncoder.encode(rangeURI, "UTF-8");
            DODDLE.getLogger().log(Level.DEBUG, "Search List Properties Of a Range Class: " + rangeURI);
            DODDLE.STATUS_BAR.setText("Search List Properties Of a Range Class: " + rangeURI);
            searchListPropertiesOfaRegionClass(queryTypeAndSearchString, rangeURI, RDFS.range);
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        }
    }

    private static void searchListRegionClassOfaProperty(String queryTypeAndSearchString, String propertyURI, Property regionType) {
        Resource property = ResourceFactory.createResource(propertyURI);
        QueryExecution qexec = null;
        try {
            Model model = getSwoogleQueryResultModel(queryTypeAndSearchString);
            String sparqlQueryString = SPARQLQueryUtil.getQueryString(getListRegionClassesOfaPropertyQuery());
            Query query = QueryFactory.create(sparqlQueryString);
            qexec = QueryExecutionFactory.create(query, model);
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution qs = results.nextSolution();
                Resource classResource = (Resource) qs.get(CLASS);
                if (regionType == RDFS.domain) {
                    swoogleWebServiceData.addPropertyDomain(property, classResource);
                } else if (regionType == RDFS.range) {
                    swoogleWebServiceData.addPropertyRange(property, classResource);
                }
            }
        } finally {
            if (qexec != null) {
                qexec.close();
            }
        }
    }

    /**
     * プロパティのURIを入力として，そのプロパティの定義域を獲得
     */
    public static void searchListDomainClassOfaProperty(String propertyURI) {
        try {
            String queryTypeAndSearchString = "queryType=rel_swd_instance_domain_p2c&searchString=" + URLEncoder.encode(propertyURI, "UTF-8");
            DODDLE.getLogger().log(Level.DEBUG, "Search List Domain Class Of a Property: " + propertyURI);
            DODDLE.STATUS_BAR.setText("Search List Domain Class Of a Property: " + propertyURI);
            searchListRegionClassOfaProperty(queryTypeAndSearchString, propertyURI, RDFS.domain);
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        }
    }

    /**
     * プロパティのURIを入力として，そのプロパティの値域を獲得
     */
    public static void searchListRangeClassOfaProperty(String propertyURI) {
        try {
            String queryTypeAndSearchString = "queryType=rel_swd_instance_range_p2c&searchString=" + URLEncoder.encode(propertyURI, "UTF-8");
            DODDLE.getLogger().log(Level.DEBUG, "Search List Range Class Of a Property: " + propertyURI);
            DODDLE.STATUS_BAR.setText("Search List Range Class Of a Property: " + propertyURI);
            searchListRegionClassOfaProperty(queryTypeAndSearchString, propertyURI, RDFS.range);
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        }
    }

    /**
     * SWTが定義されているオントロジーを獲得
     */
    public static void searchListDocumentsUsingTerm(String swtURI) {
        QueryExecution qexec = null;
        try {
            String queryTypeAndSearchString = "queryType=rel_swt_swd&searchString=" + URLEncoder.encode(swtURI, "UTF-8");
            DODDLE.getLogger().log(Level.DEBUG, "Search List Documents Using Term: " + swtURI);
            DODDLE.STATUS_BAR.setText("Search List Documents Using Term: " + swtURI);
            Model model = getSwoogleQueryResultModel(queryTypeAndSearchString);
            String sparqlQueryString = SPARQLQueryUtil.getQueryString(getListDocumentsUsingTermQuery());
            Query query = QueryFactory.create(sparqlQueryString);
            qexec = QueryExecutionFactory.create(query, model);
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution qs = results.nextSolution();
                Resource ontologyURL = (Resource) qs.get(ONTOLOGY_URL);
                Resource referenceType = (Resource) qs.get(REFERENCE_TYPE);
                if (ontologyURL != null && referenceType.getURI().equals("http://daml.umbc.edu/ontologies/webofbelief/1.4/wob.owl#hasClassDefinitionIn") || referenceType.getURI().equals("http://daml.umbc.edu/ontologies/webofbelief/1.4/wob.owl#hasPropertyDefinitionIn")) {
                    SwoogleOWLMetaData owlMetaData = swoogleWebServiceData.getSwoogleOWLMetaData(ontologyURL.getURI());
                    if (owlMetaData == null) {
                        searchDigestSemanticWebDocument(ontologyURL.getURI());
                    }
                }
            }
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        } finally {
            if (qexec != null) {
                qexec.close();
            }
        }
    }

    /**
     * SWDのメタデータを獲得
     */
    public static void searchDigestSemanticWebDocument(String swdURI) {
        QueryExecution qexec = null;
        try {
            String queryTypeAndSearchString = "queryType=digest_swd&searchString=" + URLEncoder.encode(swdURI, "UTF-8");
            DODDLE.getLogger().log(Level.DEBUG, "Search Digest Semantic Web Document: " + swdURI);
            Model model = getSwoogleQueryResultModel(queryTypeAndSearchString);
            String sparqlQueryString = SPARQLQueryUtil.getQueryString(getSearchOntologyQuery());
            Query query = QueryFactory.create(sparqlQueryString);
            qexec = QueryExecutionFactory.create(query, model);
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution qs = results.nextSolution();
                saveOntology(qs);
                break;
            }
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        } finally {
            if (qexec != null) {
                qexec.close();
            }
        }
    }

    private static void searchTerms(Set<String> inputWordSet, boolean isSearchLabel) {
        for (String inputWord : inputWordSet) {
            searchTerms(MAX_CNT, isSearchLabel, inputWord, CLASS);
            searchTerms(MAX_CNT, isSearchLabel, inputWord, PROPERTY);
        }
        for (Resource litRes : literalResourceSet) {
            swoogleWebServiceData.addClass("Literal", litRes);
        }
        DODDLE.getLogger().log(Level.INFO, "Class word set: " + classWordSet.size() + "/" + inputWordSet.size() + ": " + classWordSet);
        DODDLE.getLogger().log(Level.INFO, "Property word set: " + propertyWordSet.size() + "/" + inputWordSet.size() + ": " + propertyWordSet);
        DODDLE.getLogger().log(Level.INFO, "Class URI cnt: " + swoogleWebServiceData.getClassSet().size());
        for (Resource cls : swoogleWebServiceData.getClassSet()) {
            System.out.println("Class URI: " + cls.getURI());
        }
        DODDLE.getLogger().log(Level.INFO, "Property URI cnt: " + swoogleWebServiceData.getPropertySet().size());
        for (Resource prop : swoogleWebServiceData.getPropertySet()) {
            System.out.println("Property URI: " + prop.getURI());
        }
        DODDLE.STATUS_BAR.addProjectValue();
    }

    private static void searchListPropertiesOfRegionClasses() {
        for (Resource classResource : swoogleWebServiceData.getClassSet()) {
            if (!literalResourceSet.contains(classResource)) {
                searchListPropertiesOfaDomainClass(classResource.getURI());
            } else {
                System.out.println("literal res: " + classResource);
            }
            searchListPropertiesOfaRangeClass(classResource.getURI());
        }
        DODDLE.STATUS_BAR.addProjectValue();
    }

    private static void searchListRegionClassOfProperties() {
        Set<Resource> allPropertySet = swoogleWebServiceData.getAllProperty();
        for (Resource propertyResource : allPropertySet) {
            searchListDomainClassOfaProperty(propertyResource.getURI());
            searchListRangeClassOfaProperty(propertyResource.getURI());
        }
        DODDLE.STATUS_BAR.addProjectValue();
    }

    private static Set<String> getRelatedWordSet(Set<String> inputWordSet) {
        Set<String> relatedWordSet = new HashSet<String>();
        relatedWordSet.addAll(inputWordSet);
        for (Resource property : swoogleWebServiceData.getAllProperty()) {
            relatedWordSet.add(Utils.getLocalName(property));
        }
        return relatedWordSet;
    }

    private static void searchOntologies(Set<String> inputWordSet) {
        int cnt = 1;
        for (String word : inputWordSet) {
            DODDLE.getLogger().log(Level.INFO, "input word: " + cnt + "/" + inputWordSet.size() + ": " + word);
            searchOntology(word);
            cnt++;
        }
        setSWTSet(inputWordSet);
        cnt = 1;
        int definedConceptCnt = 0;
        Set<Resource> conceptSet = swoogleWebServiceData.getConceptSet();
        for (Resource conceptResource : conceptSet) {
            DODDLE.getLogger().log(Level.INFO, "concept: " + cnt + "/" + conceptSet.size() + ": " + conceptResource);
            DODDLE.getLogger().log(Level.INFO, "defined concept cnt: " + definedConceptCnt);
            boolean isDefinedConcept = false;
            for (String uri : swoogleWebServiceData.getRefOntologyURISet()) {
                ReferenceOWLOntology refOnto = swoogleWebServiceData.getRefOntology(uri);
                if (refOnto.getClassSet().contains(conceptResource.getURI()) || refOnto.getPropertySet().contains(conceptResource.getURI())) {
                    DODDLE.getLogger().log(Level.DEBUG, "defined concept: " + conceptResource);
                    definedConceptCnt++;
                    isDefinedConcept = true;
                    break;
                }
            }
            if (!isDefinedConcept) {
                DODDLE.getLogger().log(Level.INFO, "cannot get ontology using search ontology service: " + conceptResource);
                searchListDocumentsUsingTerm(conceptResource.getURI());
            }
            cnt++;
        }
        DODDLE.STATUS_BAR.addProjectValue();
    }

    private static void printInfo(int level) {
        DODDLE.getLogger().log(Level.INFO, +level + ": Class size: " + swoogleWebServiceData.getClassSet().size());
        DODDLE.getLogger().log(Level.INFO, level + " Related Property size: " + swoogleWebServiceData.getRelatedPropertySet().size());
        DODDLE.getLogger().log(Level.INFO, level + " Property size: " + swoogleWebServiceData.getPropertySet().size());
        DODDLE.getLogger().log(Level.INFO, level + " Relation cnt: " + swoogleWebServiceData.getAllRelationCount());
    }

    public static void refinePropertiesAndRegionSet() {
        swoogleWebServiceData.addInheritedRegionSet();
        printInfo(4);
        swoogleWebServiceData.removeUnnecessaryRegionSet();
        printInfo(5);
        swoogleWebServiceData.addNecessaryPropertySet();
        printInfo(6);
    }

    /**
     * 入力単語に関連するオントロジーを獲得する
     *
     * @param inputWordSet
     */
    public static void acquireRelevantOWLOntologies(Set<String> inputWordSet, boolean isSearchLabel) {
        DODDLE.STATUS_BAR.setText("Acquire OWL Ontologies Start");
        DODDLE.STATUS_BAR.startTime();
        DODDLE.STATUS_BAR.initNormal(6);
        DODDLE.STATUS_BAR.lock();
        searchTerms(inputWordSet, isSearchLabel);
        searchListPropertiesOfRegionClasses();
        printInfo(1);
        searchListRegionClassOfProperties();
        printInfo(2);
        searchOntologies(inputWordSet);
        printInfo(3);
        refinePropertiesAndRegionSet();
        DODDLE.STATUS_BAR.addProjectValue();
        DODDLE.STATUS_BAR.setText("Calc Ontology Rank");
        swoogleWebServiceData.calcOntologyRank(inputWordSet);
        DODDLE.STATUS_BAR.addProjectValue();
        swoogleWebServiceData.countDefinedConcept();
        Object[] refOntologies = swoogleWebServiceData.getRefOntologies().toArray();
        Arrays.sort(refOntologies);
        DODDLE.getLogger().log(Level.INFO, "獲得オントロジー数: " + refOntologies.length);
        for (int i = 0; i < refOntologies.length; i++) {
            DODDLE.getLogger().log(Level.INFO, refOntologies[i]);
        }
        DODDLE.getLogger().log(Level.INFO, "概念定義数： " + swoogleWebServiceData.getValidRelationCount());
        DODDLE.getLogger().log(Level.INFO, "オントロジーに定義されている概念定義数： " + swoogleWebServiceData.getDefinedRelationCount());
        DODDLE.STATUS_BAR.unLock();
        DODDLE.STATUS_BAR.hideProgressBar();
        DODDLE.STATUS_BAR.setText("Acquire Ontologies done");
    }

    public static void initSwoogleWebServiceWrapper() {
        System.out.println("Init OWL Ontology List");
        initOWLOntologyList();
        System.out.println("Init Swoogle Query List");
        initSwoogleQueryList();
        System.out.println("Init Swoogle OWL MetaData");
        initSwoogleOWLMetaData();
    }

    public static void main(String[] args) {
        Translator.loadDODDLEComponentOntology(DODDLEConstants.LANG);
        DODDLE.STATUS_BAR = new StatusBarPanel();
        Logger.getLogger(RDFDefaultErrorHandler.class).setLevel(Level.ERROR);
        Logger.getLogger(OntDocumentManager.class).setLevel(Level.ERROR);
        DODDLE.getLogger().setLevel(Level.INFO);
        DODDLE.setFileLogger();
        SWOOGLE_QUERY_RESULTS_DIR = SWOOGLE_QUERY_RESULTS_DIR + File.separator;
        OWL_ONTOLOGIES_DIR = OWL_ONTOLOGIES_DIR + File.separator;
        initSwoogleWebServiceWrapper();
        SwoogleWebServiceWrapper.setNameSpaceTable(new NameSpaceTable());
        String[] inputWords = new String[] { "mailbox" };
        Set<String> inputWordSet = new HashSet<String>();
        for (int i = 0; i < inputWords.length; i++) {
            inputWordSet.add(inputWords[i]);
        }
        SwoogleWebServiceWrapper.acquireRelevantOWLOntologies(inputWordSet, false);
    }
}
