package org.ttalbott.mytelly;

import java.io.*;
import java.util.Map;
import java.util.Vector;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit.Parser;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.swingui.TestRunner;

/** 
 *
 * @author  Tom Talbott
 * @version 
 */
public class ZapListingsTest extends TestCase implements LogCallback {

    ZapListings3 m_zap = new ZapListings3();

    PrintStream debugOut;

    /**
     * Constructs a ZapListingsTest with the specified name.
     *
     * @param name Test case name.
     */
    public ZapListingsTest(String name) {
        super(name);
    }

    /**
     * Sets up the test fixture.
     *
     * Called before every test case method.
     */
    protected void setUp() {
        try {
            debugOut = new PrintStream(new FileOutputStream(new File("debug_out.txt")));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            debugOut = null;
        }
    }

    /**
     * Tears down the test fixture.
     *
     * Called after every test case method.
     */
    protected void tearDown() {
    }

    public void testGetProviders() {
        assertNotNull("Unable to instantiate ZapListings", m_zap);
        Map providers = m_zap.getProviders(null, "98103", this, true);
        assertNotNull("No providers retrieved(1)", providers);
        assertTrue("No providers retrieved(2)", providers.size() > 0);
        assertEquals("Not enough providers retrieved", 11, providers.size());
    }

    public void testScrapeRowStorage() {
        org.ttalbott.mytelly.ScrapeRow sr = new ScrapeRow();
        SimpleAttributeSet sas = new SimpleAttributeSet();
        sas.addAttribute("align", "center");
        sr.handleStartTag(Tag.TD, sas, 0);
        int i = sr.getInField();
        assertEquals("ScrapeRow infield is incorrect", 1, i);
        String testText = "This is a text test";
        sr.handleText(testText.toCharArray(), 10);
        sr.handleEndTag(Tag.TD, 30);
        Vector v = sr.getVector();
        Map m = (Map) v.get(0);
        assertTrue("ScrapeRow Vector doesn't contain starttag", m.containsKey(ScrapeRow.STARTTAG));
        assertEquals("ScrapeRow Vector doesn't have right tag", Tag.TD.toString(), (String) m.get(ScrapeRow.STARTTAG));
        assertTrue("ScrapeRow Vector doesn't contain attr", m.containsKey(ScrapeRow.ATTR));
        Map attrMap = (Map) m.get(ScrapeRow.ATTR);
        assertEquals("ScrapeRow attr map size incorrect", 1, attrMap.size());
        m = (Map) v.get(1);
        assertNotNull("ScrapeRow Vector empty in pos 1", m);
        assertTrue("ScrapeRow Vector doesn't contain text", m.containsKey(ScrapeRow.TEXT));
        assertEquals("ScrapeRow Vector doesn't contain correct text", testText, m.get(ScrapeRow.TEXT));
        assertEquals("ScrapeRow Vector size is incorrect", 3, v.size());
        m = (Map) v.get(2);
        assertTrue("ScrapeRow Vector doesn't contain endtag", m.containsKey(ScrapeRow.ENDTAG));
        assertEquals("ScrapeRow Vector doesn't have right tag", Tag.TD.toString(), (String) m.get(ScrapeRow.ENDTAG));
    }

    public void testScrapeRow() throws IOException {
        ParserGetter kit = new ParserGetter();
        Parser parser = kit.getParser();
        StringReader r = new StringReader(testHTML);
        ScrapeRow callback = new ScrapeRow();
        assertNotNull("callback not instantiated", callback);
        parser.parse(r, callback, false);
        String summary = callback.summarize();
        assertEquals("Incorrect summary", testHTMLSummary, summary);
        String HREF = callback.getHREF(6);
        assertEquals("Wrong HREF", testHTMLHREF, HREF);
        String SRC = callback.getSRC(2);
        assertEquals("Wrong SRC", testHTMLSRC, SRC);
    }

    public void testGetChannelList() throws Exception {
        assertNotNull("Unable to instantiate ZapListings", m_zap);
        Vector channels = m_zap.getChannelList(null, "98103", "262184", this, true);
        assertNotNull("Null channel list returned", channels);
        assertTrue("No channels returned", channels.size() > 0);
        for (int i = 0; i < channels.size(); i++) {
            Map channel = (Map) channels.get(i);
            assertTrue("Desc does not exist for this channel (" + i + ")", channel.containsKey(ZapListings3.DESC));
            String desc = (String) channel.get(ZapListings3.DESC);
            assertTrue("Desc is empty for this channel (" + i + ")", desc.length() > 0);
        }
    }

    public void testScraper() throws Exception {
        Scraper scraper = new Scraper(null, "98103", "262184", this, 20000, debugOut);
        assertNotNull("Scraper not instantiated", scraper);
        assertTrue("Bad response from server", scraper.getLastResponseCode() < 300);
        int i = 0;
        while (testChannels[i][DESCRIPTION] != null) {
            int count = scraper.readSchedule(testChannels[i][STATION], testChannels[i][DESCRIPTION], "14", "1", "2002");
            assertTrue("No programs read", count != 0);
            Vector programs = scraper.getPrograms();
            FileWriter fw = new FileWriter(testChannels[i][STATION] + ".txt", false);
            fw.write(programs.toString());
            fw.close();
            i++;
        }
    }

    public void testGetListings() throws Exception {
        GetListings gl = new GetListings();
        Vector channels = m_zap.getChannelList(null, "98103", "262184", this, true);
        assertTrue("No channels found", channels.size() > 0);
        gl.writeChannelsToFile(channels, debugOut);
        gl.grab(channels, null, "98103", "262184", 10, 20000, this, null, debugOut);
    }

    /**
     * Assembles and returns a test suite for
     * all the test methods of this test case.
     *
     * @return A non-null test suite.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(ZapListingsTest.class);
        return suite;
    }

    /**
     * Runs the test case.
     *
     * Uncomment either the textual UI, Swing UI, or AWT UI.
     */
    public static void main(String args[]) {
        String[] testCaseName = { ZapListingsTest.class.getName(), "-noloading" };
        TestRunner.main(testCaseName);
    }

    public void start(java.lang.String text) {
        System.out.println(text);
    }

    public boolean update(java.lang.String text, java.lang.String log) {
        if (text != null) System.out.println(text);
        if (log != null) System.out.println(log);
        return true;
    }

    public void end(java.lang.String text, java.lang.String log, boolean now) {
        if (text != null) System.out.println(text);
        if (log != null) System.out.println(log);
    }

    public boolean getCancel() {
        return false;
    }

    public boolean retry(java.lang.String title, java.lang.String desc) {
        return false;
    }

    private static final String testHTML = "<td align=\"center\"><img src=\"http://tvlistings.zap2it.com/tms_network_logos/cbc.gif\"><br><font face=\"verdana,arial,helvetica\" size=\"1\" color=\"#000000\"><b><a href=\"listings_redirect.asp?station_num=10100\">1<br><nobr>CBUT</nobr></a></b></font></td><td bgcolor=\"#ffffcc\" colspan=\"1\" valign=\"top\"><font face=\"arial,helvetica,sans-serif\" size=2><b><a href=\"progdetails.asp?prog_id=1061863\">After Hours</a></font><font face=\"arial,helvetica\" size=\"-2\"></b>     CC  			</font></td><td bgcolor=\"#ffffcc\" colspan=\"4\" valign=\"top\"><font face=\"arial,helvetica,sans-serif\" size=2><b><a href=\"progdetails.asp?prog_id=1862497&series_id=1799853\">Classic Hockey: <i>1984 Stanley Cup Finals, Game Seven</i></a></font><font face=\"arial,helvetica\" size=\"-2\"></b>    N.Y. Islanders visit the Edmonton Oilers. CC  			</font></td><td bgcolor=\"#ffffcc\" colspan=\"1\" valign=\"top\"><font face=\"arial,helvetica,sans-serif\" size=2><b><a href=\"progdetails.asp?prog_id=369792\">SIGN OFF</a></font><font face=\"arial,helvetica\" size=\"-2\"></b></font></td>";

    private static final String testHTMLSummary = "<td><img><br><font><b><a><text>1</text><br><nobr><text>CBUT</text></nobr></a></b></font></td><td><font><b><a><text>After Hours</text></a><font></font></b><text> CC </text></font></td><td><font><b><a><text>Classic Hockey: </text><i><text>1984 Stanley Cup Finals, Game Seven</text></i></a><font></font></b><text> N.Y. Islanders visit the Edmonton Oilers. CC </text></font></td><td><font><b><a><text>SIGN OFF</text></a><font></font></b></font></td>";

    private static final String testHTMLHREF = "listings_redirect.asp?station_num=10100";

    private static final String testHTMLSRC = "http://tvlistings.zap2it.com/tms_network_logos/cbc.gif";

    private static final int DESCRIPTION = 0;

    private static final int ICON = 1;

    private static final int LETTERS = 2;

    private static final int NUMBER = 3;

    private static final int STATION = 4;

    private static final String testChannels[][] = { { "2 NWCN", "", "NWCN", "2", "16976" }, { "3 KWPX", "http://tvlistings.zap2it.com/tms_network_logos/Pax_30.jpg", "KWPX", "3", "15659" }, { "4 KOMO", "http://tvlistings.zap2it.com/tms_network_logos/abc_30.jpg", "KOMO", "4", "10626" }, { "5 KING", "http://tvlistings.zap2it.com/tms_network_logos/nbc_30.jpg", "KING", "5", "10518" }, { "6 KONG", "", "KONG", "6", "15662" }, { "7 KIRO", "http://tvlistings.zap2it.com/tms_network_logos/cbs_30.jpg", "KIRO", "7", "10520" }, { "8 DSC", "http://tvlistings.zap2it.com/tms_network_logos/discoverychannel_30.jpg", "DSC", "8", "12500" }, { "9 KCTS", "http://tvlistings.zap2it.com/tms_network_logos/pbs_30.jpg", "KCTS", "9", "10392" }, { "10 KTWB", "http://tvlistings.zap2it.com/tms_network_logos/wb_30.jpg", "KTWB", "10", "10769" }, { null, null, null, null, null } };
}
