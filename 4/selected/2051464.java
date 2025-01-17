package net.rptools.maptool.model;

import java.util.List;
import java.util.ListIterator;
import net.rptools.maptool.client.MapTool;

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

    private List<String> transform;

    public TextMessage(int channel, String target, String source, String message, List<String> transformHistory) {
        this.channel = channel;
        this.target = target;
        this.message = message;
        this.source = source;
        this.transform = transformHistory;
    }

    public static TextMessage say(List<String> transformHistory, String message) {
        return new TextMessage(Channel.SAY, null, MapTool.getPlayer().getName(), message, transformHistory);
    }

    public static TextMessage gm(List<String> transformHistory, String message) {
        return new TextMessage(Channel.GM, null, MapTool.getPlayer().getName(), message, transformHistory);
    }

    public static TextMessage me(List<String> transformHistory, String message) {
        return new TextMessage(Channel.ME, null, MapTool.getPlayer().getName(), message, transformHistory);
    }

    public static TextMessage group(List<String> transformHistory, String target, String message) {
        return new TextMessage(Channel.GROUP, target, MapTool.getPlayer().getName(), message, transformHistory);
    }

    public static TextMessage whisper(List<String> transformHistory, String target, String message) {
        return new TextMessage(Channel.WHISPER, target, MapTool.getPlayer().getName(), message, transformHistory);
    }

    public String toString() {
        return message;
    }

    /**
     * Attempt to cut out any redundant information
     */
    public void compact() {
        if (transform != null) {
            String lastTransform = null;
            for (ListIterator<String> iter = transform.listIterator(); iter.hasNext(); ) {
                String value = iter.next();
                if (value == null || value.length() == 0 || value.equals(lastTransform) || value.equals(message)) {
                    iter.remove();
                    continue;
                }
                lastTransform = value;
            }
        }
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

    public List<String> getTransformHistory() {
        return transform;
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
