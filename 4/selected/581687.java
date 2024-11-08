package gov.sns.xal.tools.widgets;

import gov.sns.ca.*;

/**
 * @author sako
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class XALSimpleControlPanel extends XALAbstractControlPanel {

    Channel channel;

    public XALSimpleControlPanel(String chname) {
        super(chname);
    }

    @Override
    public void readChannel() {
        if (stat) {
            try {
                chvalue = String.valueOf(channel.getValDbl());
            } catch (Exception e) {
                chvalue = "-";
                stat = false;
            }
        } else {
            chvalue = "-";
        }
    }

    @Override
    public void writeChannel(double value) {
        if (stat) {
            try {
                channel.putVal(value);
                chvalue = String.valueOf(channel.getValDbl());
            } catch (Exception e) {
                chvalue = "-";
                stat = false;
            }
        } else {
            chvalue = "-";
        }
    }

    @Override
    public void writeChannel() {
    }

    @Override
    public void connectChannel() {
        channel = ChannelFactory.defaultFactory().getChannel(channelName);
        stat = channel.connectAndWait();
    }
}
