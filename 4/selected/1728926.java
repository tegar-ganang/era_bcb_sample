package eibstack;

import eibstack.layer7.A_Connection;
import java.io.IOException;

public class ConnectionImpl extends OutConnectionImpl implements Connection {

    ConnectionImpl(int ca, int pr, int hc, A_Connection c) {
        super(ca, pr, hc, c);
    }

    ConnectionImpl(int ca, int pr, int hc, Connection.Listener l) {
        super(ca, pr, hc, l);
    }

    void setListener(Connection.Listener l) {
        super.setListener(l);
    }

    public synchronized void propertyValue_ReadInd(int pr, int hc, int objIdx, int propID, int startIdx, int noElems) {
        byte[] data = ((Connection.Listener) ocl).readPropertyValue(pr, hc, objIdx, propID, startIdx, noElems);
        if (data == null) {
            noElems = 0;
            data = new byte[0];
        }
        ac.propertyValue_ReadRes(pr, hopCount, objIdx, propID, startIdx, noElems, data);
    }

    public synchronized void propertyValue_FReadInd(int pr, int hc, int objIdx, int propID, long startIdx, int noElems) {
        byte[] data = ((Connection.Listener) ocl).readPropertyValue(pr, hc, objIdx, propID, startIdx, noElems);
        if (data == null) {
            noElems = 0;
            data = new byte[0];
        }
        ac.propertyValue_FReadRes(pr, hopCount, objIdx, propID, startIdx, noElems, data);
    }

    public synchronized void propertyValue_WriteInd(int pr, int hc, int objIdx, int propID, int startIdx, int noElems, byte[] data) {
        data = ((Connection.Listener) ocl).writePropertyValue(pr, hc, objIdx, propID, startIdx, noElems, data);
        if (data == null) {
            noElems = 0;
            data = new byte[0];
        }
        ac.propertyValue_ReadRes(pr, hopCount, objIdx, propID, startIdx, noElems, data);
    }

    public synchronized void propertyValue_FWriteInd(int pr, int hc, int objIdx, int propID, long startIdx, int noElems, byte[] data) {
        data = ((Connection.Listener) ocl).writePropertyValue(pr, hc, objIdx, propID, startIdx, noElems, data);
        if (data == null) {
            noElems = 0;
            data = new byte[0];
        }
        ac.propertyValue_FWriteRes(pr, hopCount, objIdx, propID, startIdx, noElems, data);
    }

    public synchronized void propertyDescr_ReadInd(int pr, int hc, int objIdx, int propID, int propIdx) {
        PropertyDescr descr = ((Connection.Listener) ocl).readPropertyDescr(pr, hc, objIdx, propID, propIdx);
        if (descr != null) {
            ac.propertyDescr_ReadRes(pr, hopCount, objIdx, propID, propIdx, 0, 0, 0, 0);
        } else {
            ac.propertyDescr_ReadRes(pr, hopCount, descr.objectIdx, descr.propertyID, descr.propertyIdx, descr.type, descr.maxNoElems, descr.readLevel, descr.writeLevel);
        }
    }

    public synchronized void memory_ReadInd(int pr, int hc, int memAddr, int noBytes) {
        byte[] data = ((Connection.Listener) ocl).readMemory(pr, hc, memAddr, noBytes);
        if (data == null) data = new byte[0];
        ac.memory_ReadRes(pr, hopCount, memAddr, data);
    }

    public synchronized void memory_WriteInd(int pr, int hc, int memAddr, byte[] data) {
        ((Connection.Listener) ocl).writeMemory(pr, hc, memAddr, data);
    }

    public synchronized void memBit_WriteInd(int pr, int hc, int memAddr, byte[] andData, byte[] xorData) {
        ((Connection.Listener) ocl).writeMemBit(pr, hc, memAddr, andData, xorData);
    }

    public synchronized void userMemory_ReadInd(int pr, int hc, int memAddr, int noBytes) {
        byte[] data = ((Connection.Listener) ocl).readUserMemory(pr, hc, memAddr, noBytes);
        if (data == null) data = new byte[0];
        ac.userMemory_ReadRes(pr, hopCount, memAddr, data);
    }

    public synchronized void userMemory_WriteInd(int pr, int hc, int memAddr, byte[] data) {
        ((Connection.Listener) ocl).writeUserMemory(pr, hc, memAddr, data);
    }

    public synchronized void userMemBit_WriteInd(int pr, int hc, int memAddr, byte[] andData, byte[] xorData) {
        ((Connection.Listener) ocl).writeUserMemBit(pr, hc, memAddr, andData, xorData);
    }

    public synchronized void deviceDescr_ReadInd(int pr, int hc) {
        ac.deviceDescr_ReadRes(pr, hopCount, ((Connection.Listener) ocl).readDeviceDescr(pr, hc));
    }

    public synchronized void userMfactInfo_ReadInd(int pr, int hc) {
        MfactInfo info = ((Connection.Listener) ocl).readUserMfactInfo(pr, hc);
        ac.userMfactInfo_ReadRes(pr, hopCount, info.mfactID, info.mfactInfo);
    }

    public synchronized void adc_ReadInd(int pr, int hc, int channelNo, int readCount) {
        int sum = ((Connection.Listener) ocl).readADC(pr, hc, channelNo, readCount);
        if (sum < 0) {
            readCount = 0;
            sum = 0;
        }
        ac.adc_ReadRes(pr, hopCount, channelNo, readCount, sum);
    }

    public synchronized void restart_Ind(int pr, int hc) {
        ((Connection.Listener) ocl).restart(pr, hc);
    }

    public synchronized void authorize_Ind(int pr, int hc, int key) {
        ac.authorize_Res(pr, hopCount, ((Connection.Listener) ocl).authorize(pr, hc, key));
    }

    public synchronized void key_WriteInd(int pr, int hc, int level, int key) {
        ac.key_WriteRes(pr, hopCount, ((Connection.Listener) ocl).writeKey(pr, hc, level, key));
    }

    public synchronized void enableExternMemory_Ind(int pr, int hc) {
        ((Connection.Listener) ocl).enableExternMemory(pr, hc);
    }

    public synchronized void externMemory_ReadInd(int pr, int hc, int memAddr, int noBytes) {
        byte[] data = ((Connection.Listener) ocl).readExternMemory(pr, hc, memAddr, noBytes);
        if (data == null) data = new byte[0];
        ac.externMemory_ReadRes(pr, hc, memAddr, data);
    }

    public synchronized void externMemory_WriteInd(int pr, int hc, int memAddr, byte[] data) {
        ((Connection.Listener) ocl).writeExternMemory(pr, hc, memAddr, data);
    }

    public synchronized void slaveMemory_ReadInd(int pr, int hc, int memAddr, int noBytes) {
        byte[] data = ((Connection.Listener) ocl).readSlaveMemory(pr, hc, memAddr, noBytes);
        if (data == null) data = new byte[0];
        ac.slaveMemory_ReadRes(pr, hc, memAddr, data);
    }

    public synchronized void slaveMemory_WriteInd(int pr, int hc, int memAddr, byte[] data) {
        ((Connection.Listener) ocl).writeSlaveMemory(pr, hc, memAddr, data);
    }

    public synchronized void grpRouteConfig_ReadInd(int pr, int hc) {
        GrpRouteConfig grc = ((Connection.Listener) ocl).readGrpRouteConfig(pr, hc);
        if (grc != null) {
            ac.grpRouteConfig_ReadRes(pr, hc, grc.subToMain, grc.mainToSub);
        } else {
            ac.grpRouteConfig_ReadRes(pr, hc, GrpRouteConfig.DONT_CARE, GrpRouteConfig.DONT_CARE);
        }
    }

    public synchronized void grpRouteConfig_WriteInd(int pr, int hc, int subToMain, int mainToSub) {
        GrpRouteConfig grc = new GrpRouteConfig(subToMain, mainToSub);
        ((Connection.Listener) ocl).writeGrpRouteConfig(pr, hc, grc);
    }
}
