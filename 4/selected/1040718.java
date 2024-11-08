package apple2;

public class ProDOS {

    private static String makeString(int addr) {
        int hndlStr = vm02.call(addr, 0x46);
        return (String) vm02.bitsAsRef((hndlStr & 0xFFFF) | 0x00830000 | ((hndlStr & 0x00FF0000) << 8));
    }

    public static int create(String pathname, int access, int file_type, int aux_type, int storage_type) {
        int create_time = getTime();
        byte params[] = new byte[12];
        int hmem = vm02.refAsBits((Object) pathname) & 0xFFFF;
        int stringptr = vm02.call(hmem, 0x0E);
        params[0] = 7;
        params[1] = (byte) stringptr;
        params[2] = (byte) (stringptr >> 8);
        params[3] = (byte) access;
        params[4] = (byte) file_type;
        params[5] = (byte) aux_type;
        params[6] = (byte) (aux_type >> 8);
        params[7] = (byte) storage_type;
        params[8] = (byte) create_time;
        params[9] = (byte) (create_time >> 8);
        params[10] = (byte) (create_time >> 16);
        params[11] = (byte) (create_time >> 24);
        int result = vm02.call(0xC00000 | (vm02.refAsBits((Object) params) & 0xFFFF), 0x54) & 0xFF;
        vm02.call(hmem, 0x10);
        return -result;
    }

    public static int destroy(String pathname) {
        byte params[] = new byte[3];
        int hmem = vm02.refAsBits((Object) pathname) & 0xFFFF;
        int stringptr = vm02.call(hmem, 0x0E);
        params[0] = 1;
        params[1] = (byte) stringptr;
        params[2] = (byte) (stringptr >> 8);
        int result = vm02.call(0xC10000 | (vm02.refAsBits((Object) params) & 0xFFFF), 0x54) & 0xFF;
        vm02.call(vm02.refAsBits((Object) pathname) & 0xFFFF, 0x10);
        return -result;
    }

    public static int rename(String pathname, String new_pathname) {
        byte params[] = new byte[5];
        int hmemOld = vm02.refAsBits((Object) pathname) & 0xFFFF;
        int hmemNew = vm02.refAsBits((Object) new_pathname) & 0xFFFF;
        int string1ptr = vm02.call(hmemOld, 0x0E);
        int string2ptr = vm02.call(hmemNew, 0x0E);
        params[0] = 2;
        params[1] = (byte) string1ptr;
        params[2] = (byte) (string1ptr >> 8);
        params[3] = (byte) string2ptr;
        params[4] = (byte) (string2ptr >> 8);
        int result = vm02.call(0xC20000 | (vm02.refAsBits((Object) params) & 0xFFFF), 0x54) & 0xFF;
        vm02.call(hmemOld, 0x10);
        vm02.call(hmemNew, 0x10);
        return -result;
    }

    public static int setFileInfo(String pathname, byte[] params) {
        if (params.length < 14) return -256;
        int hmem = vm02.refAsBits((Object) pathname) & 0xFFFF;
        int stringptr = vm02.call(hmem, 0x0E);
        params[0] = 7;
        params[1] = (byte) stringptr;
        params[2] = (byte) (stringptr >> 8);
        int result = vm02.call(0xC30000 | (vm02.refAsBits((Object) params) & 0xFFFF), 0x54) & 0xFF;
        vm02.call(hmem, 0x10);
        return -result;
    }

    public static byte[] getFileInfo(String pathname) {
        byte params[] = new byte[18];
        int hmem = vm02.refAsBits((Object) pathname) & 0xFFFF;
        int stringptr = vm02.call(hmem, 0x0E);
        params[0] = 10;
        params[1] = (byte) stringptr;
        params[2] = (byte) (stringptr >> 8);
        params[0] = (byte) vm02.call(0xC40000 | (vm02.refAsBits((Object) params) & 0xFFFF), 0x54);
        vm02.call(hmem, 0x10);
        return params;
    }

    public static String[] online() {
        String nameVols[] = new String[14];
        byte params[] = new byte[4];
        byte buffer[] = new byte[256];
        int hmem = vm02.refAsBits((Object) buffer) & 0xFFFF;
        int bufferptr = (vm02.call(hmem, 0x0E) & 0xFFFF) + 2;
        params[0] = 2;
        params[1] = 0;
        params[2] = (byte) bufferptr;
        params[3] = (byte) (bufferptr >> 8);
        vm02.call(0xC50000 | (vm02.refAsBits((Object) params) & 0xFFFF), 0x54);
        int numVols = 0;
        for (int i = 0; i < 256; i += 16) {
            buffer[i] &= 0x0F;
            if (buffer[i] != 0) nameVols[numVols++] = makeString(bufferptr + i);
        }
        vm02.call(hmem, 0x10);
        String onlineVols[] = new String[numVols];
        while (--numVols >= 0) onlineVols[numVols] = nameVols[numVols];
        return onlineVols;
    }

    public static int setPrefix(String pathname) {
        byte params[] = new byte[3];
        int hmem = vm02.refAsBits((Object) pathname) & 0xFFFF;
        int stringptr = vm02.call(hmem, 0x0E);
        params[0] = 1;
        params[1] = (byte) stringptr;
        params[2] = (byte) (stringptr >> 8);
        int result = vm02.call(0xC60000 | (vm02.refAsBits((Object) params) & 0xFFFF), 0x54) & 0xFF;
        vm02.call(hmem, 0x10);
        return -result;
    }

    public static String getPrefix() {
        String pathname = null;
        byte params[] = new byte[3];
        byte buffer[] = new byte[65];
        int hmem = vm02.refAsBits((Object) buffer) & 0xFFFF;
        int bufferptr = (vm02.call(hmem, 0x0E) & 0xFFFF) + 2;
        params[0] = 1;
        params[1] = (byte) bufferptr;
        params[2] = (byte) (bufferptr >> 8);
        int result = vm02.call(0xC70000 | (vm02.refAsBits((Object) params) & 0xFFFF), 0x54) & 0xFF;
        if (result == 0) pathname = makeString(bufferptr);
        vm02.call(hmem, 0x10);
        return pathname;
    }

    public static int allocIOBuffer() {
        int page, result;
        do {
            for (page = 0x40; page < 0xBB; page++) {
                result = vm02.call(0x010004 | (page << 8), 0x0A);
                if ((result & 0x01000000) == 0) return result & 0xFFFF;
            }
            result = vm02.call(0x00FF0100, 0x64);
        } while ((result & 0x01000000) == 0);
        return 0;
    }

    public static void freeIOBuffer(int io_buffer) {
        if (io_buffer != 0) vm02.call(io_buffer, 0x0C);
    }

    public static int open(String pathname, int io_buffer) {
        byte params[] = new byte[6];
        int hmem = vm02.refAsBits((Object) pathname) & 0xFFFF;
        int stringptr = vm02.call(hmem, 0x0E);
        int bufferptr = ((vm02.call(io_buffer, 0x06) & 0xFFFF) | 0x0F) + 1;
        params[0] = 3;
        params[1] = (byte) stringptr;
        params[2] = (byte) (stringptr >> 8);
        params[3] = (byte) bufferptr;
        params[4] = (byte) (bufferptr >> 8);
        int result = vm02.call(0xC80000 | (vm02.refAsBits((Object) params) & 0xFFFF), 0x54) & 0xFF;
        vm02.call(hmem, 0x10);
        if (result == 0) for (int i = 5; i >= 0; i--) if (vm02.peekByte(0x036A + i) == 0) {
            vm02.pokeByte(0x036A + i, params[5]);
            break;
        }
        return (result != 0) ? -result : ((int) params[5] & 0xFF);
    }

    public static int read(int ref_num, byte data_buffer[]) {
        return readwrite(0xCA0000, ref_num, data_buffer, 0, data_buffer.length);
    }

    public static int read(int ref_num, byte data_buffer[], int offset, int len) {
        return readwrite(0xCA0000, ref_num, data_buffer, offset, len);
    }

    public static int write(int ref_num, byte data_buffer[]) {
        return readwrite(0xCB0000, ref_num, data_buffer, 0, data_buffer.length);
    }

    public static int write(int ref_num, byte data_buffer[], int offset, int len) {
        return readwrite(0xCB0000, ref_num, data_buffer, offset, len);
    }

    public static int readwrite(int cmd, int ref_num, byte data_buffer[], int offset, int len) {
        byte params[] = new byte[8];
        if ((offset + len) > data_buffer.length) return -256;
        int hmem = vm02.refAsBits((Object) data_buffer) & 0xFFFF;
        int bufferptr = (vm02.call(hmem, 0x0E) & 0xFFFF) + 2 + offset;
        params[0] = 4;
        params[1] = (byte) ref_num;
        params[2] = (byte) bufferptr;
        params[3] = (byte) (bufferptr >> 8);
        params[4] = (byte) len;
        params[5] = (byte) (len >> 8);
        int result = vm02.call(cmd | (vm02.refAsBits((Object) params) & 0xFFFF), 0x54) & 0xFF;
        vm02.call(hmem, 0x10);
        return (result != 0) ? -result : (((int) params[6] & 0x00FF) | (((int) params[7] << 8) & 0xFF00));
    }

    public static int close(int ref_num) {
        byte params[] = new byte[2];
        params[0] = 1;
        params[1] = (byte) ref_num;
        int result = vm02.call(0xCC0000 | (vm02.refAsBits((Object) params) & 0xFFFF), 0x54) & 0xFF;
        for (int i = 5; i >= 0; i--) if (vm02.peekByte(0x036A + i) == ref_num) {
            vm02.pokeByte(0x036A + i, (byte) 0);
            break;
        }
        return -result;
    }

    public static int flush(int ref_num) {
        byte params[] = new byte[2];
        params[0] = 1;
        params[1] = (byte) ref_num;
        int result = vm02.call(0xCD0000 | (vm02.refAsBits((Object) params) & 0xFFFF), 0x54) & 0xFF;
        return -result;
    }

    public static int setMark(int ref_num, int position) {
        return setPos(0xCE0000, ref_num, position);
    }

    public static int setEOF(int ref_num, int eof) {
        return setPos(0xD00000, ref_num, eof);
    }

    public static int setPos(int cmd, int ref_num, int pos) {
        byte params[] = new byte[5];
        params[0] = 2;
        params[1] = (byte) ref_num;
        params[2] = (byte) pos;
        params[3] = (byte) (pos >> 8);
        params[4] = (byte) (pos >> 16);
        int result = vm02.call(cmd | (vm02.refAsBits((Object) params) & 0xFFFF), 0x54) & 0xFF;
        return -result;
    }

    public static int getMark(int ref_num) {
        return getPos(0xCF0000, ref_num);
    }

    public static int getEOF(int ref_num) {
        return getPos(0xD10000, ref_num);
    }

    public static int getPos(int cmd, int ref_num) {
        byte params[] = new byte[5];
        params[0] = 2;
        params[1] = (byte) ref_num;
        int result = vm02.call(cmd | (vm02.refAsBits((Object) params) & 0xFFFF), 0x54) & 0xFF;
        int pos = (int) params[2] & 0xFF;
        pos |= ((int) params[3] & 0xFF) << 8;
        pos |= ((int) params[4] & 0xFF) << 16;
        return (result != 0) ? -result : pos;
    }

    public static int readBlock(int unit_num, int block_num, byte data_buffer[]) {
        return readwriteBlock(0x800000, unit_num, block_num, data_buffer, 0);
    }

    public static int readBlock(int unit_num, int block_num, byte data_buffer[], int offset) {
        return readwriteBlock(0x800000, unit_num, block_num, data_buffer, offset);
    }

    public static int writeBlock(int unit_num, int block_num, byte data_buffer[]) {
        return readwriteBlock(0x810000, unit_num, block_num, data_buffer, 0);
    }

    public static int writeBlock(int unit_num, int block_num, byte data_buffer[], int offset) {
        return readwriteBlock(0x810000, unit_num, block_num, data_buffer, offset);
    }

    public static int readwriteBlock(int cmd, int unit_num, int block_num, byte data_buffer[], int offset) {
        byte params[] = new byte[6];
        if ((offset + 512) < data_buffer.length) return -256;
        int hmem = vm02.refAsBits((Object) data_buffer) & 0xFFFF;
        int bufferptr = vm02.call(hmem, 0x0E) + 2 + offset;
        params[0] = 3;
        params[1] = (byte) unit_num;
        params[2] = (byte) bufferptr;
        params[3] = (byte) (bufferptr >> 8);
        params[4] = (byte) block_num;
        params[5] = (byte) (block_num >> 8);
        int result = vm02.call(cmd | (vm02.refAsBits((Object) params) & 0xFFFF), 0x54) & 0xFF;
        vm02.call(hmem, 0x10);
        return -result;
    }

    public static int getTime() {
        vm02.call(0x820000, 0x54);
        return vm02.peekWord(0xBF90) | (vm02.peekWord(0xBF92) << 16);
    }
}
