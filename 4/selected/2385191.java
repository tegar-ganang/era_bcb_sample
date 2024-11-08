package net.sf.mustang.wm.ctxtool;

import java.io.File;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import org.webmacro.Context;
import net.sf.mustang.K;
import net.sf.mustang.Mustang;
import net.sf.mustang.Manager;
import net.sf.mustang.conf.Conf;
import net.sf.mustang.conf.ConfTool;
import net.sf.mustang.file.FileTool;
import net.sf.mustang.log.KLog;
import net.sf.mustang.wm.WMEvaluator;
import net.sf.mustang.wm.WMManager;

public class MessageManager implements Manager {

    private KLog log = Mustang.getLog(MessageManager.class);

    private static final String NOT_FOUND = "nd:";

    private static final String RECOVERY_FILE_PREFIX = "nd_";

    private Hashtable<String, Conf> msgFiles;

    private Object monitor = new Object();

    private static MessageManager instance = new MessageManager();

    private boolean started = false;

    private MessageManager() {
        try {
            start();
        } catch (Exception e) {
            log.error("unable to start " + getName(), e);
            started = false;
        }
        Mustang.registerManager(this);
    }

    public static MessageManager getInstance() {
        return instance;
    }

    public boolean isStarted() {
        return started;
    }

    public synchronized void restart() throws Exception {
        stop();
        start();
    }

    public synchronized void start() throws Exception {
        msgFiles = new Hashtable();
        started = true;
    }

    public Conf getChannel(String channel, String lang) throws Exception {
        Conf retVal;
        String mKey = channel;
        if (lang != null && !K.EMPTY.equals(lang)) mKey = mKey.concat(K.UNDERSCORE).concat(lang);
        retVal = msgFiles.get(mKey);
        if (retVal == null) {
            synchronized (monitor) {
                retVal = msgFiles.get(mKey.toString());
                if (retVal == null) {
                    String confFileName = FileTool.composeDirFile(ConfTool.getGlobalParameter(Mustang.CONF_DIR), mKey);
                    File f = new File(confFileName.concat(K.XML_EXTENSION));
                    if (f.exists()) retVal = ConfTool.addConf(mKey, confFileName.concat(K.XML_EXTENSION)); else retVal = ConfTool.addConf(mKey, confFileName.concat(K.PROPERTIES_EXTENSION));
                    msgFiles.put(mKey, retVal);
                    if (log.isInfo()) log.info("loaded: " + mKey);
                } else {
                    if (log.isInfo()) log.info("a concurrent request loaded: " + mKey);
                }
            }
        } else {
            if (log.isInfo()) log.info("got " + mKey + " from cache");
        }
        return retVal;
    }

    public String get(Context context, String channel, String lang, String key) {
        String msg;
        if (channel == null) channel = Mustang.getConf().get("//message/@default-channel");
        if (lang == null) lang = Mustang.getConf().get("//message/@default-language");
        try {
            msg = getChannel(channel, lang).get(key);
            if (msg.indexOf(K.DOLLAR) >= 0 || msg.indexOf(K.SHARP) >= 0) {
                if (context == null) context = WMManager.getInstance().getContext();
                return WMEvaluator.crunch(context, msg);
            } else return msg;
        } catch (Exception e) {
            try {
                String recoverFileName = ConfTool.getGlobalParameter(Mustang.AUTO_CONF_DIR).concat(RECOVERY_FILE_PREFIX).concat(channel);
                if (lang != null && !K.EMPTY.equals(lang)) recoverFileName = recoverFileName.concat(K.UNDERSCORE).concat(lang);
                recoverFileName = recoverFileName.concat(K.PROPERTIES_EXTENSION);
                File f = new File(recoverFileName);
                if (!f.exists()) f.createNewFile();
                Properties p = new Properties();
                p.load(new FileInputStream(f));
                if (p.get(key) == null) FileTool.saveTextFile(f, key + " =\n", true);
            } catch (Throwable t) {
            }
            return NOT_FOUND + key;
        }
    }

    public synchronized void stop() throws Exception {
        for (Enumeration<String> e = msgFiles.keys(); e.hasMoreElements(); ) ConfTool.removeConf(e.nextElement());
        msgFiles = null;
        started = false;
    }

    public String getName() {
        return "Message Manager";
    }
}
