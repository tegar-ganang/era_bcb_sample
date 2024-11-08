package it.newinstance.watchdog.security.context;

import it.newinstance.util.Util;
import it.newinstance.watchdog.engine.BaseContext;
import it.newinstance.watchdog.engine.InitializationException;
import it.newinstance.watchdog.engine.Server;
import it.newinstance.watchdog.engine.WatchDogException;
import it.newinstance.watchdog.security.util.Config;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * @author Luigi R. Viggiano
 * @version $Revision: 49 $
 * @since 27-nov-2005
 */
public class DefaultBlackListContext extends BaseContext implements BlackListerContext {

    private static final int DEFAULT_THRESHOLD = 3;

    private static Logger log = Logger.getLogger(DefaultBlackListContext.class);

    private Map<String, IpEntry> ipMap = new HashMap<String, IpEntry>();

    private int threshold;

    private File denyFile;

    private File saveFile;

    public DefaultBlackListContext() {
    }

    @SuppressWarnings("unchecked")
    public void init(Server server) throws InitializationException {
        synchronized (this) {
            super.init(server);
            try {
                Object loaded = Config.load(getSaveFile());
                if (loaded != null) ipMap = (Map<String, IpEntry>) loaded;
                setThreshold(DEFAULT_THRESHOLD);
            } catch (Exception e) {
                log.error("error during init", e);
                throw new InitializationException(e);
            }
        }
    }

    public void setThreshold(int threshold) {
        synchronized (this) {
            this.threshold = threshold;
            for (IpEntry entry : ipMap.values()) entry.setThreshold(threshold);
        }
    }

    public void setDenyFile(String denyFile) {
        synchronized (this) {
            this.denyFile = new File(denyFile);
        }
    }

    private IpEntry getEntry(String ip) {
        IpEntry entry = ipMap.get(ip);
        if (entry == null) {
            entry = new IpEntry(ip, threshold);
            ipMap.put(ip, entry);
        }
        return entry;
    }

    public boolean complain(String ip) {
        synchronized (this) {
            IpEntry entry = getEntry(ip);
            boolean changed = entry.complain();
            if (changed) update(ipMap);
            return entry.isBanned();
        }
    }

    public void ban(String ip) {
        synchronized (this) {
            IpEntry entry = getEntry(ip);
            boolean changed = entry.ban();
            if (changed) update(ipMap);
        }
    }

    private void update(Map<String, IpEntry> ipMap) {
        if (denyFile == null) {
            log.error("DenyFile not set");
            return;
        }
        log.info("Saving " + denyFile);
        try {
            BufferedReader template = new BufferedReader(new InputStreamReader(Util.getStream("/hosts.deny")));
            BufferedWriter output = new BufferedWriter(new FileWriter(denyFile));
            String line = null;
            while ((line = template.readLine()) != null) output.write(line + "\n");
            output.write("ALL:");
            for (String ip : ipMap.keySet()) output.write("\\\n    " + ip);
            output.write("\n\n");
            template.close();
            output.close();
            log.debug("Saved succesfully");
        } catch (IOException e) {
            log.error("Error during update", e);
        }
    }

    public void destroy() throws WatchDogException {
        synchronized (this) {
            try {
                Config.save(getSaveFile(), ipMap);
            } catch (IOException e) {
                throw new WatchDogException(e);
            }
            super.destroy();
        }
    }

    public File getSaveFile() {
        String appName = "watchdog";
        String fileName = "ipMap.ser.gz";
        if (saveFile == null) saveFile = Config.file(appName, fileName);
        return saveFile;
    }
}
