package freeguide.common.lib.fgspecific;

import freeguide.common.lib.fgspecific.Application;
import freeguide.common.lib.fgspecific.data.TVProgramme;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Formats Programme information.
 */
public class ProgrammeFormat {

    /** Plain text format */
    public static final int TEXT_FORMAT = 0;

    /**
     * HTML format including the "<code>&lt;html&gt;&lt;body&gt; ...
     * &lt;/body&gt;&lt;/html&gt;</code>" tags
     */
    public static final int HTML_FORMAT = 1;

    protected static final String CHARSET = "UTF-8";

    /**
     * HTML format without the <code>&lt;html&gt;&lt;body&gt; ...
     * &lt;/body&gt;&lt;/html&gt;</code> tags (for use in building a page
     * from many fragments)
     */
    public static final int HTML_FRAGMENT_FORMAT = 2;

    /** DOCUMENT ME! */
    public static final String LINE_FEED = System.getProperty("line.separator", "\r\n");

    /** Format used for dates in the HTML links. */
    public static final SimpleDateFormat LINK_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    /**
     * Function that returns the time difference from now in a format
     * like "2 hours and 1 minute"
     *
     * @param startTime starting time of the program
     * @param toAppend StringBuffer the resulting string gets added to
     */
    public static void calcTimeDelta(long startTime, StringBuffer toAppend) {
        long delta = startTime - System.currentTimeMillis();
        delta /= 60000;
        if (delta == 0) {
            toAppend.append(Application.getInstance().getLocalizedMessage("starts_now"));
            return;
        }
        int days = (int) (delta / (24 * 60));
        int hours = (int) ((delta / 60) % 60);
        int minutes = (int) (delta % 60);
        if (delta > 0) {
            if (days == 1) {
                toAppend.append(Application.getInstance().getLocalizedMessage("starts_in_1_day"));
            } else if (days > 1) {
                Object[] messageArguments = { new Integer(days) };
                toAppend.append(Application.getInstance().getLocalizedMessage("starts_in_days_template", messageArguments));
            } else if (hours == 1) {
                toAppend.append(Application.getInstance().getLocalizedMessage("starts_in_1_hour"));
            } else if (hours > 1) {
                Object[] messageArguments = { new Integer(hours) };
                toAppend.append(Application.getInstance().getLocalizedMessage("starts_in_hours_template", messageArguments));
            } else if (minutes == 1) {
                toAppend.append(Application.getInstance().getLocalizedMessage("starts_in_1_minute"));
            } else {
                Object[] messageArguments = { new Integer(minutes) };
                toAppend.append(Application.getInstance().getLocalizedMessage("starts_in_minutes_template", messageArguments));
            }
        } else {
            if (days == -1) {
                toAppend.append(Application.getInstance().getLocalizedMessage("started_1_day_ago"));
            } else if (days < -1) {
                Object[] messageArguments = { new Integer(-days) };
                toAppend.append(Application.getInstance().getLocalizedMessage("started_days_ago_template", messageArguments));
            } else if (hours == -1) {
                toAppend.append(Application.getInstance().getLocalizedMessage("started_1_hour_ago"));
            } else if (hours < -1) {
                Object[] messageArguments = { new Integer(-hours) };
                toAppend.append(Application.getInstance().getLocalizedMessage("started_hours_ago_template", messageArguments));
            } else if (minutes == -1) {
                toAppend.append(Application.getInstance().getLocalizedMessage("started_1_minute_ago"));
            } else {
                Object[] messageArguments = { new Integer(-minutes) };
                toAppend.append(Application.getInstance().getLocalizedMessage("started_minutes_ago_template", messageArguments));
            }
        }
    }

    /**
     * Utility method to create a unique ASCII-only name (reference)
     * to identify each program in the HTML program guide.
     *
     * @param programme programme for create reference
     *
     * @return reference string
     */
    public static String createLinkReference(final TVProgramme programme) {
        String reference = null;
        StringBuffer ref = new StringBuffer(LINK_DATE_FORMAT.format(new Date(programme.getStart())));
        ref.append(';');
        ref.append(programme.getChannel().getID());
        try {
            reference = URLEncoder.encode(ref.toString(), CHARSET);
        } catch (UnsupportedEncodingException e) {
        }
        return reference;
    }
}
