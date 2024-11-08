package com.aol.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

public class LatestVersionServlet extends HttpServlet {

    private ServletContext ctx = null;

    private DeviceManager devMgr = null;

    private String changeRoot = null;

    private long fileChangeInterval = 60000;

    private HashMap appVersions = null;

    private Logger logger = null;

    private CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();

    public void init() {
        logger = Logger.getLogger(LatestVersionServlet.class);
        ctx = getServletContext();
        devMgr = new DeviceManager();
        changeRoot = ctx.getRealPath("/WEB-INF");
        if (!changeRoot.endsWith("/")) changeRoot += "/";
        changeRoot += "changelists";
        appVersions = new HashMap();
    }

    public void destroy() {
        Iterator i = appVersions.values().iterator();
        while (i.hasNext()) {
            AppVersionRec rec = (AppVersionRec) i.next();
            rec.timer.cancel();
            rec.timer = null;
            rec.verFile = null;
            rec.repoVersions.clear();
            rec.repoVersions = null;
        }
        appVersions.clear();
        appVersions = null;
        decoder = null;
        devMgr.destroy();
        devMgr = null;
        ctx = null;
        logger = null;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Application app = (Application) request.getAttribute("aol.app");
        if (app == null) return;
        ServletOutputStream out = response.getOutputStream();
        StringBuffer msg = new StringBuffer();
        String oldVersion = request.getParameter("Version");
        if (oldVersion == null || oldVersion.length() == 0) msg.append("Version required!");
        String changelist = null;
        String ua = request.getHeader("User-Agent");
        if (ua.indexOf("Platform=BREW") == -1) {
            String curVersion = "1.0";
            logger.info("oldVersion " + oldVersion + " curVersion " + curVersion);
            if (!oldVersion.equals(curVersion)) {
                StringBuffer res = new StringBuffer();
                res.append("$UpdateAvailable=true&$LatestVersion=").append(curVersion).append("&$UpdateMessage=An update to version 1.1 is available.");
                changelist = res.toString();
            } else changelist = "$UpdateAvailable=false";
        } else {
            if (oldVersion.startsWith("0x")) oldVersion = oldVersion.substring(2);
            long oldVer = -1;
            try {
                oldVer = Long.parseLong(oldVersion, 16);
            } catch (NumberFormatException nfe) {
                msg.append("Invalid Version '").append(oldVersion).append("'");
            }
            if (msg.length() > 0) {
                out.print(msg.toString());
                logger.info("Returning: " + msg);
                return;
            }
            String appName = app.getName();
            String pathInfo = request.getPathInfo();
            String repository = null;
            if (pathInfo != null) {
                int slash = pathInfo.indexOf("/", 1);
                if (slash == -1) slash = pathInfo.length();
                repository = pathInfo.substring(1, slash);
            } else repository = appName;
            AppVersionRec appVersion = getAppVersion(app);
            long latestVersion = appVersion.getVersion(repository);
            if (oldVer < latestVersion) {
                HashMap props = devMgr.get(app, ua);
                if (props == null) {
                    msg.append("Invalid User-Agent '").append(ua).append("'");
                    out.print(msg.toString());
                    logger.info("Returning: " + msg);
                    return;
                }
                oldVersion = toVersionString(oldVer);
                changelist = getChangelist(appName, repository, oldVersion, appVersion.getVersionString(repository), props);
            }
        }
        if (changelist != null) out.print(changelist);
        logger.info("Returning: " + changelist);
        return;
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doGet(request, response);
    }

    /**
     * Returns the changelist data for the given parameters.
     * Changelist files are stored in:
     * <app name>/<repository>/<old ver>_<new ver>/<markup>_<image dir>_<image ext>
     */
    private String getChangelist(String appName, String repository, String oldVersion, String newVersion, HashMap props) {
        StringBuffer chFile = new StringBuffer(50);
        chFile.append(appName);
        chFile.append("/").append(repository);
        chFile.append("/").append(oldVersion).append("_").append(newVersion);
        chFile.append("/").append(props.get("app_markup_dir")).append("_").append(props.get("app_image_dir")).append("_").append(props.get("app_image_ext")).append(".chg");
        logger.info("Reading changelist " + chFile.toString());
        String ch = readFile(chFile.toString());
        return ch;
    }

    private String readFile(File file) {
        String contents = null;
        RandomAccessFile rfile = null;
        FileChannel chan = null;
        try {
            rfile = new RandomAccessFile(file, "r");
            chan = rfile.getChannel();
            int size = (int) chan.size();
            ByteBuffer buf = ByteBuffer.allocate(size);
            while (buf.hasRemaining()) {
                int read = chan.read(buf);
                if (read == 0) throw new IOException("Read failure: " + file.getPath());
            }
            buf.flip();
            CharBuffer cb = decoder.decode(buf);
            contents = cb.toString();
            buf = null;
            cb = null;
        } catch (FileNotFoundException fnfe) {
            logger.info(fnfe.getMessage());
        } catch (IOException ioe) {
            StackTraceElement s[] = ioe.getStackTrace();
            StringBuffer st = new StringBuffer(ioe.getMessage()).append("\n");
            for (int i = 0; i < s.length; i++) st.append(s[i]).append("\n");
            logger.error(st.toString());
        } finally {
            try {
                if (chan != null) chan.close();
                if (rfile != null) rfile.close();
            } catch (IOException ioe) {
                StackTraceElement s[] = ioe.getStackTrace();
                StringBuffer st = new StringBuffer(ioe.getMessage()).append("\n");
                for (int i = 0; i < s.length; i++) st.append(s[i]).append("\n");
                logger.error(st.toString());
            }
            rfile = null;
            chan = null;
        }
        return contents;
    }

    private String readFile(String filename) {
        return readFile(new File(changeRoot, filename));
    }

    private AppVersionRec getAppVersion(Application app) {
        String appName = app.getName();
        AppVersionRec rec = (AppVersionRec) appVersions.get(appName);
        if (rec == null) {
            synchronized (appVersions) {
                rec = new AppVersionRec(app.getVersionFile());
                appVersions.put(appName, rec);
            }
        }
        return rec;
    }

    public String toVersionString(long ver) {
        String version = Long.toHexString(ver);
        int len = version.length();
        if (len >= 8) return version;
        StringBuffer sb = new StringBuffer(8);
        len = 8 - len;
        while (len-- > 0) sb.append('0');
        sb.append(version);
        return sb.toString();
    }

    class KeyVersion {

        String key = null;

        String version = null;

        long nversion = -1;

        KeyVersion(String key, String ver) {
            this.key = key;
            if (ver.startsWith("0x")) this.nversion = Long.parseLong(ver.substring(2), 16); else this.nversion = Long.parseLong(ver, 16);
            this.version = toVersionString(nversion);
        }
    }

    class AppVersionRec {

        File verFile = null;

        Timer timer = null;

        HashMap repoVersions = null;

        AppVersionRec(String verFileName) {
            verFile = new File(ctx.getRealPath("/WEB-INF"), verFileName);
            repoVersions = new HashMap();
            readVersions();
            timer = new Timer();
            TimerTask t = new TimerTask() {

                private long timestamp = verFile.lastModified();

                public final void run() {
                    long ts = verFile.lastModified();
                    if (timestamp < ts) {
                        timestamp = ts;
                        readVersions();
                    }
                }
            };
            timer.schedule(t, fileChangeInterval, fileChangeInterval);
        }

        public String getVersionString(String repository) {
            KeyVersion kv = (KeyVersion) repoVersions.get(repository);
            if (kv != null) return kv.version; else return null;
        }

        public long getVersion(String repository) {
            KeyVersion kv = (KeyVersion) repoVersions.get(repository);
            if (kv != null) return kv.nversion; else return -1;
        }

        private synchronized void readVersions() {
            Properties props = new Properties();
            try {
                props.load(new FileInputStream(verFile));
            } catch (IOException ioe) {
                logger.info(ioe.getMessage());
            }
            if (props.size() < 1) {
                logger.log(LogLevel.LATESTVERSION_STATUS, "ERROR! Failed to read versionFile '" + verFile.getPath() + "'!");
                return;
            }
            StringBuffer dbg = new StringBuffer(64);
            Iterator i = props.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry entry = (Map.Entry) i.next();
                KeyVersion kv = new KeyVersion((String) entry.getKey(), (String) entry.getValue());
                repoVersions.put(kv.key, kv);
                dbg.append("\t").append(kv.key).append("=").append(kv.version);
            }
            logger.log(LogLevel.LATESTVERSION_STATUS, "Parsed version-file " + verFile.getPath() + "\n" + dbg.toString());
        }
    }
}
