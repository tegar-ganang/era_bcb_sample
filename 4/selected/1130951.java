package org.ala.rest;

import org.geoserver.test.GeoServerTestSupport;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import net.sf.json.JSON;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import junit.framework.Test;
import org.vfny.geoserver.global.GeoserverDataDirectory;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.geoserver.data.test.MockData;
import org.restlet.data.MediaType;
import org.w3c.dom.NodeList;

public class GazetteerSearchResourceTest extends GeoServerTestSupport {

    public static Test suite() {
        return new OneTimeTestSetup(new GazetteerSearchResourceTest());
    }

    @Override
    public MockData buildTestData() throws Exception {
        MockData dataDirectory = super.buildTestData();
        FileUtils.copyFileToDirectory(new File(dataDirectory.getDataDirectoryRoot().getParentFile().getParent(), "gazetteer.xml"), dataDirectory.getDataDirectoryRoot());
        FileUtils.copyFileToDirectory(new File(dataDirectory.getDataDirectoryRoot().getParentFile().getParent(), "gazetteer-synonyms.xml"), dataDirectory.getDataDirectoryRoot());
        return dataDirectory;
    }

    public void testGetAsXML() throws Exception {
        try {
            Document dom = getAsDOM("/rest/gazetteer/search.xml?q=Ashton");
            print(dom);
            Node message = getFirstElementByTagName(dom, "id");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testGetAsJSON() throws Exception {
    }

    public void testSearchResourceAsXML() throws Exception {
        Document dom = getAsDOM("/rest/gazetteer/result.xml?q=ashton");
    }

    public void testSearchNameCommaType() throws Exception {
        Document dom = getAsDOM("/rest/gazetteer/result.json?q=ashton,type=NamedPlaces");
    }

    public void testSearchNameAndType() throws Exception {
        Document dom = getAsDOM("/rest/gazetteer/result.json?q=ashton&type=NamedPlaces");
        print(dom);
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        XPathExpression namesExpr = xpath.compile("//search/results/result/name/text()");
        NodeList names = (NodeList) namesExpr.evaluate(dom, XPathConstants.NODESET);
    }

    public void testSearchWrongType() throws Exception {
        Document dom = getAsDOM("/rest/gazetteer/result.xml?q=ashton&type=NOT_A_VALID_TYPE");
        print(dom);
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        XPathExpression namesExpr = xpath.compile("//search/results/result/name/text()");
        NodeList names = (NodeList) namesExpr.evaluate(dom, XPathConstants.NODESET);
    }

    public void testSearchGetHyperlink() throws Exception {
        Document dom = getAsDOM("/rest/gazetteer/result.json?q=ashton");
        print(dom);
    }

    public void testPointSearchLegacy() throws Exception {
        System.out.println("Testing Legacy point search ...");
        Document dom = getAsDOM("/rest/gazetteer/result.xml?point=0.003,0.001&layer=NamedPlaces");
        print(dom);
        System.out.println("... Done");
    }

    public void testPointSearch() throws Exception {
        Document dom = getAsDOM("/rest/gazetteer/result.xml?lon=0.003&lat=0.001&layer=NamedPlaces");
        print(dom);
    }

    public void testNewPointSearch() throws Exception {
        System.out.println("TESTING new point search!!");
        Document dom = getAsDOM("/rest/gazetteer/NamedPlaces/latlon/0.001,0.003");
        print(dom);
    }

    public void testNewPointSearchDefault() throws Exception {
        System.out.println("TESTING new point search default");
        Document dom = getAsDOM("/rest/gazetteer/latlon/0.001,0.003");
        print(dom);
    }

    public void testLayerClassesDeprecated() throws Exception {
        System.out.println("*************");
        JSON json = getAsJSON("/rest/gazetteer/NamedPlaces.json");
        print(json);
        System.out.println("*************");
    }

    public void testFeatureList() throws Exception {
        System.out.println("*************");
        Document dom = getAsDOM("/rest/gazetteer/NamedPlaces/features");
        print(dom);
        JSON json = getAsJSON("/rest/gazetteer/NamedPlaces/features.json");
        print(json);
        System.out.println("*************");
    }

    public void testFeatureListPage() throws Exception {
        System.out.println("*************");
        Document dom = getAsDOM("/rest/gazetteer/NamedPlaces/features/2");
        print(dom);
        JSON json = getAsJSON("/rest/gazetteer/NamedPlaces/features/2.json");
        print(json);
        System.out.println("*************");
    }

    public void testFeaturePost() throws Exception {
        System.out.println("TESTING POST");
        InputStream is = post("/rest/points", "{ \"type\": \"MultiPoint\",\"coordinates\": [  [0.003,0.0017],[0.0017,0.003],[100.0, 0.0], [101.0, 1.0] ] }", "text/json");
        System.out.println("...DONE");
    }

    public void testFeatureGet() throws Exception {
        System.out.println("TESTING GET");
        Document dom = getAsDOM("/rest/points");
        print(dom);
    }
}
