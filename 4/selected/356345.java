package listeners;

import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import connection.Connection;
import connection.KEllyBot;
import shared.Message;

/**
 * The listener interface for receiving message events.
 * The class that is interested in processing a message
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addMessageListener<code> method. When
 * the message event occurs, that object's appropriate
 * method is invoked.
 *
 * @see MessageEvent
 */
public class MessageListener extends ConnectionListener {

    /**
	 * Instantiates a new message listener.
	 *
	 * @param nc the nc
	 */
    public MessageListener(Connection nc) {
        super(nc);
    }

    @Override
    public void onMessage(MessageEvent<KEllyBot> event) throws Exception {
        super.onMessage(event);
        manageMessage(new Message(nc, event.getMessage(), event.getUser(), event.getChannel(), Message.MSG));
    }

    @Override
    public void onAction(ActionEvent<KEllyBot> event) throws Exception {
        super.onAction(event);
        manageMessage(new Message(nc, event.getAction(), event.getUser(), event.getChannel(), Message.ACTION));
    }

    @Override
    public void onNotice(NoticeEvent<KEllyBot> event) throws Exception {
        super.onNotice(event);
        manageMessage(new Message(nc, "NOTICE: " + event.getNotice(), event.getUser(), null, Message.NOTICE));
    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent<KEllyBot> event) throws Exception {
        super.onPrivateMessage(event);
        manageMessage(new Message(nc, event.getMessage(), event.getUser().getNick(), event.getUser().getNick(), Message.PM));
    }
}
