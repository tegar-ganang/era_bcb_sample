package org.dcm4che2.tool.dcmwado;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.dcm4che2.util.CloseUtils;

/**
 * @author gunter zeilinger(gunterze@gmail.com)
 * @version $Revision: 6360 $ $Date: 2008-05-29 03:31:09 -0400 (Thu, 29 May 2008) $
 * @since Oct 13, 2005
 */
public class DcmWado {

    private static final int KB = 1024;

    private static final int MB = KB * KB;

    private static final String USAGE = "dcmwado [Options] <base-url> (-uid <uids>) or (<file>|<directory>[...])";

    private static final String DESCRIPTION = "Invokes single or multiple HTTP GET request(s) according DICOM Part 18: " + "Web Access to DICOM Persistent Objects (WADO) on WADO server specified " + "by <base-url>.\n\n" + "Options:";

    private static final String EXAMPLE = "\nExample 1: dcmwado http://localhost:8080/dcm4jboss-wado/wado -dcm \\" + "\n      -uid 1.2.3.4:1.2.3.4.5:1.2.3.4.5.6 -dir /tmp/wado  \n" + "=> Request single DICOM Object with specified uids from the local WADO " + "server listening on port 8080, and store it in directory /tmp/wado" + "\nExample 2: dcmwado http://localhost:8080/dcm4jboss-wado/wado -dcm \\" + "\n      /cdrom/DICOM -nostore\n" + "=> Scan all DICOM files under directory /cdrom/DICOM and request for " + "each file the corresponding DICOM Object from the local WADO " + "server listening on port 8080, without storing the response to disk.";

    private String baseurl;

    private String requestType = "WADO";

    private String[] psuid;

    private String[] tsuid;

    private boolean tsfile = false;

    private ArrayList<String> contentType = new ArrayList<String>();

    private String[] charset;

    private boolean anonymize = false;

    private String[] annotation;

    private int rows;

    private int columns;

    private int frameNumber;

    private String[] region;

    private String[] window;

    private int imageQuality;

    private boolean noKeepAlive;

    private boolean followsRedirect = true;

    private File dir = new File(".");

    private ArrayList<URL> url = new ArrayList<URL>();

    private byte[] buffer = new byte[8 * KB];

    private long totalSize = 0L;

    private static CommandLine parse(String[] args) {
        Options opts = new Options();
        OptionBuilder.withArgName("suid:Suid:iuid");
        OptionBuilder.hasArgs(3);
        OptionBuilder.withValueSeparator(':');
        OptionBuilder.withDescription("Retrieve object with given Study " + "Instance UID, Series Instance UID and SOP Instance UID.");
        opts.addOption(OptionBuilder.create("uid"));
        OptionBuilder.withArgName("Suid:iuid");
        OptionBuilder.hasArgs(2);
        OptionBuilder.withValueSeparator(':');
        OptionBuilder.withDescription("Series Instance UID and SOP Instance UID " + "of the presentation state storage object to be applied to the " + "image.");
        opts.addOption(OptionBuilder.create("pr"));
        opts.addOption("dcm", false, "Request DICOM object. (MIME type: application/dicom)");
        opts.addOption("jpeg", false, "Request JPEG image. (MIME type: image/jpeg)");
        opts.addOption("gif", false, "Request GIF image. (MIME type: image/gif)");
        opts.addOption("png", false, "Request PNG image. (MIME type: image/png)");
        opts.addOption("jp2", false, "Request JPEG 2000 image. (MIME type: image/jp2)");
        opts.addOption("mpeg", false, "Request MPEG video. (MIME type: video/mpeg)");
        opts.addOption("txt", false, "Request plain text document. (MIME type: text/plain)");
        opts.addOption("html", false, "Request HTML document. (MIME type: text/html)");
        opts.addOption("xml", false, "Request XML document. (MIME type: text/xml)");
        opts.addOption("rtf", false, "Request RTF document. (MIME type: text/rtf)");
        opts.addOption("pdf", false, "Request PDF document. (MIME type: application/pdf)");
        opts.addOption("cda1", false, "Request CDA Level 1 document. " + "(MIME type: application/x-hl7-cda-level-one+xml)");
        OptionBuilder.withArgName("type");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Request document with the specified MIME type." + "Alternative MIME types can be specified by additional -mime options.");
        opts.addOption(OptionBuilder.create("mime"));
        OptionBuilder.withArgName("uid");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Returned object shall be encoded with " + "the specified Transfer Syntax. Alternative Transfer Syntaxes " + "can be specified by additional -ts options.");
        opts.addOption(OptionBuilder.create("ts"));
        OptionBuilder.withArgName("name");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Returned object shall be encoded with " + "specified Character set. Alternative Character sets " + "can be specified by additional -charset options.");
        opts.addOption(OptionBuilder.create("charset"));
        opts.addOption("anonymize", false, "Remove all patient identification" + "information from returned DICOM Object");
        OptionBuilder.withArgName("type");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Burn in patient information" + "(-annotation=patient) and/or technique information " + "(-annotation=technique) in returned pixel data.");
        opts.addOption(OptionBuilder.create("annotation"));
        OptionBuilder.withArgName("num");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Maximal number of pixel rows in returned image.");
        opts.addOption(OptionBuilder.create("rows"));
        OptionBuilder.withArgName("num");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Maximal number of pixel columns in returned image.");
        opts.addOption(OptionBuilder.create("columns"));
        OptionBuilder.withArgName("num");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Return single frame with that number " + "within a multi-frame image object.");
        opts.addOption(OptionBuilder.create("frame"));
        OptionBuilder.withArgName("x1:y1:x2:y2");
        OptionBuilder.hasArgs(4);
        OptionBuilder.withValueSeparator(':');
        OptionBuilder.withDescription("Return rectangular region of image " + "matrix specified by top left (x1,y1) and bottom right (x2,y2) " + "corner in relative coordinates within the range 0.0 to 1.0.");
        opts.addOption(OptionBuilder.create("window"));
        OptionBuilder.withArgName("center/width");
        OptionBuilder.hasArgs(2);
        OptionBuilder.withValueSeparator('/');
        OptionBuilder.withDescription("Specifies center and width of the " + "VOI window to be applied to the image.");
        opts.addOption(OptionBuilder.create("window"));
        OptionBuilder.withArgName("num");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Quality of the image to be returned " + "within the range 1 to 100, 100 being the best quality.");
        opts.addOption(OptionBuilder.create("quality"));
        OptionBuilder.withArgName("path");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Directory to store retrieved objects, " + "working directory by default");
        opts.addOption(OptionBuilder.create("dir"));
        opts.addOption("nostore", false, "Do not store retrieved objects to files.");
        opts.addOption("nokeepalive", false, "Close TCP connection after each response.");
        opts.addOption("noredirect", false, "Disable HTTP redirects.");
        OptionBuilder.withArgName("kB");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Size of byte buffer in KB " + "used for copying the retrieved object to disk, 8 KB by default.");
        opts.addOption(OptionBuilder.create("buffersize"));
        opts.addOption("h", "help", false, "print this message");
        opts.addOption("V", "version", false, "print the version information and exit");
        CommandLine cl = null;
        try {
            cl = new GnuParser().parse(opts, args);
        } catch (MissingOptionException e) {
            exit("dcmwado: Missing required option " + e.getMessage());
            throw new RuntimeException("unreachable");
        } catch (ParseException e) {
            exit("dcmwado: " + e.getMessage());
            throw new RuntimeException("unreachable");
        }
        if (cl.hasOption('V')) {
            Package p = DcmWado.class.getPackage();
            System.out.println("dcmwado v" + p.getImplementationVersion());
            System.exit(0);
        }
        if (cl.hasOption('h')) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(USAGE, DESCRIPTION, opts, EXAMPLE);
            System.exit(0);
        }
        int narg = cl.getArgList().size();
        if (narg == 0) exit("Missing url of WADO server");
        if (narg == 1) {
            if (!cl.hasOption("uid")) {
                exit("You must either option -uid <uids> or <file>|<directory> specify");
            }
        } else {
            if (cl.hasOption("uid")) {
                exit("You may not specify option -uid <uids> together with " + "<file>|<directory>.");
            }
        }
        return cl;
    }

    private static int parseInt(String s, String errPrompt, int min, int max) {
        try {
            int i = Integer.parseInt(s);
            if (i >= min && i <= max) return i;
        } catch (NumberFormatException e) {
        }
        throw new IllegalArgumentException(errPrompt);
    }

    private static void checkFloats(String[] a, String errPrompt, int size, float min, float max) {
        if (a.length != size) {
            throw new IllegalArgumentException(errPrompt);
        }
        for (int i = 0; i < a.length; i++) {
            parseFloat(a[i], errPrompt, min, max);
        }
    }

    private static float parseFloat(String s, String errPrompt, float min, float max) {
        try {
            float f = Integer.parseInt(s);
            if (f >= min && f <= max) return f;
        } catch (NumberFormatException e) {
        }
        throw new IllegalArgumentException(errPrompt);
    }

    public static void main(String[] args) {
        DcmWado dcmwado = new DcmWado();
        try {
            CommandLine cl = parse(args);
            if (cl.hasOption("pr")) {
                dcmwado.setPresentation(cl.getOptionValues("pr"));
            }
            if (cl.hasOption("dcm")) {
                dcmwado.addContentType("application/dicom");
            }
            if (cl.hasOption("jpeg")) {
                dcmwado.addContentType(" image/jpeg");
            }
            if (cl.hasOption("gif")) {
                dcmwado.addContentType("image/gif)");
            }
            if (cl.hasOption("png")) {
                dcmwado.addContentType("image/png");
            }
            if (cl.hasOption("jp2")) {
                dcmwado.addContentType("image/jp2");
            }
            if (cl.hasOption("mpeg")) {
                dcmwado.addContentType("video/mpeg");
            }
            if (cl.hasOption("txt")) {
                dcmwado.addContentType("text/plain");
            }
            if (cl.hasOption("html")) {
                dcmwado.addContentType("text/html");
            }
            if (cl.hasOption("xml")) {
                dcmwado.addContentType("text/xml");
            }
            if (cl.hasOption("rtf")) {
                dcmwado.addContentType("text/rtf");
            }
            if (cl.hasOption("pdf")) {
                dcmwado.addContentType("application/pdf");
            }
            if (cl.hasOption("cda1")) {
                dcmwado.addContentType("application/x-hl7-cda-level-one+xml");
            }
            if (cl.hasOption("mime")) {
                dcmwado.addContentType(cl.getOptionValues("mime"));
            }
            if (cl.hasOption("ts")) {
                dcmwado.setTransferSyntax(cl.getOptionValues("ts"));
            }
            dcmwado.setTransferSyntaxSameAsFile(cl.hasOption("tsfile"));
            if (cl.hasOption("charset")) {
                dcmwado.setCharset(cl.getOptionValues("charset"));
            }
            dcmwado.setAnonymize(cl.hasOption("anonymize"));
            if (cl.hasOption("annotation")) {
                dcmwado.setAnnotation(cl.getOptionValues("annotation"));
            }
            if (cl.hasOption("rows")) {
                dcmwado.setRows(parseInt(cl.getOptionValue("h"), "Invalid value of -h", 1, Integer.MAX_VALUE));
            }
            if (cl.hasOption("columns")) {
                dcmwado.setColumns(parseInt(cl.getOptionValue("w"), "Invalid value of -w", 1, Integer.MAX_VALUE));
            }
            if (cl.hasOption("frame")) {
                dcmwado.setFrameNumber(parseInt(cl.getOptionValue("f"), "Invalid value of -f", 1, Integer.MAX_VALUE));
            }
            if (cl.hasOption("region")) {
                dcmwado.setRegion(cl.getOptionValues("reg"));
            }
            if (cl.hasOption("window")) {
                dcmwado.setWindow(cl.getOptionValues("voi"));
            }
            if (cl.hasOption("quality")) {
                dcmwado.setImageQuality(parseInt(cl.getOptionValue("q"), "Invalid value of -q", 1, Integer.MAX_VALUE));
            }
            if (cl.hasOption("dir")) {
                dcmwado.setDirectory(new File(cl.getOptionValue("dir")));
            }
            if (cl.hasOption("nostore")) {
                dcmwado.setDirectory(null);
            }
            dcmwado.setNoKeepAlive(cl.hasOption("nokeepalive"));
            dcmwado.setFollowsRedirect(!cl.hasOption("noredirect"));
            if (cl.hasOption("buffersize")) {
                dcmwado.setBufferSize(parseInt(cl.getOptionValue("bs"), "Invalid value of -bs", 1, 1000) * KB);
            }
            List argList = cl.getArgList();
            dcmwado.setBaseURL((String) argList.get(0));
            if (cl.hasOption("uid")) {
                dcmwado.initURL(cl.getOptionValues("uid"));
            } else {
                System.out.println("Scanning files for uids");
                long t1 = System.currentTimeMillis();
                for (int i = 1, n = argList.size(); i < n; i++) {
                    dcmwado.addURL(new File((String) argList.get(i)));
                }
                long t2 = System.currentTimeMillis();
                System.out.println("\nScanned " + dcmwado.getNumberOfRequests() + " files in " + ((t2 - t1) / 1000F) + "s");
            }
        } catch (Exception e) {
            exit(e.getMessage());
        }
        long t1 = System.currentTimeMillis();
        dcmwado.fetchObjects();
        long t2 = System.currentTimeMillis();
        float seconds = (t2 - t1) / 1000F;
        System.out.println("\nFetch " + dcmwado.getNumberOfRequests() + " objects (=" + promptBytes(dcmwado.getTotalSize()) + ") in " + seconds + "s (=" + promptBytes(dcmwado.getTotalSize() / seconds) + "/s)");
    }

    private static String promptBytes(float totalSizeSent) {
        return (totalSizeSent > MB) ? ("" + (totalSizeSent / MB) + "MB") : ("" + (totalSizeSent / KB) + "KB");
    }

    private static void exit(String msg) {
        System.err.println(msg);
        System.err.println("Try 'dcmwado -h' for more information.");
        System.exit(1);
    }

    private void setBufferSize(int size) {
        buffer = new byte[size];
    }

    public final void setDirectory(File dir) {
        if (dir != null && dir.mkdirs()) System.out.println("INFO: Create directory " + dir);
        this.dir = dir;
    }

    public final void setAnnotation(String[] annotation) {
        this.annotation = annotation;
    }

    public final void setAnonymize(boolean anonymize) {
        this.anonymize = anonymize;
    }

    public final void setBaseURL(String url) {
        checkURL(url);
        this.baseurl = url;
    }

    private void checkURL(String url) {
        try {
            String protocol = new URL(url).getProtocol();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                throw new IllegalArgumentException("Illegal base-url - " + url);
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Illegal base-url - " + url, e);
        }
    }

    public final void setCharset(String[] charset) {
        this.charset = charset;
    }

    public final void setColumns(int columns) {
        this.columns = columns;
    }

    public final void addContentType(String contentType) {
        this.contentType.add(contentType);
    }

    public final void addContentType(String[] contentType) {
        this.contentType.addAll(Arrays.asList(contentType));
    }

    public final void setFrameNumber(int frameNumber) {
        this.frameNumber = frameNumber;
    }

    public final void setImageQuality(int imageQuality) {
        this.imageQuality = imageQuality;
    }

    public final int getNumberOfRequests() {
        return url.size();
    }

    public final void setPresentation(String[] psuid) {
        if (psuid.length != 2) throw new IllegalArgumentException("Illegal argument for -pr");
        this.psuid = psuid;
    }

    public final void setRegion(String[] region) {
        checkFloats(region, "Illegal argument for -reg", 4, 0.f, 1.f);
        this.region = region;
    }

    public final void setRows(int rows) {
        this.rows = rows;
    }

    public final void setTransferSyntax(String[] tsuids) {
        this.tsuid = tsuids;
    }

    public final void setTransferSyntaxSameAsFile(boolean tsfile) {
        this.tsfile = tsfile;
    }

    public final void setFollowsRedirect(boolean followsRedirect) {
        this.followsRedirect = followsRedirect;
    }

    public final void setNoKeepAlive(boolean noKeepAlive) {
        this.noKeepAlive = noKeepAlive;
    }

    public void initURL(String[] uid) throws MalformedURLException {
        url.add(makeURL(uid, tsuid));
    }

    public final void setWindow(String[] window) {
        if (window.length != 2) throw new IllegalArgumentException("Illegal argument for -voi");
        parseFloat(window[0], "Illegal argument for -voi", Float.MIN_VALUE, Float.MAX_VALUE);
        parseFloat(window[1], "Illegal argument for -voi", 0, Float.MAX_VALUE);
        this.window = window;
    }

    public final long getTotalSize() {
        return totalSize;
    }

    public void addURL(File f) {
        if (f.isDirectory()) {
            File[] fs = f.listFiles();
            for (int i = 0; i < fs.length; i++) addURL(fs[i]);
            return;
        }
        DicomObject dcmObj = new BasicDicomObject();
        DicomInputStream in = null;
        try {
            in = new DicomInputStream(f);
            in.setHandler(new StopTagInputHandler(Tag.StudyID));
            in.readDicomObject(dcmObj, -1);
            String[] uid = { dcmObj.getString(Tag.StudyInstanceUID), dcmObj.getString(Tag.SeriesInstanceUID), dcmObj.getString(Tag.SOPInstanceUID) };
            String[] ts = tsfile ? new String[] { in.getTransferSyntax().uid() } : tsuid;
            url.add(makeURL(uid, ts));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("WARNING: Failed to parse " + f + " - skipped.");
            System.out.print('F');
            return;
        } finally {
            CloseUtils.safeClose(in);
        }
        System.out.print('.');
    }

    public void fetchObjects() {
        for (Iterator iter = url.iterator(); iter.hasNext(); ) {
            fetch((URL) iter.next());
        }
    }

    private void fetch(URL url) {
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setInstanceFollowRedirects(followsRedirect);
            con.setRequestProperty("Connection", "Keep-Alive");
            con.connect();
            InputStream in = null;
            OutputStream out = null;
            try {
                in = con.getInputStream();
                out = getOutputStream(URLEncoder.encode(url.getQuery(), "UTF-8"));
                copy(in, out);
            } finally {
                CloseUtils.safeClose(out);
                CloseUtils.safeClose(in);
                if (noKeepAlive) con.disconnect();
            }
            System.out.print('.');
        } catch (Exception e) {
            System.err.println("ERROR: Failed to GET " + url + " - " + e.getMessage());
            e.printStackTrace();
            System.out.print('F');
        }
    }

    private void copy(InputStream in, OutputStream out) throws IOException {
        int read;
        while ((read = in.read(buffer)) != -1) {
            totalSize += read;
            if (out != null) {
                out.write(buffer, 0, read);
            }
        }
    }

    private OutputStream getOutputStream(String fname) throws FileNotFoundException {
        return dir != null ? new FileOutputStream(new File(dir, fname)) : null;
    }

    private URL makeURL(String[] uid, String[] tsuid) throws MalformedURLException {
        StringBuffer sb = new StringBuffer(256);
        sb.append(baseurl).append("?requestType=").append(requestType);
        sb.append("&studyUID=").append(uid[0]);
        sb.append("&seriesUID=").append(uid[1]);
        sb.append("&objectUID=").append(uid[2]);
        if (!contentType.isEmpty()) {
            sb.append("&contentType=");
            append(contentType, sb);
        }
        if (charset != null) {
            sb.append("&charset=");
            append(charset, sb);
        }
        if (anonymize) {
            sb.append("&anonymize=yes");
        }
        if (annotation != null) {
            sb.append("&annotation=");
            append(annotation, sb);
        }
        if (rows > 0) {
            sb.append("&rows=").append(rows);
        }
        if (columns > 0) {
            sb.append("&columns=").append(columns);
        }
        if (frameNumber > 0) {
            sb.append("&frameNumber=").append(frameNumber);
        }
        if (imageQuality > 0) {
            sb.append("&imageQuality=").append(imageQuality);
        }
        if (region != null) {
            sb.append("&region=");
            append(region, sb);
        }
        if (window != null) {
            sb.append("&windowCenter=").append(window[0]);
            sb.append("&windowWidth=").append(window[1]);
        }
        if (psuid != null) {
            sb.append("&presentationSeriesUID=").append(psuid[0]);
            sb.append("&presentationUID=").append(psuid[1]);
        }
        if (tsuid != null) {
            sb.append("&transferSyntax=");
            append(tsuid, sb);
        }
        return new URL(sb.toString());
    }

    private static void append(List l, StringBuffer sb) {
        for (Iterator iter = l.iterator(); iter.hasNext(); ) {
            sb.append(iter.next()).append(',');
        }
        sb.setLength(sb.length() - 1);
    }

    private static void append(String[] a, StringBuffer sb) {
        for (int i = 0; i < a.length; i++) {
            sb.append(a[i]).append(',');
        }
        sb.setLength(sb.length() - 1);
    }
}
