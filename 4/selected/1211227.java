package com.bitgate.util.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;
import com.bitgate.server.Server;
import com.bitgate.util.archive.Archive;
import com.bitgate.util.debug.Debug;
import com.bitgate.util.scheduler.SchedulerInterface;

/**
 * This class logs the requests made from a client.  Logging information is set using a globally statically allocated
 * default instance.
 *
 * @author Kenji Hollis &lt;kenji@nuklees.com&gt;
 * @version $Id: //depot/nuklees/util/services/RequestLogger.java#28 $
 */
public class RequestLogger {

    private static final RequestLogger _default = new RequestLogger();

    private static final HashMap loggerFiles = new HashMap();

    private static final HashMap loggerFilenames = new HashMap();

    private static final HashMap loggerTimeouts = new HashMap();

    private static final HashMap loggerDests = new HashMap();

    private static final HashMap loggerCompresses = new HashMap();

    private static final HashMap loggerDays = new HashMap();

    private static final HashMap loggerArchives = new HashMap();

    private static final HashMap loggerDeletes = new HashMap();

    private static int requestCounter;

    /**
     * Constructor.
     */
    public RequestLogger() {
        Server.getScheduler().register("Request Log Rotator", new SchedulerInterface() {

            public int getScheduleRate() {
                return 0;
            }

            public void handle() {
                if (requestCounter == 0) {
                    requestCounter++;
                    return;
                }
                requestCounter++;
                Iterator it = loggerTimeouts.keySet().iterator();
                while (it.hasNext()) {
                    String host = (String) it.next();
                    String timeout = (String) loggerTimeouts.get(host);
                    int timeoutVal = Integer.parseInt(timeout);
                    if (timeoutVal > 0) {
                        timeoutVal /= 10;
                    }
                    int timeoutMod = (requestCounter % timeoutVal);
                    if (timeoutMod == 0) {
                        Object loggerFile = loggerFiles.get(host);
                        Object loggerFilenameObject = loggerFilenames.get(host);
                        Object loggerDestObject = loggerDests.get(host);
                        Object loggerCompressObject = loggerCompresses.get(host);
                        Object loggerDayObject = loggerDays.get(host);
                        Object loggerArchiveObject = loggerArchives.get(host);
                        Object loggerDeleteObject = loggerDeletes.get(host);
                        boolean rotateCompress = false;
                        boolean rotateDelete = false;
                        String rotateDest = null;
                        String logFile = null;
                        String rotateArchive = null;
                        int rotateDays = 0;
                        if (loggerCompressObject != null && ((String) loggerCompressObject).equalsIgnoreCase("true")) {
                            rotateCompress = true;
                        }
                        if (loggerDeleteObject != null && ((String) loggerDeleteObject).equalsIgnoreCase("true")) {
                            rotateDelete = true;
                        }
                        if (loggerDestObject != null) {
                            rotateDest = (String) loggerDestObject;
                        }
                        if (loggerFilenameObject != null) {
                            logFile = (String) loggerFilenameObject;
                        }
                        if (loggerArchiveObject != null) {
                            rotateArchive = (String) loggerArchiveObject;
                        }
                        if (loggerDayObject != null) {
                            rotateDays = Integer.parseInt((String) loggerDayObject);
                        }
                        FileChannel srcChannel, destChannel;
                        String destOutFile = logFile + "." + System.currentTimeMillis();
                        String destOutFileCompressed = logFile + "." + System.currentTimeMillis() + ".gz";
                        if (rotateDest != null) {
                            (new File(rotateDest)).mkdirs();
                            if (destOutFile.indexOf("/") != -1) {
                                destOutFile = rotateDest + "/" + destOutFile.substring(destOutFile.lastIndexOf("/") + 1);
                            }
                            if (destOutFileCompressed.indexOf("/") != -1) {
                                destOutFileCompressed = rotateDest + "/" + destOutFileCompressed.substring(destOutFileCompressed.lastIndexOf("/") + 1);
                            }
                        }
                        if (rotateCompress) {
                            try {
                                GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(destOutFileCompressed));
                                FileInputStream in = new FileInputStream(logFile);
                                byte buf[] = new byte[1024];
                                int len;
                                while ((len = in.read(buf)) > 0) {
                                    out.write(buf, 0, len);
                                }
                                in.close();
                                out.finish();
                                out.close();
                                Debug.debug("Rotated log file '" + logFile + "' to '" + destOutFileCompressed + "'");
                                buf = null;
                                out = null;
                                in = null;
                            } catch (Exception e) {
                                Debug.debug("Unable to rotate log file '" + logFile + "': " + e);
                            }
                        } else {
                            try {
                                srcChannel = new FileInputStream(logFile).getChannel();
                            } catch (IOException e) {
                                Debug.debug("Unable to read log file '" + logFile + "': " + e.getMessage());
                                return;
                            }
                            try {
                                destChannel = new FileOutputStream(destOutFile).getChannel();
                            } catch (IOException e) {
                                Debug.debug("Unable to rotate log file '" + logFile + "' to '" + destOutFile + "': " + e.getMessage());
                                return;
                            }
                            try {
                                destChannel.transferFrom(srcChannel, 0, srcChannel.size());
                                srcChannel.close();
                                destChannel.close();
                                destChannel = null;
                                srcChannel = null;
                            } catch (IOException e) {
                                Debug.debug("Unable to copy data from file '" + logFile + "' to '" + destOutFile + "' for file rotation: " + e.getMessage());
                                return;
                            }
                            Debug.debug("Rotated log file '" + logFile + "' to '" + destOutFile + "'");
                        }
                        if (rotateDelete) {
                            try {
                                ((PrintStream) loggerFile).close();
                            } catch (Exception e) {
                            }
                            (new File(logFile)).delete();
                            loggerFiles.remove(host);
                            addLogger(host, logFile);
                        }
                        if (rotateDest != null) {
                            long comparisonTime = rotateDays * (60 * 60 * 24 * 1000);
                            long currentTime = System.currentTimeMillis();
                            File fileList[] = (new File(rotateDest)).listFiles();
                            DateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
                            java.util.Date date = new java.util.Date(currentTime);
                            String archiveFile = format1.format(date).toString() + ".zip";
                            if (rotateArchive != null) {
                                archiveFile = rotateArchive + "/" + archiveFile;
                                (new File(rotateArchive)).mkdirs();
                            }
                            Archive archive = new Archive(archiveFile);
                            for (int i = 0; i < fileList.length; i++) {
                                String currentFilename = fileList[i].getName();
                                long timeDifference = (currentTime - fileList[i].lastModified());
                                if ((rotateCompress && currentFilename.endsWith(".gz")) || (!rotateCompress && currentFilename.indexOf(logFile + ".") != -1)) {
                                    if (rotateDest != null) {
                                        currentFilename = rotateDest + "/" + currentFilename;
                                    }
                                    if (timeDifference > comparisonTime) {
                                        archive.addFile(fileList[i].getName(), currentFilename);
                                        fileList[i].delete();
                                    }
                                }
                            }
                            fileList = null;
                            format1 = null;
                            archive = null;
                        }
                    }
                }
                it = null;
            }

            public String identString() {
                return "Request Log Rotator";
            }
        });
    }

    /**
     * Returns the default instance of the class.
     *
     * @return <code>static RequestLogger</code> object.
     */
    public static RequestLogger getDefault() {
        return _default;
    }

    /**
     * Adds a logger entry for a host, by filename.
     *
     * @param hostname The host name requested.
     * @param filename The filename that was requested in the initial request.
     */
    public void addLogger(String hostname, String filename) {
        if (!Server.getRequestLogging()) {
            return;
        }
        if (loggerFiles.get(hostname) == null) {
            PrintStream ps = null;
            try {
                ps = new PrintStream(new FileOutputStream(filename, true));
            } catch (Exception e) {
                Debug.warn("Unable to log requests to file '" + filename + "': " + e);
                return;
            }
            loggerFiles.put(hostname, ps);
            loggerFilenames.put(hostname, filename);
        }
    }

    /**
     * Adds a timeout to a logged object.
     *
     * @param hostname The hostname to add.
     * @param timeout The timeout to add.
     */
    public void addLoggerTimeout(String hostname, int timeout) {
        if (!Server.getRequestLogging()) {
            return;
        }
        loggerTimeouts.put(hostname, "" + timeout);
    }

    /**
     * Adds a destination object for a logged hostname.
     *
     * @param hostname The hostname to add.
     * @param dest The destination to add.
     */
    public void addLoggerDest(String hostname, String dest) {
        if (!Server.getRequestLogging()) {
            return;
        }
        loggerDests.put(hostname, dest);
    }

    /**
     * Whether or not the request made was compressed data.
     *
     * @param hostname The hostname to look up.
     * @param compress Whether or not to compress data.
     */
    public void addLoggerCompress(String hostname, boolean compress) {
        if (!Server.getRequestLogging()) {
            return;
        }
        if (compress) {
            loggerCompresses.put(hostname, "true");
        } else {
            loggerCompresses.put(hostname, "false");
        }
    }

    /**
     * Sets the number of days made by a request.
     *
     * @param hostname The hostname to add.
     * @param days The days to set.
     */
    public void addLoggerDays(String hostname, int days) {
        if (!Server.getRequestLogging()) {
            return;
        }
        loggerDays.put(hostname, "" + days);
    }

    /**
     * The archive to set for the hostname.
     *
     * @param hostname The hostname to add.
     * @param archive The archive entry to add.
     */
    public void addLoggerArchive(String hostname, String archive) {
        if (!Server.getRequestLogging()) {
            return;
        }
        loggerArchives.put(hostname, archive);
    }

    /**
     * Whether or not to delete an entry for a hostname.
     *
     * @param hostname The hostname to delete.
     * @param flag <code>true</code> to delete the hostname, <code>false</code> otherwise.
     */
    public void addLoggerDelete(String hostname, boolean flag) {
        if (!Server.getRequestLogging()) {
            return;
        }
        if (flag) {
            loggerDeletes.put(hostname, "true");
        } else {
            loggerDeletes.put(hostname, "false");
        }
    }

    /**
     * Adds a new log for the entry specified.
     *
     * @param ww The <code>WorkerContext</code> object to refer to.
     * @param status The resulting status from the request.
     * @param bytes The number of bytes in the request.
     */
    public void log(WorkerContext ww, int status, int bytes) {
        Object loggerObject = null;
        String workerHost = null;
        workerHost = ww.getClientContext().getRequestHost();
        if (loggerFiles.get(workerHost) != null) {
            loggerObject = loggerFiles.get(workerHost);
        } else {
            loggerObject = loggerFiles.get("*:" + ww.getVendContext().getVend().getListenPort());
        }
        if (loggerObject == null) {
            return;
        }
        Calendar now = Calendar.getInstance();
        StringBuffer dateOut = new StringBuffer(80);
        String months[] = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
        if (ww.getClientContext().getClientIp() != null) {
            dateOut.append(ww.getClientContext().getClientIp());
        } else {
            dateOut.append("unknown");
        }
        dateOut.append(" - ");
        if (ww.getClientContext().getRequestHeader("remote-user") != null) {
            dateOut.append((String) ww.getClientContext().getRequestHeader("remote-user"));
            dateOut.append(' ');
        } else {
            dateOut.append("- ");
        }
        dateOut.append('[');
        dateOut.append((now.get(Calendar.DAY_OF_MONTH) > 9) ? Integer.toString(now.get(Calendar.DAY_OF_MONTH)) : "0" + Integer.toString(now.get(Calendar.DAY_OF_MONTH)));
        dateOut.append('/');
        dateOut.append(months[now.get(Calendar.MONTH)]);
        dateOut.append('/');
        dateOut.append(Integer.toString(now.get(Calendar.YEAR)));
        dateOut.append(':');
        dateOut.append((now.get(Calendar.HOUR_OF_DAY) > 9) ? Integer.toString(now.get(Calendar.HOUR_OF_DAY)) : "0" + Integer.toString(now.get(Calendar.HOUR_OF_DAY)));
        dateOut.append(':');
        dateOut.append((now.get(Calendar.MINUTE) > 9) ? Integer.toString(now.get(Calendar.MINUTE)) : '0' + Integer.toString(now.get(Calendar.MINUTE)));
        dateOut.append(":");
        dateOut.append((now.get(Calendar.SECOND) > 9) ? Integer.toString(now.get(Calendar.SECOND)) : '0' + Integer.toString(now.get(Calendar.SECOND)));
        dateOut.append(' ');
        int zoneOffset = (now.get(Calendar.ZONE_OFFSET) / 3600000);
        String zoneAdder = null;
        if (zoneOffset < 0) {
            zoneAdder = "-";
        } else {
            zoneAdder = "+";
        }
        zoneOffset = Math.abs(zoneOffset);
        dateOut.append(zoneAdder);
        if (zoneOffset < 10) {
            dateOut.append('0');
        }
        dateOut.append(zoneOffset);
        dateOut.append("00");
        dateOut.append("] \"");
        dateOut.append(ww.getClientContext().getInitialRequest());
        dateOut.append("\" ");
        dateOut.append(Integer.toString(status));
        dateOut.append(" ");
        dateOut.append(Integer.toString(bytes));
        dateOut.append(" ");
        if (ww.getClientContext().getRequestHeader("referer") != null) {
            dateOut.append("\"");
            dateOut.append((String) ww.getClientContext().getRequestHeader("referer"));
            dateOut.append("\"");
        } else {
            dateOut.append("-");
        }
        dateOut.append(" ");
        if (ww.getClientContext().getRequestHeader("user-agent") != null) {
            dateOut.append('\"');
            dateOut.append((String) ww.getClientContext().getRequestHeader("user-agent"));
            dateOut.append('\"');
        } else {
            dateOut.append("-");
        }
        ((PrintStream) loggerObject).println(dateOut.toString());
        now = null;
    }
}
