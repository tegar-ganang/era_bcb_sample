package com.landak.ipod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import javazoom.jl.decoder.JavaLayerException;
import com.landak.ipod.gain.AnalyzeFile;
import com.landak.jshuffle.file.MetaReader;
import entagged.audioformats.exceptions.CannotReadException;

/**
 * 
 * TunesDB Database
 * ported from gnupod
 * 
 * NOTICE: if you use this code please let me know personally
 * NOTICE: consider buying me an ipod nano
 * 
 * uses litle endian 
 * 
 * @author khad (khad@users.sourceforge.net)
 *
 */
public class iTunesDB extends BaseDB {

    static final String ITUNES_DB = ITUNES_DIR + File.separator + "iTunesDB";

    static final int MAXDB = 30;

    private int dbversion;

    private int tracklist_pos;

    private int maxid = 0;

    private boolean dirty = false;

    private ArrayList db = new ArrayList();

    private HashMap typemap;

    private iTunesSD sd;

    private ArrayList files = new ArrayList();

    private boolean enableSoundCheck;

    public iTunesDB(String mpt) {
        super(mpt);
        typemap = new HashMap();
        typemap.put("title", new Integer(1));
        typemap.put("path", new Integer(2));
        typemap.put("album", new Integer(3));
        typemap.put("artist", new Integer(4));
        typemap.put("genre", new Integer(5));
        typemap.put("fdesc", new Integer(6));
        typemap.put("eq", new Integer(7));
        typemap.put("comment", new Integer(8));
        typemap.put("category", new Integer(9));
        typemap.put("composer", new Integer(12));
        typemap.put("group", new Integer(13));
        typemap.put("desc", new Integer(14));
    }

    /**
     * apply play order from itunessd
     * gonna take time
     * 
     * @param po
     */
    private void applyplayorder(ArrayList po) {
        for (int i = 0, n = po.size(); i < n; i++) {
            Object entry[] = (Object[]) po.get(i);
            int idx = Util.findPath(db, (String) entry[0]);
            if (idx == -1) continue;
            FileMeta fm = (FileMeta) db.get(i);
            fm.playcount += ((Integer) entry[1]).intValue();
            fm.skipcount += ((Integer) entry[2]).intValue();
            fm.shuffle = ((Boolean) entry[3]).booleanValue();
            if (idx == i) continue;
            Collections.swap(db, i, idx);
        }
    }

    public void load() throws IOException {
        close();
        dirty = false;
        di = new RandomAccessFile(iPodPath + File.separator + ITUNES_DB, "r");
        findfiles();
        int[][] itinfo = get_starts();
        tracklist_pos = itinfo[1][0];
        int tracklist_childs = itinfo[1][1];
        log.info("Number of musics: " + tracklist_childs);
        db.clear();
        for (int i = 0; i < tracklist_childs; i++) {
            FileMeta fm = get_mhit();
            db.add(fm);
            int fi = Collections.binarySearch(files, fm.path.replace(':', File.separatorChar).toLowerCase());
            if (fi >= 0) {
                files.remove(fi);
            } else {
                log.severe("File vanished: " + getMusicDesc(fm));
                fm.problem |= FileMeta.VANISHED;
            }
        }
        Iterator i = files.iterator();
        while (i.hasNext()) {
            String fn = (String) i.next();
            String ffn = iPodPath + fn;
            try {
                FileMeta fm = MetaReader.readmp3(ffn);
                File fo = new File(ffn);
                String name = fo.getName();
                if (fm.title.trim().compareTo("") == 0) fm.title = name;
                fm.filetype = name.substring(name.lastIndexOf('.') + 1).toUpperCase();
                fm.path = fn.replace(File.separatorChar, ':');
                appendMusic(fm);
                log.warning("Adding orphan file (use --save): " + getMusicDesc(fm));
            } catch (CannotReadException e) {
                log.warning("Orphan file, you may delete: " + fn);
            }
        }
        sd = new iTunesSD(iPodPath);
        sd.load();
        enableSoundCheck = sd.isEnableSoundCheck();
        applyplayorder(sd.getDbsd());
        close();
    }

    public void save() throws IOException {
        if (!dirty) return;
        close();
        if (enableSoundCheck) analyzegain();
        Util.copyfile(iPodPath + File.separator + ITUNES_DB, iPodPath + File.separator + ITUNES_DB + ".bak");
        di = new RandomAccessFile(iPodPath + File.separator + ITUNES_DB, "rw");
        di.getChannel().lock();
        di.setLength(0);
        byte[] mhlt;
        byte[] mhsd;
        ByteBuffer mhit = ByteBuffer.allocate(1024 * 1000);
        for (int i = 0, n = db.size(); i < n; i++) {
            FileMeta fm = (FileMeta) db.get(i);
            mhit.put(build_mkhit(fm));
        }
        byte[] mhitb = new byte[mhit.position()];
        mhit.position(0);
        mhit.get(mhitb);
        mhlt = mk_mhlt();
        mhsd = mk_mhsd(mhlt.length + mhitb.length);
        di.write(mk_mhbd(mhlt.length, mhitb.length, mhsd.length));
        di.write(mhsd);
        di.write(mhlt);
        di.write(mhitb);
        close();
        sd.setEnableSoundCheck(enableSoundCheck);
        sd.save(db);
        dirty = false;
    }

    private void analyzegain() {
        for (int i = 0, n = db.size(); i < n; i++) {
            FileMeta fm = (FileMeta) db.get(i);
            if (fm.soundcheck == 0 || Math.abs(fm.soundcheck) > MAXDB) {
                if (iTunesSD.getFiletypeCode(fm) != 1) {
                    log.warning("Not analyzing gain, file not supported: " + getMusicDesc(fm));
                    fm.soundcheck = 0;
                    continue;
                }
                log.info("Analyzing gain: " + getMusicDesc(fm));
                AnalyzeFile an;
                try {
                    an = new AnalyzeFile(new FileInputStream(getIPodPath() + fm.path.replace(':', File.separatorChar)));
                    fm.soundcheck = (int) Math.round(an.analyze());
                    if (Math.abs(fm.soundcheck) > MAXDB) {
                        log.severe("Invalid dB: " + fm.soundcheck);
                        fm.soundcheck = 0;
                    }
                    log.info("dB adjustment: " + fm.soundcheck);
                    an = null;
                } catch (FileNotFoundException e) {
                } catch (JavaLayerException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    byte[] mk_mhbd(int mhlt_length, int mhitb_length, int mhsd_length) {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.put((byte) 'm');
        bb.put((byte) 'h');
        bb.put((byte) 'b');
        bb.put((byte) 'd');
        bb.put(convert_int(104));
        bb.put(convert_int(mhlt_length + mhitb_length + mhsd_length));
        bb.put(convert_int(0x1));
        bb.put(convert_int(dbversion));
        bb.put(convert_int(0x1));
        bb.put(convert_int(0xE0ADECAD));
        bb.put(convert_int(0x0DF0ADFB));
        bb.put(convert_int(0x2));
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        byte[] res = new byte[bb.position()];
        bb.position(0);
        bb.get(res);
        return res;
    }

    byte[] mk_mhsd(int size) {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.put((byte) 'm');
        bb.put((byte) 'h');
        bb.put((byte) 's');
        bb.put((byte) 'd');
        bb.put(convert_int(96));
        bb.put(convert_int(size + 96));
        bb.put(convert_int(1));
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        byte[] res = new byte[bb.position()];
        bb.position(0);
        bb.get(res);
        return res;
    }

    byte[] mk_mhlt() {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.put((byte) 'm');
        bb.put((byte) 'h');
        bb.put((byte) 'l');
        bb.put((byte) 't');
        bb.put(convert_int(92));
        bb.put(convert_int(db.size()));
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        byte[] res = new byte[bb.position()];
        bb.position(0);
        bb.get(res);
        return res;
    }

    byte[] build_mkhit(FileMeta fm) throws UnsupportedEncodingException {
        ByteBuffer bb = ByteBuffer.allocate(1024 * 2);
        ByteBuffer cmhod = ByteBuffer.allocate(1024);
        int mhod_cnt = 0;
        Field fs[] = FileMeta.class.getFields();
        for (int i = 0; i < fs.length; i++) {
            Object fv = null;
            try {
                fv = fs[i].get(fm);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                continue;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                continue;
            }
            Class ft = fs[i].getType();
            String fn = fs[i].getName();
            if (ft == String.class) {
                if (fv == null) continue;
                String s = (String) fv;
                s = s.trim();
                if (s.compareTo("") == 0) continue;
                try {
                    cmhod.put(mk_mhod(fn, s));
                    mhod_cnt++;
                } catch (Exception e) {
                    continue;
                }
            } else continue;
        }
        bb.put(mk_mhit(cmhod.position(), mhod_cnt, fm));
        byte cmhodb[] = new byte[cmhod.position()];
        cmhod.position(0);
        cmhod.get(cmhodb);
        bb.put(cmhodb);
        byte[] res = new byte[bb.position()];
        bb.position(0);
        bb.get(res);
        return res;
    }

    byte[] mk_mhod(String type, String str) throws Exception {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        ByteBuffer apx = ByteBuffer.allocate(1024);
        Integer typ = (Integer) typemap.get(type);
        if (typ == null) {
            throw new Exception("Unknown mhod type: " + type);
        }
        int fqid = 1;
        apx.put(convert_int(fqid));
        apx.put(convert_int(str.getBytes("UTF-16LE").length));
        apx.putInt(0);
        apx.putInt(0);
        apx.put(str.getBytes("UTF-16LE"));
        byte apxb[] = new byte[apx.position()];
        apx.position(0);
        apx.get(apxb);
        bb.put((byte) 'm');
        bb.put((byte) 'h');
        bb.put((byte) 'o');
        bb.put((byte) 'd');
        bb.put(convert_int(24));
        bb.put(convert_int(24 + apxb.length));
        bb.put(convert_int(typ.intValue()));
        bb.putInt(0);
        bb.putInt(0);
        bb.put(apxb);
        byte[] res = new byte[bb.position()];
        bb.position(0);
        bb.get(res);
        return res;
    }

    byte[] mk_mhit(int i, int mhod_cnt, FileMeta fm) throws UnsupportedEncodingException {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        int myvol = fm.volume;
        bb.put((byte) 'm');
        bb.put((byte) 'h');
        bb.put((byte) 'i');
        bb.put((byte) 't');
        bb.put(convert_int(0xF4));
        bb.put(convert_int(0xF4 + i));
        bb.put(convert_int(mhod_cnt));
        bb.put(convert_int(fm.id));
        bb.put(convert_int(0x1));
        String ft = (fm.filetype + "        ").substring(0, 4);
        bb.put(new StringBuffer(ft).reverse().toString().getBytes("ASCII"));
        bb.put(convert_short(fm.enctype));
        if (fm.compilation) bb.put((byte) 0); else bb.put((byte) 1);
        bb.put((byte) fm.rating);
        bb.put(convert_int(Util.dateToMacDate(fm.modifytime)));
        bb.put(convert_int(fm.filesize));
        bb.put(convert_int(fm.length));
        bb.put(convert_int(fm.track));
        bb.put(convert_int(fm.totaltrack));
        bb.put(convert_int(fm.year));
        bb.put(convert_int(fm.bitrate));
        bb.put(convert_short((short) 0));
        bb.put(convert_short(fm.samplerate));
        bb.put(convert_int(myvol));
        bb.put(convert_int(fm.starttime));
        bb.put(convert_int(fm.stoptime));
        bb.put(convert_int(fm.soundcheck));
        bb.put(convert_int(fm.playcount));
        bb.put(convert_int(fm.skipcount));
        bb.put(convert_int(fm.lastplayed));
        bb.put(convert_int(fm.cdnumber));
        bb.put(convert_int(fm.totalcd));
        bb.putInt(0);
        bb.put(convert_int(Util.dateToMacDate(fm.addtime)));
        bb.put(convert_int(fm.bookmark));
        bb.put(convert_int(0xDEADBABE));
        bb.put(convert_int(fm.id));
        bb.put(convert_short((short) 0));
        bb.put(convert_short(fm.bpm));
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.put(convert_int(0xDEADBABE));
        bb.put(convert_int(fm.id));
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);
        byte[] res = new byte[bb.position()];
        bb.position(0);
        bb.get(res);
        return res;
    }

    private FileMeta get_mhit() throws IOException {
        int pos = tracklist_pos;
        if ("mhit".compareTo(get_string(pos, 4)) != 0) throw new IOException("Bad magic");
        FileMeta fm = new FileMeta();
        fm.id = get_int(pos + 16);
        maxid = Math.max(fm.id, maxid);
        fm.filetype = new StringBuffer(get_string(pos + 24, 4)).reverse().toString();
        fm.enctype = get_short(pos + 28);
        fm.compilation = di.readByte() == 0;
        fm.rating = di.readByte();
        fm.modifytime = Util.macDateToDate(get_int(pos + 32));
        fm.filesize = get_int(pos + 36);
        fm.length = get_int(pos + 40);
        fm.track = get_int(pos + 44);
        fm.totaltrack = get_int(pos + 48);
        fm.year = get_int(pos + 52);
        fm.bitrate = get_int(pos + 56);
        fm.unknown1 = get_short(pos + 60);
        fm.samplerate = get_short(pos + 62);
        fm.volume = get_int(pos + 64);
        fm.starttime = get_int(pos + 68);
        fm.stoptime = get_int(pos + 72);
        fm.soundcheck = get_int(pos + 76);
        fm.playcount = get_int(pos + 80);
        fm.skipcount = get_int(pos + 84);
        fm.lastplayed = get_int(pos + 88);
        fm.cdnumber = get_int(pos + 92);
        fm.totalcd = get_int(pos + 96);
        fm.addtime = Util.macDateToDate(get_int(pos + 104));
        fm.bookmark = get_int(pos + 108);
        fm.bpm = get_short(pos + 122);
        if (Math.abs(fm.volume) > 200) {
            log.severe("Invalid volume: " + fm.volume);
            fm.volume = 0;
        }
        int mhods = get_int(pos + 12);
        tracklist_pos += get_int(pos + 4);
        for (int i = 0; i < mhods; i++) {
            Object[] ret = get_mhod();
            String s = (String) ret[0];
            int t = ((Integer) ret[1]).intValue();
            switch(t) {
                case 1:
                    fm.title = s;
                    break;
                case 2:
                    fm.path = s;
                    break;
                case 3:
                    fm.album = s;
                    break;
                case 4:
                    fm.artist = s;
                    break;
                case 5:
                    fm.genre = s;
                    break;
                case 6:
                    fm.fdesc = s;
                    break;
                case 7:
                    fm.eq = s;
                    break;
                case 8:
                    fm.comment = s;
                    break;
                case 9:
                    fm.category = s;
                    break;
                case 12:
                    fm.composer = s;
                    break;
                case 13:
                    fm.group = s;
                    break;
                case 14:
                    fm.desc = s;
                    break;
                default:
                    log.warning("Unknown mhod field " + t + ": " + s);
            }
        }
        return fm;
    }

    private Object[] get_mhod() throws IOException {
        int pos = tracklist_pos;
        String id = get_string(pos, 4);
        int mhl = get_int(pos + 4);
        int ml = get_int(pos + 8);
        int mty = get_int(pos + 12);
        int xl = get_int(pos + 28);
        if (id.compareTo("mhod") != 0) {
            throw new IOException("Bad magic");
        }
        String foo = get_string_utf(pos + (ml - xl), xl);
        tracklist_pos += ml;
        Object[] tmp = new Object[2];
        tmp[0] = foo.trim();
        tmp[1] = new Integer(mty);
        return tmp;
    }

    /**
     * Search all mhbd's and return information about them
     * @return 
     * @throws IOException 
     */
    private int[][] get_starts() throws IOException {
        if ("mhbd".compareTo(get_string(0, 4)) != 0) throw new IOException("Bad magic");
        int mhbd_s = get_int(4);
        int total_len = get_int(8);
        dbversion = get_int(16);
        int childs = get_int(20);
        int cpos = mhbd_s;
        int[][] ch = new int[10][3];
        for (int i = 0; i < childs; i++) {
            int c_mhsd_hlen = get_int(cpos + 4);
            int c_mhsd_size = get_int(cpos + 8);
            int mhsd_type = get_int(cpos + 12);
            int sub_hlen = get_int(cpos + c_mhsd_hlen + 4);
            int sub_cnt = get_int(cpos + c_mhsd_hlen + 8);
            int xstart = cpos + sub_hlen + c_mhsd_hlen;
            ch[mhsd_type][0] = xstart;
            ch[mhsd_type][1] = sub_cnt;
            ch[mhsd_type][2] = mhsd_type;
            cpos += (c_mhsd_size);
        }
        return ch;
    }

    private String get_string_utf(long offset, int len) throws IOException {
        byte[] bytes = new byte[len];
        di.seek(offset);
        di.readFully(bytes);
        String s = new String(bytes, "UTF-16LE");
        return s;
    }

    private String get_string(long offset, int len) throws IOException {
        byte[] bytes = new byte[len];
        di.seek(offset);
        di.readFully(bytes);
        String s = new String(bytes, "ASCII");
        return s;
    }

    private int get_int(long offset) throws IOException {
        byte[] bytes = new byte[4];
        di.seek(offset);
        di.readFully(bytes);
        int result = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
        return result;
    }

    private short get_short(long offset) throws IOException {
        byte[] bytes = new byte[2];
        di.seek(offset);
        di.readFully(bytes);
        short result = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
        return result;
    }

    private byte[] convert_int(int intnya) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(intnya).array();
    }

    private byte[] convert_short(short intnya) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(intnya).array();
    }

    public void sort(int sorttype) {
        for (int i = 0, n = db.size(); i < n; i++) {
            FileMeta fm = (FileMeta) db.get(i);
            fm.sorttype = sorttype;
        }
        Collections.sort(db);
    }

    /**
     * @return Returns the db.
     */
    public ArrayList getDb() {
        return db;
    }

    public void appendMusic(FileMeta fm) {
        dirty = true;
        fm.id = getNextid();
        db.add(fm);
    }

    public void removeMusic(int ord) {
        dirty = true;
        db.remove(ord);
    }

    public void clear() {
        dirty = false;
        db.clear();
    }

    /**
     * @return Returns the dirty.
     */
    public boolean isDirty() {
        return dirty;
    }

    private int getNextid() {
        maxid++;
        return maxid;
    }

    public String getMd5sum() throws IOException {
        return Util.md5sum(iPodPath + File.separator + ITUNES_DB);
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    private void findfiles() {
        files.clear();
        File root = new File(iPodPath + File.separator + MUSIC_DIR);
        File[] subd = root.listFiles();
        if (subd == null) return;
        for (int i = 0; i < subd.length; i++) {
            String[] str = subd[i].list();
            if (str == null) continue;
            for (int j = 0; j < str.length; j++) {
                String s = File.separator + MUSIC_DIR + File.separator + subd[i].getName() + File.separator + str[j];
                files.add(s.toLowerCase());
            }
        }
        Collections.sort(files);
    }

    public boolean isEnableSoundCheck() {
        return enableSoundCheck;
    }

    public void setEnableSoundCheck(boolean enableSoundCheck) {
        this.enableSoundCheck = enableSoundCheck;
    }
}
