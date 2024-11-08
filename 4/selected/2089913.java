package com.rubixinfotech.SKJava.Messages.ISDN;

import com.rubixinfotech.SKJava.SKJMessage;
import com.rubixinfotech.SKJava.Data.ISDN.ICB_ISDN_Raw;
import com.rubixinfotech.SKJava.Data.ISDN.IE_CalledPartyNumber;
import com.rubixinfotech.SKJava.Data.ISDN.IE_CallingPartyNumber;
import com.rubixinfotech.SKJava.ITU.Q931;
import com.rubixinfotech.SKJava.Messages.EXS.XL_RFSWithData;

public class ISDN_Setup extends SKJMessage {

    protected ICB_ISDN_Raw icb;

    protected IE_CallingPartyNumber cgpn;

    protected IE_CalledPartyNumber cdpn;

    private int span;

    private byte channel;

    public ISDN_Setup(XL_RFSWithData original, ICB_ISDN_Raw icb) {
        this.icb = icb;
        this.span = original.getSpan();
        this.channel = original.getChannel();
        this.cgpn = (IE_CallingPartyNumber) icb.getField(Q931.IE.CALLING_PARTY_NUMBER);
        this.cdpn = (IE_CalledPartyNumber) icb.getField(Q931.IE.CALLED_PARTY_NUMBER);
    }

    public ICB_ISDN_Raw getICB() {
        return icb;
    }

    public void setICB(ICB_ISDN_Raw icb) {
        this.icb = icb;
    }

    public IE_CallingPartyNumber getCGPN() {
        return this.cgpn;
    }

    public void setCGPN(IE_CallingPartyNumber cgpn) {
        this.cgpn = cgpn;
    }

    public IE_CalledPartyNumber getCDPN() {
        return this.cdpn;
    }

    public void setCDPN(IE_CalledPartyNumber cdpn) {
        this.cdpn = cdpn;
    }

    public int getSpan() {
        return span;
    }

    public void setSpan(int span) {
        this.span = span;
    }

    public byte getChannel() {
        return channel;
    }

    public void setChannel(byte channel) {
        this.channel = channel;
    }
}
