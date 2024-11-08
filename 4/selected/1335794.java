package org.activision.util.rs2cache;

import java.io.ByteArrayInputStream;
import java.io.DataOutput;
import java.io.IOException;
import org.activision.util.CacheConstants;
import org.activision.Constants;

public class FileInformationTable {

    private int entry_count;

    private int[] entry_revision;

    private int[] entry_crc32;

    private int protocol_number;

    private int fit_revision;

    private static boolean need_rebuild = false;

    private boolean is_named;

    private int[] entry_ptr;

    private int entry_real_count;

    private int crc32;

    private int[] entry_real_sub_count;

    private int[] entry_sub_count;

    private int[] entry_name_hash;

    private byte[][] sub_entry_name_hash;

    private int[][] entry_sub_ptr;

    public FileInformationTable(int id, JagexFS fit_store, FileInformationTable main_fit) throws Exception {
        if (CacheConstants.REVISION < 605) {
            CacheContainer c = new CacheContainer(fit_store.get(id));
            crc32 = (int) c.calc_crc32();
            if (!(crc32 == main_fit.get_entry_crc32(id))) {
                need_rebuild = true;
            }
            RSInputStream in_stream = new RSInputStream(new ByteArrayInputStream(c.decompress()));
            protocol_number = in_stream.readByte();
            if (protocol_number >= 6) fit_revision = in_stream.readInt();
            if (fit_revision != main_fit.get_entry_revision(id)) {
                need_rebuild = true;
            }
            is_named = in_stream.readByte() == 1;
            entry_count = in_stream.readShort() & 0xFFFF;
            entry_ptr = new int[entry_count];
            entry_real_count = -1;
            int id_acc = 0;
            for (int write_ptr = 0; write_ptr < entry_count; write_ptr++) {
                entry_ptr[write_ptr] = id_acc += in_stream.readShort();
                if (entry_real_count < entry_ptr[write_ptr]) entry_real_count = entry_ptr[write_ptr];
            }
            if (entry_real_count == -1) {
                entry_real_count = 0;
            }
            entry_crc32 = new int[entry_real_count + 1];
            entry_revision = new int[entry_real_count + 1];
            entry_sub_count = new int[entry_real_count + 1];
            entry_real_sub_count = new int[entry_real_count + 1];
            entry_sub_ptr = new int[entry_real_count + 1][];
            sub_entry_name_hash = new byte[entry_real_count + 1][];
            if (is_named) {
                entry_name_hash = new int[entry_real_count + 1];
                sub_entry_name_hash = new byte[entry_real_count + 1][];
                for (int write_ptr = 0; write_ptr < entry_count; write_ptr++) entry_name_hash[entry_ptr[write_ptr]] = in_stream.readInt();
            }
            for (int write_ptr = 0; write_ptr < entry_count; write_ptr++) entry_crc32[entry_ptr[write_ptr]] = in_stream.readInt();
            for (int write_ptr = 0; write_ptr < entry_count; write_ptr++) entry_revision[entry_ptr[write_ptr]] = in_stream.readInt();
            for (int write_ptr = 0; write_ptr < entry_count; write_ptr++) entry_sub_count[entry_ptr[write_ptr]] = in_stream.readShort();
            for (int entry_ptr = 0; entry_ptr < entry_count; entry_ptr++) {
                id_acc = 0;
                int entry_num = this.entry_ptr[entry_ptr];
                entry_sub_ptr[entry_num] = new int[entry_sub_count[entry_num]];
                for (int write_ptr = 0; write_ptr < entry_sub_count[entry_num]; write_ptr++) {
                    entry_sub_ptr[entry_num][write_ptr] = id_acc += in_stream.readShort();
                    if (entry_real_sub_count[entry_num] < entry_sub_ptr[entry_num][write_ptr]) entry_real_sub_count[entry_num] = entry_sub_ptr[entry_num][write_ptr];
                }
                entry_real_sub_count[entry_num]++;
            }
            if (is_named) for (int entry_ptr = 0; entry_ptr < entry_count; entry_ptr++) {
                int entry_num = this.entry_ptr[entry_ptr];
                sub_entry_name_hash[entry_num] = new byte[entry_real_sub_count[entry_num]];
                for (int write_ptr = 0; write_ptr < entry_sub_count[entry_num]; write_ptr++) sub_entry_name_hash[entry_num][entry_sub_ptr[entry_num][write_ptr]] = (byte) in_stream.readInt();
            }
        } else {
            CacheContainer c = new CacheContainer((byte[]) fit_store.get(id));
            RSInputStream inStream = new RSInputStream(new ByteArrayInputStream(c.decompress()));
            int p_Number = inStream.readUnsignedByte();
            if ((p_Number ^ 0xffffffff) > -6 || (p_Number ^ 0xffffffff) < -7) throw new RuntimeException();
            if (p_Number >= 6) fit_revision = inStream.readInt(); else fit_revision = 0;
            int isNamed = inStream.readUnsignedByte();
            boolean bool = (0x1 & isNamed) != 0;
            boolean bool_6_ = (0x2 & isNamed) != 0;
            entry_count = inStream.readUnsignedShort();
            int i_7_ = 0;
            entry_ptr = new int[entry_count];
            int e_real_count = -1;
            for (int i_9_ = 0; (i_9_ ^ 0xffffffff) > (entry_count ^ 0xffffffff); i_9_++) {
                entry_ptr[i_9_] = i_7_ += inStream.readShort();
                if (e_real_count < entry_ptr[i_9_]) e_real_count = entry_ptr[i_9_];
            }
            entry_real_count = e_real_count + 1;
            entry_revision = new int[entry_real_count];
            entry_sub_count = new int[entry_real_count];
            entry_real_sub_count = new int[entry_real_count];
            entry_crc32 = new int[entry_real_count];
            if (bool_6_) sub_entry_name_hash = new byte[entry_real_count][];
            entry_sub_ptr = new int[entry_real_count][];
            if (bool) {
                entry_name_hash = new int[entry_real_count];
                for (int i_10_ = 0; ((i_10_ ^ 0xffffffff) > (entry_real_count ^ 0xffffffff)); i_10_++) entry_name_hash[i_10_] = -1;
                for (int i_11_ = 0; entry_count > i_11_; i_11_++) entry_name_hash[entry_ptr[i_11_]] = inStream.readInt();
            }
            for (int i_12_ = 0; (entry_count ^ 0xffffffff) < (i_12_ ^ 0xffffffff); i_12_++) entry_crc32[(entry_ptr[i_12_])] = inStream.readInt();
            if (bool_6_) {
                for (int i_13_ = 0; ((entry_count ^ 0xffffffff) < (i_13_ ^ 0xffffffff)); i_13_++) {
                    byte[] is_14_ = new byte[64];
                    inStream.read(is_14_, 0, 64);
                    sub_entry_name_hash[entry_ptr[i_13_]] = is_14_;
                }
            }
            for (int i_15_ = 0; (i_15_ ^ 0xffffffff) > (entry_count ^ 0xffffffff); i_15_++) entry_revision[entry_ptr[i_15_]] = inStream.readInt();
            for (int i_16_ = 0; entry_count > i_16_; i_16_++) entry_sub_count[entry_ptr[i_16_]] = inStream.readShort();
            for (int i_17_ = 0; entry_count > i_17_; i_17_++) {
                int i_18_ = entry_ptr[i_17_];
                int i_19_ = entry_sub_count[i_18_];
                i_7_ = 0;
                entry_sub_ptr[i_18_] = new int[i_19_];
                int i_20_ = -1;
                for (int i_21_ = 0; (i_19_ ^ 0xffffffff) < (i_21_ ^ 0xffffffff); i_21_++) {
                    int i_22_ = entry_sub_ptr[i_18_][i_21_] = i_7_ += inStream.readShort();
                    if ((i_20_ ^ 0xffffffff) > (i_22_ ^ 0xffffffff)) i_20_ = i_22_;
                }
                entry_real_sub_count[i_18_] = i_20_ - -1;
                if ((i_20_ - -1 ^ 0xffffffff) == (i_19_ ^ 0xffffffff)) entry_sub_ptr[i_18_] = null;
            }
            if (bool) {
                sub_entry_name_hash = new byte[1 + e_real_count][];
                for (int i_23_ = 0; (entry_count ^ 0xffffffff) < (i_23_ ^ 0xffffffff); i_23_++) {
                    int i_24_ = entry_ptr[i_23_];
                    int i_25_ = entry_sub_count[i_24_];
                    sub_entry_name_hash[i_24_] = new byte[entry_real_sub_count[i_24_]];
                    for (int i_26_ = 0; entry_real_sub_count[i_24_] > i_26_; i_26_++) sub_entry_name_hash[i_24_][i_26_] = -1;
                    for (int i_27_ = 0; (i_25_ ^ 0xffffffff) < (i_27_ ^ 0xffffffff); i_27_++) {
                        int i_28_;
                        if (entry_sub_ptr[i_24_] == null) i_28_ = i_27_; else i_28_ = (entry_sub_ptr[i_24_][i_27_]);
                        sub_entry_name_hash[i_24_][i_28_] = (byte) inStream.readInt();
                    }
                }
            }
        }
    }

    /**
     * Creates a main FIT table
     * @param data The main FIT file and headers
     * @throws java.io.IOException When invalid data is passed
     */
    public FileInformationTable(byte[] data) throws IOException {
        RSInputStream in_stream = new RSInputStream(new ByteArrayInputStream(data));
        if (in_stream.readByte() != 0) throw new IOException("Main FIT can't be compressed");
        int file_size = in_stream.readInt();
        entry_count = file_size / 8;
        entry_crc32 = new int[entry_count];
        entry_revision = new int[entry_count];
        for (int entry_ptr = 0; entry_ptr < entry_count; entry_ptr++) {
            entry_crc32[entry_ptr] = in_stream.readInt();
            entry_revision[entry_ptr] = in_stream.readInt();
        }
    }

    public FileInformationTable(FileInformationTable[] cache_fits) {
        entry_count = cache_fits.length;
        entry_crc32 = new int[entry_count];
        entry_revision = new int[entry_count];
        for (int fit_id = 0; fit_id < cache_fits.length; fit_id++) {
            entry_revision[fit_id] = cache_fits[fit_id].get_revision();
            entry_crc32[fit_id] = cache_fits[fit_id].get_crc32();
        }
    }

    public FileInformationTable(int i) {
        entry_count = i;
        entry_crc32 = new int[entry_count];
        entry_revision = new int[entry_count];
    }

    public void write(DataOutput out, boolean format) throws IOException {
        if (format) _write_main_fit(out); else _write_fit(out);
    }

    private void _write_fit(DataOutput ignored) {
        throw new UnsupportedOperationException("_write_fit not yet implemented :" + ignored);
    }

    private void _write_main_fit(DataOutput out) throws IOException {
        out.writeByte(0);
        out.writeInt(entry_count * 8);
        for (int entry_ptr = 0; entry_ptr < entry_count; entry_ptr++) {
            out.writeInt(entry_crc32[entry_ptr]);
            out.writeInt(entry_revision[entry_ptr]);
        }
    }

    public int[][] getEntry_sub_ptr() {
        return entry_sub_ptr;
    }

    public void setEntry_sub_ptr(int[][] entry_sub_ptr) {
        this.entry_sub_ptr = entry_sub_ptr;
    }

    private int get_entry_revision(int id) {
        return entry_revision[id];
    }

    public int get_entry_crc32(int id) {
        return entry_crc32[id];
    }

    public int getEntry_count() {
        return entry_count;
    }

    public void setEntry_count(int entry_count) {
        this.entry_count = entry_count;
    }

    public int[] getEntry_revision() {
        return entry_revision;
    }

    public void setEntry_revision(int[] entry_revision) {
        this.entry_revision = entry_revision;
    }

    public int[] getEntry_crc32() {
        return entry_crc32;
    }

    public void setEntry_crc32(int[] entry_crc32) {
        this.entry_crc32 = entry_crc32;
    }

    public int getProtocol_number() {
        return protocol_number;
    }

    public void setProtocol_number(int protocol_number) {
        this.protocol_number = protocol_number;
    }

    public static boolean need_rebuild() {
        return need_rebuild;
    }

    public static void setNeed_rebuild(boolean need_rebuild) {
        FileInformationTable.need_rebuild = need_rebuild;
    }

    public boolean isIs_named() {
        return is_named;
    }

    public void setIs_named(boolean is_named) {
        this.is_named = is_named;
    }

    public int[] getEntry_ptr() {
        return entry_ptr;
    }

    public void setEntry_ptr(int[] entry_ptr) {
        this.entry_ptr = entry_ptr;
    }

    public int getEntry_real_count() {
        return entry_real_count;
    }

    public void setEntry_real_count(int entry_real_count) {
        this.entry_real_count = entry_real_count;
    }

    public int[] getEntry_real_sub_count() {
        return entry_real_sub_count;
    }

    public void setEntry_real_sub_count(int[] entry_real_sub_count) {
        this.entry_real_sub_count = entry_real_sub_count;
    }

    public int[] getEntry_sub_count() {
        return entry_sub_count;
    }

    public void setEntry_sub_count(int[] entry_sub_count) {
        this.entry_sub_count = entry_sub_count;
    }

    public int[] getEntry_name_hash() {
        return entry_name_hash;
    }

    public void setEntry_name_hash(int[] entry_name_hash) {
        this.entry_name_hash = entry_name_hash;
    }

    public byte[][] getSub_entry_name_hash() {
        return sub_entry_name_hash;
    }

    public void setSub_entry_name_hash(byte[][] sub_entry_name_hash) {
        this.sub_entry_name_hash = sub_entry_name_hash;
    }

    public int get_revision() {
        return fit_revision;
    }

    public int get_crc32() {
        return crc32;
    }

    public int findName(String name) {
        int hash = calcJaghash(name);
        for (int i = 0; i < entry_name_hash.length; i++) if (entry_name_hash[i] == hash) return i;
        return -1;
    }

    public int calcJaghash(String n) {
        int count = 0;
        byte[] characters = n.getBytes();
        for (int i = 0; i < n.length(); i++) {
            count = (characters[i] & 0xff) + ((count << -1325077051) + -count);
        }
        return count;
    }
}
