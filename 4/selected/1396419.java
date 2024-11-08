package com.wisc.csvParser.plugins;

import com.wisc.csvParser.*;
import java.util.ArrayList;
import javax.swing.JPanel;
import org.jdom.Element;
import com.rbnb.sapi.*;
import java.util.Hashtable;
import java.util.Enumeration;

/**
 *
 * @author lawinslow
 */
public class DataRepositoryDT implements IDataRepository {

    public enum events {

        started, stopped, reconfigured, valuePassed, valueBuffered, valueDeBuffered
    }

    public static final int DT_ARCHIVE_SIZE = 20000;

    public static final int DT_CACHE_SIZE = 500;

    public static final String DT_HOST_TAG = "DtHost";

    private String rbnbHost = "localhost";

    private String port = "3333";

    private Hashtable<String, DTObject> site2dtObject = new Hashtable<String, DTObject>();

    private boolean disconnected = false;

    private boolean started = false;

    private long valsPassed = 0;

    private ArrayList<ValueObject> buffer = new ArrayList<ValueObject>();

    private JPanelDT statusPanel;

    private ArrayList<DTListener> listeners = new ArrayList<DTListener>();

    private String panelID;

    private IDataRepository child;

    public DataRepositoryDT() {
        panelID = getRepositoryShortname() + Integer.toString((new java.util.Random()).nextInt());
    }

    private void NewValueLocal(ValueObject val) {
        if (disconnected) {
        }
        try {
            if (!site2dtObject.containsKey(sourceNameFromVal(val))) newDTObject(val);
            site2dtObject.get(sourceNameFromVal(val)).putValue(val);
            valsPassed++;
            raiseEvent(events.valuePassed);
        } catch (SAPIException sapi) {
            sapi.printStackTrace();
            disconnected = true;
        }
    }

    private String sourceNameFromVal(ValueObject val) {
        return val.getSite() + val.getAggSpan().replace(":", "_");
    }

    private void newDTObject(ValueObject val) throws SAPIException {
        site2dtObject.put(sourceNameFromVal(val), new DTObject(sourceNameFromVal(val)));
    }

    private void tryReconnect() {
        Enumeration<DTObject> e = site2dtObject.elements();
        boolean result;
        if (e.hasMoreElements()) {
            result = e.nextElement().reconnect();
        } else {
            result = false;
        }
        disconnected = !result;
        if (!disconnected) {
            flushCached();
        }
    }

    private void flushCached() {
        for (ValueObject v : buffer) NewValueLocal(v);
        buffer.clear();
        raiseEvent(events.valueDeBuffered);
    }

    public void addListener(DTListener l) {
        listeners.add(l);
    }

    public void removeListener(DTListener l) {
        listeners.remove(l);
    }

    public void raiseEvent(events e) {
        for (DTListener l : listeners) {
            l.eventRaised(e);
        }
    }

    public long getValsPassed() {
        return valsPassed;
    }

    public int getValsBuffered() {
        return buffer.size();
    }

    public String getDtHost() {
        return rbnbHost;
    }

    public void setDtHost(String h) {
        rbnbHost = h;
    }

    @Override
    public boolean NewRow(ArrayList<ValueObject> newRow) {
        if (child != null) {
            child.NewRow(newRow);
        }
        for (ValueObject val : newRow) NewValueLocal(val);
        return true;
    }

    @Override
    public boolean NewValue(ValueObject newValue) {
        if (child != null) {
            child.NewValue(newValue);
        }
        NewValueLocal(newValue);
        return true;
    }

    @Override
    public boolean Start() {
        if (child != null) {
            if (!child.Start()) return false;
        }
        try {
            Source s = new Source();
            s.OpenRBNBConnection(rbnbHost + ":" + port, "test", "", "");
            s.CloseRBNBConnection();
            started = true;
            raiseEvent(events.started);
            return true;
        } catch (SAPIException sapi) {
            return false;
        }
    }

    @Override
    public boolean Stop() {
        boolean thisResult = true;
        Enumeration<DTObject> e = site2dtObject.elements();
        while (e.hasMoreElements()) {
            e.nextElement().close();
        }
        started = false;
        raiseEvent(events.stopped);
        if (child != null) {
            return thisResult && child.Stop();
        } else {
            return thisResult;
        }
    }

    @Override
    public void configure(Element e) throws Exception {
        rbnbHost = e.getChild(DT_HOST_TAG).getText();
        raiseEvent(events.reconfigured);
    }

    @Override
    public IDataRepository getChildRepository() {
        return child;
    }

    @Override
    public String getRepositoryDescription() {
        return "This repository places all values in automatically generated " + "channels in a specified dataturbine server";
    }

    @Override
    public String getRepositoryShortname() {
        return "DT Repository";
    }

    @Override
    public Element getSettingsXml() {
        Element e = new Element(IDataRepository.DATA_REPOSITORY_TAG);
        e.setAttribute("type", DataRepositoryDT.class.getName());
        e.addContent(new Element(DT_HOST_TAG).setText(rbnbHost));
        return e;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void setChildRepository(IDataRepository child) {
        this.child = child;
    }

    @Override
    public String getPanelID() {
        return panelID;
    }

    @Override
    public JPanel getStatusJPanel() {
        if (statusPanel == null) {
            statusPanel = new JPanelDT(this);
        }
        return statusPanel;
    }

    @Override
    public String toString() {
        return getRepositoryShortname();
    }

    public class DTObject {

        private String sourceName;

        private Source source;

        private ChannelMap cmap = new ChannelMap();

        private ChannelMap flushMap = new ChannelMap();

        private double tmpTime[] = { 0.0 };

        private double tmpVal[] = { 0.0 };

        private ArrayList<String> allChannels = new ArrayList<String>();

        private ArrayList<String> allMetadata = new ArrayList<String>();

        private Hashtable<String, String> metaData2chan = new Hashtable<String, String>();

        boolean closed = false;

        public DTObject(String sourceName) throws SAPIException {
            this.sourceName = sourceName;
            source = new Source(1000, "append", 20000);
            source.OpenRBNBConnection(rbnbHost + ":" + port, sourceName, "", "");
        }

        public void putValue(ValueObject val) throws SAPIException {
            if (!metaData2chan.containsKey(val.getMetadataXml().getValue())) {
                registerNewChannel(val);
            }
            tmpTime[0] = val.getTimeStamp().getTime() / 1000.0;
            tmpVal[0] = val.getValue();
            cmap.PutTimes(tmpTime);
            cmap.PutDataAsFloat64(cmap.GetIndex(getChannelName(val)), tmpVal);
            source.Flush(cmap);
        }

        public String getChannelName(ValueObject val) {
            return metaData2chan.get(val.getMetadataXml().getValue());
        }

        public void registerNewChannel(ValueObject val) throws SAPIException {
            source.Detach();
            source = new Source(DT_CACHE_SIZE, "append", DT_ARCHIVE_SIZE);
            source.OpenRBNBConnection(rbnbHost, sourceName, "", "");
            cmap = new ChannelMap();
            flushMap = new ChannelMap();
            int idx;
            String chnName;
            if (val.getOffsetValue() != Double.NaN) {
                chnName = val.getVariable() + "_" + val.getUnit().replace("/", "") + "_" + Double.toString(val.getOffsetValue());
            } else {
                chnName = val.getVariable() + "_" + val.getUnit().replace("/", "");
            }
            allChannels.add(chnName);
            allMetadata.add(val.getMetadataXml().getValue());
            for (int i = 0; i < allChannels.size(); i++) {
                cmap.PutUserInfo(cmap.Add(allChannels.get(i)), allMetadata.get(i));
            }
            metaData2chan.put(val.getMetadataXml().getValue(), chnName);
            source.Register(cmap);
        }

        public boolean reconnect() {
            if (closed) return false;
            try {
                source.OpenRBNBConnection(rbnbHost, sourceName, "", "");
                return true;
            } catch (SAPIException sapi) {
                return false;
            }
        }

        public void close() {
            closed = true;
            source.Detach();
        }
    }

    public interface DTListener {

        public void eventRaised(events e);
    }
}
