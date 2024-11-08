package net.sf.opendf.cal.interpreter;

/**
 * @author Christopher Chang <cbc@eecs.berkeley.edu>
 */
public class ChannelID {

    private String _portname;

    private int _channelNumber;

    public String getPortName() {
        return _portname;
    }

    public int getChannelNumber() {
        return _channelNumber;
    }

    public ChannelID(String portname, int channelNumber) {
        this._portname = portname;
        this._channelNumber = channelNumber;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChannelID)) return false;
        final ChannelID channelID = (ChannelID) o;
        if (_channelNumber != channelID._channelNumber) return false;
        if (!_portname.equals(channelID._portname)) return false;
        return true;
    }

    public int hashCode() {
        int result;
        result = _portname.hashCode();
        result = 29 * result + _channelNumber;
        return result;
    }
}
