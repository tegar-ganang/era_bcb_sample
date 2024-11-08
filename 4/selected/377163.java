package net.sourceforge.recman.backend.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.File;
import java.net.URL;
import java.util.List;
import org.apache.commons.io.FileUtils;
import net.sourceforge.recman.backend.manager.pojo.Recording;
import net.sourceforge.recman.backend.manager.pojo.StreamInfo;
import net.sourceforge.recman.backend.parser.exception.VDRParserException;
import net.sourceforge.recman.backend.util.XmlUtilities;
import org.junit.Test;

/**
 * @author Marcus Kessel
 * 
 */
public class RecordingParserImplTest {

    private RecordingParser recordingParser = new RecordingParserImpl();

    @Test
    public void testParseOldZDF() {
        String filename = "/parsertestdata/old/zdf/info.vdr";
        File file = this.urlToFile(filename);
        Recording recording = null;
        try {
            recording = recordingParser.parse(file);
        } catch (VDRParserException e) {
            e.printStackTrace();
            fail();
        }
        System.out.println(recording);
        assertEquals("ZDF", recording.getChannelName());
        assertEquals("ZDF in concert Robbie Williams in London", recording.getTitle());
        String desc = "Drei Jahre hat er die Bühne gemieden, jetzt ist er wieder da! Robbie Williams hat am 20. Oktober 2009 ein aufsehenerregendes Comeback mit einem Konzert im Londoner Roundhouse gefeiert. Das ZDF zeigt im Rahmen von \"ZDF in concert\" exklusiv einen 60-minütigen Zusammenschnitt mit allen Höhepunkten dieses einmaligen Konzertes. Deutschland, 2009";
        assertEquals(desc, recording.getDescription());
        assertEquals("Kurzer Titel des Konzerts", recording.getShortText());
        assertEquals(2, recording.getParts());
        assertEquals(file.getParent(), recording.getPath());
        assertFalse(recording.isTs());
        List<StreamInfo> streamInfos = recording.getStreamInfos();
        assertEquals(5, streamInfos.size());
        assertEquals(2, streamInfos.get(0).getStream());
        assertEquals("03", streamInfos.get(0).getType());
        assertEquals("deu", streamInfos.get(0).getLang());
        assertEquals(2, streamInfos.get(1).getStream());
        assertEquals("03", streamInfos.get(1).getType());
        assertEquals("deu", streamInfos.get(1).getLang());
        assertEquals(2, streamInfos.get(2).getStream());
        assertEquals("05", streamInfos.get(2).getType());
        assertEquals("deu", streamInfos.get(2).getLang());
        assertEquals(1, streamInfos.get(3).getStream());
        assertEquals("03", streamInfos.get(3).getType());
        assertEquals("deu", streamInfos.get(3).getLang());
        assertEquals(3, streamInfos.get(4).getStream());
        assertEquals("03", streamInfos.get(4).getType());
        assertEquals("deu", streamInfos.get(4).getLang());
        assertEquals(XmlUtilities.getXmlGregorianCalendar(1257637800), recording.getStartDate());
        assertEquals(XmlUtilities.getXmlDuration(3600), recording.getDuration());
        assertEquals(XmlUtilities.getXmlDuration(4320), recording.getTotalDuration());
    }

    @Test
    public void testParseNewSDTV() {
        String filename = "/parsertestdata/new/sdtv/info";
        File file = this.urlToFile(filename);
        Recording recording = null;
        try {
            recording = recordingParser.parse(file);
        } catch (VDRParserException e) {
            e.printStackTrace();
            fail();
        }
        System.out.println(recording);
        assertEquals("Das Erste", recording.getChannelName());
        assertEquals("Tagesschau", recording.getTitle());
        assertEquals(null, recording.getDescription());
        assertEquals(null, recording.getShortText());
        assertEquals(1, recording.getParts());
        assertEquals(file.getParent(), recording.getPath());
        assertTrue(recording.isTs());
        List<StreamInfo> streamInfos = recording.getStreamInfos();
        assertEquals(4, streamInfos.size());
        assertEquals(1, streamInfos.get(0).getStream());
        assertEquals("03", streamInfos.get(0).getType());
        assertEquals("deu", streamInfos.get(0).getLang());
        assertEquals(2, streamInfos.get(1).getStream());
        assertEquals("03", streamInfos.get(1).getType());
        assertEquals("deu", streamInfos.get(1).getLang());
        assertEquals(2, streamInfos.get(2).getStream());
        assertEquals("05", streamInfos.get(2).getType());
        assertEquals("deu", streamInfos.get(2).getLang());
        assertEquals(2, streamInfos.get(3).getStream());
        assertEquals("40", streamInfos.get(3).getType());
        assertEquals("deu", streamInfos.get(3).getLang());
        assertEquals(XmlUtilities.getXmlGregorianCalendar(1258462800), recording.getStartDate());
        assertEquals(XmlUtilities.getXmlDuration(600), recording.getDuration());
        assertEquals(XmlUtilities.getXmlDuration(0), recording.getTotalDuration());
    }

    @Test
    public void testParseNoFileExist() {
        File file = new File("xyz.vdr");
        Recording recording = null;
        try {
            recording = recordingParser.parse(file);
            fail();
            assertNull(recording);
        } catch (VDRParserException e) {
            e.printStackTrace();
        }
    }

    private File urlToFile(String filename) {
        URL url = this.getClass().getResource(filename);
        return FileUtils.toFile(url);
    }
}
