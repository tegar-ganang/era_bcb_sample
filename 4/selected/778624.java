package jcontrol.eib.extended.ai_layer;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jcontrol.eib.extended.HopCount;
import jcontrol.eib.extended.Priority;
import jcontrol.eib.extended.a_layer.A_Connection;
import jcontrol.eib.extended.a_layer.A_DataConnectedService;
import jcontrol.eib.extended.a_layer.A_DataConnlessService;

/**
 * Implementation of the {@link AI_OutgoingUnicastService} and the {@link
 * AI_IncomingUnicastService} interfaces.
 *
 * @author Bjoern Hornburg
 * @version 0.2.0.000
 * @since 0.2.0
 */
public class AI_UnicastImpl implements AI_OutgoingUnicastService, AI_IncomingUnicastService, A_DataConnlessService.Listener, A_DataConnectedService.Listener, AI_OutgoingUnicastAdmin, AI_IncomingUnicastAdmin {

    private static final int TIMEOUT_TIME = 30000;

    private A_DataConnlessService aus;

    private A_DataConnectedService acs;

    public AI_UnicastImpl(A_DataConnlessService aus, A_DataConnectedService acs) {
        this.aus = aus;
        this.acs = acs;
        aus.setListener(this);
        acs.setListener(this);
    }

    public AI_OutConnection connect(int da, AI_OutConnection.Listener l) throws IOException {
        return connect(da, Priority.DEFAULT, HopCount.DEFAULT, l);
    }

    public AI_OutConnection connect(int da, int pr, int hc, AI_OutConnection.Listener l) throws IOException {
        AI_OutConnectionImpl c = new AI_OutConnectionImpl(da, pr, hc, l);
        A_Connection ac = acs.connect(da, c);
        if (ac != null) {
            c.setConnection(ac);
            return c;
        } else {
            throw new IOException();
        }
    }

    private class PropertyRW {

        private int da;

        private int objIdx;

        private int propID;

        private long startIdx;

        private int noElems;

        private byte[] data;

        private PropertyRW(int da, int objIdx, int propID, long startIdx, int noElems) {
            this.da = da;
            this.objIdx = objIdx;
            this.propID = propID;
            this.startIdx = startIdx;
            this.noElems = noElems;
            this.data = null;
        }

        private PropertyRW(int da, int objIdx, int propID, long startIdx, int noElems, byte[] data) {
            this.da = da;
            this.objIdx = objIdx;
            this.propID = propID;
            this.startIdx = startIdx;
            this.noElems = noElems;
            this.data = data;
        }

        public boolean equals(Object o) {
            if (o instanceof PropertyRW) {
                PropertyRW p = (PropertyRW) o;
                return ((p.da == da) && (p.objIdx == objIdx) && (p.propID == propID) && (p.startIdx == startIdx) && ((p.noElems == noElems) || (p.noElems == 0)));
            } else {
                return false;
            }
        }
    }

    private List propValRWWaitings = new LinkedList();

    private List propValFReadWaitings = new LinkedList();

    private List propValFWriteWaitings = new LinkedList();

    public byte[] readPropertyValue(int da, int objIdx, int propID, long startIdx, int noElems) throws IOException {
        return readPropertyValue(da, Priority.DEFAULT, HopCount.DEFAULT, objIdx, propID, startIdx, noElems);
    }

    public byte[] readPropertyValue(int da, int pr, int hc, int objIdx, int propID, long startIdx, int noElems) throws IOException {
        PropertyRW prop = new PropertyRW(da, objIdx, propID, startIdx, noElems);
        synchronized (prop) {
            if ((startIdx < 4096) && (noElems < 16)) {
                synchronized (propValRWWaitings) {
                    propValRWWaitings.add(prop);
                }
                aus.propertyValue_ReadReq(da, pr, hc, objIdx, propID, (int) startIdx, noElems, false);
                try {
                    prop.wait(TIMEOUT_TIME);
                } catch (InterruptedException e) {
                }
                if (prop.data == null) {
                    synchronized (propValRWWaitings) {
                        propValRWWaitings.remove(prop);
                    }
                    throw new IOException();
                }
            } else {
                synchronized (propValFReadWaitings) {
                    propValFReadWaitings.add(prop);
                }
                aus.propertyValue_FReadReq(da, pr, hc, objIdx, propID, startIdx, noElems, false);
                try {
                    prop.wait(TIMEOUT_TIME);
                } catch (InterruptedException e) {
                }
                if (prop.data == null) {
                    synchronized (propValFReadWaitings) {
                        propValFReadWaitings.remove(prop);
                    }
                    throw new IOException();
                }
            }
        }
        if ((prop.data.length != 0) || (noElems == 0)) {
            return prop.data;
        } else {
            return null;
        }
    }

    public byte[] writePropertyValue(int da, int objIdx, int propID, long startIdx, int noElems, byte[] data) throws IOException {
        return writePropertyValue(da, Priority.DEFAULT, HopCount.DEFAULT, objIdx, propID, startIdx, noElems, data);
    }

    public byte[] writePropertyValue(int da, int pr, int hc, int objIdx, int propID, long startIdx, int noElems, byte[] data) throws IOException {
        PropertyRW prop = new PropertyRW(da, objIdx, propID, startIdx, noElems);
        synchronized (prop) {
            if ((startIdx < 4096) && (noElems < 16)) {
                synchronized (propValRWWaitings) {
                    propValRWWaitings.add(prop);
                }
                aus.propertyValue_WriteReq(da, pr, hc, objIdx, propID, (int) startIdx, noElems, data, false);
                try {
                    prop.wait(TIMEOUT_TIME);
                } catch (InterruptedException e) {
                }
                if (prop.data == null) {
                    synchronized (propValRWWaitings) {
                        propValRWWaitings.remove(prop);
                    }
                    throw new IOException();
                }
            } else {
                synchronized (propValFWriteWaitings) {
                    propValFWriteWaitings.add(prop);
                }
                aus.propertyValue_FWriteReq(da, pr, hc, objIdx, propID, startIdx, noElems, data, false);
                try {
                    prop.wait(TIMEOUT_TIME);
                } catch (InterruptedException e) {
                }
                if (prop.data == null) {
                    synchronized (propValFWriteWaitings) {
                        propValFWriteWaitings.remove(prop);
                    }
                    throw new IOException();
                }
            }
        }
        if ((prop.data.length != 0) || (noElems == 0)) {
            return prop.data;
        } else {
            return null;
        }
    }

    public void propertyValue_ReadCon(int sa, int pr, int hc, int objIdx, int propID, int startIdx, int noElems, byte[] data) {
        synchronized (propValRWWaitings) {
            int idx = propValRWWaitings.indexOf(new PropertyRW(sa, objIdx, propID, startIdx, noElems));
            if (idx >= 0) {
                PropertyRW prop = (PropertyRW) propValRWWaitings.remove(idx);
                synchronized (prop) {
                    prop.data = data;
                    prop.notify();
                }
            }
        }
    }

    public void propertyValue_FReadCon(int sa, int pr, int hc, int objIdx, int propID, long startIdx, int noElems, byte[] data) {
        synchronized (propValFReadWaitings) {
            int idx = propValFReadWaitings.indexOf(new PropertyRW(sa, objIdx, propID, startIdx, noElems));
            if (idx >= 0) {
                PropertyRW prop = (PropertyRW) propValFReadWaitings.remove(idx);
                synchronized (prop) {
                    prop.data = data;
                    prop.notify();
                }
            }
        }
    }

    public void propertyValue_FWriteCon(int sa, int pr, int hc, int objIdx, int propID, long startIdx, int noElems, byte[] data) {
        synchronized (propValFWriteWaitings) {
            int idx = propValFWriteWaitings.indexOf(new PropertyRW(sa, objIdx, propID, startIdx, noElems));
            if (idx >= 0) {
                PropertyRW prop = (PropertyRW) propValFWriteWaitings.remove(idx);
                synchronized (prop) {
                    prop.data = data;
                    prop.notify();
                }
            }
        }
    }

    private List propDescrReadWaitings = new LinkedList();

    public PropertyDescr readPropertyDescr(int da, int objIdx, int propID, int propIdx) throws IOException {
        return readPropertyDescr(da, Priority.DEFAULT, HopCount.DEFAULT, objIdx, propID, propIdx);
    }

    public PropertyDescr readPropertyDescr(int da, int pr, int hc, int objIdx, int propID, int propIdx) throws IOException {
        PropertyDescr descr = new PropertyDescr(da, objIdx, propID, propIdx);
        synchronized (descr) {
            synchronized (propDescrReadWaitings) {
                propDescrReadWaitings.add(descr);
            }
            aus.propertyDescr_ReadReq(da, pr, hc, objIdx, propID, propIdx, false);
            try {
                descr.wait(TIMEOUT_TIME);
            } catch (InterruptedException e) {
            }
            if (descr.type < 0) {
                synchronized (propDescrReadWaitings) {
                    propDescrReadWaitings.remove(descr);
                }
                throw new IOException();
            }
        }
        if (descr.maxNoElems > 0) {
            return descr;
        } else {
            return null;
        }
    }

    public void propertyDescr_ReadCon(int sa, int pr, int hc, int objIdx, int propID, int propIdx, int type, int maxNoElems, int readLevel, int writeLevel) {
        synchronized (propDescrReadWaitings) {
            int idx = propDescrReadWaitings.indexOf(new PropertyDescr(sa, objIdx, propID, propIdx));
            if (idx >= 0) {
                PropertyDescr descr = (PropertyDescr) propDescrReadWaitings.remove(idx);
                synchronized (descr) {
                    descr.propertyID = propID;
                    descr.propertyIdx = propIdx;
                    descr.type = type;
                    descr.maxNoElems = maxNoElems;
                    descr.readLevel = readLevel;
                    descr.writeLevel = writeLevel;
                    descr.notify();
                }
            }
        }
    }

    private Map listeners = new HashMap();

    private Listener listener = null;

    private Listener getListener(int physAddr) {
        synchronized (listeners) {
            if (listener != null) {
                return listener;
            } else {
                return (Listener) listeners.get(new Integer(physAddr));
            }
        }
    }

    private int noRegistrations = 0;

    public int getNoRegistrations() {
        return noRegistrations;
    }

    public int getNoRegAddresses() {
        if (listener == null) {
            return listeners.size();
        } else {
            return -1;
        }
    }

    public AI_IncomingUnicastService.AccessPoint register(int[] da, AI_IncomingUnicastService.Listener l) throws AlreadyRegisteredException {
        synchronized (listeners) {
            if (listener != null) throw new AlreadyRegisteredException();
            if (da.length == 0) {
                if (!listeners.isEmpty()) throw new AlreadyRegisteredException();
                listener = l;
                return new AccessPointImpl(da);
            } else {
                for (int i = 0; i < da.length; i++) {
                    if (listeners.containsKey(new Integer(da[i]))) throw new AlreadyRegisteredException();
                }
                for (int i = 0; i < da.length; i++) {
                    listeners.put(new Integer(da[i]), l);
                }
                return new AccessPointImpl(da);
            }
        }
    }

    public A_Connection.Listener connected(int sa, A_Connection ac) {
        Listener l = getListener(sa);
        if (l == null) return null;
        AI_ConnectionImpl c = new AI_ConnectionImpl(sa, Priority.DEFAULT, HopCount.DEFAULT, ac);
        AI_Connection.Listener cl = null;
        try {
            cl = l.connected(sa, c);
        } catch (RemoteException e) {
            cl = null;
        }
        if (cl == null) return null;
        c.setListener(cl);
        return c;
    }

    private class AccessPointImpl implements AI_IncomingUnicastService.AccessPoint {

        private Set regAddrs = new HashSet();

        private AccessPointImpl(int[] addrs) {
            noRegistrations++;
            for (int i = 0; i < addrs.length; i++) {
                regAddrs.add(new Integer(addrs[i]));
            }
        }

        public AI_Connection connect(int da, AI_Connection.Listener l) throws IOException {
            return connect(da, Priority.DEFAULT, HopCount.DEFAULT, l);
        }

        public AI_Connection connect(int da, int pr, int hc, AI_Connection.Listener l) throws IOException {
            if (!regAddrs.isEmpty() && !regAddrs.contains(new Integer(da))) throw new IOException();
            AI_ConnectionImpl c = new AI_ConnectionImpl(da, pr, hc, l);
            A_Connection ac = acs.connect(da, c);
            if (ac == null) throw new IOException();
            c.setConnection(ac);
            return c;
        }

        public void cancel() {
            Set addrs = regAddrs;
            regAddrs = null;
            synchronized (listeners) {
                if (addrs.isEmpty()) {
                    listeners.clear();
                    listener = null;
                } else {
                    Iterator it = addrs.iterator();
                    while (it.hasNext()) {
                        listeners.remove((Integer) it.next());
                    }
                }
                noRegistrations--;
            }
        }

        protected void finalize() throws Throwable {
            if (regAddrs != null) cancel();
            super.finalize();
        }
    }

    public void propertyValue_ReadInd(int sa, int pr, int hc, int objIdx, int propID, int startIdx, int noElems) {
        Listener l = getListener(sa);
        byte[] data = null;
        if (l != null) {
            try {
                data = l.readPropertyValue(sa, pr, hc, objIdx, propID, startIdx, noElems);
            } catch (RemoteException e) {
                data = null;
            }
        }
        if (data == null) {
            data = new byte[0];
            noElems = 0;
        }
        aus.propertyValue_ReadRes(sa, pr, HopCount.DEFAULT, objIdx, propID, startIdx, noElems, data, false);
    }

    public void propertyValue_FReadInd(int sa, int pr, int hc, int objIdx, int propID, long startIdx, int noElems) {
        Listener l = getListener(sa);
        byte[] data = null;
        if (l != null) {
            try {
                data = l.readPropertyValue(sa, pr, hc, objIdx, propID, startIdx, noElems);
            } catch (RemoteException e) {
                data = null;
            }
        }
        if (data == null) {
            data = new byte[0];
            noElems = 0;
        }
        aus.propertyValue_FReadRes(sa, pr, HopCount.DEFAULT, objIdx, propID, startIdx, noElems, data, false);
    }

    public void propertyValue_WriteInd(int sa, int pr, int hc, int objIdx, int propID, int startIdx, int noElems, byte[] data) {
        Listener l = getListener(sa);
        try {
            data = (l == null) ? null : l.writePropertyValue(sa, pr, hc, objIdx, propID, startIdx, noElems, data);
        } catch (RemoteException e) {
            data = null;
        }
        if (data == null) {
            data = new byte[0];
            noElems = 0;
        }
        aus.propertyValue_ReadRes(sa, pr, HopCount.DEFAULT, objIdx, propID, startIdx, noElems, data, false);
    }

    public void propertyValue_FWriteInd(int sa, int pr, int hc, int objIdx, int propID, long startIdx, int noElems, byte[] data) {
        Listener l = getListener(sa);
        try {
            data = (l == null) ? null : l.writePropertyValue(sa, pr, hc, objIdx, propID, startIdx, noElems, data);
        } catch (RemoteException e) {
            data = null;
        }
        if (data == null) {
            data = new byte[0];
            noElems = 0;
        }
        aus.propertyValue_FWriteRes(sa, pr, HopCount.DEFAULT, objIdx, propID, startIdx, noElems, data, false);
    }

    public void propertyDescr_ReadInd(int sa, int pr, int hc, int objIdx, int propID, int propIdx) {
        Listener l = getListener(sa);
        PropertyDescr descr = null;
        if (l != null) {
            try {
                descr = l.readPropertyDescr(sa, pr, hc, objIdx, propID, propIdx);
            } catch (RemoteException e) {
                descr = null;
            }
        }
        if (descr != null) {
            aus.propertyDescr_ReadRes(sa, pr, HopCount.DEFAULT, objIdx, descr.propertyID, descr.propertyIdx, descr.type, descr.maxNoElems, descr.readLevel, descr.writeLevel, false);
        } else {
            aus.propertyDescr_ReadRes(sa, pr, HopCount.DEFAULT, objIdx, propID, propIdx, 0, 0, 0, 0, false);
        }
    }
}
