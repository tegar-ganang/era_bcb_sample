package iwork.icrafter.remote;

import iwork.icrafter.util.*;
import iwork.icrafter.system.*;
import iwork.eheap2.*;
import iwork.state.*;
import java.io.*;
import java.util.*;
import java.net.Socket;

/**
 *
 * Helper class that allows making calls to ICrafter services on the
 * EH. All service clients can use this class to make
 * calls to ICrafter services. Typically, here's how
 * EHCallObject should be used:
 *
 * <ul>
 * <li> EventHeap eh = new EventHeap(..) </li>
 * <li> EHCallObject ehc = new EHCallObject(eh, ..) </li>
 * <li> ehc.ehCall() </li>
 * </ul> 
 */
public class EHCallObject {

    private EventHeap eh = null;

    private int opn_expire = 30000;

    private int opn_timeout = 300000;

    /**
     * Constructs an EHCallObject that uses the given EventHeap object
     * to make calls over the event heap. Uses a default time
     * (currently 30 seconds) as the call event expire time
     **/
    public EHCallObject(EventHeap eh) throws EventHeapException {
        this.eh = eh;
    }

    /**
     * Constructs an EHCallObject that uses the given EventHeap object
     * to make calls over the event heap.
     * @param opn_expire the time (in millisec) after which the call
     * event will expire
     **/
    public EHCallObject(EventHeap eh, int opn_expire) throws EventHeapException {
        this.eh = eh;
        this.opn_expire = opn_expire;
    }

    /**
     * Makes a no parameter, no return service call over the event
     * heap. See the description for {@link #ehCall(String, String,
     * boolean, Serializable[], int)} for an
     * explanation of the parameters and returns 
     */
    public Serializable ehCall(String svcName, String opName) throws ICrafterException {
        return ehCall(svcName, opName, false, null);
    }

    /**
     * Makes a no parameter service call over the event
     * heap. See the description for {@link #ehCall(String, String,
     * boolean, Serializable[], int)} for an
     * explanation of the parameters and returns 
     */
    public Serializable ehCall(String svcName, String opName, boolean hasReturn) throws ICrafterException {
        return ehCall(svcName, opName, hasReturn, null);
    }

    /**
     * Makes a no return service call over the event heap. See the
     * description for {@link #ehCall(String, String, boolean,
     * Serializable[], int)} for an explanation of the parameters and
     * returns 
     **/
    public Serializable ehCall(String svcName, String opName, Serializable params[]) throws ICrafterException {
        return ehCall(svcName, opName, false, params);
    }

    /**
     * Makes a service call over the event heap with the default
     * timeout. See the description for {@link #ehCall(String, String,
     * boolean, Serializable[], int)} for an explanation of the
     * parameters and returns
     **/
    public Serializable ehCall(String svcName, String opName, boolean hasReturn, Serializable params[]) throws ICrafterException {
        return ehCall(svcName, opName, hasReturn, params, opn_timeout);
    }

    /**
     * Makes a service call with parameters and returns over
     * the event heap. 
     * @param svcName Service to which the call is directed
     * @param opName Name of the operation to be invoked
     * @param hasReturn Does the call have return value?
     * @param params Parameters for the call
     * @param timeout Timeout for the call
     * @return Result of the operation call (if any) 
     */
    public Serializable ehCall(String svcName, String opName, boolean hasReturn, Serializable params[], int timeout) throws ICrafterException {
        try {
            CallThread ct = new CallThread(eh, svcName, opName, hasReturn, params, opn_expire);
            ct.start();
            ct.join(timeout);
            Exception ex = ct.getLastException();
            if (ex != null) {
                throw ex;
            }
            return ct.getReturn();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ICrafterException(e);
        }
    }

    /**
     * Makes a state-variable-value-based service call over the event
     * heap. In these calls, you don't need specify the name of the
     * target service. Instead, you just need to specify the interface
     * implemented by the service and the matching state variables for
     * the target service(s). If multiple services implement the given
     * interface and also export variables whose (current) values
     * match the passed matching variables (mVars), then this call is
     * invoked on all such services. Also, note that currently returns
     * are not allowed for these calls.
     * @param ifName the interface that the target service(s) must implement
     * @param opName name of the operation to invoke
     * @param params parameters for the call
     * @param mVars the matching variables that should be used to
     * filter the set of target services.
     * throws ICrafterException
     **/
    public void ehIFCall(String ifName, String opName, Serializable[] params, MatchingVariable[] mVars) throws ICrafterException {
        try {
            Event callEvent = new Event();
            callEvent.setFieldValue(Event.TIMETOLIVE, new Integer(opn_expire));
            callEvent.setFieldValue(Event.EVENTTYPE, ifName);
            callEvent.addField(ICrafterConstants.EVENTCLASS, ICrafterConstants.INTERFACE_OPERATION_EVENT_CLASS);
            callEvent.addField(ICrafterConstants.HAS_RETURN, new Boolean(false));
            if (opName != null) {
                int callID = (int) (Math.random() * Integer.MAX_VALUE);
                callEvent.addField(ICrafterConstants.OPERATION_NAME, opName);
                callEvent.addField(ICrafterConstants.CALLID, new Integer(callID));
                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        callEvent.addField(ICrafterConstants.PARAM + i, params[i]);
                    }
                }
                if (mVars != null) {
                    for (int i = 0; i < mVars.length; i++) {
                        callEvent.addField("MatchingVariable" + i, mVars[i].toXML());
                    }
                }
                Utils.debug("EHCallObject", "Putting callevent: " + callEvent.toString());
                eh.putEvent(callEvent);
            } else {
                Utils.warning("EHCallObject", "OpName is null!");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ICrafterException(e);
        }
    }
}

class CallThread extends Thread {

    String svcName;

    String opName;

    boolean hasReturn;

    Serializable[] params;

    int opn_expire;

    Serializable retVal;

    Exception lastE;

    EventHeap eh;

    public CallThread(EventHeap eh, String svcName, String opName, boolean hasReturn, Serializable[] params, int opn_expire) {
        this.eh = eh;
        this.svcName = svcName;
        this.opName = opName;
        this.hasReturn = hasReturn;
        this.params = params;
        this.opn_expire = opn_expire;
    }

    public void run() {
        try {
            eh = new EventHeap(eh, null);
            lastE = null;
            retVal = null;
            Event callEvent = new Event();
            callEvent.setFieldValue(Event.TIMETOLIVE, new Integer(opn_expire));
            callEvent.setFieldValue(Event.EVENTTYPE, svcName);
            callEvent.addField(ICrafterConstants.EVENTCLASS, ICrafterConstants.OPERATION_EVENT_CLASS);
            callEvent.addField(ICrafterConstants.HAS_RETURN, new Boolean(hasReturn));
            boolean dataparams = false;
            if (opName == null) {
                Utils.warning("EHCallObject", "OpName is null!");
                return;
            }
            int callID = (int) (Math.random() * Integer.MAX_VALUE);
            callEvent.addField(ICrafterConstants.OPERATION_NAME, opName);
            callEvent.addField(ICrafterConstants.CALLID, new Integer(callID));
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    Class[] ifs = params[i].getClass().getInterfaces();
                    boolean isrdata = false;
                    for (int j = 0; j < ifs.length; j++) {
                        if (ifs[j].getName().equals("iwork.icrafter.system.RData")) isrdata = true;
                    }
                    if (isrdata) {
                        Utils.debug("EHCallObject", i + ": RData parameter");
                        callEvent.addField("RData" + i, new Boolean(true));
                        callEvent.addField(ICrafterConstants.PARAM + i, "");
                        dataparams = true;
                        continue;
                    }
                    callEvent.addField(ICrafterConstants.PARAM + i, params[i]);
                }
            }
            Utils.debug("EHCallObject", "Putting callevent: " + callEvent.toString());
            eh.putEvent(callEvent);
            if (dataparams) {
                Event retEventTemplate = new Event();
                retEventTemplate.setFieldValue(Event.EVENTTYPE, svcName);
                retEventTemplate.addField(ICrafterConstants.EVENTCLASS, "DataReturnClass");
                retEventTemplate.addField(ICrafterConstants.CALLID, new Integer(callID));
                Utils.debug("EHCallObject", "Waiting for data ports ... ");
                Event rcdEvent = eh.waitToRemoveEvent(retEventTemplate);
                for (int i = 0; i < params.length; i++) {
                    Class[] ifs = params[i].getClass().getInterfaces();
                    boolean isrdata = false;
                    for (int j = 0; j < ifs.length; j++) {
                        if (ifs[j].getName().equals("iwork.icrafter.system.RData")) isrdata = true;
                    }
                    if (!isrdata) continue;
                    String rDataRet = (String) rcdEvent.getPostValue("RDataRet" + i);
                    int sep = rDataRet.indexOf(';');
                    String machineName = rDataRet.substring(0, sep);
                    int portNum = (new Integer(rDataRet.substring(sep + 1))).intValue();
                    Socket s = new Socket(machineName, portNum);
                    InputStream is = ((RData) params[i]).getDataStream();
                    OutputStream os = s.getOutputStream();
                    byte[] b = new byte[1000];
                    int len = -1;
                    while ((len = is.read(b)) != -1) os.write(b, 0, len);
                    is.close();
                    os.close();
                }
            }
            if (!hasReturn) return;
            Event retEventTemplate = new Event();
            retEventTemplate.setFieldValue(Event.EVENTTYPE, svcName);
            retEventTemplate.addField(ICrafterConstants.EVENTCLASS, ICrafterConstants.RETURN_EVENT_CLASS);
            retEventTemplate.addField(ICrafterConstants.CALLID, new Integer(callID));
            Utils.debug("EHCallObject", "Waiting for return... ");
            Event rcdEvent = eh.waitToRemoveEvent(retEventTemplate);
            String retCode = (String) rcdEvent.getPostValue(ICrafterConstants.RETURN_CODE);
            if (retCode.equals(ICrafterConstants.NO_ERROR_RETURN)) {
                Utils.debug("EHCallObject", "Return event " + "received! " + rcdEvent);
                retVal = (Serializable) rcdEvent.getPostValue(ICrafterConstants.RETURN_VALUE);
                return;
            } else if (retCode.equals(ICrafterConstants.NULL_RETURN)) {
                retVal = null;
                return;
            } else if (retCode.equals(ICrafterConstants.ERROR_RETURN)) {
                String err = (String) rcdEvent.getPostValue(ICrafterConstants.RETURN_ERROR_MESSAGE);
                throw new ICrafterRemoteException(err);
            } else if (retCode.equals(ICrafterConstants.RDATA_RETURN)) {
                String rDataRet = (String) rcdEvent.getPostValue(ICrafterConstants.RETURN_VALUE);
                int sep = rDataRet.indexOf(';');
                String machineName = rDataRet.substring(0, sep);
                int portNum = (new Integer(rDataRet.substring(sep + 1))).intValue();
                Socket s = new Socket(machineName, portNum);
                InputStream is = s.getInputStream();
                StreamData tmp = new StreamData(is);
                retVal = new ByteData(tmp.getDataBytes());
                s.close();
                return;
            } else {
                throw new ICrafterRemoteException("Unknown error code in return event");
            }
        } catch (Exception e) {
            e.printStackTrace();
            lastE = e;
        }
    }

    public Exception getLastException() {
        return lastE;
    }

    public Serializable getReturn() {
        return retVal;
    }
}
