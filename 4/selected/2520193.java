package jgm.glider.log;

import jgm.sound.*;
import java.util.*;
import java.util.regex.*;

public class ChatLogEntry extends RawChatLogEntry {

    static Map<String, String> COLOR_MAP = new HashMap<String, String>();

    static {
        COLOR_MAP.put("Whisper", "#FF80FF");
        COLOR_MAP.put("Yell", "#FF4040");
        COLOR_MAP.put("Guild", "#40FF40");
        COLOR_MAP.put("Public Chat", "#FFC0C0");
    }

    public static enum Urgency {

        TRIVIAL, URGENT, CRITICAL
    }

    ;

    private String channel = null;

    private String sender = null;

    public boolean fromPlayer = true;

    private Urgency urgency = Urgency.TRIVIAL;

    private String message = null;

    public ChatLogEntry(String t, String s) {
        super(t, s);
    }

    public ChatLogEntry(String s) {
        this("Chat", s);
    }

    public boolean isUrgent() {
        switch(urgency) {
            case URGENT:
            case CRITICAL:
                return true;
        }
        return false;
    }

    public boolean isCritical() {
        switch(urgency) {
            case CRITICAL:
                return true;
        }
        return false;
    }

    public String getChannel() {
        return channel;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean supportsHtmlText() {
        return true;
    }

    @Override
    public String getHtmlPreColor() {
        String ret = COLOR_MAP.get(this.type);
        return null == ret ? super.getHtmlPreColor() : ret;
    }

    private static Pattern PATTERN1 = Pattern.compile(".*?(<GM>|<Away>|<Busy>|)(\\[?)([^]]+)\\]? (whisper|say|yell)s: (.*)");

    private static Pattern PATTERN2 = Pattern.compile(".*?\\[(\\d+\\.?\\s*?|)(Guild|Officer|[^]]+)\\]\\s*(<GM>|<Away>|<Busy>|)\\[([^]]+)\\]: (.*)");

    private static Pattern PATTERN3 = Pattern.compile(".*?To \\[([^]]+)\\]: (.*)");

    private static ChatLogEntry parse(String s) {
        ChatLogEntry ret = new ChatLogEntry(s);
        s = ret.getText();
        Matcher m = PATTERN1.matcher(s);
        if (m.matches()) {
            boolean gm = m.group(1).equals("<GM>");
            ret.fromPlayer = m.group(2).equals("[");
            ret.sender = m.group(3);
            ret.type = m.group(4);
            ret.type = Character.toUpperCase(ret.type.charAt(0)) + ret.type.substring(1);
            ret.message = m.group(5);
            ret.channel = ret.type;
            ret.urgency = !ret.fromPlayer || ret.type.equals("Yell") ? Urgency.TRIVIAL : gm ? Urgency.CRITICAL : Urgency.URGENT;
            if (gm) {
                ret.type = "GM " + ret.type;
            }
        } else {
            m = PATTERN2.matcher(s);
            if (m.matches()) {
                ret.channel = m.group(2);
                ret.sender = m.group(4);
                ret.message = m.group(5);
                if (!m.group(1).equals("")) {
                    ret.type = "Public Chat";
                } else {
                    ret.type = ret.channel;
                }
            } else {
                m = PATTERN3.matcher(s);
                if (m.matches()) {
                    ret.type = "Whisper";
                    ret.channel = "Whisper";
                    ret.sender = m.group(1);
                    ret.message = m.group(2);
                }
            }
        }
        if (ret.isCritical()) {
            new Sound(Audible.Type.GM, jgm.util.Sound.File.GM_WHISPER).play(true);
            new Phrase(Audible.Type.GM, ret.getRawText()).play();
        } else if (ret.isUrgent()) {
            Audible.Type t = (ret.type.equals("Whisper")) ? Audible.Type.WHISPER : Audible.Type.SAY;
            new Sound(t, jgm.util.Sound.File.WHISPER).play(true);
            new Phrase(t, ret.getRawText()).play();
        }
        return ret;
    }

    public static ChatLogEntry factory(String rawText) {
        return parse(rawText);
    }
}
