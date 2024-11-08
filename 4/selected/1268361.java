package jelb.messaging;

import jelb.netio.Message;
import jelb.netio.Protocol;

public class RawText implements IMessage, ISendableMessage {

    private String content;

    private Message raw;

    public RawText() {
        super();
    }

    public RawText(String text) {
        super();
        this.raw = new Message(Protocol.RAW_TEXT, text);
        this.content = text;
    }

    @Override
    public Message getRaw() {
        return this.raw;
    }

    public byte getType() {
        return jelb.netio.Protocol.RAW_TEXT;
    }

    public void init(Message rawMessage) {
        this.content = rawMessage.getContent(2);
        this.raw = rawMessage;
    }

    public String getContent() {
        return this.content;
    }

    public Message getMessage() {
        return this.raw;
    }

    public boolean isServerChat() {
        return this.raw.getByte(0) == jelb.netio.Protocol.CHAT_SERVER;
    }

    public boolean isPresonalChat() {
        return this.raw.getByte(0) == jelb.netio.Protocol.CHAT_PERSONAL;
    }

    public byte getChannel() {
        return this.raw.getByte(0);
    }

    private String getChannelString() {
        switch(this.raw.getByte(0)) {
            case jelb.netio.Protocol.CHAT_CHANNEL1:
                return "@1";
            case jelb.netio.Protocol.CHAT_CHANNEL2:
                return "@2";
            case jelb.netio.Protocol.CHAT_CHANNEL3:
                return "@3";
            case jelb.netio.Protocol.CHAT_GM:
                return "#gm";
            case jelb.netio.Protocol.CHAT_MOD:
                return "#mod";
        }
        return "";
    }

    public String getLog() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getChannelString());
        if (sb.length() > 0) sb.append(" ");
        sb.append(this.content);
        return sb.toString();
    }
}
