package org.charvolant.tmsnet.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.charvolant.tmsnet.command.ChangeChannel;
import org.charvolant.tmsnet.command.GetCurrentChannel;
import org.charvolant.tmsnet.command.GetCurrentChannelResponse;
import org.charvolant.tmsnet.model.Station;

/**
 * A transaction to change the channel.
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
@XmlRootElement()
@XmlAccessorType(XmlAccessType.NONE)
public class ChangeChannelTransaction extends Transaction<Transactable> {

    /** The event that the timer was for */
    @XmlElement
    private Station station;

    /**
   * Construct an empty transaction
   *
   */
    public ChangeChannelTransaction() {
        super();
    }

    /**
   * Construct a transaction for a specific channel
   */
    public ChangeChannelTransaction(Station station) {
        super();
        this.station = station;
    }

    /**
   * Start the ball rolling by requesting a channel change.
   *
   * @see org.charvolant.tmsnet.client.Transaction#onExecute()
   */
    @Override
    protected void onExecute() {
        super.onExecute();
        this.queue(new ChangeChannel(this.station.getIndex()));
    }

    /**
   * We've changed the channel, now get the current channel information.
   */
    protected void onChangedChannel() {
        this.queue(new GetCurrentChannel());
    }

    /**
   * Capture the current channel information.
   * 
   * @param response The response
   */
    protected void onCurrentChannel(GetCurrentChannelResponse response) {
        this.client.getState().setCurrentChannel(response.getChannel());
    }
}
