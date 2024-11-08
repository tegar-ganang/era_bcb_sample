package org.charvolant.tmsnet.command;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.charvolant.tmsnet.model.ChannelSelection;
import org.charvolant.tmsnet.protocol.TMSNetField;
import org.charvolant.tmsnet.protocol.TMSNetFieldType;
import org.charvolant.tmsnet.protocol.TMSNetType;
import org.charvolant.tmsnet.protocol.TMSNetElement;

/**
 * A response to the {@link GetCurrentChannel} command.
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
@TMSNetElement(type = TMSNetType.COMMAND, id = 31)
@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)
public class GetCurrentChannelResponse {

    /** Total disk space */
    @TMSNetField(type = TMSNetFieldType.RECORD, order = 0)
    @XmlElement
    private ChannelSelection channel;

    /**
   * COnstruct an empty channel response.
   */
    public GetCurrentChannelResponse() {
        super();
        this.channel = new ChannelSelection();
    }

    /**
   * Get the current channel selection
   * 
   * @return the channel selection
   */
    public ChannelSelection getChannel() {
        return this.channel;
    }

    /**
   * Set the current channel.
   *
   * @param channel the current channel to set
   */
    public void setChannel(ChannelSelection channel) {
        this.channel = channel;
    }
}
