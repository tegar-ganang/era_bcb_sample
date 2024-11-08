package org.charvolant.tmsnet.command;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.charvolant.tmsnet.model.ChannelDescription;
import org.charvolant.tmsnet.protocol.TMSNetField;
import org.charvolant.tmsnet.protocol.TMSNetFieldType;
import org.charvolant.tmsnet.protocol.TMSNetType;
import org.charvolant.tmsnet.protocol.TMSNetElement;

/**
 * A response to the {@link GetChannelList} command.
 * <p>
 * The result is a list of channel records.
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
@TMSNetElement(type = TMSNetType.COMMAND, id = 1)
@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)
public class GetChannelListResponse {

    /** An unused record */
    @TMSNetField(type = TMSNetFieldType.RECORD, order = 0)
    @XmlElement
    private List<ChannelDescription> channels;

    /**
   * Construct an empty response.
   */
    public GetChannelListResponse() {
        super();
        this.channels = new ArrayList<ChannelDescription>();
    }

    /**
   * Get the channel list.
   *
   * @return the channels
   */
    public List<ChannelDescription> getChannels() {
        return this.channels;
    }

    /**
   * Set the channel list.
   *
   * @param channels the channels to set
   */
    public void setChannels(List<ChannelDescription> channels) {
        this.channels = channels;
    }
}
