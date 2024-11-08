package ikube.web.toolkit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import ikube.IConstants;
import ikube.toolkit.FileUtilities;
import ikube.toolkit.XmlUtilities;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.TextExtractor;
import org.dom4j.Document;
import org.dom4j.Element;

/**
 * TODO This test just executes the Jsps. What we really want to do is crawl the UI and verify that there are no exceptions. Also we want to
 * fill in all the forms and then execute the buttons and verify that there are no exceptions. We also want to verify that there are no
 * broken links in the pages.
 * 
 * @author Michael Couck
 * @since 26.07.11
 * @version 01.00
 */
public class JspStrategy extends AStrategy {

    private static final String PATH = "path";

    private static final String VIEW_CONTROLLER_TAG_NAME = "mvc:view-controller";

    private static final String WEB_CONTEXT_FILE = "web-application-context.xml";

    public JspStrategy(String context, int port) {
        super(context, port);
    }

    @Override
    public void perform() throws Exception {
        List<File> webApplicationContextFiles = FileUtilities.findFilesRecursively(new File("."), new ArrayList<File>(), WEB_CONTEXT_FILE);
        for (File webApplicationcontextFile : webApplicationContextFiles) {
            if (webApplicationcontextFile.getName().contains("svn")) {
                continue;
            }
            Document document = XmlUtilities.getDocument(new FileInputStream(webApplicationcontextFile), IConstants.ENCODING);
            List<Element> elements = XmlUtilities.getElements(document.getRootElement(), VIEW_CONTROLLER_TAG_NAME);
            for (Element element : elements) {
                String host = "localhost";
                String path = element.attributeValue(PATH);
                verifyPage(host, path);
            }
        }
    }

    private void verifyPage(String host, String path) throws Exception {
        StringBuilder urlString = new StringBuilder("http://");
        urlString.append(host);
        urlString.append(":");
        urlString.append(port);
        urlString.append(context);
        urlString.append(path);
        logger.info("Executing page : " + urlString);
        URL url = new URL(urlString.toString());
        String content = FileUtilities.getContents(url.openStream(), Integer.MAX_VALUE).toString();
        assertNotNull(content);
        verifyContent(new ByteArrayInputStream(content.getBytes()));
    }

    private void verifyContent(InputStream inputStream) throws Exception {
        Reader reader = new InputStreamReader(inputStream, IConstants.ENCODING);
        Source source = new Source(reader);
        source.fullSequentialParse();
        TextExtractor textExtractor = new TextExtractor(source);
        textExtractor.setIncludeAttributes(true);
        String textContent = textExtractor.toString();
        assertTrue(textContent.contains("ikube"));
    }
}
