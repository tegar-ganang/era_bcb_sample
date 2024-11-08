package com.io_software.utils.web.test;

import junit.framework.TestSuite;
import junit.framework.TestCase;
import java.util.Set;
import java.util.Map;
import java.util.Vector;
import java.util.Iterator;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import gnu.regexp.RE;
import com.io_software.utils.web.URLFilter;
import com.io_software.utils.web.ExcludeURLFilter;
import com.io_software.utils.web.HostBasedURLFilter;
import com.io_software.utils.web.HTMLPage;
import com.io_software.utils.web.Crawler;
import com.io_software.utils.web.CrawlerImpl;
import com.io_software.catools.search.KeywordQuery;
import com.io_software.catools.search.index.KeywordIndex;
import com.io_software.catools.search.index.IndexFileWrapper;
import com.abb.util.TeeReader;
import java.io.OutputStreamWriter;

/** Tests the {@link CrawlerImpl} class, performing a crawl over the locally
    installed webserver on <tt>localhost</tt>.

    @author Axel Uhl
    @version $Id: TestCrawler.java,v 1.4 2001/06/01 09:24:18 uhl Exp $
*/
public class TestCrawler extends TestCase {

    /** forwards to the superclass constructor

	@param name the selector telling the method to be executed in
	this test case
    */
    public TestCrawler(String name) {
        super(name);
    }

    /** tests creation of crawler based on a properties file */
    public void testCrawlerPropertiesFile() throws Throwable {
        CrawlerImpl c = new CrawlerImpl(new InputStreamReader(getClass().getResourceAsStream("CrawlerConfig.ccfg")));
        c.start();
        c.waitForLastRequestToFinish();
        KeywordIndex i = c.getKeywordIndex();
        KeywordQuery kq = new KeywordQuery("Performance");
        Set result = i.search(kq, null);
        assertEquals(new Integer(8), new Integer(result.size()));
    }

    /** Sets up a {@link CrawlerImpl} that starts its crawl at
	<tt>http://localhost/</tt>. The filter used for link filtering is a
	{@link HostBasedURLFilter} that gets the start URL as definition.
      */
    public void testCrawlerOnLocalWebserver() throws Throwable {
        File indexFile = new File("TestIndex.ser");
        computeAndStoreLocalhostIndex(indexFile);
        IndexFileWrapper ifw = new IndexFileWrapper(indexFile);
        KeywordQuery kq = new KeywordQuery("Performance");
        Set result = ifw.search(kq, null);
        assertEquals(new Integer(9), new Integer(result.size()));
        replaceIndexHtml();
        computeAndStoreLocalhostIndex(indexFile);
        result = ifw.search(kq, null);
        assertEquals(new Integer(1), new Integer(result.size()));
        undoReplaceIndexHtml();
    }

    /** replaces the root <tt>index.html</tt> file on the local web server's
	document root by a trivial file that contains only the keyword
	"Performance" in its body. This file is taken from a local resource
	and copied there. Before, the original index.html is saved as
	<tt>index.html.bak</tt>. See also {@link #undoReplaceIndexHtml}.
      */
    private void replaceIndexHtml() throws IOException {
        File indexHtml = new File(docRoot, "index.html");
        File bak = new File(docRoot, "index.html.bak");
        indexHtml.renameTo(bak);
        FileOutputStream fos = new FileOutputStream(indexHtml);
        InputStream is = getClass().getResourceAsStream("test_index.html");
        int read = is.read();
        while (read != -1) {
            fos.write(read);
            read = is.read();
        }
        fos.close();
        is.close();
    }

    /** removes the file <tt>index.html</tt> in the local web server's
	document root and moves the file <tt>index.html.bak</tt> to
	<tt>index.html</tt>. This will undo the effects of the method
	{@link #replaceIndexHtml}.
      */
    private void undoReplaceIndexHtml() throws IOException {
        File indexHtml = new File(docRoot, "index.html");
        File bak = new File(docRoot, "index.html.bak");
        indexHtml.delete();
        bak.renameTo(indexHtml);
    }

    /** computes the index over the local webserver and stores it into
	the specified file
	
	@param f the file to which to store the index in serialized format
      */
    private void computeAndStoreLocalhostIndex(File f) throws Exception {
        KeywordIndex ii = getLocalhostIndex();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
        oos.writeObject(ii);
        oos.close();
    }

    /** computes an index of the webserver running on localhost

	@return the index created from the server
    */
    private KeywordIndex getLocalhostIndex() throws Exception {
        URL initialURL = new URL("http://localhost/");
        Vector excludes = new Vector();
        RE excludeRE = new RE("/MyStoreApp/", RE.REG_MULTILINE);
        excludes.addElement(excludeRE);
        URLFilter filter = new ExcludeURLFilter(new HostBasedURLFilter(initialURL), excludes);
        Object[] initialURLAndFilter = new Object[] { initialURL, filter };
        Vector initialURLs = new Vector();
        initialURLs.addElement(initialURLAndFilter);
        Crawler c = new CrawlerImpl(initialURLs, null);
        c.start();
        c.waitForLastRequestToFinish();
        KeywordIndex i = c.getKeywordIndex();
        return i;
    }

    /** doc-root of the local webserver in the file system */
    private File docRoot = new File("d:/programme/Apache Group/Apache/htdocs");
}
