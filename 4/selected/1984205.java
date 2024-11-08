package yarfraw.rss20;

import java.io.File;
import java.util.List;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import junit.framework.TestCase;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import yarfraw.core.datamodel.CategorySubject;
import yarfraw.core.datamodel.ChannelFeed;
import yarfraw.core.datamodel.Cloud;
import yarfraw.core.datamodel.Day;
import yarfraw.core.datamodel.FeedFormat;
import yarfraw.core.datamodel.Id;
import yarfraw.core.datamodel.Image;
import yarfraw.core.datamodel.ItemEntry;
import yarfraw.core.datamodel.TextInput;
import yarfraw.io.FeedAppender;
import yarfraw.io.FeedReader;
import yarfraw.io.FeedWriter;
import yarfraw.utils.reader.FeedReaderUtils;

/**
 * Some unit tests for Reader/Writer/Appender
 * 
 * @author jliang
 *
 */
public class IOTest extends TestCase {

    private static final Log LOG = LogFactory.getLog(IOTest.class);

    @Test
    public void testBuilder() throws Exception {
        ChannelFeed c = BuilderTest.buildChannel();
        FeedWriter w = new FeedWriter("testTmpOutput/rss20/testBuilder.xml");
        c.setTitle("<test>test</test>");
        w.writeChannel(c);
        w.writeChannel(c, new ValidationEventHandler() {

            public boolean handleEvent(ValidationEvent event) {
                System.out.println(event);
                return false;
            }
        });
    }

    @Test
    public void testBuilder2() throws Exception {
        ChannelFeed c = BuilderTest.buildChannel();
        FeedWriter w = new FeedWriter("testTmpOutput/rss20/testBuilder2.xml");
        w.setFormat(FeedFormat.RSS10);
        w.writeChannel(c);
    }

    @Test
    public void testRead() throws Exception {
        FeedReader r = new FeedReader(Thread.currentThread().getContextClassLoader().getResource("yarfraw/digg.xml").toURI());
        ChannelFeed c = r.readChannel();
        r.readChannel(new ValidationEventHandler() {

            public boolean handleEvent(ValidationEvent event) {
                LOG.error(ToStringBuilder.reflectionToString(event));
                return false;
            }
        });
        assertTrue("Title is not the same", "digg".equals(c.getTitleText()));
        assertTrue("language is not the same", "en-us".equals(c.getLang()));
        assertTrue("Link is not the same", "http://digg.com/".equals(c.getLinks().get(0).getHref()));
    }

    @Test
    public void testRead2() throws Exception {
        FeedReader r = new FeedReader(Thread.currentThread().getContextClassLoader().getResource("yarfraw/yarfraw.xml").toURI());
        assertTrue(!r.isRemoteRead());
        ChannelFeed c = r.readChannel();
        ChannelFeed c2 = new ChannelFeed().setTitle("Test Title").addLink("http://www.test.com").setDescriptionOrSubtitle("Descritpion ......... ").setRights("Copyright 2002, Spartanburg Herald-Journal").addManagingEditorOrAuthorOrPublisher("geo@herald.com").addWebMasterOrCreator("betty@herald.com (Betty Guernsey)").setLang("en").setPubDate("Tue, 14 Aug 2007 15:32:11 EDT").setLastBuildOrUpdatedDate("Tue, 14 Aug 2007 15:32:11 EDT").addCategorySubject("cat1").addCategorySubject("cat2").setGenerator("MightyInHouse Content System v2.3").setDocs("http://blogs.law.harvard.edu/tech/rss").setCloud(new Cloud("rpc.sys.com", "80", "/RPC2", "pingMe", "soap")).setTtl(60).setImageOrIcon(new Image().setUrl("http://my.image.com/image.jpg").setTitle("Test Image").setLink("http://my.image.com/image.jpg")).setTexInput(new TextInput("Title", "Descritpion", "name", "http://link.com/link")).addSkipDay(Day.Saturday, Day.Sunday).addSkipHour(12, 0, 1, 2, 3, 4, 5).addItem(new ItemEntry().setTitle("Item 1").addLink("http://somelink.com/").setDescriptionOrSummary("desc").addAuthorOrCreator("oprah@oxygen.net").addCategorySubject("cat1", "cat2").addCategorySubject(new CategorySubject("cat3").setDomainOrScheme("http://somedomain")).setComments("http://www.myblog.org/cgi-local/mt/mt-comments.cgi?entry_id=290").setUid(new Id("GUID").setPermaLink(true)), new ItemEntry().setTitle("Item2").addLink("http://somelink.com/").setDescriptionOrSummary("desc").addAuthorOrCreator("oprah@oxygen.net").addCategorySubject("cat1", "cat2").addCategorySubject(new CategorySubject("cat3").setDomainOrScheme("http://somedomain")).setComments("http://www.myblog.org/cgi-local/mt/mt-comments.cgi?entry_id=290").setUid(new Id("GUID").setPermaLink(true)));
        assertEquals(c, c2);
    }

    @Test
    public void testAppend() throws Exception {
        File f = new File(Thread.currentThread().getContextClassLoader().getResource("yarfraw/digg.xml").toURI());
        FeedAppender a = new FeedAppender(f);
        List<ItemEntry> items = BuilderTest.buildChannel().getItems();
        ItemEntry item = items.get(0);
        a.appendAllItemsToEnd(item);
        ChannelFeed c = FeedReaderUtils.read(FeedFormat.RSS20, f);
        assertEquals(item, c.getItems().get(c.getItems().size() - 1));
        int oldSize = c.getItems().size();
        a.removeItem(oldSize - 1);
        c = FeedReaderUtils.read(FeedFormat.RSS20, f);
        assertEquals(oldSize - 1, c.getItems().size());
    }

    @Test
    public void testAppend2() throws Exception {
        File f = new File(Thread.currentThread().getContextClassLoader().getResource("yarfraw/digg.xml").toURI());
        File copy = new File("testTmpOutput/rss20/testAppend.xml");
        List<ItemEntry> items = BuilderTest.buildChannel().getItems();
        FeedWriter w = new FeedWriter(copy);
        w.writeChannel(new FeedReader(f).readChannel());
        FeedAppender a = new FeedAppender(copy);
        a.setNumItemToKeep(10);
        a.appendAllItemsToBeginning(items.get(0));
        FeedReader r = new FeedReader(copy);
        assertEquals(10, r.readChannel().getItems().size());
        a.appendAllItemsToBeginning(items);
        assertEquals(10, r.readChannel().getItems().size());
        a.setItem(0, BuilderTest.buildChannel().getItems().get(1));
        assertEquals("item not set correctly", r.readChannel().getItems().get(0), items.get(1));
        a.appendAllItemsToBeginning(BuilderTest.buildChannel().getItems().get(1));
        assertEquals("item not added correctly", r.readChannel().getItems().get(0), items.get(1));
    }
}
