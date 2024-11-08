package srcp.server;

import srcp.common.*;
import srcp.common.exception.*;
import java.util.*;
import java.lang.*;

public class DeviceGroupPower extends DeviceGroup {

    /** the bus which we are connected to */
    protected int intBusNr;

    protected String strBusNr;

    /** address used for storing the init info in the DB */
    protected final String strAddrPower = "1";

    /** constructor; sets identifier, bus object and bus number */
    public DeviceGroupPower(BusInterface objABus, int intABusNr) {
        super(objABus.getDaemon());
        strGroupIdentifier = Declare.strDevPower;
        objBus = objABus;
        intBusNr = intABusNr;
        strBusNr = Integer.toString(intABusNr);
    }

    /** SRCP processor object */
    protected ProcessorInterface objProcessor = null;

    /** get reference to the processor from the daemon if this
	  * is not set */
    protected void getProcessor() {
        if (objProcessor != null) {
            return;
        }
        objProcessor = objBus.getDaemon().getProcessor();
    }

    /** SRCP syntax: SET <bus> POWER <ON,OFF> <freetext> */
    public void set(StringTokenizer objStrTok, SessionThread objThread, boolean blnCheck) throws Exception {
        getProcessor();
        String strValue = null;
        try {
            strValue = objStrTok.nextToken();
        } catch (Exception e) {
            throw new ExcListToShort();
        }
        if ((!strValue.equals("ON")) && (!strValue.equals("OFF"))) {
            throw new ExcWrongValue();
        }
        String strFreetext = "";
        while (objStrTok.hasMoreElements()) {
            strFreetext += objStrTok.nextToken() + " ";
        }
        Operand objOperand = new Operand(strBusNr, Declare.strDevPower, strAddrPower, "", strValue, strFreetext, "");
        if (!objProcessor.isAvailable(objOperand)) {
            throw new ExcUnsupportedDevice();
        }
        if (!blnCheck) {
            try {
                objProcessor.set(objOperand);
            } catch (Exception e) {
                throw new ExcProcessorDied();
            }
        }
        objThread.writeAck();
    }

    /** SRCP syntax: GET <bus> POWER */
    public void get(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        getProcessor();
        Operand objOperand = new Operand(strBusNr, Declare.strDevPower, strAddrPower, "", "", "", "");
        if (!objProcessor.isAvailable(objOperand)) {
            throw new ExcUnsupportedDevice();
        }
        String strValue = objProcessor.get(objOperand);
        objThread.writeAck(Declare.intInfoMin, Integer.toString(intBusNr) + " POWER " + strValue);
    }

    public String info(String strAddr) throws Exception {
        getProcessor();
        return objProcessor.info(strBusNr, Declare.strDevPower, strAddrPower);
    }

    public void init(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        Operand objOperand = new Operand(strBusNr, Declare.strDevPower, strAddrPower, "", "", "", objSRCPDaemon.getTimestamp());
        objOperand.setInit();
        getProcessor();
        objProcessor.init(objOperand);
        objThread.writeAck();
    }

    public void term(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        Operand objOperand = new Operand(strBusNr, Declare.strDevPower, strAddrPower, "", "", "", objSRCPDaemon.getTimestamp());
        objOperand.setTerm();
        getProcessor();
        objProcessor.term(objOperand);
        objThread.writeAck();
    }
}

;
