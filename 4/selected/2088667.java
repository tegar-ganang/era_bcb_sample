package com.peterhi.net.msg;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import com.peterhi.StatusCode;
import com.peterhi.beans.ChannelBean;
import com.peterhi.beans.Role;
import com.peterhi.io.IO;

public class EChnlRsp extends AbstractMSG {

    private StatusCode statusCode;

    private ChannelBean channelBean;

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(StatusCode code) {
        this.statusCode = code;
    }

    public ChannelBean getChannelBean() {
        return channelBean;
    }

    public void setChannelBean(ChannelBean bean) {
        this.channelBean = bean;
    }

    @Override
    protected void readData(DataInputStream in) throws IOException {
        statusCode = StatusCode.toStatusCode(in.readInt());
        readChannelBean(in);
    }

    @Override
    protected void writeData(DataOutputStream out) throws IOException {
        if (statusCode == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(statusCode.ordinal());
        }
        writeChannelBean(out);
    }

    private void readChannelBean(DataInputStream in) throws IOException {
        boolean isNotNull = in.readBoolean();
        if (isNotNull) {
            channelBean = new ChannelBean();
            channelBean.setHashCode(in.readInt());
            channelBean.setName(IO.readString(in));
            channelBean.setRole(Role.toRole(in.readInt()));
        }
    }

    private void writeChannelBean(DataOutputStream out) throws IOException {
        if (channelBean == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeInt(channelBean.getHashCode());
            IO.writeString(out, channelBean.getName());
            if (channelBean.getRole() == null) {
                out.writeInt(Role.DontCare.ordinal());
            } else {
                out.writeInt(channelBean.getRole().ordinal());
            }
        }
    }

    public int getID() {
        return RSP_ECHNL;
    }
}
