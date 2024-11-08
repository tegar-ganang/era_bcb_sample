package org.exist.xslt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.StringTokenizer;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.TransformerHandler;
import junit.framework.Assert;
import org.exist.memtree.DocumentImpl;
import org.exist.memtree.NodeImpl;
import org.exist.security.xacml.AccessContext;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.w3c.tests.TestCase;
import org.exist.xquery.CompiledXQuery;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQuery;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.Sequence;
import org.junit.Before;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 *
 */
public class XSLTS_case extends TestCase {

    private static final String XSLT_COLLECTION = "XSLTS";

    private static final String XSLTS_folder = XSLT_COLLECTION + "_1_1_0";

    @Override
    public void loadTS() throws Exception {
        BrokerPool.getInstance().getConfiguration().setProperty(TransformerFactoryAllocator.PROPERTY_TRANSFORMER_CLASS, "org.exist.xslt.TransformerFactoryImpl");
    }

    @Before
    public void setUp() throws Exception {
        synchronized (database) {
            if (testCollection == null) {
                loadTS();
            }
        }
    }

    protected void testCase(String inputURL, String xslURL, String outputURL, String expectedError) throws Exception {
        DBBroker broker = null;
        try {
            XQueryContext context;
            broker = pool.get(pool.getSecurityManager().getSystemSubject());
            context = new XSLContext(pool);
            TransformerFactoryImpl factory = new TransformerFactoryImpl();
            factory.setBrokerPool(pool);
            Templates templates = factory.newTemplates(new SourceImpl(loadVarFromURI(context, testLocation + XSLTS_folder + "/TestInputs/" + xslURL).getDocument()));
            TransformerHandler handler = factory.newTransformerHandler(templates);
            NodeImpl input;
            if (inputURL != null && inputURL != "") input = loadVarFromURI(context, testLocation + XSLTS_folder + "/TestInputs/" + inputURL); else input = loadVarFromString(context, "<empty/>");
            Transformer transformer = handler.getTransformer();
            Sequence result = ((org.exist.xslt.Transformer) transformer).transform(input);
            boolean ok = false;
            if (outputURL == null || outputURL.equals("")) {
                Assert.fail("expected error: " + expectedError);
            } else {
                Document doc = new DocumentImpl(context, false);
                Element outputFile = doc.createElement("outputFile");
                outputFile.setAttribute("compare", "Fragment");
                outputFile.setTextContent(outputURL);
                if (compareResult("", XSLTS_folder + "/ExpectedTestResults/", outputFile, result)) {
                    ok = true;
                }
            }
            if (!ok) Assert.fail("expected \n" + "[" + readFileAsString(new File(testLocation + XSLTS_folder + "/ExpectedTestResults/", outputURL)) + "]\n" + ", get \n[" + sequenceToString(result) + "]");
        } catch (XPathException e) {
            String error = e.getMessage();
            if (!expectedError.isEmpty()) ; else {
                e.printStackTrace();
                Assert.fail("expected error is " + expectedError + ", get " + error + " [" + e.getMessage() + "]");
            }
        } finally {
            pool.release(broker);
        }
    }

    private boolean checkResult(String file, String result) throws Exception {
        int tokenCount = 0;
        String ref = loadFile("test/external/XSLTS_1_1_0/ExpectedTestResults/" + file, false);
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
                System.out.println("expected:");
                System.out.println(ref);
                System.out.println("get:");
                System.out.println(result);
                throw new Exception("result should have: " + refToken + ", but get EOF (at " + tokenCount + ")");
            }
            String resToken = resTokenizer.nextToken();
            if (!refToken.equals(resToken)) {
                System.out.println(ref);
                System.out.println(result);
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

    private String loadFile(String fileURL, boolean incapsulate) throws IOException {
        String result = null;
        File file = new File(fileURL);
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
            if (result.startsWith("<?xml ")) {
                int endAt = result.indexOf("?>");
                result = result.substring(endAt + 2);
            }
            if (incapsulate) {
                result = result.replaceAll("\\{", "\\{\\{");
                result = result.replaceAll("\\}", "\\}\\}");
            }
            fc.close();
        }
        return result;
    }
}
