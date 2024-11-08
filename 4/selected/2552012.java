package eibstack;

import eibstack.Result;
import eibstack.layer7.A_Connection;
import java.io.IOException;

class OutConnectionImpl implements OutConnection, A_Connection.Listener {

    protected static final int TIMEOUT = 10000;

    protected A_Connection ac;

    protected Listener ocl;

    protected int connAddr;

    protected int priority;

    protected int hopCount;

    OutConnectionImpl(int ca, int pr, int hc, A_Connection c) {
        ac = c;
        connAddr = ca;
        priority = pr;
        hopCount = hc;
    }

    OutConnectionImpl(int ca, int pr, int hc, Listener l) {
        ocl = l;
        connAddr = ca;
        priority = pr;
        hopCount = hc;
    }

    void setConnection(A_Connection c) {
        ac = c;
    }

    void setListener(Listener l) {
        ocl = l;
    }

    private static final int NONE = 0;

    private static final int PROPVAL_READ = 1;

    private static final int PROPVAL_FREAD = 2;

    private static final int PROPVAL_WRITE = 3;

    private static final int PROPVAL_FWRITE = 4;

    private static final int PROPDESCR_READ = 5;

    private static final int MEMORY_READ = 6;

    private static final int UMEMORY_READ = 7;

    private static final int DEVDESCR_READ = 8;

    private static final int UMFACTINF_READ = 9;

    private static final int ADC_READ = 10;

    private static final int AUTHORIZE_REQ = 11;

    private static final int KEY_WRITE = 12;

    private static final int EXTMEMORY_READ = 13;

    private static final int SLVMEMORY_READ = 14;

    private static final int GRPRTCNFG_READ = 15;

    private int waitingServiceID = NONE;

    private class PropRWArgs {

        private int objIdx;

        private int propID;

        private long startIdx;

        private int noElems;

        private byte[] data;

        private PropRWArgs(int objIdx, int propID, long startIdx, int noElems) {
            this.objIdx = objIdx;
            this.propID = propID;
            this.startIdx = startIdx;
            this.noElems = noElems;
            this.data = null;
        }
    }

    private class MemReadArgs {

        private int memAddr;

        private int noBytes;

        private byte[] data = null;

        private MemReadArgs(int memAddr, int noBytes) {
            this.memAddr = memAddr;
            this.noBytes = noBytes;
        }
    }

    private class ADCReadArgs {

        private int channelNo;

        private int readCount;

        private int sum = -1;

        private ADCReadArgs(int channelNo, int readCount) {
            this.channelNo = channelNo;
            this.readCount = readCount;
        }
    }

    private Object arguments;

    public synchronized void disconnect() throws IOException {
        if (waitingServiceID != NONE) throw new IOException();
        ac.disconnect();
        ac = null;
        ocl = null;
    }

    public synchronized void disconnected() {
        ocl.disconnected();
        ocl = null;
        ac = null;
        notify();
    }

    public byte[] readPropertyValue(int objIdx, int propID, long startIdx, int noElems) throws IOException {
        return readPropertyValue(priority, hopCount, objIdx, propID, startIdx, noElems);
    }

    public synchronized byte[] readPropertyValue(int pr, int hc, int objIdx, int propID, long startIdx, int noElems) throws IOException {
        if (waitingServiceID != NONE) throw new IOException();
        PropRWArgs args = new PropRWArgs(objIdx, propID, startIdx, noElems);
        arguments = args;
        int result;
        if ((startIdx < 4096) && (noElems < 16)) {
            waitingServiceID = PROPVAL_READ;
            result = ac.propertyValue_ReadReq(pr, hc, objIdx, propID, (int) startIdx, noElems);
        } else {
            waitingServiceID = PROPVAL_FREAD;
            result = ac.propertyValue_FReadReq(pr, hc, objIdx, propID, startIdx, noElems);
        }
        if ((result == Result.OK) && (args.data == null)) {
            try {
                wait(TIMEOUT);
            } catch (InterruptedException e) {
            }
        }
        arguments = null;
        waitingServiceID = NONE;
        if ((result == Result.OK) && (args.data != null)) {
            if ((args.data.length > 0) || (noElems == 0)) {
                return args.data;
            } else {
                return null;
            }
        } else {
            throw new IOException();
        }
    }

    public byte[] writePropertyValue(int objIdx, int propID, long startIdx, int noElems, byte[] data) throws IOException {
        return writePropertyValue(priority, hopCount, objIdx, propID, startIdx, noElems, data);
    }

    public synchronized byte[] writePropertyValue(int pr, int hc, int objIdx, int propID, long startIdx, int noElems, byte[] data) throws IOException {
        if (waitingServiceID != NONE) throw new IOException();
        PropRWArgs args = new PropRWArgs(objIdx, propID, startIdx, noElems);
        arguments = args;
        int result;
        if ((startIdx < 4096) && (noElems < 16)) {
            waitingServiceID = PROPVAL_WRITE;
            result = ac.propertyValue_WriteReq(pr, hc, objIdx, propID, (int) startIdx, noElems, data);
        } else {
            waitingServiceID = PROPVAL_FWRITE;
            result = ac.propertyValue_FWriteReq(pr, hc, objIdx, propID, startIdx, noElems, data);
        }
        if ((result == Result.OK) && (args.data == null)) {
            try {
                wait(TIMEOUT);
            } catch (InterruptedException e) {
            }
        }
        arguments = null;
        waitingServiceID = NONE;
        if ((result == Result.OK) && (args.data != null)) {
            if ((args.data.length > 0) || (noElems == 0)) {
                return args.data;
            } else {
                return null;
            }
        } else {
            throw new IOException();
        }
    }

    public synchronized void propertyValue_ReadCon(int pr, int hc, int objIdx, int propID, int startIdx, int noElems, byte[] data) {
        if ((waitingServiceID == PROPVAL_READ) || (waitingServiceID == PROPVAL_WRITE)) {
            PropRWArgs args = (PropRWArgs) arguments;
            if ((objIdx == args.objIdx) && (propID == args.propID) && (startIdx == args.startIdx) && (((noElems == args.noElems) && (data.length > 0)) || ((noElems == 0) && (data.length == 0)))) {
                args.data = data;
                notify();
            }
        }
    }

    public synchronized void propertyValue_FReadCon(int pr, int hc, int objIdx, int propID, long startIdx, int noElems, byte[] data) {
        if (waitingServiceID == PROPVAL_FREAD) {
            PropRWArgs args = (PropRWArgs) arguments;
            if ((objIdx == args.objIdx) && (propID == args.propID) && (startIdx == args.startIdx) && (((noElems == args.noElems) && (data.length > 0)) || ((noElems == 0) && (data.length == 0)))) {
                args.data = data;
                notify();
            }
        }
    }

    public synchronized void propertyValue_FWriteCon(int pr, int hc, int objIdx, int propID, long startIdx, int noElems, byte[] data) {
        if (waitingServiceID == PROPVAL_FWRITE) {
            PropRWArgs args = (PropRWArgs) arguments;
            if ((objIdx == args.objIdx) && (propID == args.propID) && (startIdx == args.startIdx) && (((noElems == args.noElems) && (data.length > 0)) || ((noElems == 0) && (data.length == 0)))) {
                args.data = data;
                notify();
            }
        }
    }

    public PropertyDescr readPropertyDescr(int objIdx, int propID, int propIdx) throws IOException {
        return readPropertyDescr(priority, hopCount, objIdx, propID, propIdx);
    }

    public synchronized PropertyDescr readPropertyDescr(int pr, int hc, int objIdx, int propID, int propIdx) throws IOException {
        if (waitingServiceID != NONE) throw new IOException();
        waitingServiceID = PROPDESCR_READ;
        PropertyDescr args = new PropertyDescr(connAddr, objIdx, propID, propIdx);
        arguments = args;
        int result = ac.propertyDescr_ReadReq(pr, hc, objIdx, propID, propIdx);
        if ((result == Result.OK) && (args.type < 0)) {
            try {
                wait(TIMEOUT);
            } catch (InterruptedException e) {
            }
        }
        arguments = null;
        waitingServiceID = NONE;
        if ((result == Result.OK) && (args.type >= 0)) {
            if (args.maxNoElems > 0) {
                return args;
            } else {
                return null;
            }
        } else {
            throw new IOException();
        }
    }

    public synchronized void propertyDescr_ReadCon(int pr, int hc, int objIdx, int propID, int propIdx, int type, int maxNoElems, int readLevel, int writeLevel) {
        if (waitingServiceID == PROPDESCR_READ) {
            PropertyDescr args = (PropertyDescr) arguments;
            if ((objIdx == args.objectIdx) && (propID == args.propertyID) && (propIdx == args.propertyIdx)) {
                args.type = type;
                args.maxNoElems = maxNoElems;
                args.readLevel = readLevel;
                args.writeLevel = writeLevel;
                notify();
            }
        }
    }

    public byte[] readMemory(int memAddr, int noBytes) throws IOException {
        return readMemory(priority, hopCount, memAddr, noBytes);
    }

    public synchronized byte[] readMemory(int pr, int hc, int memAddr, int noBytes) throws IOException {
        if (waitingServiceID != NONE) throw new IOException();
        waitingServiceID = MEMORY_READ;
        MemReadArgs args = new MemReadArgs(memAddr, noBytes);
        arguments = args;
        int result = ac.memory_ReadReq(pr, hc, memAddr, noBytes);
        if ((result == Result.OK) && (args.data == null)) {
            try {
                wait(TIMEOUT);
            } catch (InterruptedException e) {
            }
        }
        arguments = null;
        waitingServiceID = NONE;
        if ((result == Result.OK) && (args.data != null)) {
            if ((args.data.length > 0) || (noBytes == 0)) {
                return args.data;
            } else {
                return null;
            }
        } else {
            throw new IOException();
        }
    }

    public synchronized void memory_ReadCon(int pr, int hc, int memAddr, byte[] data) {
        if (waitingServiceID == MEMORY_READ) {
            MemReadArgs args = (MemReadArgs) arguments;
            if ((memAddr == args.memAddr) && ((data.length == args.noBytes) || (data.length == 0))) {
                args.data = data;
                notify();
            }
        }
    }

    public void writeMemory(int memAddr, byte[] data) throws IOException {
        writeMemory(priority, hopCount, memAddr, data);
    }

    public synchronized void writeMemory(int pr, int hc, int memAddr, byte[] data) throws IOException {
        if (ac.memory_WriteReq(pr, hc, memAddr, data) != Result.OK) throw new IOException();
    }

    public void writeMemBit(int memAddr, byte[] andData, byte[] xorData) throws IOException {
        writeMemBit(priority, hopCount, memAddr, andData, xorData);
    }

    public synchronized void writeMemBit(int pr, int hc, int memAddr, byte[] andData, byte[] xorData) throws IOException {
        if (ac.memBit_WriteReq(pr, hc, memAddr, andData, xorData) != Result.OK) throw new IOException();
    }

    public byte[] readUserMemory(int memAddr, int noBytes) throws IOException {
        return readUserMemory(priority, hopCount, memAddr, noBytes);
    }

    public synchronized byte[] readUserMemory(int pr, int hc, int memAddr, int noBytes) throws IOException {
        if (waitingServiceID != NONE) throw new IOException();
        waitingServiceID = UMEMORY_READ;
        MemReadArgs args = new MemReadArgs(memAddr, noBytes);
        arguments = args;
        int result = ac.userMemory_ReadReq(pr, hc, memAddr, noBytes);
        if ((result == Result.OK) && (args.data == null)) {
            try {
                wait(TIMEOUT);
            } catch (InterruptedException e) {
            }
        }
        arguments = null;
        waitingServiceID = NONE;
        if ((result == Result.OK) && (args.data != null)) {
            if ((args.data.length > 0) || (noBytes == 0)) {
                return args.data;
            } else {
                return null;
            }
        } else {
            throw new IOException();
        }
    }

    public synchronized void userMemory_ReadCon(int pr, int hc, int memAddr, byte[] data) {
        if (waitingServiceID == UMEMORY_READ) {
            MemReadArgs args = (MemReadArgs) arguments;
            if ((memAddr == args.memAddr) && ((data.length == args.noBytes) || (data.length == 0))) {
                args.data = data;
                notify();
            }
        }
    }

    public void writeUserMemory(int memAddr, byte[] data) throws IOException {
        writeUserMemory(priority, hopCount, memAddr, data);
    }

    public synchronized void writeUserMemory(int pr, int hc, int memAddr, byte[] data) throws IOException {
        if (ac.userMemory_WriteReq(pr, hc, memAddr, data) != Result.OK) throw new IOException();
    }

    public void writeUserMemBit(int memAddr, byte[] andData, byte[] xorData) throws IOException {
        writeUserMemBit(priority, hopCount, memAddr, andData, xorData);
    }

    public synchronized void writeUserMemBit(int pr, int hc, int memAddr, byte[] andData, byte[] xorData) throws IOException {
        if (ac.userMemBit_WriteReq(pr, hc, memAddr, andData, xorData) != Result.OK) throw new IOException();
    }

    public int readDeviceDescr() throws IOException {
        return readDeviceDescr(priority, hopCount);
    }

    public synchronized int readDeviceDescr(int pr, int hc) throws IOException {
        if (waitingServiceID != NONE) throw new IOException();
        waitingServiceID = DEVDESCR_READ;
        int result = ac.deviceDescr_ReadReq(pr, hc);
        if ((result == Result.OK) && (arguments == null)) {
            try {
                wait(TIMEOUT);
            } catch (InterruptedException e) {
            }
        }
        Integer mask = (Integer) arguments;
        arguments = null;
        waitingServiceID = NONE;
        if ((result == Result.OK) && (mask != null)) {
            return mask.intValue();
        } else {
            throw new IOException();
        }
    }

    public synchronized void deviceDescr_ReadCon(int pr, int hc, int maskVersion) {
        if (waitingServiceID == DEVDESCR_READ) {
            arguments = new Integer(maskVersion);
            notify();
        }
    }

    public MfactInfo readUserMfactInfo() throws IOException {
        return readUserMfactInfo(priority, hopCount);
    }

    public synchronized MfactInfo readUserMfactInfo(int pr, int hc) throws IOException {
        if (waitingServiceID != NONE) throw new IOException();
        waitingServiceID = ADC_READ;
        int result = ac.userMfactInfo_ReadReq(pr, hc);
        if ((result == Result.OK) && (arguments == null)) {
            try {
                wait(TIMEOUT);
            } catch (InterruptedException e) {
            }
        }
        MfactInfo info = (MfactInfo) arguments;
        arguments = null;
        waitingServiceID = NONE;
        if ((result == Result.OK) && (info != null)) {
            return info;
        } else {
            throw new IOException();
        }
    }

    public synchronized void userMfactInfo_ReadCon(int pr, int hc, int mfactID, int mfactInfo) {
        if (waitingServiceID == UMFACTINF_READ) {
            arguments = new MfactInfo(mfactID, mfactInfo);
            notify();
        }
    }

    public int readADC(int channelNo, int readCount) throws IOException {
        return readADC(priority, hopCount, channelNo, readCount);
    }

    public synchronized int readADC(int pr, int hc, int channelNo, int readCount) throws IOException {
        if (waitingServiceID != NONE) throw new IOException();
        waitingServiceID = ADC_READ;
        ADCReadArgs args = new ADCReadArgs(channelNo, readCount);
        arguments = args;
        int result = ac.adc_ReadReq(pr, hc, channelNo, readCount);
        if ((result == Result.OK) && (args.sum < 0)) {
            try {
                wait(TIMEOUT);
            } catch (InterruptedException e) {
            }
        }
        arguments = null;
        waitingServiceID = NONE;
        if ((result == Result.OK) && (args.sum >= 0)) {
            if (args.readCount > 0) {
                return args.sum;
            } else {
                return -1;
            }
        } else {
            throw new IOException();
        }
    }

    public synchronized void adc_ReadCon(int pr, int hc, int channelNo, int readCount, int sum) {
        if (waitingServiceID == ADC_READ) {
            ADCReadArgs args = (ADCReadArgs) arguments;
            if ((args.channelNo == channelNo) && ((args.readCount == readCount) || (readCount == 0))) {
                args.readCount = readCount;
                args.sum = sum;
                notify();
            }
        }
    }

    public void restart() throws IOException {
        restart(priority, hopCount);
    }

    public synchronized void restart(int pr, int hc) throws IOException {
        if (ac.restart_Req(pr, hc) != Result.OK) throw new IOException();
    }

    public int authorize(int key) throws IOException {
        return authorize(priority, hopCount, key);
    }

    public synchronized int authorize(int pr, int hc, int key) throws IOException {
        if (waitingServiceID != NONE) throw new IOException();
        waitingServiceID = AUTHORIZE_REQ;
        int result = ac.authorize_Req(pr, hc, key);
        if ((result == Result.OK) && (arguments == null)) {
            try {
                wait(TIMEOUT);
            } catch (InterruptedException e) {
            }
        }
        Integer level = (Integer) arguments;
        arguments = null;
        waitingServiceID = NONE;
        if ((result == Result.OK) && (level != null)) {
            return level.intValue();
        } else {
            throw new IOException();
        }
    }

    public synchronized void authorize_Con(int pr, int hc, int level) {
        if (waitingServiceID == AUTHORIZE_REQ) {
            arguments = new Integer(level);
            notify();
        }
    }

    public int writeKey(int level, int key) throws IOException {
        return writeKey(priority, hopCount, level, key);
    }

    public synchronized int writeKey(int pr, int hc, int level, int key) throws IOException {
        if (waitingServiceID != NONE) throw new IOException();
        waitingServiceID = KEY_WRITE;
        int result = ac.key_WriteReq(pr, hc, level, key);
        if ((result == Result.OK) && (arguments == null)) {
            try {
                wait(TIMEOUT);
            } catch (InterruptedException e) {
            }
        }
        Integer lvl = (Integer) arguments;
        arguments = null;
        waitingServiceID = NONE;
        if ((result == Result.OK) && (lvl != null)) {
            return lvl.intValue();
        } else {
            throw new IOException();
        }
    }

    public synchronized void key_WriteCon(int pr, int hc, int level) {
        if (waitingServiceID == KEY_WRITE) {
            arguments = new Integer(level);
            notify();
        }
    }

    public synchronized void enableExtMemory() throws IOException {
        enableExtMemory(priority, hopCount);
    }

    public synchronized void enableExtMemory(int pr, int hc) throws IOException {
        if (ac.enableExternMemory_Req(pr, hc) != Result.OK) throw new IOException();
    }

    public synchronized byte[] readExternMemory(int memAddr, int noBytes) throws IOException {
        return readExternMemory(priority, hopCount, memAddr, noBytes);
    }

    public synchronized byte[] readExternMemory(int pr, int hc, int memAddr, int noBytes) throws IOException {
        if (waitingServiceID != NONE) throw new IOException();
        waitingServiceID = EXTMEMORY_READ;
        MemReadArgs args = new MemReadArgs(memAddr, noBytes);
        arguments = args;
        int result = ac.externMemory_ReadReq(pr, hc, memAddr, noBytes);
        if ((result == Result.OK) && (args.data == null)) {
            try {
                wait(TIMEOUT);
            } catch (InterruptedException e) {
            }
        }
        arguments = null;
        waitingServiceID = NONE;
        if ((result == Result.OK) && (args.data != null)) {
            if ((args.data.length > 0) || (noBytes == 0)) {
                return args.data;
            } else {
                return null;
            }
        } else {
            throw new IOException();
        }
    }

    public synchronized void externMemory_ReadCon(int pr, int hc, int memAddr, byte[] data) {
        if (waitingServiceID == EXTMEMORY_READ) {
            MemReadArgs args = (MemReadArgs) arguments;
            if ((memAddr == args.memAddr) && ((data.length == args.noBytes) || (data.length == 0))) {
                args.data = data;
                notify();
            }
        }
    }

    public synchronized void writeExternMemory(int memAddr, byte[] data) throws IOException {
        writeExternMemory(priority, hopCount, memAddr, data);
    }

    public synchronized void writeExternMemory(int pr, int hc, int memAddr, byte[] data) throws IOException {
        if (ac.externMemory_WriteReq(pr, hc, memAddr, data) != Result.OK) throw new IOException();
    }

    public synchronized byte[] readSlaveMemory(int memAddr, int noBytes) throws IOException {
        return readSlaveMemory(priority, hopCount, memAddr, noBytes);
    }

    public synchronized byte[] readSlaveMemory(int pr, int hc, int memAddr, int noBytes) throws IOException {
        if (waitingServiceID != NONE) throw new IOException();
        waitingServiceID = SLVMEMORY_READ;
        MemReadArgs args = new MemReadArgs(memAddr, noBytes);
        arguments = args;
        int result = ac.slaveMemory_ReadReq(pr, hc, memAddr, noBytes);
        if ((result == Result.OK) && (args.data == null)) {
            try {
                wait(TIMEOUT);
            } catch (InterruptedException e) {
            }
        }
        arguments = null;
        waitingServiceID = NONE;
        if ((result == Result.OK) && (args.data != null)) {
            if ((args.data.length > 0) || (noBytes == 0)) {
                return args.data;
            } else {
                return null;
            }
        } else {
            throw new IOException();
        }
    }

    public synchronized void slaveMemory_ReadCon(int pr, int hc, int memAddr, byte[] data) {
        if (waitingServiceID == SLVMEMORY_READ) {
            MemReadArgs args = (MemReadArgs) arguments;
            if ((memAddr == args.memAddr) && ((data.length == args.noBytes) || (data.length == 0))) {
                args.data = data;
                notify();
            }
        }
    }

    public synchronized void writeSlaveMemory(int memAddr, byte[] data) throws IOException {
        writeSlaveMemory(priority, hopCount, memAddr, data);
    }

    public synchronized void writeSlaveMemory(int pr, int hc, int memAddr, byte[] data) throws IOException {
        if (ac.slaveMemory_WriteReq(pr, hc, memAddr, data) != Result.OK) throw new IOException();
    }

    public synchronized GrpRouteConfig readGrpRouteConfig() throws IOException {
        return readGrpRouteConfig(priority, hopCount);
    }

    public synchronized GrpRouteConfig readGrpRouteConfig(int pr, int hc) throws IOException {
        if (waitingServiceID != NONE) throw new IOException();
        waitingServiceID = GRPRTCNFG_READ;
        int result = ac.grpRouteConfig_ReadReq(pr, hc);
        if ((result == Result.OK) && (arguments == null)) {
            try {
                wait(TIMEOUT);
            } catch (InterruptedException e) {
            }
        }
        GrpRouteConfig grc = (GrpRouteConfig) arguments;
        arguments = null;
        waitingServiceID = NONE;
        if ((result == Result.OK) && (grc != null)) {
            return grc;
        } else {
            throw new IOException();
        }
    }

    public synchronized void grpRouteConfig_ReadCon(int pr, int hc, int subToMain, int mainToSub) {
        if (waitingServiceID == GRPRTCNFG_READ) {
            arguments = new GrpRouteConfig(subToMain, mainToSub);
            notify();
        }
    }

    public synchronized void writeGrpRouteConfig(GrpRouteConfig grc) throws IOException {
        writeGrpRouteConfig(priority, hopCount, grc);
    }

    public synchronized void writeGrpRouteConfig(int pr, int hc, GrpRouteConfig grc) throws IOException {
        if (ac.grpRouteConfig_WriteReq(pr, hc, grc.subToMain, grc.mainToSub) != Result.OK) throw new IOException();
    }

    public void propertyValue_ReadInd(int pr, int hc, int objIdx, int propID, int startIdx, int noElems) {
    }

    public void propertyValue_FReadInd(int pr, int hc, int objIdx, int propID, long startIdx, int noElems) {
    }

    public void propertyValue_WriteInd(int pr, int hc, int objIdx, int propID, int startIdx, int noElems, byte[] data) {
    }

    public void propertyValue_FWriteInd(int pr, int hc, int objIdx, int propID, long startIdx, int noElems, byte[] data) {
    }

    public void propertyDescr_ReadInd(int pr, int hc, int objIdx, int propID, int propIdx) {
    }

    public void memory_ReadInd(int pr, int hc, int memAddr, int noBytes) {
    }

    public void memory_WriteInd(int pr, int hc, int memAddr, byte[] data) {
    }

    public void memBit_WriteInd(int pr, int hc, int memAddr, byte[] andData, byte[] xorData) {
    }

    public void userMemory_ReadInd(int pr, int hc, int memAddr, int noBytes) {
    }

    public void userMemory_WriteInd(int pr, int hc, int memAddr, byte[] data) {
    }

    public void userMemBit_WriteInd(int pr, int hc, int memAddr, byte[] andData, byte[] xorData) {
    }

    public void deviceDescr_ReadInd(int pr, int hc) {
    }

    public void userMfactInfo_ReadInd(int pr, int hc) {
    }

    public void adc_ReadInd(int pr, int hc, int channelNo, int readCount) {
    }

    public void restart_Ind(int pr, int hc) {
    }

    public void authorize_Ind(int pr, int hc, int key) {
    }

    public void key_WriteInd(int pr, int hc, int level, int key) {
    }

    public void enableExternMemory_Ind(int pr, int hc) {
    }

    public void externMemory_ReadInd(int pr, int hc, int memAddr, int noBytes) {
    }

    public void externMemory_WriteInd(int pr, int hc, int memAddr, byte[] data) {
    }

    public void slaveMemory_ReadInd(int pr, int hc, int memAddr, int noBytes) {
    }

    public void slaveMemory_WriteInd(int pr, int hc, int memAddr, byte[] data) {
    }

    public void grpRouteConfig_ReadInd(int pr, int hc) {
    }

    public void grpRouteConfig_WriteInd(int pr, int hc, int subToMain, int mainToSub) {
    }
}
