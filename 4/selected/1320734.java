package jcontrol.eib.extended.a_layer;

import jcontrol.eib.extended.transceiver.TFrame;

class APDU {

    private APDU() {
    }

    static byte[] makeNoParamsReq(int apci) {
        byte[] aPDU = TFrame.getNew(1);
        aPDU[TFrame.APDU_START + 0] = (byte) (apci >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) (apci >> 16);
        return aPDU;
    }

    static byte[] makeGroupValue(int apci, byte[] data) {
        if (data.length == 0) throw new IllegalArgumentException("'data' contains no data!");
        byte[] aPDU = TFrame.getNew(data.length);
        aPDU[TFrame.APDU_START + 0] = (byte) (apci >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) ((apci >> 16) | (data[0] & 0x3F));
        System.arraycopy(data, 1, aPDU, TFrame.APDU_START + 2, data.length - 1);
        return aPDU;
    }

    static byte[] getGroupValueData(byte[] aPDU, int length) {
        byte[] data = new byte[length];
        System.arraycopy(aPDU, TFrame.APDU_START + 1, data, 0, length);
        data[0] &= 0x3F;
        return data;
    }

    static byte[] makeAddress(int apci, int address) {
        if ((address & 0xFFFF0000) != 0) throw new IllegalArgumentException("'physAddr' out of range!");
        byte[] aPDU = TFrame.getNew(3);
        aPDU[TFrame.APDU_START + 0] = (byte) (apci >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) (apci >> 16);
        aPDU[TFrame.APDU_START + 2] = (byte) (address >> 8);
        aPDU[TFrame.APDU_START + 3] = (byte) address;
        return aPDU;
    }

    static int getAddressAddr(byte[] aPDU) {
        return ((aPDU[TFrame.APDU_START + 2] & 0xFF) << 8) + (aPDU[TFrame.APDU_START + 3] & 0XFF);
    }

    static byte[] makePhysAddrSerNoRead(long serNo) {
        if ((serNo & 0xFFFF000000000000L) != 0) throw new IllegalArgumentException("'serNo' out of range!");
        byte[] aPDU = TFrame.getNew(7);
        aPDU[TFrame.APDU_START + 0] = (byte) (APCI.PHYSADDRSERNO_READ >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) (APCI.PHYSADDRSERNO_READ >> 16);
        for (int i = 7; i > 1; i--) {
            aPDU[TFrame.APDU_START + i] = (byte) serNo;
            serNo >>= 8;
        }
        return aPDU;
    }

    static byte[] makePhysAddrSerNoRes(long serNo, int domainAddr) {
        if ((serNo & 0xFFFF000000000000L) != 0) throw new IllegalArgumentException("'serNo' out of range!");
        if ((domainAddr & 0xFFFF0000) != 0) throw new IllegalArgumentException("'domainAddr' out of range!");
        byte[] aPDU = TFrame.getNew(11);
        aPDU[TFrame.APDU_START + 0] = (byte) (APCI.PHYSADDRSERNO_RES >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) (APCI.PHYSADDRSERNO_RES >> 16);
        for (int i = 7; i > 1; i--) {
            aPDU[TFrame.APDU_START + i] = (byte) serNo;
            serNo >>= 8;
        }
        aPDU[TFrame.APDU_START + 8] = (byte) (domainAddr >> 8);
        aPDU[TFrame.APDU_START + 9] = (byte) domainAddr;
        aPDU[TFrame.APDU_START + 10] = (byte) 0x00;
        aPDU[TFrame.APDU_START + 11] = (byte) 0x00;
        return aPDU;
    }

    static byte[] makePhysAddrSerNoWrite(long serNo, int physAddr) {
        if ((serNo & 0xFFFF000000000000L) != 0) throw new IllegalArgumentException("'serNo' out of range!");
        if ((physAddr & 0xFFFF0000) != 0) throw new IllegalArgumentException("'physAddr' out of range!");
        byte[] aPDU = TFrame.getNew(13);
        aPDU[TFrame.APDU_START + 0] = (byte) (APCI.PHYSADDRSERNO_WRITE >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) (APCI.PHYSADDRSERNO_WRITE >> 16);
        for (int i = 7; i > 1; i--) {
            aPDU[TFrame.APDU_START + i] = (byte) serNo;
            serNo >>= 8;
        }
        aPDU[TFrame.APDU_START + 8] = (byte) (physAddr >> 8);
        aPDU[TFrame.APDU_START + 9] = (byte) physAddr;
        for (int i = 10; i < 14; i++) {
            aPDU[TFrame.APDU_START + i] = (byte) 0x00;
        }
        return aPDU;
    }

    static long getPhysAddrSerNoSerNo(byte[] aPDU) {
        long serNo = 0;
        for (int i = 2; i < 8; i++) {
            serNo <<= 8;
            serNo += (aPDU[TFrame.APDU_START + i] & 0xFF);
        }
        return serNo;
    }

    static int getPhysAddrSerNoAddress(byte[] aPDU) {
        return ((aPDU[TFrame.APDU_START + 8] & 0xFF) << 8) + (aPDU[TFrame.APDU_START + 9] & 0XFF);
    }

    static byte[] makeDomainAddrSelRead(int domainAddr, int startAddr, int range) {
        if ((domainAddr & 0xFFFF0000) != 0) throw new IllegalArgumentException("'domainAddr' out of range!");
        if ((startAddr & 0xFFFF0000) != 0) throw new IllegalArgumentException("'startAddr' out of range!");
        if ((range & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'range' out of range!");
        byte[] aPDU = TFrame.getNew(6);
        aPDU[TFrame.APDU_START + 0] = (byte) (APCI.DOMAINADDRSEL_READ >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) (APCI.DOMAINADDRSEL_READ >> 16);
        aPDU[TFrame.APDU_START + 2] = (byte) (domainAddr >> 8);
        aPDU[TFrame.APDU_START + 3] = (byte) domainAddr;
        aPDU[TFrame.APDU_START + 4] = (byte) (startAddr >> 8);
        aPDU[TFrame.APDU_START + 5] = (byte) startAddr;
        aPDU[TFrame.APDU_START + 6] = (byte) range;
        return aPDU;
    }

    static int getDomainAddrSelReadDomainAddr(byte[] aPDU) {
        return ((aPDU[TFrame.APDU_START + 2] & 0xFF) << 8) + (aPDU[TFrame.APDU_START + 3] & 0XFF);
    }

    static int getDomainAddrSelReadStartAddr(byte[] aPDU) {
        return ((aPDU[TFrame.APDU_START + 4] & 0xFF) << 8) + (aPDU[TFrame.APDU_START + 5] & 0XFF);
    }

    static int getDomainAddrSelReadRange(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 6] & 0xFF;
    }

    static byte[] makeServiceInfoWrite(int info) {
        if ((info & 0xFF000000) != 0) throw new IllegalArgumentException("'info' out of range!");
        byte[] aPDU = TFrame.getNew(4);
        aPDU[TFrame.APDU_START + 0] = (byte) (APCI.SERVICEINFO_WRITE >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) (APCI.SERVICEINFO_WRITE >> 16);
        aPDU[TFrame.APDU_START + 2] = (byte) (info >> 16);
        aPDU[TFrame.APDU_START + 3] = (byte) (info >> 8);
        aPDU[TFrame.APDU_START + 4] = (byte) info;
        return aPDU;
    }

    static int getServiceInfoWriteInfo(byte[] aPDU) {
        return ((aPDU[TFrame.APDU_START + 2] & 0xFF) << 16) + ((aPDU[TFrame.APDU_START + 3] & 0XFF) << 8) + (aPDU[TFrame.APDU_START + 4] & 0XFF);
    }

    static byte[] makePropValue(int apci, int objIdx, int propID, int startIdx, int noElems, byte[] data) {
        if ((objIdx & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'objIdx' out of range!");
        if ((propID & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'propID' out of range!");
        if ((startIdx & 0xFFFFF000) != 0) throw new IllegalArgumentException("'startIdx' out of range!");
        if ((noElems & 0xFFFFFFF0) != 0) throw new IllegalArgumentException("'noElems' out of range!");
        byte[] aPDU = TFrame.getNew(5 + data.length);
        aPDU[TFrame.APDU_START + 0] = (byte) (apci >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) (apci >> 16);
        aPDU[TFrame.APDU_START + 2] = (byte) objIdx;
        aPDU[TFrame.APDU_START + 3] = (byte) propID;
        aPDU[TFrame.APDU_START + 4] = (byte) ((noElems << 4) | (startIdx >> 8));
        aPDU[TFrame.APDU_START + 5] = (byte) startIdx;
        System.arraycopy(data, 0, aPDU, TFrame.APDU_START + 6, data.length);
        return aPDU;
    }

    static int getPropValueObjIdx(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 2] & 0xFF;
    }

    static int getPropValuePropID(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 3] & 0xFF;
    }

    static int getPropValueNoElems(byte[] aPDU) {
        return (aPDU[TFrame.APDU_START + 4] & 0xF0) >> 4;
    }

    static int getPropValueStartIdx(byte[] aPDU) {
        return ((aPDU[TFrame.APDU_START + 4] & 0x0F) << 8) + (aPDU[TFrame.APDU_START + 5] & 0xFF);
    }

    static byte[] getPropValueData(byte[] aPDU, int length) {
        length -= 5;
        byte[] data = new byte[length];
        System.arraycopy(aPDU, TFrame.APDU_START + 6, data, 0, length);
        return data;
    }

    static byte[] makePropValueF(int apci, int objIdx, int propID, long startIdx, int noElems, byte[] data) {
        if ((objIdx & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'objIdx' out of range!");
        if ((propID & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'propID' out of range!");
        if ((noElems & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'noElems' out of range!");
        if ((startIdx & 0xFFFFFFFF00000000L) != 0) throw new IllegalArgumentException("'startIdx' out of range!");
        byte[] aPDU = TFrame.getNew(9 + data.length);
        aPDU[TFrame.APDU_START + 0] = (byte) (apci >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) (apci >> 16);
        aPDU[TFrame.APDU_START + 2] = (byte) (apci >> 8);
        aPDU[TFrame.APDU_START + 3] = (byte) objIdx;
        aPDU[TFrame.APDU_START + 4] = (byte) propID;
        aPDU[TFrame.APDU_START + 5] = (byte) noElems;
        aPDU[TFrame.APDU_START + 6] = (byte) (startIdx >> 24);
        aPDU[TFrame.APDU_START + 7] = (byte) (startIdx >> 16);
        aPDU[TFrame.APDU_START + 8] = (byte) (startIdx >> 8);
        aPDU[TFrame.APDU_START + 9] = (byte) startIdx;
        System.arraycopy(data, 0, aPDU, TFrame.APDU_START + 10, data.length);
        return aPDU;
    }

    static int getPropValueFObjIdx(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 3] & 0xFF;
    }

    static int getPropValueFPropID(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 4] & 0xFF;
    }

    static int getPropValueFNoElems(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 5] & 0xFF;
    }

    static int getPropValueFStartIdx(byte[] aPDU) {
        return ((aPDU[TFrame.APDU_START + 6] & 0xFF) << 24) + ((aPDU[TFrame.APDU_START + 7] & 0xFF) << 16) + ((aPDU[TFrame.APDU_START + 8] & 0xFF) << 8) + (aPDU[TFrame.APDU_START + 9] & 0xFF);
    }

    static byte[] getPropValueFData(byte[] aPDU, int length) {
        length -= 9;
        byte[] data = new byte[length];
        System.arraycopy(aPDU, TFrame.APDU_START + 10, data, 0, length);
        return data;
    }

    static byte[] makePropDescrRead(int objIdx, int propID, int propIdx) {
        if ((objIdx & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'objIdx' out of range!");
        if ((propID & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'propID' out of range!");
        if ((propIdx & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'propIdx' out of range!");
        byte[] aPDU = TFrame.getNew(4);
        aPDU[TFrame.APDU_START + 0] = (byte) (APCI.PROPERTYDESCR_READ >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) (APCI.PROPERTYDESCR_READ >> 16);
        aPDU[TFrame.APDU_START + 2] = (byte) objIdx;
        aPDU[TFrame.APDU_START + 3] = (byte) propID;
        aPDU[TFrame.APDU_START + 4] = (byte) propIdx;
        return aPDU;
    }

    static byte[] makePropDescrRes(int objIdx, int propID, int propIdx, int type, int maxNoElems, int readLevel, int writeLevel) {
        if ((objIdx & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'objIdx' out of range!");
        if ((propID & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'propID' out of range!");
        if ((propIdx & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'propIdx' out of range!");
        if ((type & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'type' out of range!");
        if ((maxNoElems & 0xFFFF0000) != 0) throw new IllegalArgumentException("'maxNoElems' out of range!");
        if ((readLevel & 0xFFFFFFF0) != 0) throw new IllegalArgumentException("'readLevel' out of range!");
        if ((writeLevel & 0xFFFFFFF0) != 0) throw new IllegalArgumentException("'writeLevel' out of range!");
        byte[] aPDU = TFrame.getNew(8);
        aPDU[TFrame.APDU_START + 0] = (byte) (APCI.PROPERTYDESCR_RES >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) (APCI.PROPERTYDESCR_RES >> 16);
        aPDU[TFrame.APDU_START + 2] = (byte) objIdx;
        aPDU[TFrame.APDU_START + 3] = (byte) propID;
        aPDU[TFrame.APDU_START + 4] = (byte) propIdx;
        aPDU[TFrame.APDU_START + 5] = (byte) type;
        aPDU[TFrame.APDU_START + 6] = (byte) (maxNoElems >> 8);
        aPDU[TFrame.APDU_START + 7] = (byte) maxNoElems;
        aPDU[TFrame.APDU_START + 8] = (byte) ((readLevel << 4) | writeLevel);
        return aPDU;
    }

    static int getPropDescrObjIdx(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 2] & 0xFF;
    }

    static int getPropDescrPropID(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 3] & 0xFF;
    }

    static int getPropDescrPropIdx(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 4] & 0xFF;
    }

    static int getPropDescrType(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 5] & 0xFF;
    }

    static int getPropDescrMaxNoElems(byte[] aPDU) {
        return ((aPDU[TFrame.APDU_START + 6] & 0xFF) << 8) + (aPDU[TFrame.APDU_START + 7] & 0xFF);
    }

    static int getPropDescrReadLevel(byte[] aPDU) {
        return (aPDU[TFrame.APDU_START + 8] & 0xF0) >> 4;
    }

    static int getPropDescrWriteLevel(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 8] & 0x0F;
    }

    static byte[] makeMemory(int apci, int memAddr, int noBytes, byte[] data) {
        if ((noBytes & 0xFFFFFFF0) != 0) throw new IllegalArgumentException("'noBytes' out of range!");
        if ((memAddr & 0xFFFF0000) != 0) throw new IllegalArgumentException("'memAddr' out of range!");
        byte[] aPDU = TFrame.getNew(3 + data.length);
        aPDU[TFrame.APDU_START + 0] = (byte) (apci >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) ((apci >> 16) | noBytes);
        aPDU[TFrame.APDU_START + 2] = (byte) (memAddr >> 8);
        aPDU[TFrame.APDU_START + 3] = (byte) memAddr;
        System.arraycopy(data, 0, aPDU, TFrame.APDU_START + 4, data.length);
        return aPDU;
    }

    static int getMemoryMemAddr(byte[] aPDU) {
        return ((aPDU[TFrame.APDU_START + 2] & 0xFF) << 8) + (aPDU[TFrame.APDU_START + 3] & 0xFF);
    }

    static int getMemoryNoBytes(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 1] & 0x0F;
    }

    static byte[] getMemoryData(byte[] aPDU, int noBytes) {
        byte[] data = new byte[noBytes];
        System.arraycopy(aPDU, TFrame.APDU_START + 4, data, 0, noBytes);
        return data;
    }

    static byte[] makeUserMemory(int apci, int memAddr, int noBytes, byte[] data) {
        if ((noBytes & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'noBytes' out of range!");
        if ((memAddr & 0xFFFF0000) != 0) throw new IllegalArgumentException("'memAddr' out of range!");
        byte[] aPDU = TFrame.getNew(4 + data.length);
        aPDU[TFrame.APDU_START + 0] = (byte) (apci >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) (apci >> 16);
        aPDU[TFrame.APDU_START + 2] = (byte) noBytes;
        aPDU[TFrame.APDU_START + 3] = (byte) (memAddr >> 8);
        aPDU[TFrame.APDU_START + 4] = (byte) memAddr;
        System.arraycopy(data, 0, aPDU, TFrame.APDU_START + 5, data.length);
        return aPDU;
    }

    static int getUserMemoryMemAddr(byte[] aPDU) {
        return ((aPDU[TFrame.APDU_START + 3] & 0xFF) << 8) + (aPDU[TFrame.APDU_START + 4] & 0xFF);
    }

    static int getUserMemoryNoBytes(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 2] & 0xFF;
    }

    static byte[] getUserMemoryData(byte[] aPDU, int noBytes) {
        byte[] data = new byte[noBytes];
        System.arraycopy(aPDU, TFrame.APDU_START + 5, data, 0, noBytes);
        return data;
    }

    static byte[] makeMemBitWrite(int apci, int memAddr, byte[] andData, byte[] xorData) {
        if ((memAddr & 0xFFFF0000) != 0) throw new IllegalArgumentException("'memAddr' out of range!");
        int length = andData.length;
        if (length != xorData.length) throw new IllegalArgumentException("'andData' and 'xorData' must be same size!");
        if (length >= 256) throw new IllegalArgumentException("Length of data must smaller than 256!");
        byte[] aPDU = TFrame.getNew(4 + 2 * length);
        aPDU[TFrame.APDU_START + 0] = (byte) (apci >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) (apci >> 16);
        aPDU[TFrame.APDU_START + 2] = (byte) length;
        aPDU[TFrame.APDU_START + 3] = (byte) (memAddr >> 8);
        aPDU[TFrame.APDU_START + 4] = (byte) memAddr;
        System.arraycopy(andData, 0, aPDU, TFrame.APDU_START + 5, length);
        System.arraycopy(xorData, 0, aPDU, TFrame.APDU_START + 5 + length, length);
        return aPDU;
    }

    static int getMemBitWriteMemAddr(byte[] aPDU) {
        return ((aPDU[TFrame.APDU_START + 3] & 0xFF) << 8) + (aPDU[TFrame.APDU_START + 4] & 0xFF);
    }

    static int getMemBitWriteNoBytes(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 2] & 0xFF;
    }

    static byte[] getMemBitWriteANDData(byte[] aPDU, int noBytes) {
        byte[] data = new byte[noBytes];
        System.arraycopy(aPDU, TFrame.APDU_START + 5, data, 0, noBytes);
        return data;
    }

    static byte[] getMemBitWriteXORData(byte[] aPDU, int noBytes) {
        byte[] data = new byte[noBytes];
        System.arraycopy(aPDU, TFrame.APDU_START + 5 + noBytes, data, 0, noBytes);
        return data;
    }

    static byte[] makeDeviceDescrReadRes(int maskVersion) {
        if ((maskVersion & 0xFFFF0000) != 0) throw new IllegalArgumentException("'maskVersion' out of range!");
        byte[] aPDU = TFrame.getNew(3);
        aPDU[TFrame.APDU_START + 0] = (byte) (APCI.DEVICEDESCR_RES >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) (APCI.DEVICEDESCR_RES >> 16);
        aPDU[TFrame.APDU_START + 2] = (byte) (maskVersion >> 8);
        aPDU[TFrame.APDU_START + 3] = (byte) maskVersion;
        return aPDU;
    }

    static int getDeviceDescrMaskVersion(byte[] aPDU) {
        return ((aPDU[TFrame.APDU_START + 2] & 0xFF) << 8) + (aPDU[TFrame.APDU_START + 3] & 0xFF);
    }

    static byte[] makeUserMfactInfoReadRes(int mfactID, int mfactInfo) {
        if ((mfactID & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'mfactID' out of range!");
        if ((mfactInfo & 0xFFFF0000) != 0) throw new IllegalArgumentException("'mfactInfo' out of range!");
        byte[] aPDU = TFrame.getNew(4);
        aPDU[TFrame.APDU_START + 0] = (byte) (APCI.USERMFACTINFO_RES >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) (APCI.USERMFACTINFO_RES >> 16);
        aPDU[TFrame.APDU_START + 2] = (byte) mfactID;
        aPDU[TFrame.APDU_START + 3] = (byte) (mfactInfo >> 8);
        aPDU[TFrame.APDU_START + 4] = (byte) mfactInfo;
        return aPDU;
    }

    static int getUserMfactInfoMfactID(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 2] & 0xFF;
    }

    static int getUserMfactInfoMfactInfo(byte[] aPDU) {
        return ((aPDU[TFrame.APDU_START + 3] & 0xFF) << 8) + (aPDU[TFrame.APDU_START + 4] & 0xFF);
    }

    static byte[] makeADCReadReq(int channelNo, int readCount) {
        if ((channelNo & 0xFFFFFFC0) != 0) throw new IllegalArgumentException("'channelNo' out of range!");
        if ((readCount & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'readCount' out of range!");
        byte[] aPDU = TFrame.getNew(2);
        aPDU[TFrame.APDU_START + 0] = (byte) (APCI.ADC_READ >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) ((APCI.ADC_READ >> 16) | channelNo);
        aPDU[TFrame.APDU_START + 2] = (byte) readCount;
        return aPDU;
    }

    static byte[] makeADCReadRes(int channelNo, int readCount, int sum) {
        if ((channelNo & 0xFFFFFFC0) != 0) throw new IllegalArgumentException("'channelNo' out of range!");
        if ((readCount & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'readCount' out of range!");
        if ((sum & 0xFFFF0000) != 0) throw new IllegalArgumentException("'sum' out of range!");
        byte[] aPDU = TFrame.getNew(4);
        aPDU[TFrame.APDU_START + 0] = (byte) (APCI.ADC_RES >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) ((APCI.ADC_RES >> 16) | channelNo);
        aPDU[TFrame.APDU_START + 2] = (byte) readCount;
        aPDU[TFrame.APDU_START + 3] = (byte) (sum >> 8);
        aPDU[TFrame.APDU_START + 4] = (byte) sum;
        return aPDU;
    }

    static int getADCReadChannelNo(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 1] & 0x3F;
    }

    static int getADCReadReadCount(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 2] & 0xFF;
    }

    static int getADCReadSum(byte[] aPDU) {
        return ((aPDU[TFrame.APDU_START + 3] & 0xFF) << 8) + (aPDU[TFrame.APDU_START + 4] & 0xFF);
    }

    static byte[] makeAuthorizeReq(int apci, int level, int key) {
        if ((level & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'level' out of range!");
        byte[] aPDU = TFrame.getNew(6);
        aPDU[TFrame.APDU_START + 0] = (byte) (apci >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) (apci >> 16);
        aPDU[TFrame.APDU_START + 2] = (byte) level;
        aPDU[TFrame.APDU_START + 3] = (byte) (key >> 24);
        aPDU[TFrame.APDU_START + 4] = (byte) (key >> 16);
        aPDU[TFrame.APDU_START + 5] = (byte) (key >> 8);
        aPDU[TFrame.APDU_START + 6] = (byte) key;
        return aPDU;
    }

    static byte[] makeAuthorizeRes(int apci, int level) {
        if ((level & 0xFFFFFF00) != 0) throw new IllegalArgumentException("'level' out of range!");
        byte[] aPDU = TFrame.getNew(2);
        aPDU[TFrame.APDU_START + 0] = (byte) (apci >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) (apci >> 16);
        aPDU[TFrame.APDU_START + 2] = (byte) level;
        return aPDU;
    }

    static int getAuthorizeLevel(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 2] & 0xFF;
    }

    static int getAuthorizeKey(byte[] aPDU) {
        return ((aPDU[TFrame.APDU_START + 3] & 0xFF) << 24) + ((aPDU[TFrame.APDU_START + 4] & 0xFF) << 16) + ((aPDU[TFrame.APDU_START + 5] & 0xFF) << 8) + (aPDU[TFrame.APDU_START + 6] & 0xFF);
    }

    static byte[] makeGrpRouteConfig(int apci, int subToMain, int mainToSub) {
        if ((subToMain & 0xFFFFFFF0) != 0) throw new IllegalArgumentException("'subToMain' out of range!");
        if ((mainToSub & 0xFFFFFFF0) != 0) throw new IllegalArgumentException("'mainToSub' out of range!");
        byte[] aPDU = TFrame.getNew(2);
        aPDU[TFrame.APDU_START + 0] = (byte) (apci >> 24);
        aPDU[TFrame.APDU_START + 1] = (byte) (apci >> 16);
        aPDU[TFrame.APDU_START + 2] = (byte) ((subToMain << 4) | mainToSub);
        return aPDU;
    }

    static int getGrpRouteConfigSubToMain(byte[] aPDU) {
        return (aPDU[TFrame.APDU_START + 2] & 0xF0) >> 4;
    }

    static int getGrpRouteConfigMainToSub(byte[] aPDU) {
        return aPDU[TFrame.APDU_START + 2] & 0x0F;
    }
}
