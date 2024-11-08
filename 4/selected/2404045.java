package com.lewisshell.helpyourself.psa;

import java.io.*;
import java.util.*;
import org.apache.commons.logging.*;
import com.lewisshell.helpyourself.*;
import com.thoughtworks.xstream.*;
import com.thoughtworks.xstream.io.*;
import com.thoughtworks.xstream.io.xml.*;

public class HitCounterPSA implements HitCounter {

    private static final Log LOG = LogFactory.getLog(HitCounterPSA.class);

    private static boolean loadedJars = false;

    private transient boolean dirty;

    public static class HitInfo implements Info {

        private int downloads;

        private int hits;

        HitInfo(int hits, int downloads) {
            this.hits = hits;
            this.downloads = downloads;
        }

        HitInfo() {
            this(0, 0);
        }

        boolean isEmpty() {
            return this.downloads == 0 && this.hits == 0;
        }

        public int getDownloads() {
            return this.downloads;
        }

        public int getHits() {
            return this.hits;
        }

        private synchronized void hit() {
            this.hits++;
        }

        private synchronized void download() {
            this.downloads++;
        }

        public int compareTo(Info info) {
            if (info == null) {
                return 1;
            }
            if (this.hits > info.getHits()) {
                return 1;
            } else if (this.hits < info.getHits()) {
                return -1;
            }
            if (this.downloads > info.getDownloads()) {
                return 1;
            } else if (this.downloads < info.getDownloads()) {
                return -1;
            }
            return 0;
        }
    }

    private static synchronized XStream getXStream() {
        XStream xStream = new XStream(new DomDriver());
        xStream.alias("HitCounter", HitCounterPSA.class);
        xStream.alias("HitInfo", HitCounterPSA.HitInfo.class);
        return xStream;
    }

    private Map<Integer, HitInfo> hitInfoMap;

    public HitCounterPSA() {
    }

    public boolean isDirty() {
        return this.dirty;
    }

    private static File fileOk(String fileName, boolean read, boolean write) {
        if (fileName == null || fileName.trim().length() == 0) {
            LOG.warn("No hit counter file name");
            return null;
        }
        File hitFile = new File(fileName);
        if (read && !hitFile.exists()) {
            LOG.warn("No hit counter file: " + fileName);
            try {
                new FileWriter(hitFile);
                LOG.warn("Hit counter file created: " + fileName);
            } catch (IOException ex) {
                LOG.error("cannot create hit counter file: " + fileName);
            }
            return null;
        }
        if (read && !hitFile.canRead()) {
            LOG.error("Cannot read hit counter file: " + fileName);
            return null;
        }
        if (write && hitFile.exists() && !hitFile.canWrite()) {
            LOG.error("Cannot write hit counter file: " + fileName);
            return null;
        }
        return hitFile;
    }

    public static HitCounterPSA load(String fileName) {
        File loadFile = fileOk(fileName, true, false);
        if (loadFile == null) {
            return null;
        }
        try {
            return load(new FileReader(fileName));
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("hit counter file gone missing (" + fileName + ")", ex);
        }
    }

    static HitCounterPSA load(Reader reader) {
        if (reader == null) {
            return null;
        }
        if (!loadedJars) {
            getXStream().toXML(new HitCounterPSA());
        }
        try {
            return (HitCounterPSA) getXStream().fromXML(reader);
        } catch (StreamException ex) {
            LOG.error("Cannot load hitcounter data - using new data (old data lost)", ex);
            return new HitCounterPSA();
        }
    }

    public void trimEmptyHits() {
        Map<Integer, HitInfo> map = this.getHitInfoMap();
        synchronized (map) {
            for (Iterator<Map.Entry<Integer, HitInfo>> i = map.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<Integer, HitInfo> entry = i.next();
                if (entry.getValue().isEmpty()) {
                    i.remove();
                }
            }
        }
    }

    public void save(String fileName) {
        if (!this.isDirty()) {
            return;
        }
        File saveFile = fileOk(fileName, false, true);
        if (saveFile == null) {
            return;
        }
        try {
            this.save(new FileWriter(fileName));
        } catch (IOException ex) {
            throw new RuntimeException("Cannot write hit counter to file: " + fileName, ex);
        }
    }

    void save(Writer writer) {
        if (writer == null || !this.isDirty()) {
            return;
        }
        this.trimEmptyHits();
        getXStream().toXML(this, writer);
        dirty = false;
    }

    public void hitImage(Image image) {
        if (image == null) {
            return;
        }
        ((HitInfo) this.hitInfoForImage(image)).hit();
        this.dirty = true;
    }

    public void downloadImage(Image image) {
        if (image == null) {
            return;
        }
        ((HitInfo) this.hitInfoForImage(image)).download();
        this.dirty = true;
    }

    private synchronized Map<Integer, HitInfo> getHitInfoMap() {
        if (this.hitInfoMap == null) {
            this.hitInfoMap = new HashMap<Integer, HitInfo>();
        }
        return this.hitInfoMap;
    }

    public Info hitInfoForImage(Image image) {
        if (image == null) {
            return null;
        }
        Map<Integer, HitInfo> map = this.getHitInfoMap();
        HitInfo hitInfo = null;
        synchronized (map) {
            hitInfo = map.get(image.getId());
            if (hitInfo == null) {
                hitInfo = new HitInfo();
                map.put(image.getId(), hitInfo);
            }
        }
        return hitInfo;
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            throw new IllegalArgumentException("wrong number of args - 3 expected, received " + args.length);
        }
        HitCounterPSA source = load(args[0]);
        if (source == null) {
            throw new IllegalArgumentException(args[0] + " not found");
        }
        HitCounterPSA additional = load(args[1]);
        if (additional == null) {
            throw new IllegalArgumentException(args[1] + " not found");
        }
        source.merge(additional);
        source.save(args[2]);
    }

    /** 
     * merge target hits/downloads into this
     */
    public void merge(HitCounterPSA target) {
        if (target == null || target.hitInfoMap == null) return;
        this.dirty = true;
        for (int id : target.hitInfoMap.keySet()) {
            HitInfo info = this.hitInfoMap.get(id);
            HitInfo targetHitInfo = target.hitInfoMap.get(id);
            if (info == null) {
                info = new HitInfo(targetHitInfo.hits, targetHitInfo.downloads);
                this.hitInfoMap.put(id, info);
            } else {
                info.hits += targetHitInfo.hits;
                info.downloads += targetHitInfo.downloads;
            }
        }
    }
}
