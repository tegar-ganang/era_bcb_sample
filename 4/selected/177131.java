package com.google.code.cubeirc.room;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.pircbotx.Channel;
import org.pircbotx.User;
import com.google.code.cubeirc.base.Base;
import com.google.code.cubeirc.connection.Connection;
import com.google.code.cubeirc.connection.data.ChannelResponse;
import com.google.code.cubeirc.connection.data.PrivateMessageResponse;
import com.google.code.cubeirc.queue.MessageQueue;
import com.google.code.cubeirc.queue.MessageQueueEnum;
import com.google.code.cubeirc.queue.MessageQueueEvent;
import com.google.code.cubeirc.tab.TabManager;
import com.google.code.cubeirc.ui.ChannelForm;
import com.google.code.cubeirc.ui.PrivateMessageForm;

public class RoomManager extends Base {

    @Getter
    @Setter
    private Connection connection;

    public RoomManager(String name, Connection c) {
        super(name);
        setConnection(c);
    }

    @Override
    public void actionPerformed(MessageQueueEvent e) {
        if (e.getMsgtype() == MessageQueueEnum.MSG_PRIVATE_IN || e.getMsgtype() == MessageQueueEnum.MSG_PRIVATE_OUT) {
            handlePrivateMessage(e.getMsgtype(), (PrivateMessageResponse) e.getData());
        }
        if (e.getMsgtype() == MessageQueueEnum.CHANNEL_JOIN) {
            handleChannelJoin(e.getMsgtype(), (ChannelResponse) e.getData());
        }
        super.actionPerformed(e);
    }

    private void handlePrivateMessage(final MessageQueueEnum type, final PrivateMessageResponse data) {
        asyncExec(new Runnable() {

            @Override
            public void run() {
                User dest;
                if (type == MessageQueueEnum.MSG_PRIVATE_IN) dest = data.getSender(); else dest = data.getDestination();
                if (!TabManager.checkTabExists(dest.getNick())) {
                    TabManager.addTab(dest.getNick(), "/com/google/code/cubeirc/resources/img_message.png", true, PrivateMessageForm.class.getName(), new Object[] { TabManager.getTabfolder().getParent(), SWT.NORMAL, dest.getNick(), dest }, new Class[] { Composite.class, int.class, String.class, User.class });
                    if (!data.getMessage().isEmpty()) MessageQueue.addQueue(type, data);
                }
            }
        });
    }

    private void handleChannelJoin(final MessageQueueEnum type, final ChannelResponse data) {
        asyncExec(new Runnable() {

            @Override
            public void run() {
                if (data.getUser().equals(Connection.getUserInfo())) {
                    if (!TabManager.checkTabExists(data.getChannel().getName().toLowerCase())) {
                        String channel = data.getChannel().getName().toLowerCase();
                        TabManager.addTab(channel, "/com/google/code/cubeirc/resources/img_channel.png", true, ChannelForm.class.getName(), new Object[] { TabManager.getTabfolder().getParent(), SWT.NORMAL, channel, data.getChannel() }, new Class[] { Composite.class, int.class, String.class, Channel.class });
                        MessageQueue.addQueue(type, data);
                    }
                }
            }
        });
    }
}
