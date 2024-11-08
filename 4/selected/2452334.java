package de.boardgamesonline.bgo2.webserver.wicket.util;

import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import wicket.ajax.AjaxRequestTarget;
import wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import wicket.ajax.markup.html.form.AjaxSubmitLink;
import wicket.markup.html.basic.MultiLineLabel;
import wicket.markup.html.form.Button;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.TextField;
import wicket.markup.html.panel.Panel;
import wicket.model.Model;
import wicket.model.PropertyModel;
import wicket.util.time.Duration;
import wicket.util.value.ValueMap;
import de.boardgamesonline.bgo2.webserver.model.ChatChannel;
import de.boardgamesonline.bgo2.webserver.model.ChatMessage;
import de.boardgamesonline.bgo2.webserver.model.User;
import de.boardgamesonline.bgo2.webserver.model.ChatChannel.Changed;
import de.boardgamesonline.bgo2.webserver.wicket.BGOPage;
import de.boardgamesonline.bgo2.webserver.wicket.BGOSession;

/**
 * Panel for the chat window
 * 
 * @author Ralf Niehaus, Fabian Pietsch (integration with {@link ChatChannel})
 * 
 */
public class ChatPanel extends Panel {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    /**
	 * The {@link BGOPage} for localization
	 */
    private final BGOPage page;

    /**
	 * Constructor for the lobby.
	 * 
	 * @param wicketID
	 *            the wicket id
	 * 
	 * @param page
	 *            The {@link BGOPage} to localize
	 */
    public ChatPanel(String wicketID, final BGOPage page) {
        this(wicketID, null, page);
    }

    /**
	 * Constructor for an arbitrary channel.
	 * 
	 * @param wicketID
	 *            the wicket ID
	 * @param channelID
	 *            the channel ID
	 * @param page
	 *            The {@link BGOPage} to localize
	 */
    public ChatPanel(String wicketID, String channelID, final BGOPage page) {
        super(wicketID);
        this.page = page;
        ChatForm form = new ChatForm("chatForm", channelID);
        add(form);
    }

    /**
	 * Form for the panel for the chat window
	 */
    public class ChatForm extends Form implements Observer {

        /**
		 * 
		 */
        private static final long serialVersionUID = 1L;

        /**
		 * The channel ID of the connected {@link ChatChannel}.
		 */
        private final String channelID;

        /**
		 * A buffer for those messages already processed.
		 */
        private final StringBuffer buffer = new StringBuffer(4096);

        /**
		 * Tracks how many messages have already been processed. Used for
		 * {@link ChatChannel#getNewMessages(int)}.
		 */
        private int messageCount = 0;

        /**
		 * Cached member state for notifying the user only on change.
		 */
        private boolean wasMember = false;

        /**
		 * El-cheapo model for form
		 */
        private final ValueMap properties = new ValueMap();

        /**
		 * Constructor for the lobby.
		 * 
		 * @param wicketID
		 *            the wicket id
		 */
        public ChatForm(String wicketID) {
            this(wicketID, null);
        }

        /**
		 * Constructor for an arbitrary channel.
		 * 
		 * @param wicketID
		 *            the wicket ID
		 * @param channelID
		 *            the channel ID
		 */
        public ChatForm(String wicketID, String channelID) {
            super(wicketID);
            this.channelID = channelID;
            messageCount = ((BGOSession) getSession()).getMessageCount();
            updateDisplay();
            final MultiLineLabel chatDisplay = new MultiLineLabel("chatDisplay", new PropertyModel(properties, "chatDisplay"));
            chatDisplay.setEscapeModelStrings(false);
            chatDisplay.setOutputMarkupId(true);
            chatDisplay.add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(1)));
            final TextField chatField = new TextField("chatField", new PropertyModel(properties, "chatField"));
            chatField.setOutputMarkupId(true);
            AjaxSubmitLink sendLink = new AjaxSubmitLink("sendLink", this) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    String messageStr = properties.getString("chatField");
                    if (messageStr == null || messageStr.equals("")) {
                        return;
                    }
                    target.addComponent(chatField);
                    target.addComponent(chatDisplay);
                    properties.put("chatField", "");
                    target.appendJavascript("document.getElementById('" + chatField.getMarkupId() + "').focus();");
                    target.appendJavascript("document.getElementById('chatWrap').scrollBy(0,1000);");
                    ChatMessage msg = getChannel().postMessage(getUser(), messageStr);
                    if (msg == null) {
                        buffer.append(getLocalizer().getString("cantPost", page) + "\n");
                    }
                    updateDisplay();
                }
            };
            sendLink.add(new Button("sendButton", new Model(getLocalizer().getString("send", page))));
            add(chatDisplay);
            add(chatField);
            add(sendLink);
        }

        /**
		 * Gets the ChatChannel object corresponding to this {@link ChatPanel}'s
		 * channelID.
		 * <p>
		 * (Directly storing the object in a private field could make wicket try
		 * to recreate the object later instead of always using the shared
		 * instance -- i.e., wicket would require ChatChannel to be
		 * {@link java.io.Serializable}, which it shouldn't be.)
		 * 
		 * @return The ChatChannel associated with this ChatForm.
		 */
        public ChatChannel getChannel() {
            return ChatChannel.getChannel(channelID);
        }

        /**
		 * @return The User associated with this session.
		 */
        public User getUser() {
            return ((BGOSession) getSession()).getUser();
        }

        /**
		 * Called when ChatChannel changes. Invokes {@link #updateDisplay()} on
		 * new messages.
		 * 
		 * @param o
		 *            Observed ChatChannel
		 * @param arg
		 *            Indication on what has changed
		 */
        public void update(Observable o, Object arg) {
            if (arg.equals(Changed.MESSAGES)) {
                updateDisplay();
            }
        }

        public void updateDisplay() {
            ChatChannel channel = getChannel();
            channel.addObserver(this);
            if (channel.isMember(getUser())) {
                if (!wasMember) {
                    buffer.insert(0, getLocalizer().getString("inChannel", page) + " " + channel.getName() + ".\n");
                    wasMember = true;
                }
                List newMsgs = channel.getNewMessages(messageCount);
                Iterator i = newMsgs.iterator();
                while (i.hasNext()) {
                    ChatMessage msg = (ChatMessage) i.next();
                    String style = msg.getMsg().contains("has left the Lobby") ? "chatLEFT" : "chatJOIN";
                    String insert;
                    if (msg.getNick() == null) {
                        insert = "<span class=\"" + style + "\">" + msg + "</span>";
                    } else if (msg.getNick().equals(getUser().getName())) {
                        insert = "<span class=\"chatTEXT\">" + msg.toString().replaceAll("<", "&lt;") + "</span>";
                    } else {
                        insert = msg.toString().replaceAll("<", "&lt;");
                    }
                    buffer.insert(0, insert + "\n");
                }
                messageCount += newMsgs.size();
            } else {
                if (wasMember) {
                    buffer.append(getLocalizer().getString("notInChannel", page) + " " + channel.getName() + ".\n");
                    wasMember = false;
                }
            }
            properties.put("chatDisplay", buffer.toString());
        }
    }
}
