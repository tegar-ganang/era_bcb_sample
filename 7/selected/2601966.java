package com.google.typography.font.tools.conversion.eot;

import com.google.typography.font.sfntly.Font;
import com.google.typography.font.sfntly.FontFactory;
import com.google.typography.font.sfntly.Tag;
import com.google.typography.font.sfntly.data.ReadableFontData;
import com.google.typography.font.sfntly.data.WritableFontData;
import com.google.typography.font.sfntly.table.core.FontHeaderTable;
import com.google.typography.font.sfntly.table.core.NameTable;
import com.google.typography.font.sfntly.table.core.OS2Table;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author Jeremie Lenfant-Engelmann
 */
public class EOTWriter {

    private final boolean compressed;

    private final FontFactory factory = FontFactory.getInstance();

    private static final long RESERVED = 0;

    private static final short PADDING = 0;

    private static final long VERSION = 0x00020002;

    private static final short MAGIC_NUMBER = 0x504c;

    private static final long DEFAULT_FLAGS = 0;

    private static final long FLAGS_TT_COMPRESSED = 0x4;

    private static final byte DEFAULT_CHARSET = 1;

    private static final long CS_XORKEY = 0x50475342;

    public EOTWriter() {
        compressed = false;
    }

    public EOTWriter(boolean compressed) {
        this.compressed = compressed;
    }

    public WritableFontData convert(Font font) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        factory.serializeFont(font, baos);
        byte[] fontData = baos.toByteArray();
        NameTable name = font.getTable(Tag.name);
        byte[] familyName = convertUTF16StringToLittleEndian(name.nameAsBytes(3, 1, 0x409, 1));
        byte[] styleName = convertUTF16StringToLittleEndian(name.nameAsBytes(3, 1, 0x409, 2));
        byte[] versionName = convertUTF16StringToLittleEndian(name.nameAsBytes(3, 1, 0x409, 5));
        byte[] fullName = convertUTF16StringToLittleEndian(name.nameAsBytes(3, 1, 0x409, 4));
        long flags = DEFAULT_FLAGS;
        if (compressed) {
            flags |= FLAGS_TT_COMPRESSED;
            MtxWriter mtxWriter = new MtxWriter();
            fontData = mtxWriter.compress(font);
        }
        long eotSize = computeEotSize(familyName.length, styleName.length, versionName.length, fullName.length, fontData.length);
        WritableFontData writableFontData = createWritableFontData((int) eotSize);
        OS2Table os2Table = font.getTable(Tag.OS_2);
        int index = 0;
        index += writableFontData.writeULongLE(index, eotSize);
        index += writableFontData.writeULongLE(index, fontData.length);
        index += writableFontData.writeULongLE(index, VERSION);
        index += writableFontData.writeULongLE(index, flags);
        index += writeFontPANOSE(index, os2Table, writableFontData);
        index += writableFontData.writeByte(index, DEFAULT_CHARSET);
        index += writableFontData.writeByte(index, (byte) (os2Table.fsSelectionAsInt() & 1));
        index += writableFontData.writeULongLE(index, os2Table.usWeightClass());
        index += writableFontData.writeUShortLE(index, (short) os2Table.fsTypeAsInt());
        index += writableFontData.writeUShortLE(index, MAGIC_NUMBER);
        index += writeUnicodeRanges(index, os2Table, writableFontData);
        index += writeCodePages(index, os2Table, writableFontData);
        FontHeaderTable head = font.getTable(Tag.head);
        index += writableFontData.writeULongLE(index, head.checkSumAdjustment());
        index += writeReservedFields(index, writableFontData);
        index += writePadding(index, writableFontData);
        index += writeUTF16String(index, familyName, writableFontData);
        index += writePadding(index, writableFontData);
        index += writeUTF16String(index, styleName, writableFontData);
        index += writePadding(index, writableFontData);
        index += writeUTF16String(index, versionName, writableFontData);
        index += writePadding(index, writableFontData);
        index += writeUTF16String(index, fullName, writableFontData);
        index += writePadding(index, writableFontData);
        index += writePadding(index, writableFontData);
        if (VERSION > 0x20001) {
            index += writableFontData.writeULongLE(index, CS_XORKEY);
            index += writableFontData.writeULongLE(index, 0);
            index += writePadding(index, writableFontData);
            index += writePadding(index, writableFontData);
            index += writableFontData.writeULongLE(index, 0);
            index += writableFontData.writeULongLE(index, 0);
        }
        writableFontData.writeBytes(index, fontData, 0, fontData.length);
        return writableFontData;
    }

    private long computeEotSize(int familyNameSize, int styleNameSize, int versionNameSize, int fullNameSize, int fontDataSize) {
        return 16 * ReadableFontData.DataSize.ULONG.size() + 12 * ReadableFontData.DataSize.BYTE.size() + 12 * ReadableFontData.DataSize.USHORT.size() + familyNameSize * ReadableFontData.DataSize.BYTE.size() + styleNameSize * ReadableFontData.DataSize.BYTE.size() + versionNameSize * ReadableFontData.DataSize.BYTE.size() + fullNameSize * ReadableFontData.DataSize.BYTE.size() + fontDataSize * ReadableFontData.DataSize.BYTE.size() + (VERSION > 0x20001 ? 5 * ReadableFontData.DataSize.ULONG.size() : 0);
    }

    private int writeFontPANOSE(int index, OS2Table os2Table, WritableFontData writableFontData) {
        byte[] fontPANOSE = os2Table.panose();
        return writableFontData.writeBytes(index, fontPANOSE, 0, fontPANOSE.length);
    }

    private int writeReservedFields(int start, WritableFontData writableFontData) {
        int index = start;
        for (int i = 0; i < 4; i++) {
            index += writableFontData.writeULongLE(index, RESERVED);
        }
        return index - start;
    }

    private int writeUnicodeRanges(int start, OS2Table os2Table, WritableFontData writableFontData) {
        int index = start;
        index += writableFontData.writeULongLE(index, os2Table.ulUnicodeRange1());
        index += writableFontData.writeULongLE(index, os2Table.ulUnicodeRange2());
        index += writableFontData.writeULongLE(index, os2Table.ulUnicodeRange3());
        index += writableFontData.writeULongLE(index, os2Table.ulUnicodeRange4());
        return index - start;
    }

    private int writeCodePages(int start, OS2Table os2Table, WritableFontData writableFontData) {
        int index = start;
        index += writableFontData.writeULongLE(index, os2Table.ulCodePageRange1());
        index += writableFontData.writeULongLE(index, os2Table.ulCodePageRange2());
        return index - start;
    }

    private int writePadding(int index, WritableFontData writableFontData) {
        return writableFontData.writeUShortLE(index, PADDING);
    }

    private int writeUTF16String(int start, byte[] str, WritableFontData writableFontData) {
        int index = start;
        index += writableFontData.writeUShortLE(index, (short) str.length);
        index += writableFontData.writeBytes(index, str, 0, str.length);
        return index - start;
    }

    private byte[] convertUTF16StringToLittleEndian(byte[] bytesString) {
        if (bytesString == null) {
            return new byte[0];
        }
        for (int i = 0; i < bytesString.length; i += 2) {
            byte tmp = bytesString[i];
            bytesString[i] = bytesString[i + 1];
            bytesString[i + 1] = tmp;
        }
        return bytesString;
    }

    private WritableFontData createWritableFontData(int length) {
        return WritableFontData.createWritableFontData(length);
    }
}
