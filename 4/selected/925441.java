package net.sf.sageplugins.webserver;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import net.sf.sageplugins.sageutils.SageApi;
import net.sf.sageplugins.sageutils.Translate;

/**
 * @author Owner
 * 
 */
public abstract class EpgServlet extends SageServlet {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private boolean enableExtras = false;

    private boolean markHDTV = false;

    private boolean markFirstRuns = false;

    public EpgServlet() {
        try {
            enableExtras = new Boolean(SageApi.GetProperty("enable_extras", "false")).booleanValue();
            markHDTV = new Boolean(SageApi.GetProperty("ui/epg_mark_HDTV", "false")).booleanValue();
            markFirstRuns = new Boolean(SageApi.GetProperty("ui/epg_mark_first_runs", "false")).booleanValue();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    protected GregorianCalendar getStartDate(HttpServletRequest req) {
        GregorianCalendar start = null;
        String startdate = req.getParameter("startdate");
        start = parseDate(startdate);
        if (start == null) start = new GregorianCalendar();
        int starthr = getStartHour(req);
        if (starthr == -1) {
            starthr = new GregorianCalendar().get(Calendar.HOUR_OF_DAY);
        }
        start.set(Calendar.HOUR_OF_DAY, starthr);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        return start;
    }

    protected int getStartHour(HttpServletRequest req) {
        String starthr = req.getParameter("starthr");
        if (starthr != null) {
            return Integer.parseInt(starthr);
        } else {
            return -1;
        }
    }

    protected GregorianCalendar parseDate(String dateString) {
        GregorianCalendar start = null;
        if (dateString != null) {
            Pattern p = Pattern.compile("([0-9]{4})/([0-9]{2})/([0-9]{2})");
            Matcher m = p.matcher(dateString);
            if (m.matches()) {
                start = new GregorianCalendar();
                start.set(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) - 1, Integer.parseInt(m.group(3)));
            } else {
                try {
                    int dayoffset = Integer.parseInt(dateString);
                    start = new GregorianCalendar();
                    start.add(Calendar.DAY_OF_YEAR, dayoffset);
                } catch (Exception e) {
                }
            }
        }
        return start;
    }

    protected String buildLink(HttpServletRequest req, int dayoffset, int houroffset, int channeloffset) throws Exception {
        GregorianCalendar start = getStartDate(req);
        if (dayoffset != 0) start.add(Calendar.DAY_OF_MONTH, dayoffset);
        if (houroffset != 0) start.add(Calendar.HOUR_OF_DAY, houroffset);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd");
        String rv = req.getQueryString();
        if (rv == null) rv = "";
        if (houroffset != 0 || dayoffset != 0) {
            rv = rv.replaceAll("&*startdate=[^&]*", "");
            rv = rv + "&startdate=" + fmt.format(start.getTime());
            rv = rv.replaceAll("&*starthr=[^&]*", "");
            rv = rv + "&starthr=" + Integer.toString(start.get(Calendar.HOUR_OF_DAY));
        }
        if (channeloffset != 0) {
            String[] Channels = req.getParameterValues("Channels");
            if (Channels == null || Channels.length == 0) {
                Object channellist = SageApi.Api("GetAllChannels");
                channellist = SageApi.Api("FilterByBoolMethod", new Object[] { channellist, "IsChannelViewable", Boolean.TRUE });
                channellist = SageApi.Api("Sort", new Object[] { channellist, Boolean.FALSE, "GetChannelNumber" });
                int StartChanIndex = 0;
                Integer StartChanID = null;
                try {
                    String s = req.getParameter("startchan");
                    StartChanID = new Integer(s);
                } catch (Exception e) {
                }
                Object StartChanObj = null;
                if (StartChanID != null) {
                    StartChanObj = SageApi.Api("GetChannelForStationID", new Object[] { StartChanID });
                    if (StartChanObj != null && !SageApi.booleanApi("IsChannelViewable", new Object[] { StartChanObj })) {
                        StartChanObj = null;
                    }
                    if (StartChanObj != null) {
                        Object index = SageApi.Api("FindElementIndex", new Object[] { channellist, StartChanObj });
                        if (rv != null && ((Integer) index).intValue() >= 0 && ((Integer) index).intValue() <= SageApi.Size(channellist)) StartChanIndex = ((Integer) index).intValue();
                    }
                }
                StartChanIndex = StartChanIndex + channeloffset;
                if (StartChanIndex < 0) StartChanIndex = 0;
                Object startChannel = SageApi.GetElement(channellist, StartChanIndex);
                String chanId = Integer.toString(SageApi.IntApi("GetStationID", new Object[] { startChannel }));
                rv = rv.replaceAll("&*startchan=[^&]*", "");
                rv = rv + "&startchan=" + chanId;
            }
        }
        return req.getRequestURI() + "?" + Translate.encode(rv);
    }

    protected void channelListSelector(HttpServletRequest req, PrintWriter out) throws Exception {
        out.println("Show channels:\r\n");
        out.println("<select name=\"Channels\" Multiple Size=\"10\">");
        String[] Channels = req.getParameterValues("Channels");
        List<String> Channels_l = null;
        if (Channels != null) Channels_l = Arrays.asList(Channels); else Channels_l = new Vector<String>();
        Object channellist = SageApi.Api("GetAllChannels");
        channellist = SageApi.Api("Sort", new Object[] { channellist, Boolean.FALSE, "GetChannelNumber" });
        int numchans = SageApi.Size(channellist);
        for (int i = 0; i < numchans; i++) {
            Object chan = SageApi.GetElement(channellist, i);
            if (SageApi.booleanApi("IsChannelViewable", new Object[] { chan })) {
                String channame = SageApi.StringApi("GetChannelName", new Object[] { chan });
                String channumber = SageApi.StringApi("GetChannelNumber", new Object[] { chan });
                String chanId = Integer.toString(SageApi.IntApi("GetStationID", new Object[] { chan }));
                out.print("   <option value=\"" + chanId + "\"");
                if (Channels != null && Channels.length > 0 && Channels_l.contains(chanId)) out.print(" selected=\"selected\"");
                out.println(">" + Translate.encode(channumber + " -- " + channame) + "</option>");
            }
        }
        out.println("</select>");
    }

    protected Vector<Object> getChannels(HttpServletRequest req, int numchans) throws Exception {
        Vector<Object> rv = new Vector<Object>();
        String[] Channels = req.getParameterValues("Channels");
        if (Channels == null) {
            Integer StartChanID = null;
            try {
                String s = req.getParameter("startchan");
                StartChanID = new Integer(s);
            } catch (Exception e) {
            }
            Object StartChanObj = null;
            if (StartChanID != null) {
                StartChanObj = SageApi.Api("GetChannelForStationID", new Object[] { StartChanID });
                if (StartChanObj != null && !SageApi.booleanApi("IsChannelViewable", new Object[] { StartChanObj })) {
                    StartChanObj = null;
                }
            }
            Object channellist = SageApi.Api("GetAllChannels");
            channellist = SageApi.Api("FilterByBoolMethod", new Object[] { channellist, "IsChannelViewable", Boolean.TRUE });
            channellist = SageApi.Api("Sort", new Object[] { channellist, Boolean.FALSE, "GetChannelNumber" });
            int channum = 0;
            if (StartChanObj != null) for (; channum < SageApi.Size(channellist); channum++) if (SageApi.GetElement(channellist, channum) == StartChanObj) break;
            if (channum > SageApi.Size(channellist)) channum = 0;
            for (; channum < SageApi.Size(channellist) && rv.size() < numchans; channum++) rv.add(SageApi.GetElement(channellist, channum));
        } else {
            for (int i = 0; i < Channels.length; i++) {
                Integer chID = new Integer(Channels[i]);
                Object channelObj = SageApi.Api("GetChannelForStationID", new Object[] { chID });
                if (channelObj != null) rv.add(channelObj);
            }
        }
        return rv;
    }

    protected Vector<GregorianCalendar> getDays(HttpServletRequest req, int numdays) throws Exception {
        Vector<GregorianCalendar> rv = new Vector<GregorianCalendar>();
        String[] Days = req.getParameterValues("Days");
        if (Days == null) {
            GregorianCalendar StartDate = null;
            try {
                StartDate = getStartDate(req);
            } catch (Exception e) {
            }
            int daynum = 0;
            for (; rv.size() < numdays; daynum++) {
                GregorianCalendar columnDate = (GregorianCalendar) StartDate.clone();
                columnDate.add(GregorianCalendar.DAY_OF_MONTH, daynum);
                rv.add(columnDate);
            }
        } else {
            System.out.println("Days!=null");
            for (int i = 0; i < Days.length; i++) {
                GregorianCalendar day = parseDate(Days[i]);
                if (day != null) rv.add(day);
            }
        }
        return rv;
    }

    protected void printDayHourChannelSelectors(PrintWriter out, HttpServletRequest req, int numhrs) throws InvocationTargetException {
        out.println("<div class=\"dateselect\"><form method=\"get\" action=\"" + req.getRequestURI() + "\">\r\n" + "Displaying: ");
        Integer StartChanID = null;
        try {
            String s = req.getParameter("startchan");
            StartChanID = new Integer(s);
        } catch (Exception e) {
        }
        out.println("   <select name=\"startdate\">");
        GregorianCalendar start = getStartDate(req);
        GregorianCalendar dateopt = new GregorianCalendar();
        SimpleDateFormat argfmt = new SimpleDateFormat("yyyy/MM/dd");
        SimpleDateFormat dispfmt = new SimpleDateFormat("EEEE, MMM d");
        dateopt.add(Calendar.DAY_OF_MONTH, -1);
        out.print("   <option value=\"" + argfmt.format(dateopt.getTime()) + "\"");
        if (dateopt.get(Calendar.DAY_OF_YEAR) == start.get(Calendar.DAY_OF_YEAR)) {
            out.print(" selected=\"selected\"");
        }
        out.println(">Yesterday, " + dispfmt.format(dateopt.getTime()) + "</option>");
        dateopt.add(Calendar.DAY_OF_MONTH, 1);
        out.print("   <option value=\"" + argfmt.format(dateopt.getTime()) + "\"");
        if (dateopt.get(Calendar.DAY_OF_YEAR) == start.get(Calendar.DAY_OF_YEAR)) {
            out.print(" selected=\"selected\"");
        }
        out.println(">Today, " + dispfmt.format(dateopt.getTime()) + "</option>");
        dateopt.add(Calendar.DAY_OF_MONTH, 1);
        out.print("   <option value=\"" + argfmt.format(dateopt.getTime()) + "\"");
        if (dateopt.get(Calendar.DAY_OF_YEAR) == start.get(Calendar.DAY_OF_YEAR)) {
            out.print(" selected=\"selected\"");
        }
        out.println(">Tomorrow, " + dispfmt.format(dateopt.getTime()) + "</option>");
        for (int i = 1; i < 12; i++) {
            dateopt.add(Calendar.DAY_OF_MONTH, 1);
            out.print("   <option value=\"" + argfmt.format(dateopt.getTime()) + "\"");
            if (dateopt.get(Calendar.DAY_OF_YEAR) == start.get(Calendar.DAY_OF_YEAR)) {
                out.print(" selected=\"selected\"");
            }
            out.println(">" + dispfmt.format(dateopt.getTime()) + "</option>");
        }
        out.println("   </select>");
        out.println("   <select name=\"starthr\">");
        dateopt.set(Calendar.HOUR_OF_DAY, 0);
        dateopt.set(Calendar.MINUTE, 0);
        dateopt.set(Calendar.SECOND, 0);
        dateopt.set(Calendar.MILLISECOND, 0);
        DateFormat fmt = DateFormat.getTimeInstance(DateFormat.SHORT);
        for (int i = 0; i < 24; i += numhrs) {
            out.print("   <option value=\"" + Integer.toString(i) + "\"");
            if (i == start.get(Calendar.HOUR_OF_DAY)) out.print(" selected=\"selected\"");
            dateopt.set(Calendar.HOUR_OF_DAY, i);
            out.println(">" + fmt.format(dateopt.getTime()) + "</option>");
            if (i < start.get(Calendar.HOUR_OF_DAY) && i + numhrs > start.get(Calendar.HOUR_OF_DAY)) {
                out.print("   <option value=\"" + Integer.toString(start.get(Calendar.HOUR_OF_DAY)) + "\" selected=\"selected\"");
                dateopt.set(Calendar.HOUR_OF_DAY, start.get(Calendar.HOUR_OF_DAY));
                out.println(">" + fmt.format(dateopt.getTime()) + "</option>");
            }
        }
        out.println("   </select>");
        out.println("   <select name=\"startchan\">");
        Object channels = SageApi.Api("GetAllChannels");
        channels = SageApi.Api("FilterByBoolMethod", new Object[] { channels, "IsChannelViewable", Boolean.TRUE });
        channels = SageApi.Api("Sort", new Object[] { channels, Boolean.FALSE, "ChannelNumber" });
        for (int i = 0; i < SageApi.Size(channels); i++) {
            Object channel = SageApi.GetElement(channels, i);
            Object stationId = SageApi.Api("GetStationID", new Object[] { channel });
            Object channelnum = SageApi.Api("GetChannelNumber", new Object[] { channel });
            out.print("        <option value=\"" + stationId + "\"");
            if ((StartChanID == null && i == 0) || (StartChanID != null && StartChanID.equals(stationId))) out.print(" selected=\"selected\"");
            out.println(">" + channelnum + " - " + Translate.encode((String) SageApi.Api("GetChannelName", new Object[] { channel })) + "</option>");
        }
        out.println("   </select>");
        out.println("   <input type=\"submit\" value=\"Update\"/>");
        out.println("</form></div>");
    }

    protected boolean getMarkHDTV(HttpServletRequest req) throws Exception {
        String markHDTVOption = GetOption(req, "epg_mark_hdtv", "##AsSageTV##");
        if (markHDTVOption.equals("##AsSageTV##")) {
            return (enableExtras && markHDTV);
        } else {
            return new Boolean(markHDTVOption).booleanValue();
        }
    }

    protected boolean getMarkFirstRuns(HttpServletRequest req) throws Exception {
        String markFirstRunsOption = GetOption(req, "epg_mark_first_runs", "##AsSageTV##");
        if (markFirstRunsOption.equals("##AsSageTV##")) {
            return (enableExtras && markFirstRuns);
        } else {
            return new Boolean(markFirstRunsOption).booleanValue();
        }
    }

    static final String[][] NUM_CHANS_OPTS = new String[][] { { "1", "1" }, { "2", "2" }, { "4", "4" }, { "6", "6" }, { "10", "10" }, { "15", "15" }, { "20", "20" }, { "100", "100" }, { "999", "999" } };

    static final String[][] NUM_DAYS_OPTS = new String[][] { { "1", "1" }, { "2", "2" }, { "3", "3" }, { "4", "4" }, { "5", "5" }, { "6", "6" }, { "7", "7" }, { "8", "8" }, { "9", "9" }, { "10", "10" }, { "11", "11" }, { "12", "12" }, { "13", "13" }, { "14", "14" }, { "15", "15" } };

    static final String[][] NUM_HRS_OPTS = new String[][] { { "1", "1" }, { "2", "2" }, { "3", "3" }, { "4", "4" }, { "6", "6" }, { "12", "12" }, { "24", "24" } };

    static final String[][] FIRST_HR_OPTS = new String[][] { { "-1", "Current Hour" }, { "0", "12:00 AM" }, { "1", "1:00 AM" }, { "2", "2:00 AM" }, { "3", "3:00 AM" }, { "4", "4:00 AM" }, { "5", "5:00 AM" }, { "6", "6:00 AM" }, { "7", "7:00 AM" }, { "8", "8:00 AM" }, { "9", "9:00 AM" }, { "10", "10:00 AM" }, { "11", "11:00 AM" }, { "12", "12:00 PM" }, { "13", "1:00 PM" }, { "14", "2:00 PM" }, { "15", "3:00 PM" }, { "16", "4:00 PM" }, { "17", "5:00 PM" }, { "18", "6:00 PM" }, { "19", "7:00 PM" }, { "20", "8:00 PM" }, { "21", "9:00 PM" }, { "22", "10:00 PM" }, { "23", "11:00 PM" } };

    static String[][] EPG_MARKER_OPTS = new String[][] { { "##AsSageTV##", "Same as SageTV" }, { "true", "Enabled" }, { "false", "Disabled" } };
}
