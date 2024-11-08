package vqwiki.file;

import java.io.File;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import vqwiki.Change;
import vqwiki.WikiBase;
import vqwiki.utils.Utilities;

/**
 * Test for FileChangeLog
 *
 * @author mteodori
 */
public class FileChangeLogTest extends AbstractFileTest {

    private static final Logger logger = Logger.getLogger(FileChangeLogTest.class.getName());

    private FileChangeLog fileChangeLog = new FileChangeLog();

    public void testNewParser() throws Exception {
        String filename = getClass().getResource("/recent-newformat.xml").getFile();
        List changes = FileChangeLog.parseXml(new File(filename));
        assertEquals(new Change(WikiBase.DEFAULT_VWIKI, "NextMeeting", "MisterX", Timestamp.valueOf("2008-10-12 08:02:12.327")), changes.get(0));
        assertEquals(new Change(WikiBase.DEFAULT_VWIKI, "NextMeeting", "Pippo", Timestamp.valueOf("2008-10-12 08:01:12.327")), changes.get(1));
        assertEquals(new Change(WikiBase.DEFAULT_VWIKI, "SocialNetworking", "MarioRossi", Timestamp.valueOf("2008-10-12 08:00:12.327")), changes.get(2));
    }

    public void testNewSerializer() throws Exception {
        String filename = getClass().getResource("/recent-newformat.xml").getFile();
        String changesXmlData = FileUtils.readFileToString(new File(filename), "UTF-8");
        List changes = new ArrayList();
        changes.add(new Change(WikiBase.DEFAULT_VWIKI, "NextMeeting", "MisterX", Timestamp.valueOf("2008-10-12 08:02:12.327")));
        changes.add(new Change(WikiBase.DEFAULT_VWIKI, "NextMeeting", "Pippo", Timestamp.valueOf("2008-10-12 08:01:12.327")));
        changes.add(new Change(WikiBase.DEFAULT_VWIKI, "SocialNetworking", "MarioRossi", Timestamp.valueOf("2008-10-12 08:00:12.327")));
        String serialized = FileChangeLog.serializeXml(changes);
        assertEquals(changesXmlData, serialized);
    }

    public void testSerializer() throws Exception {
        Date changeDate = new Date();
        Change change = new Change(WikiBase.DEFAULT_VWIKI, "MyTopic", "MyUser", changeDate);
        fileChangeLog.logChange(change, new MockHttpServletRequest());
        File serializedFile = FileHandler.getPathFor(WikiBase.DEFAULT_VWIKI, FileChangeLog.RECENT_FILE);
        String serialized = FileUtils.readFileToString(serializedFile, "UTF-8");
        logger.fine(serialized);
        assertTrue(serialized.indexOf("MyTopic") > -1);
        fileChangeLog.clearCache();
        Collection changes = fileChangeLog.getChanges(WikiBase.DEFAULT_VWIKI, changeDate);
        assertNotNull(changes);
        assertEquals(1, changes.size());
        change = (Change) changes.toArray(new Change[] {})[0];
        assertEquals("MyTopic", change.getTopic());
        assertEquals("MyUser", change.getUser());
        DateFormat df = DateFormat.getDateInstance();
        assertEquals(df.format(changeDate), df.format(change.getTime()));
        assertEquals(changeDate, change.getTime());
    }

    public void testParser() throws Exception {
        String filename = getClass().getResource("/" + FileChangeLog.RECENT_FILE).getFile();
        File file = new File(filename);
        FileUtils.copyFileToDirectory(file, wikiDir);
        assertTrue(Utilities.isJsxFile(file));
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DATE, 13);
        calendar.set(Calendar.MONTH, 8);
        calendar.set(Calendar.YEAR, 2008);
        Collection changes = fileChangeLog.getChanges(WikiBase.DEFAULT_VWIKI, calendar.getTime());
        logger.info("loaded changes: " + changes);
    }

    public void testParserFunc() throws Exception {
        String filename = getClass().getResource("/" + FileChangeLog.RECENT_FILE).getFile();
        File file = new File(filename);
        Map data = FileChangeLog.parseJsx(file);
        Collection changes = data.values();
        logger.info("loaded changes: " + changes);
    }
}
