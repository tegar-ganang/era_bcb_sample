package net.rptools.chartool.ui.component;

@SuppressWarnings("all")
public class TextMessage {

    public interface Channel {

        public static final int ALL = 0;

        public static final int SAY = 1;

        public static final int GM = 2;

        public static final int ME = 3;

        public static final int GROUP = 4;

        public static final int WHISPER = 5;
    }

    private int channel;

    private String target;

    private String message;

    private String source;

    public TextMessage(int channel, String target, String source, String message) {
        this.channel = channel;
        this.target = target;
        this.message = message;
        this.source = source;
    }

    public String toString() {
        return message;
    }

    public int getChannel() {
        return channel;
    }

    public String getTarget() {
        return target;
    }

    public String getMessage() {
        return message;
    }

    public String getSource() {
        return source;
    }

    public boolean isGM() {
        return channel == Channel.GM;
    }

    public boolean isMessage() {
        return channel == Channel.ALL;
    }

    public boolean isSay() {
        return channel == Channel.SAY;
    }

    public boolean isMe() {
        return channel == Channel.ME;
    }

    public boolean isGroup() {
        return channel == Channel.GROUP;
    }

    public boolean isWhisper() {
        return channel == Channel.WHISPER;
    }
}
