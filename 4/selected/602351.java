package de.ios.framework.basic;

import java.io.*;
import java.awt.*;
import java.util.*;
import java.sql.SQLException;
import java.rmi.UnmarshalException;

public final class Debug {

    /**
   * Classname, used by static methods.
   * Eine static-Variable laesst sich nicht ueber nicht-static-Methoden initialisieren
   * Hier muesste in jeder Klasse zur Initialisierung eine Dummy-Instanz erzeugt werden
   * (C++ - CastDown-Technologie laesst gruessen... :)
   */
    public static String CLASS = "Debug";

    /** Debugging Flags. */
    public static final boolean DBG = true;

    protected static boolean OUT = false;

    protected static final boolean CLASSLOG = false;

    /** The possible kinds of messages. */
    public static final int EXCEPTION = 0;

    public static final int ERROR = 1;

    public static final int WARNING = 2;

    public static final int SQLINFO = 3;

    public static final int INFO = 4;

    public static final int BIGINFO = 5;

    public static final int EVENTINFO = 6;

    public static final int RUN = 7;

    public static String kindDescription[] = { "EXCEPTION", "ERROR....", "WARNING..", "SQLINFO..", "INFO.....", "BIGINFO..", "EVENTINFO", "RUN......" };

    /** Which kind off messages shall be printed. */
    public static boolean showExceptions = true;

    public static boolean showErrors = true;

    public static boolean showWarnings = false;

    public static boolean showSQLInfo = false;

    public static boolean showInfo = false;

    public static boolean showBigInfo = false;

    public static boolean showEventInfo = false;

    public static boolean showRuns = true;

    /** Message Channels: */
    public static PrintStream[] channel = { System.err, System.err, System.err, System.err, System.err };

    public static int channels = channel.length;

    /** Print an extra Message on an extra Channel. */
    public static boolean extraErrorChannel = false;

    public static boolean extraWarningChannel = false;

    public static boolean extraInfoChannel = false;

    public static boolean extraRunChannel = false;

    /** Which channel to use. */
    public static int allChannel = 0;

    public static int errorChannel = 1;

    public static int warningChannel = 2;

    public static int infoChannel = 3;

    public static int runChannel = 4;

    /** Switches for adding the Date and Time to the messages (only after a println). */
    public static boolean addTime = false;

    public static boolean addDate = false;

    public static boolean addName = false;

    public static boolean addKind = false;

    /** Maximum number of Bytes of a ByteArray to be shown. */
    public static int maxByteArrayShown = 0x0100;

    /** The Line-Separator (get's initialized with corresponding property via Parameters later). */
    public static String lineSeparator = "\n";

    protected static boolean newLine = true;

    protected static final Hashtable classLog = new Hashtable(1000, (float) 0.5);

    /**
   * The Constructor - nothing to do yet...
   * Actual it's no need for an instance of this class.
   */
    protected Debug() {
    }

    /**
   * Add the Time, Date and Class-Name to each "line"
   */
    public static final void showAdditional() {
        addDate = true;
        addTime = true;
        addName = true;
        addKind = true;
    }

    /**
   * Turn all debugging on
   */
    public static final void showAll() {
        showAllExceptMassInfo();
        showBigInfo = true;
        showEventInfo = true;
    }

    /**
   * Turn all debugging on (except Event/BigInfo-Showing - these flag keeps unchanged)
   */
    public static final void showAllExceptMassInfo() {
        showExceptions = true;
        showErrors = true;
        showWarnings = true;
        showRuns = true;
        showSQLInfo = true;
        showInfo = true;
    }

    /**
   * Show channels by values of Parameters (Properties, Commandline-Parameters, Applet-Parameters).
   *
   * Channels are activated by the value of the following properties:<br>
   * <pre>
   * <ul>
   *  <li>OUT			debug.OUT
   *  <li>traceInstructions	debug.trace.instructions
   *  <li>traceMethodCalls	debug.trace.methodcalls
   *  <li>showExceptions	debug.show.exceptions
   *  <li>showErrors		debug.show.errors
   *  <li>showWarnings		debug.show.warnings
   *  <li>showRuns		debug.show.runs   (should not be deactivated!)
   *  <li>showSQLInfo		debug.show.info.sql
   *  <li>showInfo		debug.show.info
   *  <li>showBigInfo		debug.show.info.big
   *  <li>showEventInfo		debug.show.info.event
   *  <li>addDate		debug.show.additional.date
   *  <li>addTime		debug.show.additional.time
   *  <li>addName		debug.show.additional.name
   *  <li>addName		debug.show.additional.kind
   *  <li>showAll		debug.show.all		(shows all the messages)
   *  <li>showMost		debug.show.most		(shows all the messages except Big-Info and Event-Info)
   *  <li>showAdditional	debug.show.additional	(adds date, time and the Class-Name)
   *  <li>redirect		debug.redirect.x	(where x is the Channel-No and the Value a filename, or .err or .out - redirect the Output of a Channel)
   *  <li>redirect-all		debug.redirect.all	(where the Value is a filename, or .err or .out - redirect the Output of all channels)
   *  <li>extraError		debug.extra.error	(activate output error-duplicates on the error-channel)
   *  <li>extraWarning		debug.extra.warning	(activate output warning-duplicates on the error-channel)
   *  <li>extraInfo		debug.extra.info	(activate output info-duplicates on the error-channel)
   *  <li>mapError		debug.map.error		(map the logical extra-error-channel to a physical channel)
   *  <li>mapWarning		debug.map.warning	(map the logical extra-warning-channel to a physical channel)
   *  <li>mapInfo		debug.map.info		(map the logical extra-info-channel to a physical channel)
   *  <li>mapAll		debug.map.all		(map the regular logical output-channel to a physical channel)
   * </ul>
   * </pre>
   *
   */
    public static final void showByParameters() {
        PrintStream ps;
        OUT = Parameters.getBooleanParameter("debug.OUT", OUT);
        lineSeparator = Parameters.getOptionalParameter("line.separator", lineSeparator);
        if (Parameters.getBooleanParameter("debug.show.all")) showAll();
        if (Parameters.getBooleanParameter("debug.show.most")) showAllExceptMassInfo();
        showExceptions = Parameters.getBooleanParameter("debug.show.exceptions", showExceptions);
        showErrors = Parameters.getBooleanParameter("debug.show.errors", showErrors);
        showWarnings = Parameters.getBooleanParameter("debug.show.warnings", showWarnings);
        showRuns = Parameters.getBooleanParameter("debug.show.runs", showRuns);
        showInfo = Parameters.getBooleanParameter("debug.show.info", showInfo);
        showSQLInfo = Parameters.getBooleanParameter("debug.show.info.sql", showSQLInfo);
        showBigInfo = Parameters.getBooleanParameter("debug.show.info.big", showBigInfo);
        showEventInfo = Parameters.getBooleanParameter("debug.show.info.event", showEventInfo);
        if (Parameters.getBooleanParameter("debug.show.additional")) showAdditional();
        addDate = Parameters.getBooleanParameter("debug.show.additional.date", addDate);
        addTime = Parameters.getBooleanParameter("debug.show.additional.time", addTime);
        addName = Parameters.getBooleanParameter("debug.show.additional.name", addName);
        addKind = Parameters.getBooleanParameter("debug.show.additional.kind", addKind);
        extraErrorChannel = Parameters.getBooleanParameter("debug.extra.error", extraErrorChannel);
        extraWarningChannel = Parameters.getBooleanParameter("debug.extra.warning", extraWarningChannel);
        extraInfoChannel = Parameters.getBooleanParameter("debug.extra.info", extraInfoChannel);
        for (int i = 0; i < channels; i++) {
            ps = getPrintStream(Parameters.getParameter("debug.redirect." + i));
            if (ps != null) channel[i] = ps;
        }
        ps = getPrintStream(Parameters.getParameter("debug.redirect.all"));
        if (ps != null) for (int i = 0; i < channels; i++) channel[i] = ps;
        errorChannel = getChannel("debug.map.error", errorChannel);
        warningChannel = getChannel("debug.map.warning", warningChannel);
        infoChannel = getChannel("debug.map.info", infoChannel);
        runChannel = getChannel("debug.map.run", runChannel);
        allChannel = getChannel("debug.map.all", allChannel);
    }

    /**
   * Get a Stream specified by a Parameter
   */
    public static final PrintStream getPrintStream(String value) {
        if (value == null) return null;
        if (value.length() == 0) return System.err;
        if (value.toUpperCase().compareTo(".ERR") == 0) return System.err;
        if (value.toUpperCase().compareTo(".OUT") == 0) return System.out;
        try {
            return new PrintStream(new BufferedOutputStream(new FileOutputStream(value)));
        } catch (IOException e) {
            printException("DEBUG", e);
            return null;
        }
    }

    /**
   * Get a channel specified by a parameter
   */
    public static final int getChannel(String param, int deflt) {
        int i;
        try {
            i = Integer.parseInt(Parameters.getOptionalParameter(param, "" + deflt));
            return ((i >= channels) || (i < 0)) ? deflt : i;
        } catch (NumberFormatException e) {
            printException("DEBUG", e);
            return deflt;
        }
    }

    /**
   * Add the Date, Time and Class-Name, Message-Kind, if desired...
   */
    protected static final String getDateTimeNameKind(Object cl, int kind) {
        String s = "";
        EUCalendar c = null;
        if (addDate || addTime) c = new EUCalendar();
        if (addDate) s = s + c.getCompleteDateString() + " ";
        if (addTime) s = s + c.getCompletePrecisionTimeString() + " ";
        if (addKind) s = s + "[" + kindDescription[kind] + "] ";
        if (addName) if (cl != null) if (cl instanceof String) s = s + "<" + ((String) cl) + "> "; else {
            try {
                s = s + "<" + cl.getClass().getName() + "> ";
            } catch (Exception e) {
                s = s + "<***> ";
            }
        } else s = s + "<---> ";
        if (addDate || addTime || addName || addKind) s = s + ": ";
        return s;
    }

    /**
   * Translate an Event.id (int) into a String
   */
    public static final String getId(int id) {
        switch(id) {
            case -1:
                return "(NONE)............";
            case 0:
                return "(0=none?).........";
            case Event.ACTION_EVENT:
                return "<Action>..........";
            case Event.GOT_FOCUS:
                return "<GotFocus>........";
            case Event.KEY_ACTION:
                return "<KeyAction>.......";
            case Event.KEY_ACTION_RELEASE:
                return "<KeyActionRelease>";
            case Event.KEY_PRESS:
                return "<KeyPress>........";
            case Event.KEY_RELEASE:
                return "<KeyRelease>......";
            case Event.LIST_SELECT:
                return "<ListSelect>......";
            case Event.LIST_DESELECT:
                return "<ListDeselect>....";
            case Event.LOAD_FILE:
                return "<LoadFile>........";
            case Event.LOST_FOCUS:
                return "<LostFocus>.......";
            case Event.MOUSE_DOWN:
                return "<MouseDown>.......";
            case Event.MOUSE_ENTER:
                return "<MouseEnter>......";
            case Event.MOUSE_EXIT:
                return "<MouseExit>.......";
            case Event.MOUSE_MOVE:
                return "<MouseMove>.......";
            case Event.MOUSE_DRAG:
                return "<MouseDrag>.......";
            case Event.MOUSE_UP:
                return "<MouseUp>.........";
            case Event.SAVE_FILE:
                return "<SaveFile>........";
            case Event.SCROLL_ABSOLUTE:
                return "<ScrollAbsolute>..";
            case Event.SCROLL_BEGIN:
                return "<ScrollBegin>.....";
            case Event.SCROLL_END:
                return "<ScrollEnd>.......";
            case Event.SCROLL_LINE_DOWN:
                return "<ScrollLineDown>..";
            case Event.SCROLL_LINE_UP:
                return "<ScrollLineUp>....";
            case Event.SCROLL_PAGE_DOWN:
                return "<ScrollPageDown>..";
            case Event.SCROLL_PAGE_UP:
                return "<ScrollPageUp>....";
            case Event.WINDOW_DEICONIFY:
                return "<WindowDeiconify>.";
            case Event.WINDOW_DESTROY:
                return "<WindowDestroy>...";
            case Event.WINDOW_EXPOSE:
                return "<WindowExpose>....";
            case Event.WINDOW_ICONIFY:
                return "<WindowIconify>...";
            case Event.WINDOW_MOVED:
                return "<WindowMoved>.....";
            default:
                return "<(" + id + ")>..........";
        }
    }

    /**
   * Translate a Modifier-value into the String representing it.
   */
    public static final String getModifiers(int modifiers) {
        String s = "";
        if ((modifiers & Event.ALT_MASK) != 0) s += "[ALT]";
        if ((modifiers & Event.CTRL_MASK) != 0) s += "[CTRL]";
        if ((modifiers & Event.META_MASK) != 0) s += "[META]";
        if ((modifiers & Event.SHIFT_MASK) != 0) s += "[SHIFT]";
        if (s.length() == 0) s = "(NONE)";
        return s;
    }

    /**
   * Translate a Key (int) into the String representing it
   */
    public static final String getKey(int key) {
        switch(key) {
            case -1:
                return "(NONE)";
            case 0:
                return "(none)";
            case Event.BACK_SPACE:
                return "[BackSpace]";
            case Event.CAPS_LOCK:
                return "[CapsLock]";
            case Event.DELETE:
                return "[Delete]";
            case Event.DOWN:
                return "[Down]";
            case Event.END:
                return "[End]";
            case Event.ENTER:
                return "[Enter]";
            case Event.ESCAPE:
                return "[Escape]";
            case Event.F1:
                return "[F1]";
            case Event.F2:
                return "[F2]";
            case Event.F3:
                return "[F3]";
            case Event.F4:
                return "[F4]";
            case Event.F5:
                return "[F5]";
            case Event.F6:
                return "[F6]";
            case Event.F7:
                return "[F7]";
            case Event.F8:
                return "[F8]";
            case Event.F9:
                return "[F9]";
            case Event.F10:
                return "[F10]";
            case Event.F11:
                return "[F11]";
            case Event.F12:
                return "[F12]";
            case Event.HOME:
                return "[Home]";
            case Event.INSERT:
                return "[Insert]";
            case Event.LEFT:
                return "[Left]";
            case Event.NUM_LOCK:
                return "[NumLock]";
            case Event.PAUSE:
                return "[Pause]";
            case Event.PGDN:
                return "[PgDn]";
            case Event.PGUP:
                return "[PgUp]";
            case Event.PRINT_SCREEN:
                return "[PrintScreen]";
            case Event.RIGHT:
                return "[Right]";
            case Event.SCROLL_LOCK:
                return "[ScrollLock]";
            case Event.TAB:
                return "[Tab]";
            case Event.UP:
                return "[Up]";
            default:
                return "[0x" + (((key & 0xf0) == 0) ? "0" : "") + Integer.toHexString(key & 0xff) + " '" + (Character.isLetterOrDigit((char) key) ? (char) key : '.') + "']";
        }
    }

    /**
   * Create an PrintStream redirecting the Output of a certain Object into one of Debug's Channels.
   * @param channel the Channel of Debug to be used.
   * @param obj     the Object the Output will come from.
   * @return a PrintStream for redirecting the Object's Output to Debug.
   */
    public static final PrintStream createDebugRedirectionStream(int channel, Object obj) {
        return new PrintStream(new DebugOutputStream(channel, obj));
    }

    /**
   * Print a message.
   * If 'kind' is BIGINFO and 'o' isn't a String, 'o' gets dumped detailed (if that Object-Type is supported by Debug)
   */
    public static final void print(int kind, Object cl, Object o) {
        String s;
        if (OUT || (kind == ERROR) || (kind == EXCEPTION) || (kind == WARNING) || (kind == RUN)) {
            s = toString(kind, o);
            if (newLine) s = getDateTimeNameKind(cl, kind) + s;
            newLine = (s.endsWith(lineSeparator));
            switch(kind) {
                case EXCEPTION:
                    if (showExceptions) {
                        channel[allChannel].print(s);
                        channel[allChannel].flush();
                        if (extraErrorChannel) {
                            channel[errorChannel].print(s);
                            channel[errorChannel].flush();
                        }
                    }
                    break;
                case ERROR:
                    if (showErrors) {
                        channel[allChannel].print(s);
                        channel[allChannel].flush();
                        if (extraErrorChannel) {
                            channel[errorChannel].print(s);
                            channel[errorChannel].flush();
                        }
                    }
                    break;
                case WARNING:
                    if (showWarnings) {
                        channel[allChannel].print(s);
                        channel[allChannel].flush();
                        if (extraWarningChannel) {
                            channel[warningChannel].print(s);
                            channel[warningChannel].flush();
                        }
                    }
                    break;
                case RUN:
                    if (showRuns) {
                        channel[allChannel].print(s);
                        channel[allChannel].flush();
                        if (extraRunChannel) {
                            channel[runChannel].print(s);
                            channel[runChannel].flush();
                        }
                    }
                    break;
                case SQLINFO:
                    if (showSQLInfo) {
                        channel[allChannel].print(s);
                        channel[allChannel].flush();
                        if (extraInfoChannel) {
                            channel[infoChannel].print(s);
                            channel[infoChannel].flush();
                        }
                    }
                    break;
                case INFO:
                    if (showInfo) {
                        channel[allChannel].print(s);
                        channel[allChannel].flush();
                        if (extraInfoChannel) {
                            channel[infoChannel].print(s);
                            channel[infoChannel].flush();
                        }
                    }
                    break;
                case BIGINFO:
                    if (showBigInfo) {
                        channel[allChannel].print(s);
                        channel[allChannel].flush();
                        if (extraInfoChannel) {
                            channel[infoChannel].print(s);
                            channel[infoChannel].flush();
                        }
                    }
                    break;
                case EVENTINFO:
                    if (showEventInfo) {
                        channel[allChannel].print(s);
                        channel[allChannel].flush();
                        if (extraInfoChannel) {
                            channel[infoChannel].print(s);
                            channel[infoChannel].flush();
                        }
                    }
                    break;
                default:
                    s = " [*** " + CLASS + " - UNKNOWN KIND OF MESSAGE: ***] " + s;
                    channel[allChannel].print(s);
                    channel[allChannel].flush();
                    if (extraErrorChannel) {
                        channel[errorChannel].print(s);
                        channel[errorChannel].flush();
                    }
            }
        }
    }

    /**
   * Print a message-line.
   * If 'kind' is BIGINFO and 'o' isn't a String, 'o' gets dumped detailed (if that Object-Type is supported by Debug)
   */
    public static final void println(int kind, Object cl, Object o) {
        if (OUT || (kind == ERROR) || (kind == EXCEPTION) || (kind == WARNING) || (kind == RUN)) {
            print(kind, cl, toString(kind, o) + lineSeparator);
            newLine = true;
        }
    }

    /**
   * Print the actual Thread.
   */
    public static final void dumpThreads(int kind) {
        if (OUT || (kind == ERROR) || (kind == EXCEPTION) || (kind == WARNING) || (kind == RUN)) {
            try {
                int i;
                int c = Thread.activeCount();
                Thread ta[] = new Thread[c];
                Thread t;
                Thread current = Thread.currentThread();
                Thread.enumerate(ta);
                for (i = 0; i < c; i++) {
                    t = ta[i];
                    println(kind, null, "" + i + (t.isAlive() ? " A" : "  ") + (t.isDaemon() ? " D" : "  ") + " " + t.getPriority() + ((t == current) ? " *" : "  ") + " [" + t.getThreadGroup().getName() + ":" + t.getName() + "]");
                }
            } catch (Throwable e) {
                printThrowable(null, e);
            }
        }
    }

    /**
   * Print the Properties.
   */
    public static final void printProperties(int kind, Object cl, Properties p) {
        if (!newLine) println(kind, null, "");
        println(kind, cl, toString(p));
    }

    /**
   * Print the extended StackTrace of an Exception
   */
    public static final void printException(Object cl, Exception e) {
        if (!newLine) println(EXCEPTION, null, "");
        println(EXCEPTION, cl, lineSeparator + "******************************** EXCEPTION ********************************" + lineSeparator + getExtendedMessage(e) + "***************************************************************************");
    }

    /**
   * Print the extended StackTrace of an Error
   */
    public static final void printError(Object cl, Error e) {
        if (!newLine) println(ERROR, null, "");
        println(ERROR, cl, lineSeparator + "********************************** ERROR **********************************" + lineSeparator + getExtendedMessage(e) + "***************************************************************************");
    }

    /**
   * Print the extended StackTrace of a Throwable
   */
    public static final void printThrowable(Object cl, Throwable e) {
        if (e instanceof Exception) printException(cl, (Exception) e); else if (e instanceof Error) printError(cl, (Error) e); else {
            if (!newLine) println(ERROR, null, "");
            println(ERROR, cl, lineSeparator + "******************************** THROWABLE ********************************" + lineSeparator + getExtendedMessage(e) + "***************************************************************************");
        }
    }

    /**
   * Print an Event
   */
    public static final void printEvent(Object cl, Event ev, boolean isAction) {
        if (OUT) {
            String s = null;
            if (ev == null) println(EVENTINFO, cl, (isAction ? "(action)" : "(event)") + " NULL!"); else if (isAction) {
                try {
                    if (ev.target == null) s = "---"; else s = ev.target.getClass().getName();
                } catch (Exception e) {
                    s = "***";
                }
                println(EVENTINFO, cl, "(action) id: " + getId(ev.id) + ",  target: " + s + ",  arg: " + ev.arg + ",  key: " + getKey(ev.key) + ",  modifiers: " + getModifiers(ev.modifiers) + ",  clickCount: " + ev.clickCount + ",  x: " + ev.x + ",  y: " + ev.y);
            } else if ((ev.id != ev.MOUSE_MOVE) && (ev.id != ev.MOUSE_DRAG)) {
                try {
                    if (ev.target == null) s = "---"; else s = ev.target.getClass().getName();
                } catch (Exception e) {
                    s = "***";
                }
                println(EVENTINFO, cl, "(event)  id: " + getId(ev.id) + ",  target: " + s + ",  arg: " + ev.arg + ",  key: " + getKey(ev.key) + ",  modifiers: " + getModifiers(ev.modifiers) + ",  clickCount: " + ev.clickCount + ",  x: " + ev.x + ",  y: " + ev.y);
            }
        }
    }

    /**
   * Print a ByteArray, depricated
   */
    public static final void println(Object cl, byte[] b, int l) {
        if (OUT) {
            if (!newLine) println(BIGINFO, null, "");
            println(BIGINFO, cl, toString(b, l));
        }
    }

    /**
   * Flush all channels
   */
    public static final void flush() {
        for (int i = 0; i < channel.length; i++) channel[i].flush();
    }

    /**
   * toString-Method expanding Objects (big expanding, if kind is BIGINFO)
   */
    public static final String toString(int kind, Object o) {
        if (o == null) return "" + o;
        if (kind != BIGINFO) return o.toString(); else return toString(o);
    }

    /**
   * toString-Method for ByteArray
   */
    public static final String toString(byte[] b, int l) {
        if (b == null) return "" + b;
        if (OUT) {
            String s = "";
            String h = null;
            String a = "";
            int pl;
            int i;
            if (l < 0) l = b.length;
            if (l > maxByteArrayShown) l = maxByteArrayShown;
            pl = (l > 0x10000) ? 8 : 4;
            for (i = 0; i < l; i++) {
                if ((i & 0x0F) == 0) {
                    h = Integer.toHexString(i);
                    h = ((a.length() > 0) ? ("  :  \"" + a + "\"" + lineSeparator) : "") + "[" + "00000000".substring(0, (h.length() >= pl) ? 0 : (pl - h.length())) + h + "]  ";
                    a = "";
                } else h = " ";
                s = s + h + (((b[i] & 0xf0) == 0) ? "0" : "") + Integer.toHexString(b[i] & 0xff);
                a = a + (Character.isLetterOrDigit((char) b[i]) ? (char) b[i] : '.');
            }
            s = "Byte[" + b.length + "]:" + lineSeparator + s + "                                                ".substring(0, 3 * (15 - ((i + 15) & 0x0f))) + "  :  \"" + a + "\"" + ((l < b.length) ? (lineSeparator + "...") : "");
            return s;
        } else return "Byte[" + b.length + "]" + lineSeparator;
    }

    /**
   * toString-Method for ByteArray
   */
    public static final String toString(byte[] b) {
        return (b == null) ? ("" + b) : toString(b, b.length);
    }

    /**
   * Methode casting an array of Objects to String
   */
    public static final String toString(Object[] o) {
        if (o == null) return "" + o;
        if (OUT) {
            int i;
            String s = "";
            for (i = 0; i < o.length; i++) s += ((i > 0) ? ("  ,  ") : ("")) + toString(o[i]);
            return "[  " + s + "  ]";
        } else return "[ (" + o.length + ") ]";
    }

    /**
   * Methode casting a Vector of Objects to String
   */
    public static final String toString(Vector v) {
        if (v == null) return "" + v;
        if (OUT) {
            int i;
            String s = "";
            for (i = 0; i < v.size(); i++) s += ((i > 0) ? ("  ,  ") : ("")) + toString(v.elementAt(i));
            return "<  " + s + "  >";
        } else return "<  (" + v.size() + ")  >";
    }

    /**
   * toString(...) for Ptoperties.
   */
    public static final String toString(Properties p) {
        if (p == null) return "" + p;
        if (OUT) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(20000);
            PrintStream os = new PrintStream(bos);
            try {
                p.list(os);
                return lineSeparator + bos;
            } catch (Exception e) {
                println(EXCEPTION, "DEBUG", "Exception in Debug, while trying to print Properties!");
                printException("DEBUG", e);
                return "### EXCEPTION ###";
            }
        }
        return "(Properties)";
    }

    /**
   * Methode casting an Object to String
   */
    public static final String toString(Object o) {
        if (o == null) return "" + o;
        if (!(o instanceof String)) if (o instanceof Throwable) return toString((Throwable) o); else if (o instanceof Vector) return toString((Vector) o); else if (o instanceof Properties) return toString((Properties) o); else if (o instanceof byte[]) return toString((byte[]) o); else if (o instanceof Object[]) return toString((Object[]) o); else if (o instanceof ObjectMatrix) return toString((ObjectMatrix) o);
        if (OUT) return o.toString(); else return "[" + o.getClass().getName() + "]";
    }

    /**
   * Methode casting an ObjectMatrix to String
   */
    public static final String toString(ObjectMatrix o) {
        if (o == null) return "" + o;
        if (OUT) return o.toStringBig(); else return "[" + o.getClass().getName() + "]";
    }

    /**
   * Methode casting an Throwable to String
   */
    public static final String toString(Throwable t) {
        if (t == null) return "" + t;
        return lineSeparator + "***" + lineSeparator + getExtendedMessage(t) + "***" + lineSeparator;
    }

    /**
   * Get an extended Message of an Exception
   */
    public static final String getExtendedMessage(Throwable e) {
        return getExtendedMessage(e, true);
    }

    /**
   * Get an extended Message of an Exception
   */
    public static final String getExtendedMessage(Throwable e, boolean withTrace) {
        String msg = "";
        String trace = "";
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
            PrintStream os = new PrintStream(bos);
            if (e != null) {
                e.printStackTrace(os);
                trace = bos.toString();
            } else trace = "Kein Trace verf�gbar";
            if (e == null) msg = "Throwable ist null."; else {
                String classN = e.getClass().getName();
                if (e instanceof NestingExceptions) {
                    NestingExceptions se = (NestingExceptions) e;
                    msg = " Nesting(Runtime)Exception (" + classN + "):" + lineSeparator + " Meldung: '" + e.getMessage() + "'" + lineSeparator;
                    if (se.getInternalException() != null) msg += " Intern:" + lineSeparator + getExtendedMessage(se.getInternalException(), false);
                    trace = se.getNestedTrace();
                } else if (e instanceof SQLException) {
                    SQLException se;
                    se = (SQLException) e;
                    msg = " SQLException (" + classN + "):" + lineSeparator;
                    while (se != null) {
                        String sqlState;
                        int sqlCode;
                        String sqlMsg;
                        sqlMsg = se.getMessage();
                        sqlCode = se.getErrorCode();
                        sqlState = se.getSQLState();
                        if (sqlMsg != null) sqlMsg = sqlMsg.trim();
                        if (sqlState != null) sqlState = sqlState.trim();
                        msg = msg + " Meldung: '" + sqlMsg + "'" + lineSeparator + " Code: " + sqlCode + "" + lineSeparator + " Status: '" + sqlState + "'" + lineSeparator;
                        se = se.getNextException();
                    }
                } else if (e instanceof NullPointerException) msg = "NullPointerException (" + classN + ")"; else if (e instanceof MissingResourceException) {
                    MissingResourceException me = (MissingResourceException) e;
                    msg = "MissingResourceException (" + classN + "):" + lineSeparator + " Resource-class '" + me.getClassName() + "'" + lineSeparator + " key '" + me.getKey() + "'" + lineSeparator;
                } else if (e instanceof FileNotFoundException) msg = "FileNotFoundException (" + classN + ")"; else if (e instanceof IOException) msg = "IOException (" + classN + ")"; else if (e instanceof Exception) msg = "Allgemeine Exception (" + classN + ")"; else msg = "Allgemeiner Error (" + classN + ")";
            }
        } catch (Exception ee) {
            if (e == null) msg = "Exception ist null"; else {
                msg = "(" + e.getClass().getName() + "):" + lineSeparator + "Fehler bei Auswertung der Fehlermeldung!" + lineSeparator + "Meldung: '" + ee.getMessage() + "'" + lineSeparator + "Urspr�ngliche Meldung: '" + e.getMessage() + "'";
            }
        }
        if ((trace != null) && withTrace) return msg + lineSeparator + " Trace:" + lineSeparator + " " + trace; else return msg + lineSeparator;
    }

    /**
   * Analyse the Exception, extend the message, if possible.
   */
    public static String analyseException(String msg, Throwable exc) {
        String db = Parameters.getOptionalParameter("exceptiondialog.autoanalyse.db", "ADABAS-D").toUpperCase();
        Throwable t = exc;
        SQLException s;
        int errCode;
        String errState;
        String errMsg;
        String message = null;
        if (Parameters.getBooleanParameter("exceptiondialog.autoanalyse", true)) if (t != null) {
            if (t instanceof NestingExceptions) t = ((NestingExceptions) t).getBasicException();
            if (t instanceof OutOfMemoryError) message = "\n(Nicht gen�gend Speicher verf�gbar!)"; else if (t instanceof UnmarshalException) message = "\n(Verbindung zum Server gest�rt!)"; else if (t instanceof IOException) message = "\n(Fehler beim Lesen/Schreiben von Dateien: " + exc.getMessage() + ")"; else if (t instanceof FileNotFoundException) message = "\n(Datei nicht gefunden: " + exc.getMessage() + ")"; else if (t instanceof KontorIllegalAccessException) message = "\n(Unzul�ssige Benutzerkennung,\nfalsches Passwort oder\nf�r diesen Benutzer nicht zugelassene Aktion!)"; else if (t instanceof SQLException) {
                s = (SQLException) t;
                errCode = s.getErrorCode();
                errState = s.getSQLState();
                errMsg = s.getMessage();
                if (db.equals("ADABAS-D") || db.equals("ADABAS D")) {
                    switch(errCode) {
                        case 250:
                            message = "\n(Objekt kann nicht gespeichert werden,\nein anderes Objekt mit gleicher EINDEUTIGER Bezeichnung existiert bereits!\nHinweis: Eindeutigkeitsregel '" + errMsg.substring("DUPLICATE SECONDARY KEY:".length()).trim() + "')";
                            break;
                        case 350:
                            message = "\n(Objekt kann nicht gel�scht/ge�ndert werden,\nes wird noch anderweitig verwendet!)";
                            break;
                        case -3014:
                            message = "\n(Fehler beim Datenbank-Zugriff,\nunzul�ssiges Zeichen in einem der Objekt-Felder oder Programmfehler!)";
                            break;
                        case -5005:
                            message = "\n(Objekt kann nicht gespeichert werden,\nein Pflichtfeld wurde nicht ausgef�llt!\nHinweis: Pflichtfeld '" + errMsg.substring("MISSING COLUMN, NULL VALUE NOT ALLOWED:".length()).trim() + "')";
                            break;
                        case -8000:
                            message = "\n(Die Datenbank ist nicht verf�gbar!)";
                            break;
                        case -8004:
                            message = "\n(Objekt kann nicht gespeichert werden,\neines der Felder enth�lt vermutlich eine zu lange Eingabe!)";
                            break;
                        default:
                            if ((errCode == 0) && (errMsg.startsWith("REMOTE-SQL SERVER MUST BE"))) message = "\n(Verbindung zum Datenbank-Server nicht verf�gbar!)"; else if ((errCode <= -9000) && (errCode > -9800)) message = "\n(Schwerwiegender interner Datenbankfehler!)"; else message = "\n(Unbekannter Datenbankfehler!)";
                            break;
                    }
                } else if (db.equals("ORACLE")) {
                    message = "\n(Unbekannter Datenbankfehler!)";
                } else if (db.equals("SOLID")) {
                    message = "\n(Unbekannter Datenbankfehler!)";
                } else message = "\n(Unbekannter Datenbankfehler bei unbekannter Datenbank: " + db + "!)";
            } else {
                String m = Parameters.getBooleanParameter("exceptiondialog.autoanalyse.extended", false) ? t.getMessage() : null;
                message = "\n(" + ((m == null) ? "" : ("'" + m + "'\n")) + exc.getMessage() + ")";
            }
        }
        if (msg == null) msg = "";
        return ((message == null) ? msg : ((msg.length() == 0) ? message : (msg + "\n" + message)));
    }

    protected static final void modifyClassLog(String name, long modifier) {
        synchronized (classLog) {
            Long l = (Long) classLog.get(name);
            if (l == null) l = new Long(modifier); else l = new Long(l.longValue() + modifier);
            classLog.put(name, l);
        }
    }

    public static final void addClass(Object o, boolean hasReferenceManagement) {
        if (DBG && CLASSLOG) {
            String name = (o instanceof String) ? (String) o : o.getClass().getName();
            modifyClassLog(name, 1);
            if (hasReferenceManagement) modifyClassLog("[_u_]" + name, 0);
        }
    }

    public static final void removeClass(Object o, boolean wasUnreferenced) {
        if (DBG && CLASSLOG) {
            String name = (o instanceof String) ? (String) o : o.getClass().getName();
            modifyClassLog(name, -1);
            if (wasUnreferenced) modifyClassLog("[_u_]" + name, -1);
        }
    }

    public static final void unreferencedClass(Object o) {
        if (DBG && CLASSLOG) modifyClassLog("[_u_]" + ((o instanceof String) ? (String) o : o.getClass().getName()), 1);
    }

    public static final String dumpClasses() {
        if (DBG) {
            System.gc();
            System.runFinalization();
            System.gc();
            System.runFinalization();
            System.gc();
            System.runFinalization();
            System.gc();
            System.runFinalization();
            System.gc();
            System.runFinalization();
            Enumeration e = classLog.keys();
            String n;
            StringBuffer dump = new StringBuffer(lineSeparator + "=== Dump of instanciated, announced Classes: ===");
            if (CLASSLOG) while (e.hasMoreElements()) {
                n = (String) e.nextElement();
                if (!n.startsWith("[_u_]")) {
                    Long l1 = (Long) classLog.get(n);
                    Long l2 = (Long) classLog.get("[_u_]" + n);
                    long v1 = l1.longValue();
                    long v2 = (l2 == null) ? 0 : l2.longValue();
                    dump.append(lineSeparator + EUCalendar.numString(v1, 7, '0') + ((l2 == null) ? " -------------------" : " - " + (EUCalendar.numString(v2, 7, '0') + " = " + EUCalendar.numString(v1 - v2, 7, '0'))) + " :   " + n);
                }
            } else dump.append(lineSeparator + "( Class-Logging deaktivated in Debug! )");
            return dump.append(lineSeparator + "================================================" + lineSeparator).toString();
        } else return "";
    }

    /** TEST-main(). */
    public static void main(String[] args) {
        Parameters.defineArguments(args);
        Debug.println(Debug.RUN, "Debug-Test", "Starte Debug-Test (Ergebnis gemaess Property-Definition):");
        Debug.println(Debug.RUN, "Debug-Test", "----------------------------------");
        Debug.print(Debug.RUN, "Debug-Test", "Ausgaben auf");
        Debug.println(Debug.RUN, "Debug-Test", " den verschiedenen logischen Kan�len:");
        Debug.println(Debug.ERROR, "Debug-Test", "ERROR-Kanal...");
        Debug.println(Debug.EXCEPTION, "Debug-Test", "EXCEPTION-Kanal...");
        Debug.println(Debug.WARNING, "Debug-Test", "WARNING-Kanal...");
        Debug.println(Debug.INFO, "Debug-Test", "INFO-Kanal...");
        Debug.println(Debug.SQLINFO, "Debug-Test", "SQLINFO-Kanal...");
        Debug.println(Debug.BIGINFO, "Debug-Test", "BIGINFO-Kanal...");
        Debug.println(Debug.EVENTINFO, "Debug-Test", "EVENTINFO-Kanal...");
        Debug.println(Debug.RUN, "Debug-Test", "RUN-Kanal...");
        Debug.println(Debug.RUN, "Debug-Test", "----------------------------------");
        Debug.println(Debug.RUN, "Debug-Test", "printTrowable(Exception/Error):");
        Debug.printThrowable("Debug-Test", new Exception("Debug-Exception-Test"));
        Debug.printThrowable("Debug-Test", new Error("Debug-Error-Test"));
        Debug.println(Debug.RUN, "Debug-Test", "----------------------------------");
        Debug.println(Debug.RUN, "Debug-Test", "Obekt-Expansion:");
        Debug.println(Debug.BIGINFO, "Debug-Test", "Byte-Array (0-35):");
        Debug.println(Debug.BIGINFO, "Debug-Test", new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35 });
        Debug.println(Debug.BIGINFO, "Debug-Test", "Vector (Element-1 bis -5, byte[5]):");
        Vector v = new Vector(5);
        v.addElement("Element-1");
        v.addElement("Element-2");
        v.addElement("Element-3");
        v.addElement("Element-4");
        v.addElement("Element-5");
        v.addElement(new byte[] { 49, 50, 51, 52, 53 });
        Debug.println(Debug.BIGINFO, "Debug-Test", v);
        Debug.println(Debug.BIGINFO, "Debug-Test", "System-Properties:");
        Debug.print(Debug.BIGINFO, "Debug-Test", System.getProperties());
        Debug.println(Debug.RUN, "Debug-Test", "----------------------------------");
        Debug.println(Debug.RUN, "Debug-Test", "Debug-Test beendet.");
    }
}

final class DebugOutputStream extends OutputStream {

    int dbgCh;

    Object obj;

    /** Create a Redirection for a Object's Output into a certain Channel of Debug. */
    public DebugOutputStream(int debugChannel, Object o) {
        dbgCh = debugChannel;
        obj = (o == null) ? (this) : (o);
    }

    public final void write(int b) throws IOException {
        Debug.print(dbgCh, obj, String.valueOf((char) b));
    }

    ;

    public final void write(byte b[]) throws IOException {
        Debug.print(dbgCh, obj, new String(b));
    }

    public final void write(byte b[], int off, int len) throws IOException {
        Debug.print(dbgCh, obj, new String(b, off, len));
    }
}
