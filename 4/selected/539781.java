package jcontrol.eib.extended.a_layer;

import jcontrol.eib.extended.t_layer.T_Connection;
import jcontrol.eib.extended.transceiver.TFrame;

class A_ConnectionImpl implements A_Connection, T_Connection.Listener {

    private T_Connection tc = null;

    private Listener acl = null;

    void setConnection(T_Connection tc) {
        this.tc = tc;
    }

    void setListener(Listener l) {
        acl = l;
    }

    public void disconnect() {
        tc.disconnect();
        tc = null;
        acl = null;
    }

    public void disconnected() {
        acl.disconnected();
        tc = null;
        acl = null;
    }

    public int propertyValue_ReadReq(int pr, int hc, int objIdx, int propID, int startIdx, int noElems) {
        byte[] aPDU = APDU.makePropValue(APCI.PROPERTYVALUE_READ, objIdx, propID, startIdx, noElems, new byte[0]);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int propertyValue_ReadRes(int pr, int hc, int objIdx, int propID, int startIdx, int noElems, byte[] data) {
        byte[] aPDU = APDU.makePropValue(APCI.PROPERTYVALUE_RES, objIdx, propID, startIdx, noElems, data);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int propertyValue_WriteReq(int pr, int hc, int objIdx, int propID, int startIdx, int noElems, byte[] data) {
        byte[] aPDU = APDU.makePropValue(APCI.PROPERTYVALUE_WRITE, objIdx, propID, startIdx, noElems, data);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int propertyValue_FReadReq(int pr, int hc, int objIdx, int propID, long startIdx, int noElems) {
        byte[] aPDU = APDU.makePropValueF(APCI.FREAD_PROPERTY_REQ, objIdx, propID, startIdx, noElems, new byte[0]);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int propertyValue_FReadRes(int pr, int hc, int objIdx, int propID, long startIdx, int noElems, byte[] data) {
        byte[] aPDU = APDU.makePropValueF(APCI.FREAD_PROPERTY_RES, objIdx, propID, startIdx, noElems, data);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int propertyValue_FWriteReq(int pr, int hc, int objIdx, int propID, long startIdx, int noElems, byte[] data) {
        byte[] aPDU = APDU.makePropValueF(APCI.FWRITE_PROPERTY_REQ, objIdx, propID, startIdx, noElems, data);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int propertyValue_FWriteRes(int pr, int hc, int objIdx, int propID, long startIdx, int noElems, byte[] data) {
        byte[] aPDU = APDU.makePropValueF(APCI.FWRITE_PROPERTY_RES, objIdx, propID, startIdx, noElems, data);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int propertyDescr_ReadReq(int pr, int hc, int objIdx, int propID, int propIdx) {
        byte[] aPDU = APDU.makePropDescrRead(objIdx, propID, propIdx);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int propertyDescr_ReadRes(int pr, int hc, int objIdx, int propID, int propIdx, int type, int maxNoElems, int readLevel, int writeLevel) {
        byte[] aPDU = APDU.makePropDescrRes(objIdx, propID, propIdx, type, maxNoElems, readLevel, writeLevel);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int memory_ReadReq(int pr, int hc, int memAddr, int noBytes) {
        byte[] aPDU = APDU.makeMemory(APCI.MEMORY_READ, memAddr, noBytes, new byte[0]);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int memory_ReadRes(int pr, int hc, int memAddr, byte[] data) {
        byte[] aPDU = APDU.makeMemory(APCI.MEMORY_RES, memAddr, data.length, data);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int memory_WriteReq(int pr, int hc, int memAddr, byte[] data) {
        byte[] aPDU = APDU.makeMemory(APCI.MEMORY_WRITE, memAddr, data.length, data);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int memBit_WriteReq(int pr, int hc, int memAddr, byte[] andData, byte[] xorData) {
        byte[] aPDU = APDU.makeMemBitWrite(APCI.MEMORYBIT_WRITE, memAddr, andData, xorData);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int userMemory_ReadReq(int pr, int hc, int memAddr, int noBytes) {
        byte[] aPDU = APDU.makeUserMemory(APCI.USERMEMORY_READ, memAddr, noBytes, new byte[0]);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int userMemory_ReadRes(int pr, int hc, int memAddr, byte[] data) {
        byte[] aPDU = APDU.makeUserMemory(APCI.USERMEMORY_RES, memAddr, data.length, data);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int userMemory_WriteReq(int pr, int hc, int memAddr, byte[] data) {
        byte[] aPDU = APDU.makeUserMemory(APCI.USERMEMORY_WRITE, memAddr, data.length, data);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int userMemBit_WriteReq(int pr, int hc, int memAddr, byte[] andData, byte[] xorData) {
        byte[] aPDU = APDU.makeMemBitWrite(APCI.USERMEMORYBIT_WRITE, memAddr, andData, xorData);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int userMfactInfo_ReadReq(int pr, int hc) {
        byte[] aPDU = APDU.makeNoParamsReq(APCI.USERMFACTINFO_READ);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int userMfactInfo_ReadRes(int pr, int hc, int mfactID, int mfactInfo) {
        byte[] aPDU = APDU.makeUserMfactInfoReadRes(mfactID, mfactInfo);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int deviceDescr_ReadReq(int pr, int hc) {
        byte[] aPDU = APDU.makeNoParamsReq(APCI.DEVICEDESCR_READ);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int deviceDescr_ReadRes(int pr, int hc, int maskVersion) {
        byte[] aPDU = APDU.makeDeviceDescrReadRes(maskVersion);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int adc_ReadReq(int pr, int hc, int channelNo, int readCount) {
        byte[] aPDU = APDU.makeADCReadReq(channelNo, readCount);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int adc_ReadRes(int pr, int hc, int channelNo, int readCount, int sum) {
        byte[] aPDU = APDU.makeADCReadRes(channelNo, readCount, sum);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int restart_Req(int pr, int hc) {
        byte[] aPDU = APDU.makeNoParamsReq(APCI.RESTART);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int authorize_Req(int pr, int hc, int key) {
        byte[] aPDU = APDU.makeAuthorizeReq(APCI.AUTHORIZE_REQ, 0, key);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int authorize_Res(int pr, int hc, int level) {
        byte[] aPDU = APDU.makeAuthorizeRes(APCI.AUTHORIZE_RES, level);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int key_WriteReq(int pr, int hc, int level, int key) {
        byte[] aPDU = APDU.makeAuthorizeReq(APCI.KEY_WRITE, level, key);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int key_WriteRes(int pr, int hc, int level) {
        byte[] aPDU = APDU.makeAuthorizeRes(APCI.KEY_RES, level);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int enableExternMemory_Req(int pr, int hc) {
        byte[] aPDU = APDU.makeNoParamsReq(APCI.ENABLEEXTERNMEMORY);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int externMemory_ReadReq(int pr, int hc, int memAddr, int noBytes) {
        byte[] aPDU = APDU.makeUserMemory(APCI.EXTERNMEMORY_READ, memAddr, noBytes, new byte[0]);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int externMemory_ReadRes(int pr, int hc, int memAddr, byte[] data) {
        byte[] aPDU = APDU.makeUserMemory(APCI.EXTERNMEMORY_RES, memAddr, data.length, data);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int externMemory_WriteReq(int pr, int hc, int memAddr, byte[] data) {
        byte[] aPDU = APDU.makeUserMemory(APCI.EXTERNMEMORY_WRITE, memAddr, data.length, data);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int slaveMemory_ReadReq(int pr, int hc, int memAddr, int noBytes) {
        byte[] aPDU = APDU.makeUserMemory(APCI.SLAVEMEMORY_READ, memAddr, noBytes, new byte[0]);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int slaveMemory_ReadRes(int pr, int hc, int memAddr, byte[] data) {
        byte[] aPDU = APDU.makeUserMemory(APCI.SLAVEMEMORY_RES, memAddr, data.length, data);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int slaveMemory_WriteReq(int pr, int hc, int memAddr, byte[] data) {
        byte[] aPDU = APDU.makeUserMemory(APCI.SLAVEMEMORY_WRITE, memAddr, data.length, data);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int grpRouteConfig_ReadReq(int pr, int hc) {
        byte[] aPDU = APDU.makeNoParamsReq(APCI.GRPROUTECONFIG_READ);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int grpRouteConfig_ReadRes(int pr, int hc, int subToMain, int mainToSub) {
        byte[] aPDU = APDU.makeGrpRouteConfig(APCI.GRPROUTECONFIG_RES, subToMain, mainToSub);
        return tc.data_Req(pr, hc, aPDU);
    }

    public int grpRouteConfig_WriteReq(int pr, int hc, int subToMain, int mainToSub) {
        byte[] aPDU = APDU.makeGrpRouteConfig(APCI.GRPROUTECONFIG_WRITE, subToMain, mainToSub);
        return tc.data_Req(pr, hc, aPDU);
    }

    public boolean data_Ind(int pr, int hc, byte[] tSDU) {
        if (acl == null) return false;
        int length = tSDU.length - TFrame.MIN_LENGTH;
        if (length < 1) return false;
        int apci = ((tSDU[TFrame.APDU_START + 0] & 0x03) << 24) + ((tSDU[TFrame.APDU_START + 1] & 0xFF) << 16);
        switch(apci & APCI._4) {
            case APCI.ADC_READ:
                if (length == 2) {
                    int channelNo = APDU.getADCReadChannelNo(tSDU);
                    int readCount = APDU.getADCReadReadCount(tSDU);
                    return acl.adc_ReadInd(pr, hc, channelNo, readCount);
                }
                return false;
            case APCI.ADC_RES:
                if (length == 4) {
                    int channelNo = APDU.getADCReadChannelNo(tSDU);
                    int readCount = APDU.getADCReadReadCount(tSDU);
                    int sum = APDU.getADCReadSum(tSDU);
                    return acl.adc_ReadCon(pr, hc, channelNo, readCount, sum);
                }
                return false;
            case APCI.MEMORY_READ:
                if (length == 3) {
                    int memAddr = APDU.getMemoryMemAddr(tSDU);
                    int noBytes = APDU.getMemoryNoBytes(tSDU);
                    return acl.memory_ReadInd(pr, hc, memAddr, noBytes);
                }
                return false;
            case APCI.MEMORY_RES:
                if (length >= 3) {
                    int memAddr = APDU.getMemoryMemAddr(tSDU);
                    int noBytes = APDU.getMemoryNoBytes(tSDU);
                    if (length - 3 >= noBytes) {
                        byte[] data = APDU.getMemoryData(tSDU, noBytes);
                        return acl.memory_ReadCon(pr, hc, memAddr, data);
                    }
                }
                return false;
            case APCI.MEMORY_WRITE:
                if (length >= 3) {
                    int memAddr = APDU.getMemoryMemAddr(tSDU);
                    int noBytes = APDU.getMemoryNoBytes(tSDU);
                    if (length - 3 >= noBytes) {
                        byte[] data = APDU.getMemoryData(tSDU, noBytes);
                        return acl.memory_WriteInd(pr, hc, memAddr, data);
                    }
                }
                return false;
            case APCI.DEVICEDESCR_READ:
                if (length == 1) {
                    return acl.deviceDescr_ReadInd(pr, hc);
                }
                return false;
            case APCI.DEVICEDESCR_RES:
                if (length == 3) {
                    int maskVersion = APDU.getDeviceDescrMaskVersion(tSDU);
                    return acl.deviceDescr_ReadCon(pr, hc, maskVersion);
                }
                return false;
            case APCI.RESTART:
                if (length == 1) {
                    return acl.restart_Ind(pr, hc);
                }
                return false;
        }
        switch(apci & APCI._10) {
            case APCI.USERMEMORY_READ:
                if (length == 4) {
                    int memAddr = APDU.getUserMemoryMemAddr(tSDU);
                    int noBytes = APDU.getUserMemoryNoBytes(tSDU);
                    return acl.userMemory_ReadInd(pr, hc, memAddr, noBytes);
                }
                return false;
            case APCI.USERMEMORY_RES:
                if (length >= 4) {
                    int memAddr = APDU.getUserMemoryMemAddr(tSDU);
                    int noBytes = APDU.getUserMemoryNoBytes(tSDU);
                    if (length - 4 >= noBytes) {
                        byte[] data = APDU.getUserMemoryData(tSDU, noBytes);
                        return acl.userMemory_ReadCon(pr, hc, memAddr, data);
                    }
                }
                return false;
            case APCI.USERMEMORY_WRITE:
                if (length >= 4) {
                    int memAddr = APDU.getUserMemoryMemAddr(tSDU);
                    int noBytes = APDU.getUserMemoryNoBytes(tSDU);
                    if (length - 4 >= noBytes) {
                        byte[] data = APDU.getUserMemoryData(tSDU, noBytes);
                        return acl.userMemory_WriteInd(pr, hc, memAddr, data);
                    }
                }
                return false;
            case APCI.USERMEMORYBIT_WRITE:
                if (length >= 4) {
                    int memAddr = APDU.getMemBitWriteMemAddr(tSDU);
                    int noBytes = APDU.getMemBitWriteNoBytes(tSDU);
                    if (length - 4 >= 2 * noBytes) {
                        byte[] andData = APDU.getMemBitWriteANDData(tSDU, noBytes);
                        byte[] xorData = APDU.getMemBitWriteXORData(tSDU, noBytes);
                        return acl.userMemBit_WriteInd(pr, hc, memAddr, andData, xorData);
                    }
                }
                return false;
            case APCI.USERMFACTINFO_READ:
                if (length == 1) {
                    return acl.userMfactInfo_ReadInd(pr, hc);
                }
                return false;
            case APCI.USERMFACTINFO_RES:
                if (length == 4) {
                    int mfactID = APDU.getUserMfactInfoMfactID(tSDU);
                    int mfactInfo = APDU.getUserMfactInfoMfactInfo(tSDU);
                    return acl.userMfactInfo_ReadCon(pr, hc, mfactID, mfactInfo);
                }
                return false;
            case APCI.ENABLEEXTERNMEMORY:
                if (length == 1) {
                    return acl.enableExternMemory_Ind(pr, hc);
                }
                return false;
            case APCI.EXTERNMEMORY_READ:
                if (length == 4) {
                    int memAddr = APDU.getUserMemoryMemAddr(tSDU);
                    int noBytes = APDU.getUserMemoryNoBytes(tSDU);
                    return acl.externMemory_ReadInd(pr, hc, memAddr, noBytes);
                }
                return false;
            case APCI.EXTERNMEMORY_RES:
                if (length >= 4) {
                    int memAddr = APDU.getUserMemoryMemAddr(tSDU);
                    int noBytes = APDU.getUserMemoryNoBytes(tSDU);
                    if (length - 4 >= noBytes) {
                        byte[] data = APDU.getUserMemoryData(tSDU, noBytes);
                        return acl.externMemory_ReadCon(pr, hc, memAddr, data);
                    }
                }
                return false;
            case APCI.EXTERNMEMORY_WRITE:
                if (length >= 4) {
                    int memAddr = APDU.getUserMemoryMemAddr(tSDU);
                    int noBytes = APDU.getUserMemoryNoBytes(tSDU);
                    if (length - 4 >= noBytes) {
                        byte[] data = APDU.getUserMemoryData(tSDU, noBytes);
                        return acl.externMemory_WriteInd(pr, hc, memAddr, data);
                    }
                }
                return false;
            case APCI.SLAVEMEMORY_READ:
                if (length == 4) {
                    int memAddr = APDU.getUserMemoryMemAddr(tSDU);
                    int noBytes = APDU.getUserMemoryNoBytes(tSDU);
                    return acl.slaveMemory_ReadInd(pr, hc, memAddr, noBytes);
                }
                return false;
            case APCI.SLAVEMEMORY_RES:
                if (length >= 4) {
                    int memAddr = APDU.getUserMemoryMemAddr(tSDU);
                    int noBytes = APDU.getUserMemoryNoBytes(tSDU);
                    if (length - 4 >= noBytes) {
                        byte[] data = APDU.getUserMemoryData(tSDU, noBytes);
                        return acl.slaveMemory_ReadCon(pr, hc, memAddr, data);
                    }
                }
                return false;
            case APCI.SLAVEMEMORY_WRITE:
                if (length >= 4) {
                    int memAddr = APDU.getUserMemoryMemAddr(tSDU);
                    int noBytes = APDU.getUserMemoryNoBytes(tSDU);
                    if (length - 4 >= noBytes) {
                        byte[] data = APDU.getUserMemoryData(tSDU, noBytes);
                        return acl.slaveMemory_WriteInd(pr, hc, memAddr, data);
                    }
                }
                return false;
            case APCI.GRPROUTECONFIG_READ:
                if (length == 1) {
                    return acl.grpRouteConfig_ReadInd(pr, hc);
                }
                return false;
            case APCI.GRPROUTECONFIG_RES:
                if (length == 2) {
                    int subToMain = APDU.getGrpRouteConfigSubToMain(tSDU);
                    int mainToSub = APDU.getGrpRouteConfigMainToSub(tSDU);
                    return acl.grpRouteConfig_ReadCon(pr, hc, subToMain, mainToSub);
                }
                return false;
            case APCI.GRPROUTECONFIG_WRITE:
                if (length == 2) {
                    int subToMain = APDU.getGrpRouteConfigSubToMain(tSDU);
                    int mainToSub = APDU.getGrpRouteConfigMainToSub(tSDU);
                    return acl.grpRouteConfig_WriteInd(pr, hc, subToMain, mainToSub);
                }
                return false;
            case APCI.MEMORYBIT_WRITE:
                if (length >= 4) {
                    int memAddr = APDU.getMemBitWriteMemAddr(tSDU);
                    int noBytes = APDU.getMemBitWriteNoBytes(tSDU);
                    if (length - 4 >= 2 * noBytes) {
                        byte[] andData = APDU.getMemBitWriteANDData(tSDU, noBytes);
                        byte[] xorData = APDU.getMemBitWriteXORData(tSDU, noBytes);
                        return acl.memBit_WriteInd(pr, hc, memAddr, andData, xorData);
                    }
                }
                return false;
            case APCI.AUTHORIZE_REQ:
                if (length == 6) {
                    int key = APDU.getAuthorizeKey(tSDU);
                    return acl.authorize_Ind(pr, hc, key);
                }
                return false;
            case APCI.AUTHORIZE_RES:
                if (length == 2) {
                    int level = APDU.getAuthorizeLevel(tSDU);
                    return acl.authorize_Con(pr, hc, level);
                }
                return false;
            case APCI.KEY_WRITE:
                if (length == 6) {
                    int level = APDU.getAuthorizeLevel(tSDU);
                    int key = APDU.getAuthorizeKey(tSDU);
                    return acl.key_WriteInd(pr, hc, level, key);
                }
                return false;
            case APCI.KEY_RES:
                if (length == 2) {
                    int level = APDU.getAuthorizeLevel(tSDU);
                    return acl.key_WriteCon(pr, hc, level);
                }
                return false;
            case APCI.PROPERTYVALUE_READ:
                if (length == 5) {
                    int objIdx = APDU.getPropValueObjIdx(tSDU);
                    int propID = APDU.getPropValuePropID(tSDU);
                    int startIdx = APDU.getPropValueStartIdx(tSDU);
                    int noElems = APDU.getPropValueNoElems(tSDU);
                    return acl.propertyValue_ReadInd(pr, hc, objIdx, propID, startIdx, noElems);
                }
                return false;
            case APCI.PROPERTYVALUE_RES:
                if (length >= 5) {
                    int objIdx = APDU.getPropValueObjIdx(tSDU);
                    int propID = APDU.getPropValuePropID(tSDU);
                    int startIdx = APDU.getPropValueStartIdx(tSDU);
                    int noElems = APDU.getPropValueNoElems(tSDU);
                    byte[] data = APDU.getPropValueData(tSDU, length);
                    return acl.propertyValue_ReadCon(pr, hc, objIdx, propID, startIdx, noElems, data);
                }
                return false;
            case APCI.PROPERTYVALUE_WRITE:
                if (length >= 5) {
                    int objIdx = APDU.getPropValueObjIdx(tSDU);
                    int propID = APDU.getPropValuePropID(tSDU);
                    int startIdx = APDU.getPropValueStartIdx(tSDU);
                    int noElems = APDU.getPropValueNoElems(tSDU);
                    byte[] data = APDU.getPropValueData(tSDU, length);
                    return acl.propertyValue_WriteInd(pr, hc, objIdx, propID, startIdx, noElems, data);
                }
                return false;
            case APCI.PROPERTYDESCR_READ:
                if (length == 4) {
                    int objIdx = APDU.getPropDescrObjIdx(tSDU);
                    int propID = APDU.getPropDescrPropID(tSDU);
                    int propIdx = APDU.getPropDescrPropIdx(tSDU);
                    return acl.propertyDescr_ReadInd(pr, hc, objIdx, propID, propIdx);
                }
                return false;
            case APCI.PROPERTYDESCR_RES:
                if (length == 8) {
                    int objIdx = APDU.getPropDescrObjIdx(tSDU);
                    int propID = APDU.getPropDescrPropID(tSDU);
                    int propIdx = APDU.getPropDescrPropIdx(tSDU);
                    int type = APDU.getPropDescrType(tSDU);
                    int maxElems = APDU.getPropDescrMaxNoElems(tSDU);
                    int rLevel = APDU.getPropDescrReadLevel(tSDU);
                    int wLevel = APDU.getPropDescrWriteLevel(tSDU);
                    return acl.propertyDescr_ReadCon(pr, hc, objIdx, propID, propIdx, type, maxElems, rLevel, wLevel);
                }
                return false;
        }
        if (length < TFrame.MIN_LENGTH + 2) return false;
        apci += ((tSDU[TFrame.APDU_START + 2] & 0XFF) << 8);
        switch(apci & APCI._18) {
            case APCI.FREAD_PROPERTY_REQ:
                if (length == 9) {
                    int objIdx = APDU.getPropValueFObjIdx(tSDU);
                    int propID = APDU.getPropValueFPropID(tSDU);
                    long startIdx = APDU.getPropValueFStartIdx(tSDU);
                    int noElems = APDU.getPropValueFNoElems(tSDU);
                    return acl.propertyValue_FReadInd(pr, hc, objIdx, propID, startIdx, noElems);
                }
                return false;
            case APCI.FREAD_PROPERTY_RES:
                if (length >= 9) {
                    int objIdx = APDU.getPropValueFObjIdx(tSDU);
                    int propID = APDU.getPropValueFPropID(tSDU);
                    long startIdx = APDU.getPropValueFStartIdx(tSDU);
                    int noElems = APDU.getPropValueFNoElems(tSDU);
                    byte[] data = APDU.getPropValueFData(tSDU, length);
                    return acl.propertyValue_FReadCon(pr, hc, objIdx, propID, startIdx, noElems, data);
                }
                return false;
            case APCI.FWRITE_PROPERTY_REQ:
                if (length >= 9) {
                    int objIdx = APDU.getPropValueFObjIdx(tSDU);
                    int propID = APDU.getPropValueFPropID(tSDU);
                    long startIdx = APDU.getPropValueFStartIdx(tSDU);
                    int noElems = APDU.getPropValueFNoElems(tSDU);
                    byte[] data = APDU.getPropValueFData(tSDU, length);
                    return acl.propertyValue_FWriteInd(pr, hc, objIdx, propID, startIdx, noElems, data);
                }
                return false;
            case APCI.FWRITE_PROPERTY_RES:
                if (length >= 9) {
                    int objIdx = APDU.getPropValueFObjIdx(tSDU);
                    int propID = APDU.getPropValueFPropID(tSDU);
                    long startIdx = APDU.getPropValueFStartIdx(tSDU);
                    int noElems = APDU.getPropValueFNoElems(tSDU);
                    byte[] data = APDU.getPropValueFData(tSDU, length);
                    return acl.propertyValue_FWriteCon(pr, hc, objIdx, propID, startIdx, noElems, data);
                }
                return false;
        }
        return false;
    }
}
