package jcontrol.eib.extended.ai_layer;

import java.io.IOException;
import java.rmi.RemoteException;
import jcontrol.eib.extended.a_layer.A_Connection;

/**
 * Implementation of the {@link AI_Connection} interface.
 *
 * @author Bjoern Hornburg
 * @version 0.2.0.000
 * @since 0.2.0
 */
class AI_ConnectionImpl extends AI_OutConnectionImpl implements AI_Connection {

    AI_ConnectionImpl(int ca, int pr, int hc, A_Connection c) {
        super(ca, pr, hc, c);
    }

    AI_ConnectionImpl(int ca, int pr, int hc, AI_Connection.Listener l) {
        super(ca, pr, hc, l);
    }

    void setListener(AI_Connection.Listener l) {
        super.setListener(l);
    }

    public synchronized boolean propertyValue_ReadInd(int pr, int hc, int objIdx, int propID, int startIdx, int noElems) {
        try {
            byte[] data = ((AI_Connection.Listener) ocl).readPropertyValue(pr, hc, objIdx, propID, startIdx, noElems);
            if (data == null) {
                noElems = 0;
                data = new byte[0];
            }
            ac.propertyValue_ReadRes(pr, hopCount, objIdx, propID, startIdx, noElems, data);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean propertyValue_FReadInd(int pr, int hc, int objIdx, int propID, long startIdx, int noElems) {
        try {
            byte[] data = ((AI_Connection.Listener) ocl).readPropertyValue(pr, hc, objIdx, propID, startIdx, noElems);
            if (data == null) {
                noElems = 0;
                data = new byte[0];
            }
            ac.propertyValue_FReadRes(pr, hopCount, objIdx, propID, startIdx, noElems, data);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean propertyValue_WriteInd(int pr, int hc, int objIdx, int propID, int startIdx, int noElems, byte[] data) {
        try {
            data = ((AI_Connection.Listener) ocl).writePropertyValue(pr, hc, objIdx, propID, startIdx, noElems, data);
            if (data == null) {
                noElems = 0;
                data = new byte[0];
            }
            ac.propertyValue_ReadRes(pr, hopCount, objIdx, propID, startIdx, noElems, data);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean propertyValue_FWriteInd(int pr, int hc, int objIdx, int propID, long startIdx, int noElems, byte[] data) {
        try {
            data = ((AI_Connection.Listener) ocl).writePropertyValue(pr, hc, objIdx, propID, startIdx, noElems, data);
            if (data == null) {
                noElems = 0;
                data = new byte[0];
            }
            ac.propertyValue_FWriteRes(pr, hopCount, objIdx, propID, startIdx, noElems, data);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean propertyDescr_ReadInd(int pr, int hc, int objIdx, int propID, int propIdx) {
        try {
            PropertyDescr descr = ((AI_Connection.Listener) ocl).readPropertyDescr(pr, hc, objIdx, propID, propIdx);
            if (descr != null) {
                ac.propertyDescr_ReadRes(pr, hopCount, objIdx, propID, propIdx, 0, 0, 0, 0);
            } else {
                ac.propertyDescr_ReadRes(pr, hopCount, descr.objectIdx, descr.propertyID, descr.propertyIdx, descr.type, descr.maxNoElems, descr.readLevel, descr.writeLevel);
            }
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean memory_ReadInd(int pr, int hc, int memAddr, int noBytes) {
        try {
            byte[] data = ((AI_Connection.Listener) ocl).readMemory(pr, hc, memAddr, noBytes);
            if (data == null) data = new byte[0];
            ac.memory_ReadRes(pr, hopCount, memAddr, data);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean memory_WriteInd(int pr, int hc, int memAddr, byte[] data) {
        try {
            ((AI_Connection.Listener) ocl).writeMemory(pr, hc, memAddr, data);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean memBit_WriteInd(int pr, int hc, int memAddr, byte[] andData, byte[] xorData) {
        try {
            ((AI_Connection.Listener) ocl).writeMemBit(pr, hc, memAddr, andData, xorData);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean userMemory_ReadInd(int pr, int hc, int memAddr, int noBytes) {
        try {
            byte[] data = ((AI_Connection.Listener) ocl).readUserMemory(pr, hc, memAddr, noBytes);
            if (data == null) data = new byte[0];
            ac.userMemory_ReadRes(pr, hopCount, memAddr, data);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean userMemory_WriteInd(int pr, int hc, int memAddr, byte[] data) {
        try {
            ((AI_Connection.Listener) ocl).writeUserMemory(pr, hc, memAddr, data);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean userMemBit_WriteInd(int pr, int hc, int memAddr, byte[] andData, byte[] xorData) {
        try {
            ((AI_Connection.Listener) ocl).writeUserMemBit(pr, hc, memAddr, andData, xorData);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean deviceDescr_ReadInd(int pr, int hc) {
        try {
            ac.deviceDescr_ReadRes(pr, hopCount, ((AI_Connection.Listener) ocl).readDeviceDescr(pr, hc));
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean userMfactInfo_ReadInd(int pr, int hc) {
        try {
            MfactInfo info = ((AI_Connection.Listener) ocl).readUserMfactInfo(pr, hc);
            ac.userMfactInfo_ReadRes(pr, hopCount, info.mfactID, info.mfactInfo);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean adc_ReadInd(int pr, int hc, int channelNo, int readCount) {
        try {
            int sum = ((AI_Connection.Listener) ocl).readADC(pr, hc, channelNo, readCount);
            if (sum < 0) {
                readCount = 0;
                sum = 0;
            }
            ac.adc_ReadRes(pr, hopCount, channelNo, readCount, sum);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean restart_Ind(int pr, int hc) {
        try {
            ((AI_Connection.Listener) ocl).restart(pr, hc);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean authorize_Ind(int pr, int hc, int key) {
        try {
            ac.authorize_Res(pr, hopCount, ((AI_Connection.Listener) ocl).authorize(pr, hc, key));
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean key_WriteInd(int pr, int hc, int level, int key) {
        try {
            ac.key_WriteRes(pr, hopCount, ((AI_Connection.Listener) ocl).writeKey(pr, hc, level, key));
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean enableExternMemory_Ind(int pr, int hc) {
        try {
            ((AI_Connection.Listener) ocl).enableExternMemory(pr, hc);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean externMemory_ReadInd(int pr, int hc, int memAddr, int noBytes) {
        try {
            byte[] data = ((AI_Connection.Listener) ocl).readExternMemory(pr, hc, memAddr, noBytes);
            if (data == null) data = new byte[0];
            ac.externMemory_ReadRes(pr, hc, memAddr, data);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean externMemory_WriteInd(int pr, int hc, int memAddr, byte[] data) {
        try {
            ((AI_Connection.Listener) ocl).writeExternMemory(pr, hc, memAddr, data);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean slaveMemory_ReadInd(int pr, int hc, int memAddr, int noBytes) {
        try {
            byte[] data = ((AI_Connection.Listener) ocl).readSlaveMemory(pr, hc, memAddr, noBytes);
            if (data == null) data = new byte[0];
            ac.slaveMemory_ReadRes(pr, hc, memAddr, data);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean slaveMemory_WriteInd(int pr, int hc, int memAddr, byte[] data) {
        try {
            ((AI_Connection.Listener) ocl).writeSlaveMemory(pr, hc, memAddr, data);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean grpRouteConfig_ReadInd(int pr, int hc) {
        try {
            GrpRouteConfig grc = ((AI_Connection.Listener) ocl).readGrpRouteConfig(pr, hc);
            if (grc != null) {
                ac.grpRouteConfig_ReadRes(pr, hc, grc.subToMain, grc.mainToSub);
            } else {
                ac.grpRouteConfig_ReadRes(pr, hc, GrpRouteConfig.DONT_CARE, GrpRouteConfig.DONT_CARE);
            }
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public synchronized boolean grpRouteConfig_WriteInd(int pr, int hc, int subToMain, int mainToSub) {
        try {
            GrpRouteConfig grc = new GrpRouteConfig(subToMain, mainToSub);
            ((AI_Connection.Listener) ocl).writeGrpRouteConfig(pr, hc, grc);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }
}
