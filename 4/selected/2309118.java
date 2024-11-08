package com.peterhi.net.server.sgs.ref;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import com.peterhi.net.conv.impl.*;
import com.peterhi.net.conv.Converter;
import com.peterhi.net.conv.Convertible;
import com.peterhi.net.server.sgs.SGSBusinessProcess;
import com.peterhi.net.server.sgs.SGSChannelData;
import com.peterhi.net.server.sgs.SGSClientData;
import com.peterhi.net.server.sgs.SGSServer;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.ClientSessionListener;
import com.sun.sgs.app.ManagedReference;

public class SGSClientSessionListener implements Serializable, ClientSessionListener, Cloneable {

    /**
	 * Serializable ID
	 */
    private static final long serialVersionUID = 1727076524846899350L;

    private ManagedReference server;

    private ClientSession session;

    public SGSClientSessionListener(ManagedReference server, ClientSession session) {
        this.server = server;
        this.session = session;
    }

    public SGSServer getServer() {
        return server.get(SGSServer.class);
    }

    public void disconnected(boolean graceful) {
        server.get(SGSServer.class).removeClientData(SGSBusinessProcess.accFromSesName(session.getName()));
    }

    @SuppressWarnings("unchecked")
    public void receivedMessage(byte[] bytes) {
        try {
            Convertible conv = Converter.getInstance().revert(bytes);
            if (conv instanceof QueryChannel) {
                SGSBusinessProcess.doQueryChannel(server.get(SGSServer.class), session, (QueryChannel) conv);
            } else if (conv instanceof EnterChannel) {
                SGSBusinessProcess.doEnterChannel(server.get(SGSServer.class), session, (EnterChannel) conv);
                SGSChannelData chnlData = getChannelData(session);
                HashSet<AbstractDrawing> set = (HashSet<AbstractDrawing>) chnlData.get(SGSChannelData.WHITEBOARD_DATA);
                for (Iterator itor = set.iterator(); itor.hasNext(); ) {
                    AbstractDrawing cur = (AbstractDrawing) itor.next();
                    session.send(Converter.getInstance().convert(cur));
                }
            } else if (conv instanceof LeaveChannel) {
                SGSBusinessProcess.doLeaveChannel(server.get(SGSServer.class), session, (LeaveChannel) conv);
            } else if (conv instanceof Logout) {
                SGSBusinessProcess.doLogout(server.get(SGSServer.class), session, (Logout) conv);
            } else if (conv instanceof QueryPeer) {
                SGSBusinessProcess.doQueryPeer(server.get(SGSServer.class), session, (QueryPeer) conv);
            } else if (conv instanceof AbstractDrawing) {
                SGSChannelData chnlData = getChannelData(session);
                chnlData.get(SGSChannelData.WHITEBOARD_DATA, HashSet.class).add((AbstractDrawing) conv);
                Channel chnl = chnlData.getChannel();
                SGSBusinessProcess.channelwideDispatch(conv, session, chnl);
            } else if (conv instanceof Clear) {
                SGSChannelData chnlData = getChannelData(session);
                chnlData.get(SGSChannelData.WHITEBOARD_DATA, HashSet.class).clear();
                Channel chnl = chnlData.getChannel();
                SGSBusinessProcess.channelwideDispatch(conv, session, chnl);
            } else if (conv instanceof OpenMic) {
                OpenMic om = (OpenMic) conv;
                String account = SGSBusinessProcess.accFromSesName(session.getName());
                SGSClientData data = getServer().getClientData(account);
                String channelName = data.get(SGSClientData.CURRENT_CHANNEL_NAME, String.class);
                SGSChannelData chnlData = getServer().getChannelData(channelName);
                data.set(SGSClientData.TALKING, Boolean.TRUE);
                NOpenMic tom = new NOpenMic();
                tom.nid = om.nid;
                getServer().sendToNServer(Converter.getInstance().convert(tom));
                Channel chnl = chnlData.getChannel();
                SGSBusinessProcess.channelwideDispatch(conv, session, chnl);
            } else if (conv instanceof CloseMic) {
                CloseMic cm = (CloseMic) conv;
                String account = SGSBusinessProcess.accFromSesName(session.getName());
                SGSClientData data = getServer().getClientData(account);
                String channelName = data.get(SGSClientData.CURRENT_CHANNEL_NAME, String.class);
                SGSChannelData chnlData = getServer().getChannelData(channelName);
                data.set(SGSClientData.TALKING, Boolean.FALSE);
                NCloseMic tcm = new NCloseMic();
                tcm.nid = cm.nid;
                getServer().sendToNServer(Converter.getInstance().convert(tcm));
                Channel chnl = chnlData.getChannel();
                SGSBusinessProcess.channelwideDispatch(conv, session, chnl);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private SGSChannelData getChannelData(ClientSession ses) {
        String account = SGSBusinessProcess.accFromSesName(session.getName());
        SGSClientData data = getServer().getClientData(account);
        String channelName = data.get(SGSClientData.CURRENT_CHANNEL_NAME, String.class);
        return getServer().getChannelData(channelName);
    }
}
