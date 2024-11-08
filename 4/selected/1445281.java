package net.charabia.jsmoothgen.pe;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 *
 * @author  Rodrigo
 */
public class PEOldMSHeader implements Cloneable {

    int e_cblp;

    int e_cp;

    int e_crlc;

    int e_cparhdr;

    int e_minalloc;

    int e_maxalloc;

    int e_ss;

    int e_sp;

    int e_csum;

    int e_ip;

    int e_cs;

    int e_lfarlc;

    int e_ovno;

    int[] e_res = new int[4];

    int e_oemid;

    int e_oeminfo;

    int[] e_res2 = new int[10];

    long e_lfanew;

    private PEFile m_pe;

    /** Creates a new instance of PEOldMSHeader */
    public PEOldMSHeader(PEFile pe) {
        m_pe = pe;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void read() throws IOException {
        FileChannel ch = m_pe.getChannel();
        ByteBuffer mz = ByteBuffer.allocate(64);
        mz.order(ByteOrder.LITTLE_ENDIAN);
        ch.read(mz, 0);
        mz.position(0);
        byte m = mz.get();
        byte z = mz.get();
        if ((m == 77) && (z == 90)) {
        }
        e_cblp = mz.getShort();
        e_cp = mz.getShort();
        e_crlc = mz.getShort();
        e_cparhdr = mz.getShort();
        e_minalloc = mz.getShort();
        e_maxalloc = mz.getShort();
        e_ss = mz.getShort();
        e_sp = mz.getShort();
        e_csum = mz.getShort();
        e_ip = mz.getShort();
        e_cs = mz.getShort();
        e_lfarlc = mz.getShort();
        e_ovno = mz.getShort();
        for (int i = 0; i < 4; i++) e_res[i] = mz.getShort();
        e_oemid = mz.getShort();
        e_oeminfo = mz.getShort();
        for (int i = 0; i < 10; i++) e_res2[i] = mz.getShort();
        e_lfanew = mz.getInt();
    }

    public void dump(PrintStream out) {
        out.println("MSHeader:");
        out.println("e_cblp: " + e_cblp + " // Bytes on last page of file //  2");
        out.println("e_cp: " + e_cp + " // Pages in file //  4");
        out.println("e_crlc: " + e_crlc + " // Relocations //  6");
        out.println("e_cparhdr: " + e_cparhdr + " // Size of header in paragraphs //  8");
        out.println("e_minalloc: " + e_minalloc + " // Minimum extra paragraphs needed //  A");
        out.println("e_maxalloc: " + e_maxalloc + " // Maximum extra paragraphs needed //  C");
        out.println("e_ss: " + e_ss + " // Initial (relative) SS value //  E");
        out.println("e_sp: " + e_sp + " // Initial SP value // 10");
        out.println("e_csum: " + e_csum + " // Checksum // 12");
        out.println("e_ip: " + e_ip + " // Initial IP value // 14");
        out.println("e_cs: " + e_cs + " // Initial (relative) CS value // 16");
        out.println("e_lfarlc: " + e_lfarlc + " // File address of relocation table // 18");
        out.println("e_ovno: " + e_ovno + " // Overlay number // 1A");
        out.println("e_oemid: " + e_oemid + " // OEM identifier (for e_oeminfo) // 24");
        out.println("e_oeminfo: " + e_oeminfo + " // OEM information; e_oemid specific // 26");
        out.println("e_lfanew: " + e_lfanew + " // File address of new exe header // 3C");
    }

    public ByteBuffer get() {
        ByteBuffer mz = ByteBuffer.allocate(64);
        mz.order(ByteOrder.LITTLE_ENDIAN);
        mz.position(0);
        mz.put((byte) 77);
        mz.put((byte) 90);
        mz.putShort((short) e_cblp);
        mz.putShort((short) e_cp);
        mz.putShort((short) e_crlc);
        mz.putShort((short) e_cparhdr);
        mz.putShort((short) e_minalloc);
        mz.putShort((short) e_maxalloc);
        mz.putShort((short) e_ss);
        mz.putShort((short) e_sp);
        mz.putShort((short) e_csum);
        mz.putShort((short) e_ip);
        mz.putShort((short) e_cs);
        mz.putShort((short) e_lfarlc);
        mz.putShort((short) e_ovno);
        for (int i = 0; i < 4; i++) mz.putShort((short) e_res[i]);
        mz.putShort((short) e_oemid);
        mz.putShort((short) e_oeminfo);
        for (int i = 0; i < 10; i++) mz.putShort((short) e_res2[i]);
        mz.putInt((int) e_lfanew);
        mz.position(0);
        return mz;
    }
}
