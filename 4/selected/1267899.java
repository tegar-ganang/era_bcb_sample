package barde.log.view;

import java.text.DateFormat;
import java.util.Date;
import barde.log.Message;

/**
 * Encapsulates a {@link barde.log.Message}.<br>
 * With the use of {@link ChannelRef} and {@link AvatarRef}, it's lighter than
 * the <tt>Message</tt> class, which is usefull for obvious memory reasons.<br>
 * TODO : keep this class ?
 * 
 * @author cbonar
 */
public class MessageRef extends Message {

    public DateFormat dateFormat;

    protected Date date;

    protected ChannelRef channel;

    protected AvatarRef avatar;

    protected String content;

    public MessageRef(ChannelRef channel, AvatarRef avatar, Message source) {
        this.dateFormat = source.getDateFormat();
        this.date = source.getDate();
        this.channel = channel;
        this.avatar = avatar;
        this.content = source.getContent();
    }

    public DateFormat getDateFormat() {
        return this.dateFormat;
    }

    public Date getDate() {
        return this.date;
    }

    public String getChannel() {
        return this.channel.getName();
    }

    public String getAvatar() {
        return this.avatar.getName();
    }

    public String getContent() {
        return this.content;
    }
}
