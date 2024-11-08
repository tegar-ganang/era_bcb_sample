package com.yahoo.zookeeper.server;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import com.yahoo.zookeeper.server.quorum.FollowerHandler;
import com.yahoo.zookeeper.server.quorum.QuorumPacket;

/**
 * This class encapsulates and centralizes the logging for the zookeeper server.
 * Log messages go to System.out or System.err depending on the severity of the
 * message.
 * <p>
 * If the methods that do not take an explicit location are used, the location
 * will be derived by creating a stack trace and then looking one frame up the
 * stack trace. (It's a hack, but it works rather well.)
 */
public class ZooLog {

    private static String requestTraceFile = System.getProperty("requestTraceFile");

    private static void formatLine(PrintStream ps, String mess, String location) {
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
        StringBuffer entry = new StringBuffer(dateFormat.format(new Date()) + " [" + location + "][" + Long.toHexString(Thread.currentThread().getId()) + "]: ");
        while (entry.length() < 45) {
            entry.append(' ');
        }
        entry.append(mess);
        ps.println(entry);
    }

    public static void logError(String mess) {
        RuntimeException re = new RuntimeException();
        StackTraceElement ste = re.getStackTrace()[1];
        String location = ZooLog.stackTrace2Location(ste);
        ZooLog.logError(mess, location);
        ZooLog.logTextTraceMessage(mess + "location: " + location, textTraceMask);
    }

    public static void logError(String mess, String location) {
        formatLine(System.err, mess, location);
        System.err.flush();
    }

    public static void logWarn(String mess) {
        RuntimeException re = new RuntimeException();
        StackTraceElement ste = re.getStackTrace()[1];
        String location = ZooLog.stackTrace2Location(ste);
        ZooLog.logWarn(mess, location);
        ZooLog.logTextTraceMessage(mess + " location: " + location, WARNING_TRACE_MASK);
    }

    private static String stackTrace2Location(StackTraceElement ste) {
        String location = ste.getFileName() + "@" + ste.getLineNumber();
        return location;
    }

    public static void logWarn(String mess, String location) {
        formatLine(System.out, mess, location);
        System.out.flush();
    }

    private static void logException(Exception e, String mess, String location) {
        StringWriter sw = new StringWriter();
        sw.append(mess);
        sw.append(": ");
        e.printStackTrace(new PrintWriter(sw));
        if (location == null) {
            RuntimeException re = new RuntimeException();
            StackTraceElement ste = re.getStackTrace()[1];
            location = stackTrace2Location(ste);
        }
        logError(sw.toString(), location);
    }

    public static void logException(Exception e, String mess) {
        RuntimeException re = new RuntimeException();
        StackTraceElement ste = re.getStackTrace()[1];
        logException(e, mess, stackTrace2Location(ste));
    }

    public static void logException(Exception e) {
        RuntimeException re = new RuntimeException();
        StackTraceElement ste = re.getStackTrace()[1];
        logException(e, "", stackTrace2Location(ste));
    }

    static FileChannel tos = null;

    static boolean loggedTraceError = false;

    static boolean traceInitialiazed = false;

    public static final long CLIENT_REQUEST_TRACE_MASK = 1 << 1;

    public static final long CLIENT_DATA_PACKET_TRACE_MASK = 1 << 2;

    public static final long CLIENT_PING_TRACE_MASK = 1 << 3;

    public static final long SERVER_PACKET_TRACE_MASK = 1 << 4;

    public static final long SESSION_TRACE_MASK = 1 << 5;

    public static final long EVENT_DELIVERY_TRACE_MASK = 1 << 6;

    public static final long SERVER_PING_TRACE_MASK = 1 << 7;

    public static final long WARNING_TRACE_MASK = 1 << 8;

    static long binaryTraceMask = CLIENT_REQUEST_TRACE_MASK | SERVER_PACKET_TRACE_MASK | SESSION_TRACE_MASK | WARNING_TRACE_MASK;

    static FileChannel textTos = null;

    static long textTosCreationTime = 0;

    static boolean loggedTextTraceError = false;

    static boolean textTraceInitialiazed = false;

    public static long textTraceMask = CLIENT_REQUEST_TRACE_MASK | SERVER_PACKET_TRACE_MASK | SESSION_TRACE_MASK | WARNING_TRACE_MASK;

    public static void setTextTraceLevel(long mask) {
        textTraceMask = mask;
        logTextTraceMessage("Set text trace mask to " + Long.toBinaryString(mask), textTraceMask);
    }

    public static long getTextTraceLevel() {
        return textTraceMask;
    }

    private static final boolean doLog(long traceMask) {
        return requestTraceFile != null && (textTraceMask & traceMask) != 0;
    }

    public static void logRequest(char rp, Request request, String header, long traceMask) {
        if (!doLog(traceMask)) {
            return;
        }
        RuntimeException re = new RuntimeException();
        StackTraceElement ste = re.getStackTrace()[1];
        String location = ZooLog.stackTrace2Location(ste);
        logRequestText(rp, request, header, traceMask, location);
    }

    private static void write(FileChannel os, String msg) throws IOException {
        os.write(ByteBuffer.wrap(msg.getBytes()));
    }

    private static void writeText(FileChannel os, char rp, Request request, String header, String location) throws IOException {
        StringBuffer sb = new StringBuffer();
        long time = System.currentTimeMillis();
        sb.append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(new Date(time))).append(" ").append(location).append(" ");
        sb.append(header).append(":").append(rp);
        sb.append(request.toString());
        write(os, sb.toString());
        write(textTos, "\n");
    }

    private static void writeText(FileChannel os, String message, String location) throws IOException {
        StringBuffer sb = new StringBuffer();
        long time = System.currentTimeMillis();
        sb.append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(new Date(time))).append(" ").append(location).append(" ");
        sb.append(message);
        write(os, sb.toString());
        write(textTos, "\n");
    }

    private static long ROLLOVER_TIME = 24 * 3600 * 1000;

    private static synchronized void checkTextTraceFile() {
        long time = System.currentTimeMillis();
        if ((time - textTosCreationTime) > ROLLOVER_TIME) {
            textTraceInitialiazed = false;
            if (textTos != null) {
                try {
                    textTos.close();
                } catch (IOException e) {
                }
                textTos = null;
            }
        }
        if (!textTraceInitialiazed) {
            textTraceInitialiazed = true;
            Calendar d = new GregorianCalendar();
            long year = d.get(Calendar.YEAR);
            long month = d.get(Calendar.MONTH) + 1;
            long day = d.get(Calendar.DAY_OF_MONTH);
            if (requestTraceFile == null) {
                return;
            }
            String currentTextFile = requestTraceFile + "." + year + "." + month + "." + day;
            try {
                textTos = new FileOutputStream(currentTextFile + ".txt", true).getChannel();
                textTosCreationTime = time;
                write(textTos, "\n");
            } catch (IOException e) {
                ZooLog.logException(e);
                return;
            }
            ZooLog.logWarn("*********** Traced requests text saved to " + currentTextFile + ".txt");
        }
    }

    private static void checkTraceFile() {
        if (!traceInitialiazed) {
            traceInitialiazed = true;
            String requestTraceFile = System.getProperty("requestTraceFile");
            if (requestTraceFile == null) {
                return;
            }
            try {
                tos = new FileOutputStream(requestTraceFile, true).getChannel();
            } catch (IOException e) {
                ZooLog.logException(e);
                return;
            }
            ZooLog.logWarn("*********** Traced requests saved to " + requestTraceFile);
        }
    }

    public static void logQuorumPacket(char direction, QuorumPacket qp, long traceMask) {
        if (!doLog(traceMask)) {
            return;
        }
        logTextTraceMessage(direction + " " + FollowerHandler.packetToString(qp), traceMask);
    }

    public static void logTextTraceMessage(String text, long traceMask) {
        if (!doLog(traceMask)) {
            return;
        }
        synchronized (ZooLog.class) {
            checkTextTraceFile();
            if (textTos != null && !loggedTextTraceError && ((textTraceMask & traceMask) != 0)) {
                try {
                    RuntimeException re = new RuntimeException();
                    StackTraceElement ste = re.getStackTrace()[1];
                    String location = ZooLog.stackTrace2Location(ste);
                    writeText(textTos, text, location);
                } catch (IOException e1) {
                    logException(e1);
                    loggedTextTraceError = true;
                }
            }
        }
    }

    private static synchronized void logRequestText(char rp, Request request, String header, long traceMask, String location) {
        if (!doLog(traceMask)) {
            return;
        }
        checkTextTraceFile();
        if (textTos != null && !loggedTextTraceError && ((traceMask & textTraceMask) != 0)) {
            try {
                writeText(textTos, rp, request, header, location);
            } catch (IOException e1) {
                logException(e1);
                loggedTextTraceError = true;
            }
        }
    }

    public void logRequestBinary(char rp, Request request, long traceMask) {
        if (!doLog(traceMask)) {
            return;
        }
        synchronized (ZooLog.class) {
            checkTraceFile();
            if (tos != null && !loggedTraceError && ((traceMask & binaryTraceMask) != 0)) {
                ByteBuffer bb = ByteBuffer.allocate(41);
                bb.put((byte) rp);
                bb.putLong(System.currentTimeMillis());
                bb.putLong(request.sessionId);
                bb.putInt(request.cxid);
                bb.putLong(request.hdr == null ? -2 : request.hdr.getZxid());
                bb.putInt(request.hdr == null ? -2 : request.hdr.getType());
                bb.putInt(request.type);
                if (request.request != null) {
                    bb.putInt(request.request.remaining());
                } else {
                    bb.putInt(0);
                }
                bb.flip();
                try {
                    if (request.request == null) {
                        tos.write(bb);
                    } else {
                        tos.write(new ByteBuffer[] { bb, request.request.duplicate() });
                    }
                } catch (IOException e) {
                    logException(e);
                    loggedTraceError = true;
                }
            }
        }
    }
}
