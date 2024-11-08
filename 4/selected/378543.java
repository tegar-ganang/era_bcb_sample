package net.sourceforge.recman.backend.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import java.io.File;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import net.sourceforge.recman.backend.manager.pojo.Timer;
import net.sourceforge.recman.backend.parser.exception.VDRParserException;
import net.sourceforge.recman.backend.util.XmlUtilities;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

/**
 * @author Marcus Kessel
 * 
 */
public class TimerParserImplTest {

    private String channels = "/channels.conf";

    private String timers = "/timers.conf";

    private TimerParser timerParser;

    private ChannelParser channelParser;

    public TimerParserImplTest() {
        File channelsConf = this.urlToFile(channels);
        File timersConf = this.urlToFile(timers);
    }

    @Test
    public void testParse() throws ParseException {
        List<Timer> timers = null;
        try {
            timers = timerParser.parse();
        } catch (VDRParserException e) {
            e.printStackTrace();
            fail();
        }
        assertEquals(7, timers.size());
        Timer timer = timers.get(0);
        assertEquals("kabel eins", timer.getChannel());
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date date = formatter.parse("2010-01-04");
        assertEquals(XmlUtilities.getXmlDate(date), timer.getDay());
        assertFalse(timer.isMultiple());
        assertEquals(null, timer.getDays());
        assertEquals(null, timer.getDuration());
        assertEquals("What's up, Dad?", timer.getTitle());
        assertEquals(null, timer.getShortText());
        assertEquals(99, timer.getLifetime());
        assertEquals(50, timer.getPriority());
        assertEquals(XmlUtilities.getXmlTime("1610"), timer.getStart());
        assertEquals(1, timer.getStatus());
        assertEquals(XmlUtilities.getXmlTime("1647"), timer.getStop());
        timer = timers.get(1);
        assertEquals("DMAX", timer.getChannel());
        date = formatter.parse("2010-01-07");
        assertEquals(XmlUtilities.getXmlDate(date), timer.getDay());
        assertFalse(timer.isMultiple());
        assertEquals(null, timer.getDays());
        assertEquals(null, timer.getDuration());
        assertEquals("DMAX Wissen", timer.getTitle());
        assertEquals(null, timer.getShortText());
        assertEquals(99, timer.getLifetime());
        assertEquals(50, timer.getPriority());
        assertEquals(XmlUtilities.getXmlTime("2323"), timer.getStart());
        assertEquals(1, timer.getStatus());
        assertEquals(XmlUtilities.getXmlTime("0035"), timer.getStop());
        timer = timers.get(2);
        assertEquals("kabel eins", timer.getChannel());
        date = formatter.parse("2010-01-04");
        assertEquals(XmlUtilities.getXmlDate(date), timer.getDay());
        assertFalse(timer.isMultiple());
        assertEquals(null, timer.getDays());
        assertEquals(null, timer.getDuration());
        assertEquals("Anacondas", timer.getTitle());
        assertEquals("Die Jagd nach der Blut-Orchidee", timer.getShortText());
        assertEquals(99, timer.getLifetime());
        assertEquals(50, timer.getPriority());
        assertEquals(XmlUtilities.getXmlTime("2013"), timer.getStart());
        assertEquals(1, timer.getStatus());
        assertEquals(XmlUtilities.getXmlTime("2211"), timer.getStop());
        timer = timers.get(3);
        assertEquals("RTL Television,RTL", timer.getChannel());
        date = formatter.parse("2009-11-18");
        assertEquals(XmlUtilities.getXmlDate(date), timer.getDay());
        assertFalse(timer.isMultiple());
        assertEquals(null, timer.getDays());
        assertEquals(null, timer.getDuration());
        assertEquals("Serien~Gute Zeiten, schlechte Zeiten~Mit 18.11.2009-19", timer.getTitle());
        assertEquals("40", timer.getShortText());
        assertEquals(99, timer.getLifetime());
        assertEquals(50, timer.getPriority());
        assertEquals(XmlUtilities.getXmlTime("1930"), timer.getStart());
        assertEquals(1, timer.getStatus());
        assertEquals(XmlUtilities.getXmlTime("2025"), timer.getStop());
    }

    private File urlToFile(String filename) {
        URL url = this.getClass().getResource(filename);
        return FileUtils.toFile(url);
    }
}
