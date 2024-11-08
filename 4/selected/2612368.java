package org.ttalbott.mytelly;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;
import org.apache.oro.text.perl.MalformedPerl5PatternException;
import org.apache.oro.text.perl.Perl5Util;

/**
 *
 * @author  Tom Talbott
 * @version 
 */
public class Programs extends java.lang.Object {

    private static Programs m_instance = null;

    private Perl5Util m_regexp = new Perl5Util();

    private static Channels m_channels = null;

    private static Map m_channelMap = null;

    private ProgramData m_programData = null;

    /** Creates new Programs */
    private Programs() {
    }

    private Programs(ProgramData programData) {
        setProgramData(programData);
    }

    public void setProgramData(ProgramData programData) {
        m_channels = null;
        m_channelMap = null;
        m_programData = programData;
    }

    public ProgramData getProgramData() {
        return m_programData;
    }

    public void releaseProgramData() {
        m_channels = null;
        m_channelMap = null;
        m_programData = null;
    }

    public static Programs getInstance() {
        if (m_instance == null) m_instance = new Programs();
        return m_instance;
    }

    public static void release() {
        m_instance = null;
    }

    public ProgramList advancedSearch(Hiddens m_hiddens, String m_searchText, String m_channelText, String m_category, boolean m_distinct, boolean m_titlesOnly, Progress progress) throws MalformedPerl5PatternException {
        if (m_programData != null) {
            ProgramList progs = m_programData.getPrograms();
            String hiddenIndex = m_hiddens.getHiddenIndex();
            return m_programData.advancedSearch(progs, m_searchText, m_channelText, m_category, m_distinct, m_titlesOnly, hiddenIndex, progress);
        } else return null;
    }

    public ProgramList search(String regexp, Progress progress) throws MalformedPerl5PatternException {
        return search(regexp, null, progress);
    }

    public ProgramList search(String regexp, String field, Progress progress) throws MalformedPerl5PatternException {
        if (m_programData != null) {
            ProgramList progs = m_programData.getPrograms();
            return search(progs, regexp, field, progress);
        } else return null;
    }

    private ProgramList search(ProgramList progs, String regexp, String field, Progress progress) throws MalformedPerl5PatternException {
        if (m_programData != null) {
            return m_programData.search(progs, regexp, field, progress);
        } else return null;
    }

    public String getData(ProgItem prog, String tag) {
        if (m_programData != null) {
            return m_programData.getData(prog, tag);
        } else return null;
    }

    public String getStartTime(ProgItem prog) {
        return getData(prog, ProgramData.START);
    }

    public String getStopTime(ProgItem prog) {
        return getData(prog, ProgramData.STOP);
    }

    public String getChannel(ProgItem prog) {
        return getData(prog, ProgramData.CHANNEL);
    }

    public static String getChannelDesc(String channel) {
        Channels channels = getChannels();
        if (channels != null) {
            if (m_channelMap == null) m_channelMap = channels.getItems();
            Map data = (Map) m_channelMap.get(channel);
            if (data != null) return (String) data.get(ChannelData.DISPLAYNAME);
        }
        return channel;
    }

    public static Channels getChannels() {
        if (m_channels == null) {
            m_channels = Channels.getInstance();
        }
        return m_channels;
    }

    public Vector getCategories(ProgItem prog) {
        if (m_programData != null) {
            return m_programData.getCategories(prog);
        } else return null;
    }

    public boolean getPreviouslyShown(ProgItem prog) {
        if (m_programData != null) {
            return m_programData.getPreviouslyShown(prog);
        } else return false;
    }

    public boolean getStereo(ProgItem prog) {
        if (m_programData != null) {
            return m_programData.getStereo(prog);
        } else return false;
    }

    public boolean getClosedCaption(ProgItem prog) {
        if (m_programData != null) {
            return m_programData.getClosedCaption(prog);
        } else return false;
    }

    public int getProgramCount() {
        if (m_programData != null) return m_programData.getProgramCount(); else return 0;
    }

    public ProgramList getPrograms() {
        if (m_programData != null) return m_programData.getPrograms(); else return null;
    }

    public ProgramList getProgramsSortedByTime() {
        if (m_programData != null) return m_programData.getProgramsSortedByTime(); else return null;
    }

    public ProgramList getEmptyProgramList() {
        if (m_programData != null) return m_programData.getEmptyProgramList(); else return null;
    }

    public void formatProgs(Writer out, boolean html) throws IOException {
        ProgramList progs = getPrograms();
        formatProgs(progs, out, html);
    }

    public void formatProgs(ProgramList progs, Writer out, boolean html) throws IOException {
        StringBuffer ret = new StringBuffer();
        String curDate = "###";
        String curTime = "###";
        if (progs == null) return;
        if (html) {
            ret.append("<html>\r\n<head>\r\n<title>Schedule</title>\r\n</head>\r\n<body>\r\n<font size=3>");
        }
        progs.sortAndRemoveDups();
        Iterator it = progs.iterator();
        ProgItem elProgram;
        while (it.hasNext()) {
            elProgram = (ProgItem) it.next();
            if (elProgram != null) {
                String start = getStartTime(elProgram);
                Calendar startTime = Utilities.makeCal(start);
                if (start.indexOf(curDate) != 0) {
                    curDate = start.substring(0, 8);
                    if (html) ret.append("<b><font size=\"3\"><p>");
                    ret.append(formatDay(startTime));
                    if (html) ret.append("</p></font></b>");
                    ret.append("\r\n\r\n");
                }
                if (!start.equals(curTime)) {
                    curTime = start;
                    if (html) ret.append("<p><font size=\"2\">");
                    ret.append(formatTime(startTime));
                    if (html) ret.append("</font></p>");
                    ret.append("\r\n");
                }
                formatProg(elProgram, startTime, ret, html);
                ret.append("\r\n");
            }
            out.write(ret.toString());
            ret.delete(0, ret.length());
        }
        if (html) {
            ret.append("</font></body>\r\n</html>\r\n");
            out.write(ret.toString());
        }
    }

    public void formatProg(ProgItem prog, Calendar start, StringBuffer buf, boolean html) {
        if (html) buf.append("<blockquote><p><font size=\"2\">");
        buf.append("  ");
        String channel = getChannel(prog);
        buf.append(getChannelDesc(channel));
        buf.append(' ');
        if (html) buf.append("<b><i>");
        buf.append(getData(prog, ProgramData.TITLE));
        String subtitle = getData(prog, ProgramData.SUBTITLE);
        if (subtitle != null) {
            if (html) buf.append(" &quot;"); else buf.append(" \"");
            buf.append(subtitle);
            if (html) buf.append("&quot;"); else buf.append("\"");
        }
        if (html) buf.append("</i></b>");
        String desc = getData(prog, ProgramData.DESC);
        if (desc != null) {
            buf.append(" - ");
            buf.append(desc);
        }
        String date = getData(prog, ProgramData.DATE);
        if (date != null) {
            buf.append(" (");
            buf.append(date);
            buf.append(')');
        }
        StringBuffer categories = new StringBuffer();
        Vector vCat = getCategories(prog);
        int vCatSize = vCat.size();
        for (int i = 0; i < vCatSize; i++) {
            if (categories.length() > 0) categories.append(", ");
            categories.append(vCat.get(i));
        }
        if (categories.length() > 0) {
            buf.append(" (");
            buf.append(categories.toString());
            buf.append(')');
        }
        StringBuffer qualifiers = new StringBuffer();
        if (getClosedCaption(prog)) {
            if (qualifiers.length() > 0) qualifiers.append(", ");
            qualifiers.append("CC");
        }
        String stop = getStopTime(prog);
        if (stop != null) {
            if (qualifiers.length() > 0) qualifiers.append(", ");
            Calendar end = Utilities.makeCal(getStopTime(prog));
            qualifiers.append("ends at " + formatTime(end));
        }
        if (qualifiers.length() > 0) {
            buf.append(" (");
            buf.append(qualifiers.toString());
            buf.append(')');
        }
        if (html) buf.append("</font></p></blockquote>");
        buf.append("\r\n");
    }

    private static SimpleDateFormat m_UTCTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss z");

    public void formatProgsAsTVPI(ProgramList progs, Writer out, boolean gmt) throws IOException {
        PrintWriter outPW = new PrintWriter(out);
        outPW.println("<tv-program-info version=\"1.0\">");
        progs.sortAndRemoveDups();
        Iterator it = progs.iterator();
        ProgItem elProgram;
        if (gmt) {
            TimeZone tz = TimeZone.getTimeZone("GMT");
            m_UTCTimeFormat.setTimeZone(tz);
        }
        while (it.hasNext()) {
            elProgram = (ProgItem) it.next();
            if (elProgram != null) {
                String station = getChannel(elProgram);
                station = getChannelDesc(station);
                int chPos;
                String channel = null;
                if ((chPos = station.indexOf(' ')) != -1) {
                    channel = station.substring(0, chPos);
                    station = station.substring(chPos + 1);
                }
                outPW.println("<program>");
                outPW.println("<station>" + station + "</station>");
                outPW.println("<tv-mode>cable</tv-mode>");
                outPW.print("<program-title>" + getData(elProgram, ProgramData.TITLE));
                String subtitle = getData(elProgram, ProgramData.SUBTITLE);
                if (subtitle != null) {
                    outPW.print(": " + subtitle);
                }
                outPW.println("</program-title>");
                String desc = getData(elProgram, ProgramData.DESC);
                if (desc != null) {
                    outPW.println("<program-description>" + desc + "</program-description>");
                }
                String start = getStartTime(elProgram);
                String end = getStopTime(elProgram);
                Calendar startTime = Utilities.makeCal(start);
                Calendar endTime = Utilities.makeCal(end);
                String startUTC = m_UTCTimeFormat.format(startTime.getTime());
                String endUTC = m_UTCTimeFormat.format(endTime.getTime());
                outPW.println("<start-date>" + startUTC.substring(0, 8) + "</start-date>");
                outPW.println("<start-time>" + startUTC.substring(8, 10) + ":" + startUTC.substring(10, 12) + "</start-time>");
                outPW.println("<end-date>" + endUTC.substring(0, 8) + "</end-date>");
                outPW.println("<end-time>" + endUTC.substring(8, 10) + ":" + endUTC.substring(10, 12) + "</end-time>");
                long diff = endTime.getTime().getTime() - startTime.getTime().getTime();
                NumberFormat nf = NumberFormat.getInstance();
                nf.setMinimumIntegerDigits(2);
                nf.setMaximumFractionDigits(0);
                long hours = diff / (60 * 60 * 1000);
                diff -= hours * (60 * 60 * 1000);
                long minutes = diff / (60 * 1000);
                outPW.println("<duration>" + nf.format(hours) + ":" + nf.format(minutes) + "</duration>");
                if (channel != null) {
                    outPW.println("<rf-channel>" + channel + "</rf-channel>");
                }
                outPW.println("</program>");
            }
        }
        outPW.println("</tv-program-info>");
    }

    private static SimpleDateFormat m_dateFormat = new SimpleDateFormat("EEEEEEEEE, MMM d, yyyy");

    public static String formatDay(Calendar day) {
        return m_dateFormat.format(day.getTime());
    }

    private static SimpleDateFormat m_timeFormat = new SimpleDateFormat("h:mma");

    public static String formatTime(Calendar time) {
        return m_timeFormat.format(time.getTime());
    }

    public Vector getAvailableDates() {
        ProgramList progs = getProgramsSortedByTime();
        Vector dates = new Vector();
        if (progs != null) {
            Iterator it = progs.iterator();
            String curDate = "###";
            int iDay = 0;
            while (it.hasNext()) {
                ProgItem prog = (ProgItem) it.next();
                String start = getStartTime(prog);
                if (start.indexOf(curDate) != 0) {
                    curDate = start.substring(0, 8);
                    dates.add(iDay, new ProgDate(Utilities.makeCal(start)));
                    iDay++;
                }
            }
        }
        return dates;
    }
}
