package yarfraw.coverage;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import yarfraw.atom10.BuilderTest;
import yarfraw.core.datamodel.ChannelFeed;
import yarfraw.core.datamodel.FeedFormat;
import yarfraw.core.datamodel.ItemEntry;
import yarfraw.io.FeedAppender;
import yarfraw.io.FeedReader;
import yarfraw.io.FeedWriter;

/**
 * Random tests to invoke code that was reported no covered in cobertura' coverage report
 * @author jliang
 *
 */
public class IOTest extends TestCase {

    @Test
    public void testAppender() throws Exception {
        FeedAppender a = new FeedAppender("test.xml");
        assertTrue("default format is Rss 2.0", a.getFormat() == FeedFormat.RSS20);
        a.setFormat(FeedFormat.RSS10);
        assertTrue("format should be Rss 1.0", a.getFormat() == FeedFormat.RSS10);
        a.setFormat(FeedFormat.ATOM10);
        assertTrue("format should be Atom 1.0", a.getFormat() == FeedFormat.ATOM10);
        a.setNumItemToKeep(10);
        assertTrue("num to keep not correct", a.getNumItemToKeep() == 10);
    }

    @Test
    public void testInputStream() throws Exception {
        ChannelFeed rss20 = FeedReader.readChannel(FeedFormat.RSS20, Thread.currentThread().getContextClassLoader().getResourceAsStream("yarfraw/digg.xml"));
        ChannelFeed rss10 = FeedReader.readChannel(FeedFormat.RSS10, Thread.currentThread().getContextClassLoader().getResourceAsStream("yarfraw/rss10/rdfModule.xml"));
        ChannelFeed atom10 = FeedReader.readChannel(FeedFormat.ATOM10, Thread.currentThread().getContextClassLoader().getResourceAsStream("yarfraw/atom10/atom10b.xml"));
        assertTrue(rss20.getTitle() != null);
        assertTrue(rss10.getTitle() != null);
        assertTrue(atom10.getTitle() != null);
    }

    @Test
    public void testOutputStream() throws Exception {
        File f = new File("testTmpOutput/coverage/testOutputStream.xml");
        FeedWriter.writeChannel(FeedFormat.ATOM10, BuilderTest.buildChannel(), new FileOutputStream(f));
    }

    @Test
    public void testAppend() throws Exception {
        File f = new File(Thread.currentThread().getContextClassLoader().getResource("yarfraw/rss2sample.xml").toURI());
        File copy = new File("testTmpOutput/coverage/testAppend.xml");
        FileUtils.copyFile(f, copy);
        FeedAppender a = new FeedAppender(copy);
        a.setNumItemToKeep(3);
        List<ItemEntry> items = yarfraw.rss20.BuilderTest.buildChannel().getItems();
        a.appendAllItemsToBeginning(items.get(0));
        FeedReader r = new FeedReader(copy);
        assertEquals(3, r.readChannel().getItems().size());
        a.appendAllItemsToBeginning(items);
        assertEquals(3, r.readChannel().getItems().size());
        a.setItem(2, items.get(0));
        assertEquals("item not set correctly", r.readChannel().getItems().get(2), items.get(0));
        a.appendAllItemsToEnd(items);
        ChannelFeed c = r.readChannel();
        assertEquals("item not added correctly", c.getItems().get(2), items.get(0));
    }

    @Test
    public void testEncoding() throws Exception {
        ChannelFeed c = new ChannelFeed().setDescriptionOrSubtitle("<div xmlns=\"http://www.w3.org/1999/xhtml\">" + "<p><i>[Update: The Atom draft is finished.]</i></p>" + "</div>");
        FeedWriter w = new FeedWriter("testTmpOutput/atom10/testEncoding.xml");
        w.writeChannel(c);
    }
}
