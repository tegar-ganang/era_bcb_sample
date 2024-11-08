package org.thole.phiirc.client.controller.extchatlistener;

import org.thole.phiirc.client.controller.ChatListener;
import org.thole.phiirc.client.controller.Controller;
import org.thole.phiirc.client.model.Channel;
import org.thole.phiirc.client.model.Server;
import org.thole.phiirc.client.model.UserChannelPermission;

public class ECLUserList extends ExtChatListener {

    public ECLUserList(final ChatListener listener) {
        super(listener);
    }

    @Override
    public void onReply(final String value, final String msg) {
        final String[] valueToParse = value.split(" ");
        final String[] userlist = msg.split(" ");
        final String chan = valueToParse[2];
        if (Controller.getInstance().getCWatcher().getChanMap().containsKey(chan)) {
            Controller.getInstance().getCWatcher().getChan(chan).getUserList().clear();
            for (String user : userlist) {
                System.out.println("User: " + user);
                final Channel channel = Controller.getInstance().getCWatcher().getChan(chan);
                Controller.getInstance().getUWatcher().addUser(user, channel);
                UserChannelPermission chanRole;
                if (user.charAt(0) == '@') {
                    chanRole = UserChannelPermission.OPERATOR;
                    user = user.substring(1);
                } else if (user.charAt(0) == '+') {
                    chanRole = UserChannelPermission.VOICE;
                    user = user.substring(1);
                } else {
                    chanRole = UserChannelPermission.STANDARD;
                }
                Controller.getInstance().getUWatcher().getUser(user).getChannels().put(channel, chanRole);
                channel.getUserList().add(Controller.getInstance().getUWatcher().getUser(user));
            }
        }
    }
}
