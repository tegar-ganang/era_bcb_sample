package skycastle.chat.channel;

import skycastle.chat.ChatAvatar;
import skycastle.chat.event.ChatEvent;
import skycastle.chat.event.ChatEventImpl;
import java.util.HashSet;
import java.util.Set;

/**
 * Implements common functionality of a channel.
 *
 * @author Hans H�ggstr�m
 */
public abstract class AbstractChannel implements Channel {

    private final String myChannelIdentifier;

    /**
     * Keeps track of ChatAvatar listening to this instance.
     */
    private Set<ChatAvatar> myChatAvatars = new HashSet<ChatAvatar>(51);

    public String getChannelIdentifier() {
        return myChannelIdentifier;
    }

    public void join(ChatAvatar chatAvatar) {
        if (chatAvatar != null && !myChatAvatars.contains(chatAvatar)) {
            myChatAvatars.add(chatAvatar);
            onChatEvent(new ChatEventImpl(chatAvatar.getNickname() + " joined channel " + getChannelIdentifier() + "."));
        }
    }

    public void leave(ChatAvatar chatAvatar) {
        if (chatAvatar != null && myChatAvatars.contains(chatAvatar)) {
            myChatAvatars.remove(chatAvatar);
        }
    }

    public boolean isInChannel(final ChatAvatar chatAvatar) {
        return myChatAvatars.contains(chatAvatar);
    }

    public void onChatEvent(ChatEvent event) {
        for (ChatAvatar chatAvatar : myChatAvatars) {
            chatAvatar.onChatEvent(event);
        }
    }

    /**
     * @param channelIdentifier the name of this channel.
     */
    protected AbstractChannel(final String channelIdentifier) {
        myChannelIdentifier = channelIdentifier;
    }
}
