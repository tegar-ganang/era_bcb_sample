package org.exist.xslt;

import static org.junit.Assert.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XPathQueryService;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 *
 */
public class XSLTestCase {

    private static final String URI = "xmldb:exist://" + DBBroker.ROOT_COLLECTION;

    private static final String DRIVER = "org.exist.xmldb.DatabaseImpl";

    private static final String XSLT_COLLECTION = "xslt_tests";

    static File existDir = new File(".");

    private Collection col = null;

    /**
	 * @throws java.lang.Exception
	 */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
	 * @throws java.lang.Exception
	 */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
	 * @throws java.lang.Exception
	 */
    @Before
    public void setUp() throws Exception {
        try {
            Class<?> cl = Class.forName(DRIVER);
            Database database = (Database) cl.newInstance();
            database.setProperty("create-database", "true");
            DatabaseManager.registerDatabase(database);
            col = DatabaseManager.getCollection(URI + "/" + XSLT_COLLECTION);
            if (col == null) {
                Collection root = DatabaseManager.getCollection(URI);
                CollectionManagementService mgtService = (CollectionManagementService) root.getService("CollectionManagementService", "1.0");
                col = mgtService.createCollection(XSLT_COLLECTION);
                System.out.println("collection created.");
            }
            BrokerPool.getInstance().getConfiguration().setProperty(TransformerFactoryAllocator.PROPERTY_TRANSFORMER_CLASS, "org.exist.xslt.TransformerFactoryImpl");
            loadBench("test/src/org/exist/xslt/test/bench/v1_0", bench);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, Map<String, String>> bench = new TreeMap<String, Map<String, String>>();

    private void loadBench(String benchLocation, Map<String, Map<String, String>> bench) throws Exception {
        File testConf = new File(benchLocation + "/default.conf");
        if (testConf.canRead()) {
            FileInputStream fis = new FileInputStream(testConf);
            FileChannel fc = fis.getChannel();
            int sz = (int) fc.size();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);
            Charset charset = Charset.forName("ISO-8859-15");
            CharsetDecoder decoder = charset.newDecoder();
            CharBuffer cb = decoder.decode(bb);
            loadBench(testConf, cb, bench);
            fc.close();
        }
    }

    private void loadBench(File testConf, CharBuffer cb, Map<String, Map<String, String>> bench) {
        Pattern linePattern = Pattern.compile(".*\r?\n");
        String testName = null;
        Map<String, String> testInfo = null;
        int position;
        Matcher lm = linePattern.matcher(cb);
        int lines = 0;
        while (lm.find()) {
            lines++;
            CharSequence cs = lm.group();
            String str = cs.toString();
            if (cs.charAt(0) == (char) 0x005B) {
                position = str.indexOf("]");
                testName = str.substring(1, position);
                if (bench.containsKey(testName)) {
                    testInfo = bench.get(testName);
                } else {
                    testInfo = new HashMap<String, String>();
                    bench.put(testName, testInfo);
                }
            } else if (testName != null) {
                position = str.indexOf("=");
                if (position != -1) {
                    String key = str.substring(0, position).trim();
                    String value = str.substring(position + 1).trim();
                    testInfo.put(key, value);
                }
            }
            if (lm.end() == cb.limit()) break;
        }
    }

    /**
	 * @throws java.lang.Exception
	 */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSimpleTransform() {
        try {
            XPathQueryService service = (XPathQueryService) col.getService("XPathQueryService", "1.0");
            String query = "xquery version \"1.0\";\n" + "declare namespace transform=\"http://exist-db.org/xquery/transform\";\n" + "declare variable $xml {\n" + "	<node xmlns=\"http://www.w3.org/1999/xhtml\">text</node>\n" + "};\n" + "declare variable $xslt {\n" + "	<xsl:stylesheet xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\">\n" + "		<xsl:template match=\"node\">\n" + "			<div><xsl:value-of select=\".\"/></div>\n" + "		</xsl:template>\n" + "	</xsl:stylesheet>\n" + "};\n" + "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" + "	<body>\n" + "		{transform:transform($xml, $xslt, ())}\n" + "	</body>\n" + "</html>";
            ResourceSet result = service.query(query);
            assertEquals(1, result.getSize());
            String content = (String) result.getResource(0).getContent();
            assertTrue(content.startsWith("<html xmlns=\"http://www.w3.org/1999/xhtml\">"));
            assertTrue(content.indexOf("<div>text</div>") > -1);
        } catch (XMLDBException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testComplexTransform() throws Exception {
        try {
            XPathQueryService service = (XPathQueryService) col.getService("XPathQueryService", "1.0");
            String query = "xquery version \"1.0\";\n" + "declare namespace transform=\"http://exist-db.org/xquery/transform\";\n" + "declare variable $xml {\n" + "<salesdata>\n" + " <year>\n" + "  <year>1997</year>\n" + "  <region>\n" + "   <name>west</name>\n" + "   <sales unit=\"millions\">32</sales>\n" + "  </region>\n" + "  <region>\n" + "   <name>central</name>\n" + "   <sales unit=\"millions\">11</sales>\n" + "  </region>\n" + "  <region>\n" + "   <name>east</name>\n" + "   <sales unit=\"millions\">19</sales>\n" + "  </region>\n" + " </year>\n" + " <year>\n" + "  <year>1998</year>\n" + "  <region>\n" + "   <name>west</name>\n" + "   <sales unit=\"millions\">35</sales>\n" + "  </region>\n" + "  <region>\n" + "   <name>central</name>\n" + "   <sales unit=\"millions\">12</sales>\n" + "  </region>\n" + "  <region>\n" + "   <name>east</name>\n" + "   <sales unit=\"millions\">25</sales>\n" + "  </region>\n" + " </year>\n" + " <year>\n" + "  <year>1999</year>\n" + "  <region>\n" + "   <name>west</name>\n" + "   <sales unit=\"millions\">36</sales>\n" + "  </region>\n" + "  <region>\n" + "   <name>central</name>\n" + "   <sales unit=\"millions\">12</sales>\n" + "  </region>\n" + "  <region>\n" + "   <name>east</name>\n" + "   <sales unit=\"millions\">31</sales>\n" + "  </region>\n" + " </year>\n" + " <year>\n" + "  <year>2000</year>\n" + "  <region>\n" + "   <name>west</name>\n" + "   <sales unit=\"millions\">37</sales>\n" + "  </region>\n" + "  <region>\n" + "   <name>central</name>\n" + "   <sales unit=\"millions\">11</sales>\n" + "  </region>\n" + "  <region>\n" + "   <name>east</name>\n" + "   <sales unit=\"millions\">40</sales>\n" + "  </region>\n" + " </year>\n" + "</salesdata>\n" + "};\n" + "declare variable $xslt {\n" + "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" + "<xsl:output method=\"html\" encoding=\"utf-8\"/>\n" + "<xsl:template match=\"/\">\n" + "  <html>\n" + "    <table border=\"1\">\n" + "      <tr>\n" + "        <td colspan=\"2\">Total Sales</td>\n" + "      </tr>\n" + "      <xsl:for-each select=\"salesdata/year\">\n" + "        <tr>\n" + "          <td>\n" + "            <xsl:value-of select=\"year\"/>\n" + "          </td>\n" + "          <td align=\"right\">\n" + "            <xsl:value-of select=\"sum(region/sales)\"/>\n" + "          </td>\n" + "        </tr>\n" + "      </xsl:for-each>\n" + "      <tr>\n" + "        <td>Grand Total</td>\n" + "        <td align=\"right\">\n" + "          <xsl:value-of select=\"sum(salesdata/year/region/sales)\"/>\n" + "        </td>\n" + "      </tr>\n" + "    </table>\n" + "  </html>\n" + "</xsl:template>\n" + "</xsl:stylesheet>\n" + "};\n" + "transform:transform($xml, $xslt, ())";
            ResourceSet result = service.query(query);
            assertEquals(1, result.getSize());
            String content = (String) result.getResource(0).getContent();
            assertTrue(checkResult("total.ref", content));
        } catch (XMLDBException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkResult(String file, String result) throws Exception {
        int tokenCount = 0;
        String ref = loadFile(file);
        ref = ref.replaceAll("\\n", " ");
        ref = ref.replaceAll("<dgnorm_document>", "");
        ref = ref.replaceAll("</dgnorm_document>", "");
        String delim = " \t\n\r\f<>";
        StringTokenizer refTokenizer = new StringTokenizer(ref, delim);
        StringTokenizer resTokenizer = new StringTokenizer(result, delim);
        while (refTokenizer.hasMoreTokens()) {
            tokenCount++;
            String refToken = refTokenizer.nextToken();
            if (!resTokenizer.hasMoreTokens()) {
                System.out.println(ref);
                throw new Exception("result should have: " + refToken + ", but get EOF (at " + tokenCount + ")");
            }
            String resToken = resTokenizer.nextToken();
            if (!refToken.equals(resToken)) {
                System.out.println(ref);
                throw new Exception("result should have: " + refToken + ", but get " + resToken + " (at " + tokenCount + ")");
            }
        }
        if (resTokenizer.hasMoreTokens()) {
            String resToken = resTokenizer.nextToken();
            System.out.println(ref);
            throw new Exception("result should have nothing, but get " + resToken + " (at " + tokenCount + ")");
        }
        return true;
    }

    @Test
    public void testBench() throws Exception {
        long start_time;
        long end_time;
        String query = null;
        String content = null;
        int passed = 0;
        boolean passing;
        Map<String, String> testInfo;
        String reqTest = null;
        for (String testName : bench.keySet()) {
            if ((reqTest != null) && (!testName.equals(reqTest))) continue;
            passing = true;
            query = null;
            content = null;
            testInfo = bench.get(testName);
            System.out.print(testName + ": ");
            if (testInfo.containsKey("storeBeforeTest")) {
                System.out.print("skipping");
                if (testInfo.containsKey("comment")) System.out.print(" (" + testInfo.get("comment") + ")");
                System.out.println();
                continue;
            }
            String input = loadFile(testInfo.get("input"));
            String stylesheet = loadFile(testInfo.get("stylesheet"));
            try {
                start_time = System.currentTimeMillis();
                XPathQueryService service = (XPathQueryService) col.getService("XPathQueryService", "1.0");
                query = "xquery version \"1.0\";\n" + "declare namespace transform=\"http://exist-db.org/xquery/transform\";\n" + "declare variable $xml {" + input + "};\n" + "declare variable $xslt {" + stylesheet + "};\n" + "transform:transform($xml, $xslt, ())\n";
                ResourceSet result = service.query(query);
                end_time = System.currentTimeMillis();
                content = "";
                for (int i = 0; i < result.getSize(); i++) content = content + (String) result.getResource(i).getContent();
                assertTrue(checkResult(testInfo.get("reference"), content));
            } catch (Exception e) {
                System.out.println();
                System.out.println("************************************* content ******************************");
                System.out.println(content);
                passing = false;
                throw new RuntimeException(e);
            }
            if (passing) {
                end_time = end_time - start_time;
                System.out.println("pass (" + end_time + " ms)");
                passed++;
            } else System.out.println("faild");
        }
        System.out.println(" " + passed + " of " + bench.keySet().size());
    }

    private String loadFile(String string) throws IOException {
        String result = null;
        File file = new File("test/src/org/exist/xslt/test/bench/v1_0/" + string);
        if (!file.canRead()) {
            throw new IOException("can load information.");
        } else {
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            int sz = (int) fc.size();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);
            Charset charset = Charset.forName("ISO-8859-15");
            CharsetDecoder decoder = charset.newDecoder();
            CharBuffer cb = decoder.decode(bb);
            result = cb.toString();
            if (result.startsWith("<?xml version=\"1.0\"?>")) result = result.substring("<?xml version=\"1.0\"?>".length());
            if (result.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>")) result = result.substring("<?xml version=\"1.0\" encoding=\"utf-8\"?>".length());
            fc.close();
        }
        return result;
    }
}
