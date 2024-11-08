import java.util.logging.Logger;
import com.rubixinfotech.SKJava.*;
import com.rubixinfotech.SKJava.Messages.EXS.*;

public class Leg implements SKJEventListener {

    static Logger logger;

    protected SicapSim ss;

    protected SKJConnection csp;

    protected SKJava sk;

    protected int span;

    protected int channel;

    protected CallRecord cr;

    protected char type;

    Leg(SicapSim _ss, SKJConnection _csp, CallRecord _cr, int _span, int _channel, char _type) {
        if (logger == null) {
            logger = java.util.logging.Logger.getLogger("SicapSim." + this.getClass().getName());
            try {
                sk = SKJava.instance();
            } catch (Exception e) {
            }
        }
        ss = _ss;
        csp = _csp;
        span = _span;
        channel = _channel;
        cr = _cr;
        type = _type;
    }

    public void setSpan(int _span) {
        span = _span;
    }

    public int getSpan() {
        return span;
    }

    public void setChannel(int _channel) {
        channel = _channel;
    }

    public int getChannel() {
        return channel;
    }

    public boolean release() {
        logger.finest("Leg::release()");
        if ((span != -1) && (channel != -1)) {
            XL_ReleaseChannel rc = new XL_ReleaseChannel();
            rc.setSpanA(span);
            rc.setChannelA(channel);
            rc.setSpanB(span);
            rc.setChannelB(channel);
            csp.sendMessage(rc, this);
        }
        return true;
    }

    public boolean connect(int otherSpan, int otherChannel) {
        logger.finest("Leg::connect()");
        if ((span != -1) && (channel != -1) && (otherSpan != -1) && (otherChannel != -1)) {
            XL_Connect cc = new XL_Connect();
            cc.setSpanA(span);
            cc.setChannelA(channel);
            cc.setSpanB(otherSpan);
            cc.setChannelB(otherChannel);
            csp.sendMessage(cc, this);
        }
        return true;
    }

    public boolean onEvent(SKJMessage msg) {
        boolean result = false;
        logger.finest("Leg::onEvent() got a message!");
        if (msg instanceof XL_ChannelReleasedWithData) {
            XL_ChannelReleasedWithData crwd = (XL_ChannelReleasedWithData) msg;
            if ((crwd.getSpan() == span) && (crwd.getChannel() == channel)) {
                logger.finest("Leg RELEASED");
                Integer hashKey = new Integer((span * 100) + channel);
                ss.channelLeg.remove(hashKey);
                span = -1;
                channel = -1;
                result = true;
            }
        } else if (msg instanceof XL_CallProcessingEvent) {
            XL_CallProcessingEvent cpe = (XL_CallProcessingEvent) msg;
            logger.finest("Leg received a Call Processing Event");
            if ((cpe.getSpan() == span) && (cpe.getChannel() == channel)) {
                switch(cpe.getEvent()) {
                    case 0x20:
                        {
                            logger.finest("Leg ANSWERED");
                            if (type == 'A') {
                                logger.finest("Starting digit collection on A Leg");
                                XL_ParkChannel pc = new XL_ParkChannel();
                                pc.setSpanA(span);
                                pc.setChannelA(channel);
                                pc.setSpanB(span);
                                pc.setChannelB(channel);
                                csp.sendMessage(pc, this);
                                XL_CollectDigitString cds = new XL_CollectDigitString();
                                cds.setSpan(span);
                                cds.setChannel(channel);
                                cds.setMode(0x04);
                                cds.setMaxDigits(10);
                                cds.setNumTermChars(1);
                                cds.setConfigBits(0x20);
                                cds.setTermChars(0xf000);
                                cds.setInterDigitTimer(0x01f4);
                                cds.setFirstDigitTimer(0x0bb8);
                                cds.setCompletionTimer(0x0bb8);
                                cds.setMinReceiveDigitDuration(3);
                                cds.setAddressSignallingType(0x01);
                                cds.setNumDigitStrings(1);
                                cds.setResumeDigitCltnTimer(1);
                                csp.sendMessage(cds, this);
                                XL_RecAnnConnect rac = new XL_RecAnnConnect();
                                rac.setSpan(span);
                                rac.setChannel(channel);
                                rac.setConfig(0x00);
                                rac.setEvent(0x03);
                                rac.setCnt(1);
                                rac.setID1(0x6f);
                                csp.sendMessage(rac, this);
                            } else {
                                logger.finest("Starting prompts on B Leg");
                                XL_RecAnnConnect rac = new XL_RecAnnConnect();
                                rac.setSpan(span);
                                rac.setChannel(channel);
                                rac.setConfig(0x00);
                                rac.setEvent(0x03);
                                rac.setCnt(2);
                                rac.setID1(0x5e);
                                rac.setID2(0x5f);
                                csp.sendMessage(rac, this);
                            }
                            break;
                        }
                    case 0x25:
                        {
                            logger.finest("Prompt started");
                            break;
                        }
                    case 0x26:
                        {
                            logger.finest("Prompt completed");
                            break;
                        }
                    case 0x02:
                        {
                            byte[] digitData = cpe.getData();
                            int digitLength = digitData[4];
                            String digits = SKJMessage.printableFormat(digitData).substring(4 * 3 + 1, 4 * 3 + 1 + digitLength * 3).replaceAll(":", "");
                            logger.finest("Received a digit string: " + digits + " of length:" + digitLength);
                            break;
                        }
                    default:
                        {
                            logger.warning("Leg received unknown event of: " + cpe.getEvent());
                            break;
                        }
                }
                result = true;
            }
        } else if (msg instanceof XL_ReleaseChannelAck) {
            XL_ReleaseChannelAck ack = (XL_ReleaseChannelAck) msg;
            logger.finest("Leg::onEvent: received ReleaseChannel Ack with status: " + SKJava.statusText(ack.getStatus()));
            result = true;
        } else if (msg instanceof XL_ConnectAck) {
            XL_ConnectAck ack = (XL_ConnectAck) msg;
            logger.finest("Leg::onEvent: received Connect Ack with status: " + SKJava.statusText(ack.getStatus()));
            result = true;
        } else if (msg instanceof XL_ParkChannelAck) {
            XL_ParkChannelAck ack = (XL_ParkChannelAck) msg;
            logger.finest("Leg::onEvent: received Park Channel Ack with status: " + SKJava.statusText(ack.getStatus()));
            result = true;
        } else if (msg instanceof XL_CollectDigitStringAck) {
            XL_CollectDigitStringAck ack = (XL_CollectDigitStringAck) msg;
            logger.finest("Leg::onEvent: received Collect Digit String Ack with status: " + SKJava.statusText(ack.getStatus()));
            result = true;
        } else if (msg instanceof XL_RecAnnConnectAck) {
            XL_RecAnnConnectAck ack = (XL_RecAnnConnectAck) msg;
            logger.finest("Leg::onEvent: received Recorded Announcement Connect Ack with status: " + SKJava.statusText(ack.getStatus()));
            result = true;
        }
        return result;
    }

    public void onSent(SKJMessage msg, int Status) {
        logger.finest("Leg::onSent() called for " + msg.ID + " with a status of: " + Status);
        return;
    }
}
