package org.iptc.nar.core.model.message;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import org.iptc.nar.interfaces.datatype.Int1to9Type;
import org.iptc.nar.interfaces.model.message.MessageHeaderType;

public class MessageHeaderTypeImpl implements MessageHeaderType {

    /**
	 * The date and time of transmission of the message.
	 */
    private Calendar m_dateOfTransmission;

    /**
	 * The sender of the items, which may be an organisation or a person.
	 */
    private String m_sender;

    /**
	 * The transmission identifier associated with the message.
	 */
    private String m_transmissionIdentifier;

    /**
	 * The priority of transmission.
	 */
    private Int1to9Type m_priority;

    /**
	 * The point of origin of the transmission of the message.
	 */
    private String m_origin;

    /**
	 * The point(s) of destination of the message.
	 */
    private String m_destination;

    /**
	 * A transmission channel used by the message.
	 */
    private List<String> m_channels = new LinkedList<String>();

    public Calendar getDateOfTransmission() {
        return m_dateOfTransmission;
    }

    public void setDateOfTransmission(Calendar date) {
        this.m_dateOfTransmission = date;
    }

    public String getDestination() {
        return m_destination;
    }

    public void setDestination(String destination) {
        this.m_destination = destination;
    }

    public void addChannel(String channel) {
        m_channels.add(channel);
    }

    public List<String> getChannels() {
        return m_channels;
    }

    public void setChannels(List<String> lchannels) {
        this.m_channels = lchannels;
    }

    public String getOrigin() {
        return m_origin;
    }

    public void setOrigin(String origin) {
        this.m_origin = origin;
    }

    public Int1to9Type getPriority() {
        return m_priority;
    }

    public void setPriority(Int1to9Type priority) {
        this.m_priority = priority;
    }

    public String getSender() {
        return m_sender;
    }

    public void setSender(String sender) {
        this.m_sender = sender;
    }

    public String getTransmissionIdentifier() {
        return m_transmissionIdentifier;
    }

    public void setTransmissionIdentifier(String transmitId) {
        this.m_transmissionIdentifier = transmitId;
    }
}
