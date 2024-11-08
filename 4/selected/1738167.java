package OpenMCS.tpkt;

import java.util.ArrayList;
import java.util.Arrays;

public class ConnectionInfo {

    private int id = -1;

    private String host = null;

    private String textual = null;

    private int portNumber = -1;

    private int timeout = -1;

    private boolean enabled = false;

    private boolean realTimeDataStream = false;

    private String virtualChanelId = null;

    private String channelType = null;

    private String distrubutionType = null;

    private int priority = -1;

    private int bufferSize = 1024;

    private int tpktno = -1;

    private boolean simul = false;

    private boolean fixed = false;

    public ConnectionInfo() {
    }

    public ConnectionInfo(String line) {
        String orgLine = line;
        String tmp[] = orgLine.split(",*[ |\t]+");
        System.out.println(tmp);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getTextual() {
        return textual;
    }

    public void setTextual(String textual) {
        this.textual = textual;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRealTimeDataStream() {
        return realTimeDataStream;
    }

    public void setRealTimeDataStream(boolean realTimeDataStream) {
        this.realTimeDataStream = realTimeDataStream;
    }

    public String getVirtualChanelId() {
        return virtualChanelId;
    }

    public void setVirtualChanelId(String virtualChanelId) {
        this.virtualChanelId = virtualChanelId;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public String getDistrubutionType() {
        return distrubutionType;
    }

    public void setDistrubutionType(String distrubutionType) {
        this.distrubutionType = distrubutionType;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public int getTpktno() {
        return tpktno;
    }

    public void setTpktno(int tpktno) {
        this.tpktno = tpktno;
    }

    public boolean isSimul() {
        return simul;
    }

    public void setSimul(boolean simul) {
        this.simul = simul;
    }

    public boolean isFixed() {
        return fixed;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }
}
