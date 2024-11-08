package org.charvolant.tmsnet.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import org.charvolant.tmsnet.command.GetCurrentChannel;
import org.charvolant.tmsnet.command.GetCurrentChannelResponse;
import org.charvolant.tmsnet.command.GetPlayInfo;
import org.charvolant.tmsnet.command.GetPlayInfoResponse;
import org.charvolant.tmsnet.command.GetRecInfo;
import org.charvolant.tmsnet.command.GetRecInfoResponse;
import org.charvolant.tmsnet.model.Channel;
import org.charvolant.tmsnet.model.Event;
import org.charvolant.tmsnet.model.EventDescription;
import org.charvolant.tmsnet.model.ServiceType;

/**
 * A transaction to get an update on the state of the PVR.
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
@XmlRootElement()
@XmlAccessorType(XmlAccessType.NONE)
public class UpdateTransaction extends Transaction<Transactable> {

    /**
   * Construct an empty transaction
   *
   */
    public UpdateTransaction() {
        super();
    }

    /**
   * Start the ball rolling by requesting a channel update.
   *
   * @see org.charvolant.tmsnet.client.Transaction#onExecute()
   */
    @Override
    protected void onExecute() {
        super.onExecute();
        this.queue(new GetCurrentChannel());
    }

    /**
   * Capture the current channel information.
   * 
   * @param response The response
   */
    protected void onGetCurrentChannelResponse(GetCurrentChannelResponse response) {
        this.client.getState().setCurrentChannel(response.getChannel());
    }

    /**
   * Get playback information
   */
    protected void onGetPlayInfo() {
        this.queue(new GetPlayInfo());
    }

    /**
   * Capture the playback information.
   * 
   * @param response The response
   */
    protected void onGetPlayInfoResponse(GetPlayInfoResponse response) {
        EventDescription ed = response.getEvent();
        Channel channel = ed == null ? null : this.state.getEPG(ServiceType.TV, ed.getServiceId());
        Event event = channel == null ? null : new Event(channel, ed, channel.getResource());
        this.state.setPlayInfo(response.getPlayInfo(), event, response.getFileInfo());
    }

    /**
   * Get the current recording information
   */
    protected void onGetRecInfo() {
        this.queue(new GetRecInfo());
    }

    /**
   * Capture the current recording information.
   * 
   * @param response The response
   */
    protected void onGetRecInfoResponse(GetRecInfoResponse response) {
        this.client.getState().setRecordings(response.getRecordings());
    }
}
