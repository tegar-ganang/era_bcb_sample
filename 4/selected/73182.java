package com.lucky.msole;

import com.lucky.msole.util.Consts;
import com.lucky.msole.util.Utils;
import com.lucky.msole.obj.OleEntry;
import com.lucky.msole.obj.SstEntry;
import java.io.*;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: lucky
 * Date: 11.04.2006
 * Time: 20:05:01
 * To change this template use File | Settings | File Templates.
 */
public class OleFile implements Consts {

    public String filename;

    public byte[] header;

    public byte[] BBD;

    public byte[] SBD;

    public byte[] properties;

    public OleEntry mainOle = null;

    public OleEntry root = null;

    public SstEntry sst = null;

    public int type;

    public int sectorSize;

    public int shortSectorSize;

    public int bbdStart;

    public int bbdNumBlocks;

    public int sbdNumber;

    public int sbdStart;

    public int sbdLen;

    public int propNumber;

    public String _filename;

    public long _filelength;

    public boolean isUnicode;

    public OleFile() {
    }

    public OleFile(String filename) throws Exception {
        this.filename = filename;
        File f = new File(filename);
        if (!f.exists()) throw new FileNotFoundException();
        _filename = f.getName();
        _filelength = f.length();
        setGlobals();
        setEntries();
    }

    private int actualRead(byte data[], int boff, long off, long len) throws Exception {
        if (data == null) {
            throw new NullPointerException();
        } else if ((boff < 0) || (boff > data.length) || (len < 0) || ((boff + len) > data.length) || ((boff + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }
        FileInputStream fis = new FileInputStream(filename);
        long skip = 0;
        long toskip = off;
        try {
            while ((skip = fis.skip(toskip)) != off) toskip = (off - skip);
        } catch (Exception e) {
            System.err.println("Cant skip " + off + " bytes: " + e.getMessage());
            throw e;
        }
        int c = fis.read();
        if (c == -1) throw new Exception("actualRead got -1 bytes");
        data[boff] = (byte) c;
        int i = 1;
        for (; i < len; i++) {
            c = fis.read();
            if (c == -1) break;
            data[boff + i] = (byte) c;
        }
        fis.close();
        return i;
    }

    public String getType() {
        switch(type) {
            case oleWord:
                return "MS-Word";
            case olePpt:
                return "PowerPoint presentation";
            case oleXls:
                return "Excel sheet";
            case oleRtf:
                return "RichTextFormat";
            default:
                return "Unknown";
        }
    }

    public String getName() {
        return _filename;
    }

    public String getAllText() throws Exception {
        if (type != oleRtf) {
            if (mainOle == null) {
                System.err.print("Main oleEntry is NULL.\n");
                return null;
            }
            if (root == null) {
                System.err.print("Root Entry is NULL.\n");
                return null;
            }
        }
        if (type == oleWord) return getWordText(); else if (type == olePpt) return getPptText(); else if (type == oleXls) return getXlsText(); else if (type == oleRtf) return getRtfText(); else {
            System.err.print("Unknow file-format\n");
            return null;
        }
    }

    public String getRtfText() {
        RtfParser rp = new RtfParser(new File(filename));
        try {
            return rp.parse();
        } catch (Exception e) {
            System.err.println("Cant parse RTF: " + e.getMessage());
            e.printStackTrace();
        }
        System.err.println("Returning null");
        return null;
    }

    public String getPptText() throws Exception {
        StringBuffer out = new StringBuffer();
        int itemsread = 1;
        int rectype;
        int reclen;
        byte[] recbuf = new byte[8];
        while (itemsread > 0) {
            itemsread = (int) readStream(recbuf, 8);
            if (oleEof()) {
                processPptItem(DOCUMENT_END, 0, out);
                return out.toString();
            }
            if (itemsread < 8) {
                System.err.println("Error reading itemsread.");
                break;
            }
            rectype = Utils.getShort(recbuf, 2);
            reclen = (int) Utils.getUInt(recbuf, 4);
            processPptItem(rectype, reclen, out);
        }
        return out.toString();
    }

    private void processPptItem(int rectype, int reclen, StringBuffer out) throws Exception {
        System.err.println("Rectype: " + rectype);
        int i = 0;
        byte[] buf = new byte[8 * 1024];
        switch(rectype) {
            case TEXT_BYTES_ATOM:
                {
                    out.append("Text_Bytes_Atom: ");
                    int fin = (int) readStream(buf, reclen);
                    isUnicode = false;
                    for (i = 0; i < fin; i++) {
                        readStream(buf, 1);
                        if (buf[0] != 0x0d) appChar(buf, 0, out); else out.append(" ");
                    }
                    out.append(" ");
                    break;
                }
            case TEXT_CHARS_ATOM:
            case CSTRING:
                {
                    long text_len = reclen / 2;
                    isUnicode = true;
                    for (i = 0; i < text_len; i++) {
                        readStream(buf, 2);
                        appChar(buf, 0, out);
                    }
                    out.append("\n");
                    break;
                }
            case DOCUMENT:
                {
                    out.append("Document: ");
                    long text_len = reclen / 2;
                    isUnicode = false;
                    for (i = 0; i < text_len; i++) {
                        readStream(buf, 2);
                        appChar(buf, 0, out);
                    }
                    out.append("\n");
                    break;
                }
            case SLIDE:
                {
                    out.append("Slide: ");
                    long text_len = reclen / 2;
                    isUnicode = true;
                    for (i = 0; i < text_len; i++) {
                        readStream(buf, 2);
                        appChar(buf, 0, out);
                    }
                    out.append("\n");
                    break;
                }
            case SLIDE_BASE:
                break;
            case NOTES:
                break;
            case HEADERS_FOOTERS:
                break;
            case MAIN_MASTER:
                break;
            case LIST:
                break;
            case SLIDE_LIST_WITH_TEXT:
                break;
            case DOCUMENT_END:
                oleSeek(reclen);
                break;
            case DOCUMENT_ATOM:
                oleSeek(reclen);
                break;
            case SLIDE_ATOM:
                oleSeek(reclen);
                break;
            case SLIDE_BASE_ATOM:
                oleSeek(reclen);
                break;
            case NOTES_ATOM:
                oleSeek(reclen);
                break;
            case HEADERS_FOOTERS_ATOM:
                oleSeek(reclen);
                break;
            case USER_EDIT_ATOM:
                oleSeek(reclen);
                break;
            case COLOR_SCHEME_ATOM:
                oleSeek(reclen);
                break;
            case PPDRAWING:
                oleSeek(reclen);
                break;
            case ENVIRONMENT:
                oleSeek(reclen);
                break;
            case SSDOC_INFO_ATOM:
                oleSeek(reclen);
                break;
            case SSSLIDE_INFO_ATOM:
                oleSeek(reclen);
                break;
            case PROG_TAGS:
                oleSeek(reclen);
                break;
            case PROG_STRING_TAG:
                oleSeek(reclen);
                break;
            case PROG_BINARY_TAG:
                oleSeek(reclen);
                break;
            case PERSIST_PTR_INCREMENTAL_BLOCK:
                oleSeek(reclen);
                break;
            case EX_OLE_OBJ_STG:
                oleSeek(reclen);
                break;
            case PPDRAWING_GROUP:
                oleSeek(reclen);
                break;
            case EX_OBJ_LIST:
                oleSeek(reclen);
                break;
            case TX_MASTER_STYLE_ATOM:
                oleSeek(reclen);
                break;
            case HANDOUT:
                oleSeek(reclen);
                break;
            case SLIDE_PERSIST_ATOM:
                oleSeek(reclen);
                break;
            case TEXT_HEADER_ATOM:
                oleSeek(reclen);
                break;
            case TEXT_SPEC_INFO:
                oleSeek(reclen);
                break;
            case STYLE_TEXT_PROP_ATOM:
                oleSeek(reclen);
                break;
            default:
                oleSeek(reclen);
        }
    }

    private void oleSeek(long offset) {
        long new_ole_offset = 0;
        long ssize, blockNumber;
        new_ole_offset = mainOle.ole_offset + offset;
        if (new_ole_offset < 0) new_ole_offset = 0;
        if (new_ole_offset >= mainOle.length) new_ole_offset = mainOle.length;
        ssize = (mainOle.isBigBlock ? sectorSize : shortSectorSize);
        blockNumber = new_ole_offset / ssize;
        if (blockNumber >= mainOle.numOfBlocks) return;
        if (new_ole_offset > mainOle.ole_offset) mainOle.ole_offset = (int) new_ole_offset;
    }

    public String getXlsText() throws Exception {
        StringBuffer out = new StringBuffer();
        long rectype;
        int reclen, build_year = 0, offset = 0;
        boolean eof_flag = false;
        boolean bufs = false;
        boolean cont = false;
        int itemsread = 1;
        sst = new SstEntry();
        sst.rec = new byte[MAX_MS_RECSIZE];
        sst.sstBuf = null;
        while (itemsread > 0) {
            readStream(sst.rec, 2);
            int biff_version = Utils.getShort(sst.rec, 0);
            readStream(sst.rec, 2);
            reclen = Utils.getShort(sst.rec, 0);
            if (biff_version == 0x0809 || biff_version == 0x0409 || biff_version == 0x0209 || biff_version == 0x0009) {
                if (reclen == 8 || reclen == 16) {
                    if (biff_version == 0x0809) {
                        itemsread = (int) readStream(sst.rec, 4);
                        build_year = Utils.getShort(sst.rec, 2);
                        if (build_year > 5) {
                            itemsread = (int) readStream(sst.rec, 8);
                            biff_version = 8;
                            offset = 12;
                        } else {
                            biff_version = 7;
                            offset = 4;
                        }
                    } else if (biff_version == 0x0209) {
                        biff_version = 3;
                        offset = 2;
                    } else if (biff_version == 0x0409) {
                        offset = 2;
                        biff_version = 4;
                    } else {
                        biff_version = 2;
                    }
                    itemsread = (int) readStream(sst.rec, reclen - offset);
                    break;
                } else {
                    System.err.println("Invalid BOF record");
                    return "";
                }
            } else itemsread = (int) readStream(sst.rec, 126);
        }
        if (oleEof()) {
            System.err.println("No BOF record found\n");
            return "";
        }
        while (itemsread > 0) {
            if (bufs && cont) {
                parseSst(sst.sstBuf);
                bufs = cont = false;
            }
            byte[] buffer = new byte[2];
            rectype = 0;
            itemsread = (int) readStream(buffer, 2);
            rectype = Utils.getShort(buffer, 0);
            if (itemsread == 0) break;
            reclen = 0;
            itemsread = (int) readStream(buffer, 2);
            if (oleEof()) break;
            reclen = Utils.getShort(buffer, 0);
            if (reclen < MAX_MS_RECSIZE && reclen > 0) itemsread = (int) readStream(sst.rec, reclen);
            if (eof_flag && rectype != BOF) {
                System.err.println("EOF breakin...");
                break;
            }
            cont = (bufs && rectype != CONTINUE);
            if (rectype == SST) {
                if (sst.sstBuf != null) {
                    System.err.println("Duplicated SST found");
                    return out.toString();
                }
                sst.sstBuf = new byte[(int) reclen];
                sst.sstBytes = reclen;
                for (int i = 0; i < reclen; i++) sst.sstBuf[i] = sst.rec[i];
                bufs = true;
            } else if (rectype == CONTINUE) {
                if (!bufs) return new String();
                byte[] tmp = new byte[(int) (sst.sstBytes + reclen)];
                for (int i = 0; i < tmp.length; i++) tmp[i] = sst.rec[i];
                sst.sstBuf = tmp;
                sst.sstBytes += reclen;
            } else if (rectype == LABEL) {
                byte[] tmp = new byte[sst.rec.length - 6];
                for (int i = 6; i < sst.rec.length; i++) tmp[i - 6] = sst.rec[i];
                out.append(copyUCstring(tmp, 0));
                out.append(" ");
            } else if (rectype == STRING) {
                out.append(copyUCstring(sst.rec, 0));
                out.append(" ");
            } else if (rectype == CONSTANT_STRING) {
                int string_no = Utils.getShort(sst.rec, 6);
                if (string_no < sst.sstSize && string_no >= 0) {
                    out.append(sst.sStr[string_no]);
                    out.append(" \n");
                }
            } else if (rectype == FILEPASS) return null;
            eof_flag = (rectype == MSEOF);
        }
        return out.toString();
    }

    private String copyUCstring(byte[] src, int off) {
        StringBuffer out = new StringBuffer();
        int count = 0;
        int flags = 0;
        int start_offset = 0;
        int to_skip = 0;
        int offset = 1;
        int charsize;
        flags = src[1 + offset];
        if (!(flags == 0 || flags == 1 || flags == 8 || flags == 9 || flags == 4 || flags == 5 || flags == 0x0c || flags == 0x0d)) {
            count = flags;
            flags = src[offset];
            offset--;
            flags = src[1 + offset];
            if (!(flags == 0 || flags == 1 || flags == 8 || flags == 9 || flags == 4 || flags == 5 || flags == 0x0c || flags == 0x0d)) {
                System.err.println("GetUCString: strange flags " + flags + ", returing null.");
                return null;
            }
        } else count = Utils.getShort(src, 0);
        charsize = ((flags & 0x01) > 0) ? 2 : 1;
        isUnicode = charsize == 2;
        switch(flags & 12) {
            case 0x0c:
                to_skip = 4 * Utils.getShort(src, 2 + offset) + (int) Utils.getLong(src, 4 + offset);
                start_offset = 2 + offset + 2 + 4;
                break;
            case 0x08:
                to_skip = 4 * Utils.getShort(src, 2 + offset);
                start_offset = 2 + offset + 2;
                break;
            case 0x04:
                to_skip = (int) Utils.getLong(src, 2 + offset);
                start_offset = 2 + offset + 4;
                break;
            default:
                to_skip = 0;
                start_offset = 2 + offset;
        }
        int i = 0;
        for (i = start_offset + off; i < (start_offset + off + count * charsize); i += charsize) {
            appChar(src, i, out);
        }
        sst.strStart += to_skip + start_offset + (count * charsize);
        return out.toString();
    }

    private void parseSst(byte[] buf) {
        int i;
        sst.strStart = 8;
        int barrier = sst.sstBuf.length - 1;
        sst.sstSize = (int) Utils.getLong(sst.sstBuf, 4);
        sst.sStr = new String[sst.sstSize];
        int e = 0;
        for (i = 0; i < sst.sstSize && sst.strStart <= barrier; i++, e++) {
            byte[] tmp = new byte[buf.length - sst.strStart];
            for (int j = sst.strStart; j < buf.length; j++) tmp[j - sst.strStart] = buf[j];
            sst.sStr[e] = copyUCstring(tmp, 0);
        }
        sst.strStart = 0;
    }

    private long getlong(byte[] buffer, int offset) {
        return (long) buffer[offset] | ((long) buffer[offset + 1] << 8L) | ((long) buffer[offset + 2] << 16L) | ((long) buffer[offset + 3] << 24L);
    }

    public String getWordText() throws Exception {
        long offset = 0;
        StringBuffer out = new StringBuffer();
        byte[] headbuf = new byte[129];
        readStream(headbuf, 128);
        long textstart = Utils.getUInt(headbuf, 24);
        long textend = Utils.getUInt(headbuf, 28);
        long textlength = textend - textstart;
        System.err.println("Ole.file_offset (" + isUnicode + "): " + mainOle.file_offset);
        System.err.println("Ole.ole_offset: " + mainOle.ole_offset);
        System.err.println("textstart: " + textstart);
        for (int i = 0; i < textstart - 128; i++) {
            readStream(headbuf, 1);
            if (oleEof()) {
                System.err.println("File is corrupted.\n");
                System.exit(-1);
            }
        }
        boolean reset = false;
        boolean tabmode = false;
        boolean hyperlink_mode = false;
        long[] off = new long[1];
        off[0] = offset;
        StringBuffer tmp;
        System.err.println("Ole.file_offset (" + isUnicode + "): " + mainOle.file_offset);
        System.err.println("Ole.ole_offset: " + mainOle.ole_offset);
        while (!oleEof() && (off[0] < textlength)) {
            tmp = new StringBuffer();
            do {
                char c = getUC(off, textlength);
                if (tabmode) {
                    tabmode = false;
                    if (c == 0x007) {
                        tmp.append('\n');
                        continue;
                    } else tmp.append(" | ");
                }
                if (c < 32 && c > 0) {
                    switch(c) {
                        case 0x007:
                            tabmode = true;
                            break;
                        case 0x000D:
                        case 0x000B:
                            tmp.append("\n");
                            reset = true;
                            break;
                        case 0x000C:
                            break;
                        case 0x001E:
                            tmp.append('-');
                            break;
                        case 0x0002:
                            break;
                        case 0x001F:
                            tmp.append('*');
                            break;
                        case 0x0009:
                            tmp.append(c);
                            break;
                        case 0x0013:
                            hyperlink_mode = true;
                            tmp.append(' ');
                            break;
                        case 0x0014:
                            hyperlink_mode = false;
                        case 0x0015:
                            tmp.append(' ');
                            break;
                        case 0x0001:
                            if (hyperlink_mode) break;
                        default:
                            tmp = new StringBuffer();
                            break;
                    }
                } else if (c != 0 && c != 0xfeff) tmp.append(c); else if (tmp.length() > 1) reset = true;
            } while (!oleEof() && !reset && tmp.length() < PARAGRAPH_BUFFER - 2);
            out.append(tmp);
            reset = false;
        }
        return out.toString();
    }

    private char getUC(long off[], long fileend) throws Exception {
        int i;
        if ((i = (int) off[0] % 256) == 0) {
            int count = (int) readStream(mainOle.rb, 256);
            if (off[0] + count > fileend) count = (int) fileend - (int) off[0];
            Arrays.fill(mainOle.rb, count, 256, (byte) '\0');
            isUnicode = false;
            for (int j = 0; j < count - 1; j++) {
                char c = (char) mainOle.rb[j];
                if (c == 0x0007) break;
                if ((c == 0x20 || c == 0x0D) && mainOle.rb[j + 1] == 0x00) {
                    isUnicode = true;
                    break;
                }
            }
        }
        off[0] += isUnicode ? 2 : 1;
        return isUnicode ? (char) (mainOle.rb[i] | mainOle.rb[i + 1] << 8) : (char) mainOle.rb[i];
    }

    private int calcFileBlockOffset(int blk) {
        int res;
        if (mainOle.isBigBlock) res = 512 + mainOle.blocks[blk] * sectorSize; else {
            int sbdPerSector = sectorSize / shortSectorSize;
            int sbdSecNum = mainOle.blocks[blk] / sbdPerSector;
            int sbdSecMod = mainOle.blocks[blk] % sbdPerSector;
            res = 512 + root.blocks[sbdSecNum] * sectorSize + sbdSecMod * shortSectorSize;
        }
        return res;
    }

    private long readStream(byte bufp[], int len) throws Exception {
        int rread = 0, i = 0;
        int blockNumber, modBlock, toReadBlocks, toReadBytes, bytesInBlock;
        int ssize = 0;
        int newoffset = 0;
        if (mainOle.ole_offset + len > mainOle.length) len = mainOle.length - mainOle.ole_offset;
        ssize = (mainOle.isBigBlock ? sectorSize : SBD_BLOCK_SIZE);
        blockNumber = mainOle.ole_offset / ssize;
        if (blockNumber >= mainOle.numOfBlocks || len <= 0) return 0;
        modBlock = mainOle.ole_offset % ssize;
        bytesInBlock = ssize - modBlock;
        if (bytesInBlock < len) {
            toReadBlocks = (len - bytesInBlock) / ssize;
            toReadBytes = (len - bytesInBlock) % ssize;
        } else toReadBlocks = toReadBytes = 0;
        newoffset = calcFileBlockOffset(blockNumber) + modBlock;
        if (mainOle.file_offset != newoffset) mainOle.file_offset = newoffset;
        rread = actualRead(bufp, 0, mainOle.file_offset, Math.min(len, bytesInBlock));
        mainOle.file_offset += rread;
        for (i = 0; i < toReadBlocks; i++) {
            int readbytes;
            blockNumber++;
            newoffset = calcFileBlockOffset(blockNumber);
            if (newoffset != mainOle.file_offset) mainOle.file_offset = newoffset;
            readbytes = actualRead(bufp, rread, mainOle.file_offset, Math.min(len - rread, ssize));
            rread += readbytes;
            mainOle.file_offset += readbytes;
        }
        if (toReadBytes > 0) {
            int readbytes;
            blockNumber++;
            newoffset = calcFileBlockOffset(blockNumber);
            mainOle.file_offset = newoffset;
            readbytes = actualRead(bufp, rread, mainOle.file_offset, toReadBytes);
            rread += readbytes;
            mainOle.file_offset += readbytes;
        }
        mainOle.ole_offset += rread;
        return rread;
    }

    private long readStreamOld(byte bufp[], long len) throws Exception {
        long rread = 0;
        int i;
        long blockNumber, modBlock, toReadBlocks, toReadBytes, bytesInBlock;
        long size;
        long newoffset;
        if (mainOle.ole_offset + len > mainOle.length) len = mainOle.length - mainOle.ole_offset;
        size = (mainOle.isBigBlock ? sectorSize : shortSectorSize);
        blockNumber = (int) (mainOle.ole_offset / size);
        if (blockNumber >= mainOle.numOfBlocks || len <= 0) return 0;
        modBlock = mainOle.ole_offset % size;
        bytesInBlock = size - modBlock;
        if (bytesInBlock < len) {
            toReadBlocks = (len - bytesInBlock) / size;
            toReadBytes = (len - bytesInBlock) % size;
        } else toReadBlocks = toReadBytes = 0;
        newoffset = calcFileBlockOffset((int) blockNumber) + modBlock;
        if (mainOle.file_offset != newoffset) mainOle.file_offset = (int) newoffset;
        long finish = (mainOle.file_offset + (len < bytesInBlock ? len : bytesInBlock));
        int read = actualRead(bufp, 0, mainOle.file_offset, finish - mainOle.file_offset);
        rread = finish - mainOle.file_offset;
        mainOle.file_offset += rread;
        for (i = 0; i < toReadBlocks; i++) {
            long readbytes;
            blockNumber++;
            newoffset = calcFileBlockOffset((int) blockNumber);
            if (mainOle.file_offset != newoffset) mainOle.file_offset = (int) newoffset;
            finish = (int) (mainOle.file_offset + (len < bytesInBlock ? len : bytesInBlock));
            read += actualRead(bufp, read, mainOle.file_offset, finish - mainOle.file_offset);
            readbytes = finish - mainOle.file_offset;
            rread += readbytes;
            mainOle.file_offset += readbytes;
        }
        if (toReadBytes > 0) {
            long readbytes;
            blockNumber++;
            newoffset = calcFileBlockOffset((int) blockNumber);
            mainOle.file_offset = (int) newoffset;
            finish = (int) (mainOle.file_offset + toReadBytes);
            read += actualRead(bufp, read, mainOle.file_offset, finish - mainOle.file_offset);
            readbytes = finish - mainOle.file_offset;
            rread += readbytes;
            mainOle.file_offset += readbytes;
        }
        mainOle.ole_offset += rread;
        return rread;
    }

    final boolean oleEof() {
        return (mainOle.ole_offset >= mainOle.length);
    }

    private char getuc(byte[] buf, int pos) {
        return isUnicode ? (char) Utils.convertBytesToShort(buf, pos) : (char) buf[pos];
    }

    final boolean appChar(byte[] buf, int pos, StringBuffer out) {
        boolean res = false;
        char c = getuc(buf, pos);
        if ((int) c < 32) {
            switch(c) {
                case 0x0007:
                    break;
                case 0x000D:
                    out.append("\n");
                    break;
                case 0x000B:
                    out.append("\n");
                    break;
                case 0x000C:
                    out.append("\n");
                    break;
                case 0x001E:
                    break;
                case 0x0002:
                    break;
                case 0x001F:
                    break;
                case 0x0009:
                    out.append("\t");
                    break;
                default:
                    {
                        res = true;
                        break;
                    }
            }
        } else if (c != 0xfeff) {
            if (Character.isLetterOrDigit(c) || Character.isSpaceChar(c) || c == '.' || c == ',' || c == '?' || c == ':' || c == '/' || c == '\\' || c == '=' || c == '_' || c == '-' || c == '&' || c == '(' || c == ')') out.append(c);
        }
        return res;
    }

    private void setGlobals() throws Exception {
        header = new byte[BBD_BLOCK_SIZE];
        actualRead(header, 0, 0, BBD_BLOCK_SIZE);
        if (header[0] == '{' && header[1] == '\\' && header[2] == 'r' && header[3] == 't' && header[4] == 'f') {
            type = oleRtf;
            return;
        }
        for (int i = 0; i < oleSign.length; i++) if (header[i] != (byte) oleSign[i]) {
            System.err.println("Its not OLE.");
            System.exit(-1);
        }
        sectorSize = 1 << Utils.getShort(header, 0x1e);
        shortSectorSize = 1 << Utils.getShort(header, 0x20);
        bbdStart = (int) Utils.getLong(header, 0x4c);
        bbdNumBlocks = (int) Utils.getUInt(header, 0x2c);
        long mblock = Utils.getLong(header, 0x44);
        long msat_size = Utils.getUInt(header, 0x48);
        BBD = new byte[(int) (bbdNumBlocks * sectorSize)];
        byte[] tmpBuf = new byte[MSAT_ORIG_SIZE];
        for (int e = 0, i = 0x4c; e < MSAT_ORIG_SIZE; e++, i++) tmpBuf[e] = header[i];
        int i = 0;
        while ((mblock >= 0) && (i < msat_size)) {
            byte[] newbuf = new byte[sectorSize * (i + 1) + MSAT_ORIG_SIZE];
            for (int j = 0; j < tmpBuf.length; j++) newbuf[j] = tmpBuf[j];
            tmpBuf = newbuf;
            int e = (int) (512 + mblock * sectorSize);
            int finish = e + (int) sectorSize;
            int j = MSAT_ORIG_SIZE + (int) sectorSize * i - (i == 0 ? 0 : (4 * i));
            actualRead(tmpBuf, j, e, finish - e);
            i++;
            mblock = Utils.getLong(tmpBuf, MSAT_ORIG_SIZE + (int) sectorSize * i - 4);
        }
        for (i = 0; i < bbdNumBlocks; i++) {
            long bbdSector = Utils.getUInt(tmpBuf, 4 * i);
            if (bbdSector >= _filelength / sectorSize || bbdSector < 0) {
                System.err.println("Bad BBD entry!");
                System.exit(-1);
            }
            int e = (int) (512 + bbdSector * sectorSize);
            int j = i * (int) sectorSize;
            int finish = e + (int) sectorSize;
            actualRead(BBD, j, e, finish - e);
        }
        int sbdLen = 0;
        int sbdMaxLen = 10;
        int sbdCurrent = Utils.getShort(header, 0x3c);
        int sbdStart = sbdCurrent;
        if (sbdStart > 0) {
            SBD = new byte[(int) (sectorSize * sbdMaxLen)];
            while (true) {
                int e = (int) (512 + sbdCurrent * sectorSize);
                if (e < 0) e *= -1;
                int j = (int) (sbdLen * sectorSize);
                int finish = e + (int) sectorSize;
                actualRead(SBD, j, e, finish - e);
                sbdLen++;
                if (sbdLen >= sbdMaxLen) {
                    byte[] newSBD = new byte[(int) (sectorSize * sbdMaxLen)];
                    sbdMaxLen += 5;
                    for (int ek = 0; ek < SBD.length; ek++) newSBD[ek] = SBD[ek];
                    SBD = newSBD;
                    newSBD = null;
                }
                sbdCurrent = Utils.getShort(BBD, (int) sbdCurrent * 4);
                if (sbdCurrent < 0 || sbdCurrent >= _filelength / sectorSize) break;
            }
            sbdNumber = (sbdLen * sectorSize) / shortSectorSize;
        } else SBD = null;
        int propLen = 0;
        int propMaxLen = 5;
        int propCurrent = (int) Utils.getLong(header, 0x30);
        int propStart = propCurrent;
        if (propStart >= 0) {
            properties = new byte[(int) (propMaxLen * sectorSize)];
            while (true) {
                int e = (int) (512 + propCurrent * sectorSize);
                int j = (int) (propLen * sectorSize);
                int finish = e + (int) sectorSize;
                actualRead(properties, j, e, finish - e);
                propLen++;
                if (propLen >= propMaxLen) {
                    propMaxLen += 5;
                    byte[] newProp = new byte[(int) (propMaxLen * sectorSize)];
                    for (int ek = 0; ek < properties.length; ek++) newProp[ek] = properties[ek];
                    properties = newProp;
                }
                propCurrent = Utils.getShort(BBD, (int) propCurrent * 4);
                if (propCurrent < 0 || (int) propCurrent >= _filelength / sectorSize) break;
            }
            propNumber = (propLen * sectorSize) / PROP_BLOCK_SIZE;
        } else {
            System.err.println("No properties.");
            System.exit(-1);
        }
    }

    private void setEntries() {
        if (type == oleRtf) return;
        root = null;
        mainOle = null;
        boolean setRoot = false;
        boolean setMain = false;
        long propCurNumber = 0;
        if (properties == null) {
            System.err.println("Properties r null, breaking...");
            System.exit(-1);
        }
        while (propCurNumber < propNumber && (root == null || mainOle == null)) {
            OleEntry e = new OleEntry();
            int i, nLen;
            byte[] oleBuf = new byte[PROP_BLOCK_SIZE];
            int ie = (int) propCurNumber * PROP_BLOCK_SIZE;
            int finish = ie + PROP_BLOCK_SIZE;
            for (int ee = 0; ie < finish; ee++, ie++) oleBuf[ee] = properties[ie];
            propCurNumber++;
            if (!rightOleType(oleBuf)) {
                System.err.println("Wrong OLE type, break...");
                System.exit(-1);
            }
            nLen = Utils.getShort(oleBuf, 0x40);
            for (i = 0; i < (nLen / 2) - 1; i++) e.name += (char) oleBuf[i * 2];
            if (root == null && e.name.equalsIgnoreCase("root entry")) {
                setRoot = true;
            } else if (mainOle == null) {
                if (e.name.equalsIgnoreCase("worddocument")) {
                    setMain = true;
                    type = oleWord;
                } else if (e.name.equalsIgnoreCase("powerpoint document")) {
                    setMain = true;
                    type = olePpt;
                } else if (e.name.equalsIgnoreCase("workbook") || e.name.equalsIgnoreCase("book")) {
                    setMain = true;
                    type = oleXls;
                }
            }
            int chainMaxLen, chainCurrent;
            e.dirPos = oleBuf;
            e.type = ((int) oleBuf[0x42]);
            e.startBlock = (int) Utils.getLong(oleBuf, 0x74);
            e.blocks = null;
            e.length = (int) Utils.getUInt(oleBuf, 0x78);
            chainMaxLen = 25;
            e.numOfBlocks = 0;
            chainCurrent = e.startBlock;
            e.isBigBlock = (e.length >= 0x1000) || e.name.equalsIgnoreCase("Root Entry");
            if (e.startBlock >= 0 && e.length >= 0 && ((int) e.startBlock <= _filelength / (e.isBigBlock ? sectorSize : shortSectorSize))) {
                e.blocks = new int[(int) chainMaxLen * 4];
                while (true) {
                    e.blocks[(int) e.numOfBlocks++] = chainCurrent;
                    if (e.numOfBlocks >= chainMaxLen) {
                        int[] newChain = new int[e.blocks.length + (int) chainMaxLen * 4];
                        chainMaxLen += 25;
                        for (int qq = 0; qq < e.blocks.length; qq++) newChain[qq] = e.blocks[qq];
                        e.blocks = newChain;
                    }
                    if (e.isBigBlock) chainCurrent = (int) Utils.getUInt(BBD, (int) chainCurrent * 4); else if (SBD != null) chainCurrent = (int) Utils.getUInt(SBD, (int) chainCurrent * 4); else chainCurrent = -1;
                    if (chainCurrent <= 0 || chainCurrent >= (e.isBigBlock ? ((bbdNumBlocks * sectorSize) / 4) : ((sbdNumber * shortSectorSize) / 4)) || (e.numOfBlocks > (long) e.length / (e.isBigBlock ? sectorSize : shortSectorSize))) {
                        break;
                    }
                }
            }
            if (e.length > (e.isBigBlock ? sectorSize : shortSectorSize) * e.numOfBlocks) e.length = (e.isBigBlock ? sectorSize : shortSectorSize) * e.numOfBlocks;
            e.ole_offset = 0;
            e.file_offset = 0;
            if (setRoot) {
                root = e;
                setRoot = false;
            } else if (setMain) {
                mainOle = e;
                setMain = false;
            } else e = null;
        }
    }

    public static int getQuickFileTypeOld(File f) throws Exception {
        InputStream fis = new FileInputStream(f);
        ByteArrayOutputStream is = new ByteArrayOutputStream();
        int read = 0;
        byte[] buf = new byte[512];
        while ((read = fis.read(buf, 0, 512)) > 0) is.write(buf);
        fis.close();
        is.flush();
        is.close();
        byte[] alldata = is.toByteArray();
        byte[] header = new byte[BBD_BLOCK_SIZE];
        byte[] properties;
        byte[] BBD;
        for (int i = 0; i < BBD_BLOCK_SIZE; i++) header[i] = alldata[i];
        for (int i = 0; i < oleSign.length; i++) if (header[i] != (byte) oleSign[i]) {
            System.err.println("Its not OLE.");
            return oleUnknown;
        }
        long sectorSize = 1 << Utils.getShort(header, 0x1e);
        long bbdNumBlocks = Utils.getUInt(header, 0x2c);
        long mblock = Utils.getLong(header, 0x44);
        long msat_size = Utils.getUInt(header, 0x48);
        BBD = new byte[(int) (bbdNumBlocks * sectorSize)];
        byte[] tmpBuf = new byte[MSAT_ORIG_SIZE];
        for (int e = 0, i = 0x4c; e < MSAT_ORIG_SIZE; e++, i++) tmpBuf[e] = header[i];
        int i = 0;
        while ((mblock >= 0) && (i < msat_size)) {
            byte[] newbuf = new byte[(int) sectorSize * (i + 1) + MSAT_ORIG_SIZE];
            for (int j = 0; j < tmpBuf.length; j++) newbuf[j] = tmpBuf[j];
            tmpBuf = newbuf;
            int e = (int) (512 + mblock * sectorSize);
            int finish = e + (int) sectorSize;
            for (int j = MSAT_ORIG_SIZE + (int) sectorSize * i - (i == 0 ? 0 : (4 * i)); e < finish; e++, j++) tmpBuf[j] = alldata[e];
            i++;
            mblock = Utils.getLong(tmpBuf, MSAT_ORIG_SIZE + (int) sectorSize * i - 4);
        }
        for (i = 0; i < bbdNumBlocks; i++) {
            long bbdSector = Utils.getUInt(tmpBuf, 4 * i);
            if (bbdSector >= alldata.length / sectorSize || bbdSector < 0) {
                System.err.println("Bad BBD entry!");
                return oleUnknown;
            }
            int e = (int) (512 + bbdSector * sectorSize);
            int j = i * (int) sectorSize;
            int finish = e + (int) sectorSize;
            for (; e < finish; e++, j++) BBD[j] = alldata[e];
        }
        long propLen = 0;
        long propMaxLen = 5;
        long propNumber = 0;
        long propCurrent = Utils.getLong(header, 0x30);
        long propStart = propCurrent;
        if (propStart >= 0) {
            properties = new byte[(int) (propMaxLen * sectorSize)];
            while (true) {
                int e = (int) (512 + propCurrent * sectorSize);
                int j = (int) (propLen * sectorSize);
                int finish = e + (int) sectorSize;
                for (; e < finish; e++, j++) properties[j] = alldata[e];
                propLen++;
                if (propLen >= propMaxLen) {
                    byte[] newProp = new byte[(int) (propMaxLen * sectorSize)];
                    propMaxLen += 5;
                    for (int ek = 0; ek < properties.length; ek++) newProp[ek] = properties[ek];
                    properties = newProp;
                }
                propCurrent = Utils.getLong(BBD, (int) propCurrent * 4);
                if (propCurrent < 0 || (int) propCurrent >= alldata.length / sectorSize) break;
            }
            propNumber = (propLen * sectorSize) / PROP_BLOCK_SIZE;
        } else {
            System.err.println("No properties.");
            return oleUnknown;
        }
        long propCurNumber = 0;
        while (propCurNumber < propNumber) {
            OleEntry e;
            int nLen;
            byte[] oleBuf = new byte[PROP_BLOCK_SIZE];
            int ie = (int) propCurNumber * PROP_BLOCK_SIZE;
            int finish = ie + PROP_BLOCK_SIZE;
            for (int ee = 0; ie < finish; ee++, ie++) oleBuf[ee] = properties[ie];
            propCurNumber++;
            if (!rightOleType(oleBuf)) {
                System.err.println("Wrong OLE type, break...");
                break;
            }
            e = new OleEntry();
            nLen = Utils.getShort(oleBuf, 0x40);
            for (i = 0; i < (nLen / 2) - 1; i++) e.name += (char) oleBuf[i * 2];
            if (e.name.equalsIgnoreCase("worddocument")) return oleWord; else if (e.name.equalsIgnoreCase("powerpoint document")) return olePpt; else if (e.name.equalsIgnoreCase("workbook") || e.name.equalsIgnoreCase("book")) return oleXls;
        }
        return oleUnknown;
    }

    private static boolean rightOleType(byte[] oleBuf) {
        return (oleBuf[0x42] == 1 || oleBuf[0x42] == 2 || oleBuf[0x42] == 3 || oleBuf[0x42] == 5);
    }
}
