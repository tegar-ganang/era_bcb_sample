package org.catacombae.hfsexplorer.types.hfsplus;

import org.catacombae.hfsexplorer.Util;
import org.catacombae.csjc.MutableStruct;
import java.util.Date;
import java.io.*;
import org.catacombae.csjc.StructElements;
import org.catacombae.csjc.structelements.ASCIIStringField;
import org.catacombae.csjc.structelements.Dictionary;
import org.catacombae.csjc.structelements.DictionaryBuilder;

public class HFSPlusVolumeHeader extends MutableStruct implements StructElements {

    public static final short SIGNATURE_HFS_PLUS = 0x482B;

    public static final short SIGNATURE_HFSX = 0x4858;

    private final byte[] signature = new byte[2];

    private final byte[] version = new byte[2];

    private final byte[] attributes = new byte[4];

    private final byte[] lastMountedVersion = new byte[4];

    private final byte[] journalInfoBlock = new byte[4];

    private final byte[] createDate = new byte[4];

    private final byte[] modifyDate = new byte[4];

    private final byte[] backupDate = new byte[4];

    private final byte[] checkedDate = new byte[4];

    private final byte[] fileCount = new byte[4];

    private final byte[] folderCount = new byte[4];

    private final byte[] blockSize = new byte[4];

    private final byte[] totalBlocks = new byte[4];

    private final byte[] freeBlocks = new byte[4];

    private final byte[] nextAllocation = new byte[4];

    private final byte[] rsrcClumpSize = new byte[4];

    private final byte[] dataClumpSize = new byte[4];

    private final HFSCatalogNodeID nextCatalogID;

    private final byte[] writeCount = new byte[4];

    private final byte[] encodingsBitmap = new byte[8];

    private final byte[] finderInfo = new byte[4 * 8];

    private final HFSPlusForkData allocationFile;

    private final HFSPlusForkData extentsFile;

    private final HFSPlusForkData catalogFile;

    private final HFSPlusForkData attributesFile;

    private final HFSPlusForkData startupFile;

    public HFSPlusVolumeHeader(byte[] data) {
        this(data, 0);
    }

    public HFSPlusVolumeHeader(byte[] data, int offset) {
        System.arraycopy(data, offset + 0, signature, 0, 2);
        System.arraycopy(data, offset + 2, version, 0, 2);
        System.arraycopy(data, offset + 4, attributes, 0, 4);
        System.arraycopy(data, offset + 8, lastMountedVersion, 0, 4);
        System.arraycopy(data, offset + 12, journalInfoBlock, 0, 4);
        System.arraycopy(data, offset + 16, createDate, 0, 4);
        System.arraycopy(data, offset + 20, modifyDate, 0, 4);
        System.arraycopy(data, offset + 24, backupDate, 0, 4);
        System.arraycopy(data, offset + 28, checkedDate, 0, 4);
        System.arraycopy(data, offset + 32, fileCount, 0, 4);
        System.arraycopy(data, offset + 36, folderCount, 0, 4);
        System.arraycopy(data, offset + 40, blockSize, 0, 4);
        System.arraycopy(data, offset + 44, totalBlocks, 0, 4);
        System.arraycopy(data, offset + 48, freeBlocks, 0, 4);
        System.arraycopy(data, offset + 52, nextAllocation, 0, 4);
        System.arraycopy(data, offset + 56, rsrcClumpSize, 0, 4);
        System.arraycopy(data, offset + 60, dataClumpSize, 0, 4);
        nextCatalogID = new HFSCatalogNodeID(data, offset + 64);
        System.arraycopy(data, offset + 68, writeCount, 0, 4);
        System.arraycopy(data, offset + 72, encodingsBitmap, 0, 4);
        System.arraycopy(data, offset + 80, finderInfo, 0, 4 * 8);
        allocationFile = new HFSPlusForkData(data, offset + 112);
        extentsFile = new HFSPlusForkData(data, offset + 192);
        catalogFile = new HFSPlusForkData(data, offset + 272);
        attributesFile = new HFSPlusForkData(data, offset + 352);
        startupFile = new HFSPlusForkData(data, offset + 432);
    }

    public HFSPlusVolumeHeader(InputStream is) throws IOException {
        this(Util.fillBuffer(is, new byte[_getSize()]), 0);
    }

    private static int _getSize() {
        return 512;
    }

    public short getSignature() {
        return Util.readShortBE(signature);
    }

    public short getVersion() {
        return Util.readShortBE(version);
    }

    public int getAttributes() {
        return Util.readIntBE(attributes);
    }

    public int getLastMountedVersion() {
        return Util.readIntBE(lastMountedVersion);
    }

    public int getJournalInfoBlock() {
        return Util.readIntBE(journalInfoBlock);
    }

    public int getCreateDate() {
        return Util.readIntBE(createDate);
    }

    public int getModifyDate() {
        return Util.readIntBE(modifyDate);
    }

    public int getBackupDate() {
        return Util.readIntBE(backupDate);
    }

    public int getCheckedDate() {
        return Util.readIntBE(checkedDate);
    }

    public int getFileCount() {
        return Util.readIntBE(fileCount);
    }

    public int getFolderCount() {
        return Util.readIntBE(folderCount);
    }

    public int getBlockSize() {
        return Util.readIntBE(blockSize);
    }

    public int getTotalBlocks() {
        return Util.readIntBE(totalBlocks);
    }

    public int getFreeBlocks() {
        return Util.readIntBE(freeBlocks);
    }

    public int getNextAllocation() {
        return Util.readIntBE(nextAllocation);
    }

    public int getRsrcClumpSize() {
        return Util.readIntBE(rsrcClumpSize);
    }

    public int getDataClumpSize() {
        return Util.readIntBE(dataClumpSize);
    }

    public HFSCatalogNodeID getNextCatalogID() {
        return nextCatalogID;
    }

    public int getWriteCount() {
        return Util.readIntBE(writeCount);
    }

    public long getEncodingsBitmap() {
        return Util.readLongBE(encodingsBitmap);
    }

    public int[] getFinderInfo() {
        return Util.readIntArrayBE(finderInfo);
    }

    public HFSPlusForkData getAllocationFile() {
        return allocationFile;
    }

    public HFSPlusForkData getExtentsFile() {
        return extentsFile;
    }

    public HFSPlusForkData getCatalogFile() {
        return catalogFile;
    }

    public HFSPlusForkData getAttributesFile() {
        return attributesFile;
    }

    public HFSPlusForkData getStartupFile() {
        return startupFile;
    }

    public Date getCreateDateAsDate() {
        return HFSPlusDate.localTimestampToDate(getCreateDate());
    }

    public Date getModifyDateAsDate() {
        return HFSPlusDate.localTimestampToDate(getModifyDate());
    }

    public Date getBackupDateAsDate() {
        return HFSPlusDate.localTimestampToDate(getBackupDate());
    }

    public Date getCheckedDateAsDate() {
        return HFSPlusDate.localTimestampToDate(getCheckedDate());
    }

    public boolean getAttributeVolumeHardwareLock() {
        return ((getAttributes() >> 7) & 0x1) != 0;
    }

    public boolean getAttributeVolumeUnmounted() {
        return ((getAttributes() >> 8) & 0x1) != 0;
    }

    public boolean getAttributeVolumeSparedBlocks() {
        return ((getAttributes() >> 9) & 0x1) != 0;
    }

    public boolean getAttributeVolumeNoCacheRequired() {
        return ((getAttributes() >> 10) & 0x1) != 0;
    }

    public boolean getAttributeBootVolumeInconsistent() {
        return ((getAttributes() >> 11) & 0x1) != 0;
    }

    public boolean getAttributeCatalogNodeIDsReused() {
        return ((getAttributes() >> 12) & 0x1) != 0;
    }

    public boolean getAttributeVolumeJournaled() {
        return ((getAttributes() >> 13) & 0x1) != 0;
    }

    public boolean getAttributeVolumeSoftwareLock() {
        return ((getAttributes() >> 15) & 0x1) != 0;
    }

    public void print(PrintStream ps, String prefix) {
        ps.println(prefix + "signature: \"" + Util.toASCIIString(getSignature()) + "\"");
        ps.println(prefix + "version: " + getVersion());
        ps.println(prefix + "attributes: " + getAttributes());
        printAttributes(ps, prefix + "  ");
        ps.println(prefix + "lastMountedVersion: " + getLastMountedVersion());
        ps.println(prefix + "journalInfoBlock: " + getJournalInfoBlock());
        ps.println(prefix + "createDate: " + getCreateDateAsDate());
        ps.println(prefix + "modifyDate: " + getModifyDateAsDate());
        ps.println(prefix + "backupDate: " + getBackupDateAsDate());
        ps.println(prefix + "checkedDate: " + getCheckedDateAsDate());
        ps.println(prefix + "fileCount: " + getFileCount());
        ps.println(prefix + "folderCount: " + getFolderCount());
        ps.println(prefix + "blockSize: " + getBlockSize());
        ps.println(prefix + "totalBlocks: " + getTotalBlocks());
        ps.println(prefix + "freeBlocks: " + getFreeBlocks());
        ps.println(prefix + "nextAllocation: " + getNextAllocation());
        ps.println(prefix + "rsrcClumpSize: " + getRsrcClumpSize());
        ps.println(prefix + "dataClumpSize: " + getDataClumpSize());
        ps.println(prefix + "nextCatalogID: " + getNextCatalogID().toString());
        ps.println(prefix + "writeCount: " + getWriteCount());
        ps.println(prefix + "encodingsBitmap: " + getEncodingsBitmap());
        ps.println(prefix + "encodingsBitmap (hex): 0x" + Util.toHexStringBE(getEncodingsBitmap()));
        int[] finderInfoInts = getFinderInfo();
        for (int i = 0; i < finderInfoInts.length; ++i) ps.println(prefix + "finderInfo[" + i + "]: " + finderInfoInts[i]);
        ps.println(prefix + "allocationFile: ");
        allocationFile.print(ps, prefix + "  ");
        ps.println(prefix + "extentsFile: ");
        extentsFile.print(ps, prefix + "  ");
        ps.println(prefix + "catalogFile: ");
        catalogFile.print(ps, prefix + "  ");
        ps.println(prefix + "attributesFile: ");
        attributesFile.print(ps, prefix + "  ");
        ps.println(prefix + "startupFile: ");
        startupFile.print(ps, prefix + "  ");
    }

    public void printAttributes(PrintStream ps, int pregap) {
        String pregapString = "";
        for (int i = 0; i < pregap; ++i) pregapString += " ";
    }

    public void printAttributes(PrintStream ps, String prefix) {
        int attributesInt = getAttributes();
        ps.println(prefix + "kHFSVolumeHardwareLockBit = " + ((attributesInt >> 7) & 0x1));
        ps.println(prefix + "kHFSVolumeUnmountedBit = " + ((attributesInt >> 8) & 0x1));
        ps.println(prefix + "kHFSVolumeSparedBlocksBit = " + ((attributesInt >> 9) & 0x1));
        ps.println(prefix + "kHFSVolumeNoCacheRequiredBit = " + ((attributesInt >> 10) & 0x1));
        ps.println(prefix + "kHFSBootVolumeInconsistentBit = " + ((attributesInt >> 11) & 0x1));
        ps.println(prefix + "kHFSCatalogNodeIDsReusedBit = " + ((attributesInt >> 12) & 0x1));
        ps.println(prefix + "kHFSVolumeJournaledBit = " + ((attributesInt >> 13) & 0x1));
        ps.println(prefix + "kHFSVolumeSoftwareLockBit = " + ((attributesInt >> 15) & 0x1));
    }

    private Dictionary getAttributeElements() {
        DictionaryBuilder db = new DictionaryBuilder("Attributes");
        db.addFlag("kHFSVolumeHardwareLockBit", attributes, 7);
        db.addFlag("kHFSVolumeUnmountedBit", attributes, 8);
        db.addFlag("kHFSVolumeSparedBlocksBit", attributes, 9);
        db.addFlag("kHFSVolumeNoCacheRequiredBit", attributes, 10);
        db.addFlag("kHFSBootVolumeInconsistentBit", attributes, 11);
        db.addFlag("kHFSCatalogNodeIDsReusedBit", attributes, 12);
        db.addFlag("kHFSVolumeJournaledBit", attributes, 13);
        db.addFlag("kHFSVolumeSoftwareLockBit", attributes, 15);
        return db.getResult();
    }

    public Dictionary getStructElements() {
        DictionaryBuilder db = new DictionaryBuilder(HFSPlusVolumeHeader.class.getSimpleName());
        db.add("signature", new ASCIIStringField(signature));
        db.addUIntBE("version", version);
        db.add("attributes", getAttributeElements());
        db.addUIntBE("lastMountedVersion", lastMountedVersion);
        db.addUIntBE("journalInfoBlock", journalInfoBlock);
        db.add("createDate", new HFSPlusDateField(createDate, true));
        db.add("modifyDate", new HFSPlusDateField(modifyDate, true));
        db.add("backupDate", new HFSPlusDateField(backupDate, true));
        db.add("checkedDate", new HFSPlusDateField(checkedDate, true));
        db.addUIntBE("fileCount", fileCount);
        db.addUIntBE("folderCount", folderCount);
        db.addUIntBE("blockSize", blockSize);
        db.addUIntBE("totalBlocks", totalBlocks);
        db.addUIntBE("freeBlocks", freeBlocks);
        db.addUIntBE("nextAllocation", nextAllocation);
        db.addUIntBE("rsrcClumpSize", rsrcClumpSize);
        db.addUIntBE("dataClumpSize", dataClumpSize);
        db.add("nextCatalogID", nextCatalogID.getStructElements());
        db.addUIntBE("writeCount", writeCount);
        db.addUIntBE("encodingsBitmap", encodingsBitmap);
        db.addIntArray("finderInfo", finderInfo, BITS_32, SIGNED, BIG_ENDIAN);
        db.add("allocationFile", allocationFile.getStructElements());
        db.add("extentsFile", extentsFile.getStructElements());
        db.add("catalogFile", catalogFile.getStructElements());
        db.add("attributesFile", attributesFile.getStructElements());
        db.add("startupFile", startupFile.getStructElements());
        return db.getResult();
    }
}
