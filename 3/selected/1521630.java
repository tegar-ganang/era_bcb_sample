package sun.security.util;

import java.security.*;
import java.util.HashMap;

/**
 * This class is used to compute digests on sections of the Manifest.
 */
public class ManifestDigester {

    /** the raw bytes of the manifest */
    byte rawBytes[];

    /** the offset/length pair for a section */
    HashMap entries;

    /** state returned by findSection */
    static class Position {

        int endOfFirstLine;

        int endOfSection;

        int startOfNext;
    }

    /**
     * find a section in the manifest.
     *
     * @param offset should point to the starting offset with in the
     * raw bytes of the next section.
     *
     * @pos set by
     *
     * @returns false if end of bytes has been reached, otherwise returns
     *          true
     */
    private boolean findSection(int offset, Position pos) {
        int i = offset, len = rawBytes.length;
        int last = offset;
        int next;
        boolean allBlank = true;
        pos.endOfFirstLine = -1;
        while (i < len) {
            byte b = rawBytes[i];
            switch(b) {
                case '\r':
                    if (pos.endOfFirstLine == -1) pos.endOfFirstLine = i - 1;
                    if ((i < len) && (rawBytes[i + 1] == '\n')) i++;
                case '\n':
                    if (pos.endOfFirstLine == -1) pos.endOfFirstLine = i - 1;
                    if (allBlank || (i == len - 1)) {
                        if (i == len - 1) pos.endOfSection = i; else pos.endOfSection = last;
                        pos.startOfNext = i + 1;
                        return true;
                    } else {
                        last = i;
                        allBlank = true;
                    }
                    break;
                default:
                    allBlank = false;
                    break;
            }
            i++;
        }
        return false;
    }

    public ManifestDigester(byte bytes[]) {
        rawBytes = bytes;
        entries = new HashMap();
        Position pos = new Position();
        if (!findSection(0, pos)) return;
        int start = pos.startOfNext;
        while (findSection(start, pos)) {
            int len = pos.endOfFirstLine - start + 1;
            int sectionLen = pos.endOfSection - start + 1;
            int sectionLenWithBlank = pos.startOfNext - start;
            if (len > 6) {
                if (isNameAttr(bytes, start)) {
                    StringBuffer nameBuf = new StringBuffer();
                    nameBuf.append(new String(bytes, start + 6, len - 6));
                    int i = start + len;
                    if ((i - start) < sectionLen) {
                        if (bytes[i] == '\r') {
                            i += 2;
                        } else {
                            i += 1;
                        }
                    }
                    while ((i - start) < sectionLen) {
                        if (bytes[i++] == ' ') {
                            int wrapStart = i;
                            while (((i - start) < sectionLen) && (bytes[i++] != '\n')) ;
                            if (bytes[i - 1] != '\n') return;
                            int wrapLen;
                            if (bytes[i - 2] == '\r') wrapLen = i - wrapStart - 2; else wrapLen = i - wrapStart - 1;
                            nameBuf.append(new String(bytes, wrapStart, wrapLen));
                        } else {
                            break;
                        }
                    }
                    String name = nameBuf.toString();
                    entries.put(name, new Entry(start, sectionLen, sectionLenWithBlank, rawBytes));
                }
            }
            start = pos.startOfNext;
        }
    }

    private boolean isNameAttr(byte bytes[], int start) {
        return ((bytes[start] == 'N') || (bytes[start] == 'n')) && ((bytes[start + 1] == 'a') || (bytes[start + 1] == 'A')) && ((bytes[start + 2] == 'm') || (bytes[start + 2] == 'M')) && ((bytes[start + 3] == 'e') || (bytes[start + 3] == 'E')) && (bytes[start + 4] == ':') && (bytes[start + 5] == ' ');
    }

    public static class Entry {

        int offset;

        int length;

        int lengthWithBlankLine;

        byte[] rawBytes;

        boolean oldStyle;

        public Entry(int offset, int length, int lengthWithBlankLine, byte[] rawBytes) {
            this.offset = offset;
            this.length = length;
            this.lengthWithBlankLine = lengthWithBlankLine;
            this.rawBytes = rawBytes;
        }

        public byte[] digest(MessageDigest md) {
            md.reset();
            if (oldStyle) {
                doOldStyle(md, rawBytes, offset, lengthWithBlankLine);
            } else {
                md.update(rawBytes, offset, lengthWithBlankLine);
            }
            return md.digest();
        }

        private void doOldStyle(MessageDigest md, byte[] bytes, int offset, int length) {
            int i = offset;
            int start = offset;
            int max = offset + length;
            int prev = -1;
            while (i < max) {
                if ((bytes[i] == '\r') && (prev == ' ')) {
                    md.update(bytes, start, i - start - 1);
                    start = i;
                }
                prev = bytes[i];
                i++;
            }
            md.update(bytes, start, i - start);
        }

        /** Netscape doesn't include the new line. Others do. */
        public byte[] digestWorkaround(MessageDigest md) {
            md.reset();
            md.update(rawBytes, offset, length);
            return md.digest();
        }
    }

    public Entry get(String name, boolean oldStyle) {
        Entry e = (Entry) entries.get(name);
        if (e != null) e.oldStyle = oldStyle;
        return e;
    }

    public byte[] manifestDigest(MessageDigest md) {
        md.reset();
        md.update(rawBytes, 0, rawBytes.length);
        return md.digest();
    }
}
