package eibstack.layer7;

import eibstack.layer4.T_DataUnackService;
import eibstack.transceiver.TFrame;

public class A_DataConnlessImpl implements A_DataConnlessService, T_DataUnackService.Listener {

    private T_DataUnackService tus;

    private Listener adul;

    public A_DataConnlessImpl(T_DataUnackService tus) {
        this.tus = tus;
        tus.setListener(this);
    }

    public void setListener(Listener l) {
        adul = l;
    }

    public int propertyValue_ReadReq(int da, int pr, int hc, int objIdx, int propID, int startIdx, int noElems, boolean waitL2Con) {
        byte[] aPDU = APDU.makePropValue(APCI.PROPERTYVALUE_READ, objIdx, propID, startIdx, noElems, new byte[0]);
        return tus.dataUnack_Req(da, pr, hc, aPDU, waitL2Con);
    }

    public int propertyValue_ReadRes(int da, int pr, int hc, int objIdx, int propID, int startIdx, int noElems, byte[] data, boolean waitL2Con) {
        byte[] aPDU = APDU.makePropValue(APCI.PROPERTYVALUE_RES, objIdx, propID, startIdx, noElems, data);
        return tus.dataUnack_Req(da, pr, hc, aPDU, waitL2Con);
    }

    public int propertyValue_WriteReq(int da, int pr, int hc, int objIdx, int propID, int startIdx, int noElems, byte[] data, boolean waitL2Con) {
        byte[] aPDU = APDU.makePropValue(APCI.PROPERTYVALUE_WRITE, objIdx, propID, startIdx, noElems, data);
        return tus.dataUnack_Req(da, pr, hc, aPDU, waitL2Con);
    }

    public int propertyValue_FReadReq(int da, int pr, int hc, int objIdx, int propID, long startIdx, int noElems, boolean waitL2Con) {
        byte[] aPDU = APDU.makePropValueF(APCI.FREAD_PROPERTY_REQ, objIdx, propID, startIdx, noElems, new byte[0]);
        return tus.dataUnack_Req(da, pr, hc, aPDU, waitL2Con);
    }

    public int propertyValue_FReadRes(int da, int pr, int hc, int objIdx, int propID, long startIdx, int noElems, byte[] data, boolean waitL2Con) {
        byte[] aPDU = APDU.makePropValueF(APCI.FREAD_PROPERTY_RES, objIdx, propID, startIdx, noElems, data);
        return tus.dataUnack_Req(da, pr, hc, aPDU, waitL2Con);
    }

    public int propertyValue_FWriteReq(int da, int pr, int hc, int objIdx, int propID, long startIdx, int noElems, byte[] data, boolean waitL2Con) {
        byte[] aPDU = APDU.makePropValueF(APCI.FWRITE_PROPERTY_REQ, objIdx, propID, startIdx, noElems, data);
        return tus.dataUnack_Req(da, pr, hc, aPDU, waitL2Con);
    }

    public int propertyValue_FWriteRes(int da, int pr, int hc, int objIdx, int propID, long startIdx, int noElems, byte[] data, boolean waitL2Con) {
        byte[] aPDU = APDU.makePropValueF(APCI.FWRITE_PROPERTY_RES, objIdx, propID, startIdx, noElems, data);
        return tus.dataUnack_Req(da, pr, hc, aPDU, waitL2Con);
    }

    public int propertyDescr_ReadReq(int da, int pr, int hc, int objIdx, int propID, int propIdx, boolean waitL2Con) {
        byte[] aPDU = APDU.makePropDescrRead(objIdx, propID, propIdx);
        return tus.dataUnack_Req(da, pr, hc, aPDU, waitL2Con);
    }

    public int propertyDescr_ReadRes(int da, int pr, int hc, int objIdx, int propID, int propIdx, int type, int maxNoElems, int readLevel, int writeLevel, boolean waitL2Con) {
        byte[] aPDU = APDU.makePropDescrRes(objIdx, propID, propIdx, type, maxNoElems, readLevel, writeLevel);
        return tus.dataUnack_Req(da, pr, hc, aPDU, waitL2Con);
    }

    public void dataUnack_Ind(int sa, int pr, int hc, byte[] tSDU) {
        if (adul == null) return;
        int length = tSDU.length - TFrame.MIN_LENGTH;
        if (length < 1) return;
        int apci = ((tSDU[TFrame.APDU_START + 0] & 0x03) << 24) + ((tSDU[TFrame.APDU_START + 1] & 0xFF) << 16);
        switch(apci & APCI._10) {
            case APCI.PROPERTYVALUE_READ:
                if (length == 5) {
                    int objIdx = APDU.getPropValueObjIdx(tSDU);
                    int propID = APDU.getPropValuePropID(tSDU);
                    int startIdx = APDU.getPropValueStartIdx(tSDU);
                    int noElems = APDU.getPropValueNoElems(tSDU);
                    adul.propertyValue_ReadInd(sa, pr, hc, objIdx, propID, startIdx, noElems);
                }
                return;
            case APCI.PROPERTYVALUE_RES:
                if (length >= 5) {
                    int objIdx = APDU.getPropValueObjIdx(tSDU);
                    int propID = APDU.getPropValuePropID(tSDU);
                    int startIdx = APDU.getPropValueStartIdx(tSDU);
                    int noElems = APDU.getPropValueNoElems(tSDU);
                    byte[] data = APDU.getPropValueData(tSDU, length);
                    adul.propertyValue_ReadCon(sa, pr, hc, objIdx, propID, startIdx, noElems, data);
                }
                return;
            case APCI.PROPERTYVALUE_WRITE:
                if (length >= 5) {
                    int objIdx = APDU.getPropValueObjIdx(tSDU);
                    int propID = APDU.getPropValuePropID(tSDU);
                    int startIdx = APDU.getPropValueStartIdx(tSDU);
                    int noElems = APDU.getPropValueNoElems(tSDU);
                    byte[] data = APDU.getPropValueData(tSDU, length);
                    adul.propertyValue_WriteInd(sa, pr, hc, objIdx, propID, startIdx, noElems, data);
                }
                return;
            case APCI.PROPERTYDESCR_READ:
                if (length == 4) {
                    int objIdx = APDU.getPropDescrObjIdx(tSDU);
                    int propID = APDU.getPropDescrPropID(tSDU);
                    int propIdx = APDU.getPropDescrPropIdx(tSDU);
                    adul.propertyDescr_ReadInd(sa, pr, hc, objIdx, propID, propIdx);
                }
                return;
            case APCI.PROPERTYDESCR_RES:
                if (length == 8) {
                    int objIdx = APDU.getPropDescrObjIdx(tSDU);
                    int propID = APDU.getPropDescrPropID(tSDU);
                    int propIdx = APDU.getPropDescrPropIdx(tSDU);
                    int type = APDU.getPropDescrType(tSDU);
                    int maxElems = APDU.getPropDescrMaxNoElems(tSDU);
                    int rLevel = APDU.getPropDescrReadLevel(tSDU);
                    int wLevel = APDU.getPropDescrWriteLevel(tSDU);
                    adul.propertyDescr_ReadCon(sa, pr, hc, objIdx, propID, propIdx, type, maxElems, rLevel, wLevel);
                }
                return;
        }
        if (length < TFrame.MIN_LENGTH + 2) return;
        apci += ((tSDU[TFrame.APDU_START + 2] & 0XFF) << 8);
        switch(apci & APCI._18) {
            case APCI.FREAD_PROPERTY_REQ:
                if (length == 9) {
                    int objIdx = APDU.getPropValueFObjIdx(tSDU);
                    int propID = APDU.getPropValueFPropID(tSDU);
                    long startIdx = APDU.getPropValueFStartIdx(tSDU);
                    int noElems = APDU.getPropValueFNoElems(tSDU);
                    adul.propertyValue_FReadInd(sa, pr, hc, objIdx, propID, startIdx, noElems);
                }
                return;
            case APCI.FREAD_PROPERTY_RES:
                if (length >= 9) {
                    int objIdx = APDU.getPropValueFObjIdx(tSDU);
                    int propID = APDU.getPropValueFPropID(tSDU);
                    long startIdx = APDU.getPropValueFStartIdx(tSDU);
                    int noElems = APDU.getPropValueFNoElems(tSDU);
                    byte[] data = APDU.getPropValueFData(tSDU, length);
                    adul.propertyValue_FReadCon(sa, pr, hc, objIdx, propID, startIdx, noElems, data);
                }
                return;
            case APCI.FWRITE_PROPERTY_REQ:
                if (length >= 9) {
                    int objIdx = APDU.getPropValueFObjIdx(tSDU);
                    int propID = APDU.getPropValueFPropID(tSDU);
                    long startIdx = APDU.getPropValueFStartIdx(tSDU);
                    int noElems = APDU.getPropValueFNoElems(tSDU);
                    byte[] data = APDU.getPropValueFData(tSDU, length);
                    adul.propertyValue_FWriteInd(sa, pr, hc, objIdx, propID, startIdx, noElems, data);
                }
                return;
            case APCI.FWRITE_PROPERTY_RES:
                if (length >= 9) {
                    int objIdx = APDU.getPropValueFObjIdx(tSDU);
                    int propID = APDU.getPropValueFPropID(tSDU);
                    long startIdx = APDU.getPropValueFStartIdx(tSDU);
                    int noElems = APDU.getPropValueFNoElems(tSDU);
                    byte[] data = APDU.getPropValueFData(tSDU, length);
                    adul.propertyValue_FWriteCon(sa, pr, hc, objIdx, propID, startIdx, noElems, data);
                }
                return;
        }
    }
}
