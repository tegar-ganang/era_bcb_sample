package com.landak.ipod;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * 
 * Shuffle Database
 * ported from gnupod
 * 
 * NOTICE: if you use this code please let me know personally
 * NOTICE: consider buying me an ipod nano
 * 
 * Big Endian (3bytes)
 * 
 * @author khad (khad@users.sourceforge.net)
 *
 */
public class iTunesSD extends BaseDB {

    public static final String ITUNES_SD = ITUNES_DIR + File.separator + "iTunesSD";

    public static final String ITUNES_SHUFFLE = ITUNES_DIR + File.separator + "iTunesShuffle";

    public static final String ITUNES_STATS = ITUNES_DIR + File.separator + "iTunesStats";

    private boolean enableSoundCheck;

    private ArrayList dbsd = new ArrayList();

    private RandomAccessFile dii;

    public iTunesSD(String mpt) {
        super(mpt);
    }

    /**
     * helps preserving play order
     * 
     * @throws FileNotFoundException
     */
    public void load() throws IOException {
        close();
        di = new RandomAccessFile(iPodPath + File.separator + ITUNES_SD, "r");
        try {
            dii = new RandomAccessFile(iPodPath + File.separator + ITUNES_STATS, "r");
            get_tnp_le(dii);
            get_tnp_le(dii);
        } catch (FileNotFoundException e) {
        }
        dbsd.clear();
        int f = 0;
        get_tnp(di);
        int version = get_tnp(di);
        if (version == 0x010700) enableSoundCheck = true; else enableSoundCheck = false;
        try {
            di.seek(18);
            while (true) {
                Object entry[] = new Object[4];
                get_tnp(di);
                get_tnp(di);
                get_tnp(di);
                get_tnp(di);
                get_tnp(di);
                get_tnp(di);
                get_tnp(di);
                get_tnp(di);
                int vol = get_tnp(di);
                int typ = get_tnp(di);
                get_tnp(di);
                String s = get_string_utf(522).trim();
                entry[0] = s.replace('/', ':');
                int sbu = get_tnp(di);
                entry[3] = new Boolean(sbu >> 16 == 1);
                f++;
                if (dii != null) {
                    get_tnp_le(dii);
                    get_tnp_le(dii);
                    get_tnp_le(dii);
                    get_tnp_le(dii);
                    entry[1] = new Integer(get_tnp_le(dii));
                    entry[2] = new Integer(get_tnp_le(dii));
                } else {
                    entry[1] = new Integer(0);
                    entry[2] = new Integer(0);
                }
                dbsd.add(entry);
            }
        } catch (IOException e) {
        }
        log.info("Musics in iTunesSD: " + f);
        if (dii != null) dii.close();
        close();
    }

    public void save(ArrayList db) throws IOException {
        close();
        Util.copyfile(iPodPath + File.separator + ITUNES_SD, iPodPath + File.separator + ITUNES_SD + ".bak");
        di = new RandomAccessFile(iPodPath + File.separator + iTunesSD.ITUNES_SD, "rw");
        di.getChannel().lock();
        di.setLength(0);
        ByteBuffer buf = ByteBuffer.allocate(1024 * 240);
        buf.put(mk_itunes_sd_header(db.size()));
        for (int i = 0, n = db.size(); i < n; i++) {
            FileMeta fm = (FileMeta) db.get(i);
            buf.put(mk_itunes_sd_file(fm));
        }
        byte[] bufb = new byte[buf.position()];
        buf.position(0);
        buf.get(bufb);
        di.write(bufb);
        new File(iPodPath + File.separator + ITUNES_SHUFFLE).delete();
        new File(iPodPath + File.separator + ITUNES_STATS).delete();
        close();
    }

    private String get_string_utf(int len) throws IOException {
        byte[] bytes = new byte[len];
        di.readFully(bytes);
        String s = new String(bytes, "UTF-16LE");
        return s;
    }

    /**
     * @return Returns the dbsd.
     */
    public ArrayList getDbsd() {
        return dbsd;
    }

    /**
     * @param dbsd The dbsd to set.
     */
    public void setDbsd(ArrayList db) {
        this.dbsd = db;
    }

    private byte[] convert_tnp(int intnya) {
        byte[] ret = new byte[3];
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(intnya);
        bb.position(1);
        bb.get(ret, 0, 3);
        return ret;
    }

    private int get_tnp(RandomAccessFile inp) throws IOException {
        byte[] bytes = { 0, 0, 0, 0 };
        inp.readFully(bytes, 1, 3);
        int result = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
        return result;
    }

    private int get_tnp_le(RandomAccessFile inp) throws IOException {
        byte[] bytes = { 0, 0, 0, 0 };
        inp.readFully(bytes, 0, 3);
        int result = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
        return result;
    }

    private byte[] mk_itunes_sd_header(int size) {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        bb.put(convert_tnp(size));
        if (enableSoundCheck) bb.put(convert_tnp(0x010700)); else bb.put(convert_tnp(0x010600));
        bb.put(convert_tnp(0x12));
        bb.put(convert_tnp(0x0));
        bb.put(convert_tnp(0x0));
        bb.put(convert_tnp(0x0));
        byte[] res = new byte[bb.position()];
        bb.position(0);
        bb.get(res);
        return res;
    }

    private byte[] mk_itunes_sd_file(FileMeta fm) throws UnsupportedEncodingException {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        String mypath = fm.path.replace(':', '/');
        bb.put(convert_tnp(0x00022E));
        bb.put(convert_tnp(0x5AA501));
        int sta = (fm.starttime * fm.bitrate) / 8;
        bb.put(convert_tnp(sta >> 8 >> 4));
        bb.put(convert_tnp(0x0));
        bb.put(convert_tnp(sta));
        int sto = (fm.stoptime * fm.bitrate) / 8;
        bb.put(convert_tnp(sto >> 8 >> 4));
        bb.put(convert_tnp(0x0));
        bb.put(convert_tnp(sto));
        if (enableSoundCheck) bb.put(convert_tnp(fm.soundcheck)); else bb.put(convert_tnp(0x64 + fm.volume));
        bb.put(convert_tnp(getFiletypeCode(fm)));
        bb.put(convert_tnp(0x000200));
        bb.put(mypath.getBytes("UTF-16LE"));
        for (int y = 0; y < 522 - mypath.getBytes("UTF-16LE").length; y++) bb.put((byte) 0);
        int sbu = (fm.shuffle ? 1 : 0) << 16;
        sbu = sbu | (0 << 8);
        sbu = sbu | (0);
        bb.put(convert_tnp(sbu));
        byte[] res = new byte[bb.position()];
        bb.position(0);
        bb.get(res);
        return res;
    }

    public static int getFiletypeCode(FileMeta fm) {
        if (fm.filetype.trim().compareTo("MP3") == 0) return 1; else if (fm.filetype.trim().compareTo("M4A") == 0) return 2; else if (fm.filetype.trim().compareTo("M4B") == 0) return 2; else if (fm.filetype.trim().compareTo("WAV") == 0) return 4; else return 1;
    }

    public String getMd5sum() throws IOException {
        return Util.md5sum(iPodPath + File.separator + ITUNES_SD);
    }

    public boolean isEnableSoundCheck() {
        return enableSoundCheck;
    }

    public void setEnableSoundCheck(boolean enableSoundCheck) {
        this.enableSoundCheck = enableSoundCheck;
    }
}
