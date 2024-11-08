package ps.client.plugin.eq2.log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatLine {

    public static final int TYPE_UNKNOWN = -1;

    public static final int TYPE_TELL = 1;

    public static final int TYPE_SAY = 2;

    public static final int TYPE_NPC_SAY = 3;

    public static final int TYPE_SHOUT = 4;

    public static final int TYPE_OOC = 5;

    public static final int TYPE_GUILD = 6;

    public static final int TYPE_OFFICER = 7;

    public static final int TYPE_GROUP = 8;

    public static final int TYPE_RAID = 9;

    public static final String YOU_AS_SENDER = "Ihr";

    public static final String YOU_AS_RECIEVER = "Euch";

    public static final String GUILD = "Gilde";

    public static final String GROUP = "Gruppe";

    public static final String RAID = "Raid";

    public static final String OFFICER = "Offiziere";

    public static final String SAY = "Say";

    public static final String NPC_SAY = "NPC say";

    public static final String SHOUT = "Shout";

    public static final String OOC = "Spielerkommentar";

    public static final String UNKNOWN = "UNKNOWN";

    public static final Pattern PATTERN_TELLS = Pattern.compile("(sagt |teilt ).*");

    public static final Pattern PATTERN_TELLS_TO = Pattern.compile("(sagt |teilt )zu.*");

    public static final Pattern PATTERN_SAYS = Pattern.compile("sagt( auf .*)?, .*");

    public static final Pattern PATTERN_SAYS_TO_GROUP = Pattern.compile("sagt zu Gruppe,.*");

    public static final Pattern PATTERN_SAYS_TO_GUILD = Pattern.compile("sagt zu Gilde,.*");

    public static final Pattern PATTERN_SAYS_TO_RAID = Pattern.compile("sagt zu der R�ubertruppe,.*");

    public static final Pattern PATTERN_SHOUTS = Pattern.compile("ruft( auf .*)?,.*");

    public static final Pattern PATTERN_SAYS_OOC = Pattern.compile("sagt \\(Spielerkommentar\\),.*");

    public static final Pattern PATTERN_SAYS_OFFICER = Pattern.compile("says? to the officers,.*");

    public static final Pattern PATTERN_NPC_SAYS = Pattern.compile("sagt zu .*,.*");

    public static final String REGEX_TO = " zu ";

    public static final Pattern PATTERN_CHANNEL_NUMBER = Pattern.compile("\\([0-9]+\\)");

    String avatar;

    String server;

    String time;

    String sender;

    String verb;

    String reciever;

    String msg;

    boolean isTargetChannel = false;

    String channelNumber = "*";

    int type;

    private ChatLine() {
    }

    public ChatLine(LogLine logLine) {
        avatar = logLine.getAvatarName();
        server = logLine.getAvatarServer();
        time = logLine.timeStr;
        String str = logLine.content;
        if (str.startsWith(YOU_AS_SENDER + " ")) {
            sender = YOU_AS_SENDER;
            str = str.substring(YOU_AS_SENDER.length() + 1);
        } else if (str.startsWith(ChatLink.PREFIX)) {
            sender = ChatLink.parseTitle(str);
            str = str.substring(str.indexOf(ChatLink.POSTFIX) + ChatLink.POSTFIX.length() + 1);
        }
        verb = str.substring(0, str.indexOf(','));
        if (PATTERN_SAYS_TO_GROUP.matcher(str).matches()) {
            type = TYPE_GROUP;
            reciever = GROUP;
            verb = "sagt zur";
            str = str.substring(str.indexOf(' ') + 1);
        } else if (PATTERN_SAYS_TO_GUILD.matcher(str).matches()) {
            type = TYPE_GUILD;
            reciever = GUILD;
            verb = "sagt zur";
            str = str.substring(str.indexOf(' ') + 1);
        } else if (PATTERN_SAYS_TO_RAID.matcher(str).matches()) {
            type = TYPE_RAID;
            reciever = RAID;
            verb = "sagt zum";
            str = str.substring(str.indexOf(' ') + 1);
        } else if (PATTERN_SHOUTS.matcher(str).matches()) {
            type = TYPE_SHOUT;
            reciever = SHOUT;
            str = str.substring(str.indexOf(' ') + 1);
        } else if (PATTERN_SAYS_OOC.matcher(str).matches()) {
            type = TYPE_OOC;
            reciever = OOC;
            verb = "sagt als Spielerkommentar";
            str = str.substring(str.indexOf(' ') + 1);
        } else if (PATTERN_SAYS_OFFICER.matcher(str).matches()) {
            type = TYPE_OFFICER;
            reciever = OFFICER;
            verb = "sagt zu";
            str = str.substring(str.indexOf(' ') + 1);
        } else if (PATTERN_NPC_SAYS.matcher(str).matches()) {
            type = TYPE_NPC_SAY;
            int index = str.indexOf(REGEX_TO) + REGEX_TO.length() - 1;
            verb = str.substring(0, index);
            str = str.substring(index + 1);
        } else if (PATTERN_SAYS.matcher(str).matches()) {
            type = TYPE_SAY;
            reciever = SAY;
            str = str.substring(str.indexOf(',') + 2);
        } else if (PATTERN_TELLS_TO.matcher(str).matches()) {
            type = TYPE_TELL;
            int endIndex = str.indexOf(REGEX_TO) + REGEX_TO.length() - 1;
            verb = "sagt zu";
            str = str.substring(endIndex + 1);
        } else if (PATTERN_TELLS.matcher(str).matches()) {
            type = TYPE_TELL;
            verb = "sagt zu";
            str = str.substring(str.indexOf(' ') + 1);
        } else {
            System.out.println("UNKNOWN: " + logLine);
            type = TYPE_UNKNOWN;
            reciever = UNKNOWN;
        }
        if (type == TYPE_TELL || type == TYPE_NPC_SAY) {
            reciever = str.substring(0, str.indexOf(','));
            Matcher channelNumberMatcher = PATTERN_CHANNEL_NUMBER.matcher(reciever);
            if (channelNumberMatcher.find()) {
                isTargetChannel = true;
                channelNumber = reciever.substring(channelNumberMatcher.start(), channelNumberMatcher.end());
                reciever = reciever.substring(0, channelNumberMatcher.start() - 1);
                type = TYPE_TELL;
            } else if (type == TYPE_TELL) {
                reciever = reciever.substring(0, 1).toUpperCase() + reciever.substring(1).toLowerCase();
            }
            int i = reciever.indexOf('.') + 1;
            if (i > 0 && type == TYPE_TELL) {
                String newReciever = reciever.substring(0, i);
                newReciever += reciever.substring(i, i + 1).toUpperCase();
                newReciever += reciever.length() > i ? reciever.substring(i + 1).toLowerCase() : "";
                reciever = newReciever;
            }
            str = str.substring(str.indexOf(',') + 1);
            if (reciever.equalsIgnoreCase(YOU_AS_RECIEVER)) {
                reciever = YOU_AS_RECIEVER;
            }
        }
        msg = str.substring(str.indexOf('"') + 1, str.lastIndexOf('"'));
    }

    public int getType() {
        return type;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getServer() {
        return server;
    }

    public String getTime() {
        return time;
    }

    public String getShortTime() {
        return time.substring(0, 5);
    }

    public String getSender() {
        return sender;
    }

    public String getVerb() {
        return verb;
    }

    public String getReciever() {
        return reciever;
    }

    public String getMessage() {
        return msg;
    }

    public String toString() {
        return "[" + time + "] " + sender + ": " + msg;
    }

    public int writeToArray(byte[] bytes, int offset) {
        int byteCount = offset;
        bytes[byteCount] = (byte) type;
        byteCount++;
        byteCount += writeStringToArray(bytes, byteCount, avatar);
        byteCount += writeStringToArray(bytes, byteCount, server);
        byteCount += writeStringToArray(bytes, byteCount, time);
        byteCount += writeStringToArray(bytes, byteCount, sender);
        byteCount += writeStringToArray(bytes, byteCount, verb);
        byteCount += writeStringToArray(bytes, byteCount, reciever);
        byteCount += writeStringToArray(bytes, byteCount, msg);
        return byteCount - offset;
    }

    private int writeStringToArray(byte[] bytes, int offset, String str) {
        int byteCount = 0;
        byte[] strBytes = str.getBytes();
        if (offset < bytes.length) {
            bytes[offset] = (byte) strBytes.length;
            byteCount++;
            for (int i = 0; i < strBytes.length && (byteCount + offset < bytes.length); i++) {
                bytes[offset + byteCount] = strBytes[i];
                byteCount++;
            }
        }
        return byteCount;
    }

    public static ChatLine createFromBytes(byte[] bytes, int offset) {
        int byteCount = offset;
        ChatLine chatLine = new ChatLine();
        chatLine.type = bytes[byteCount];
        byteCount += 1;
        chatLine.avatar = createStringfromBytes(bytes, byteCount);
        byteCount += chatLine.avatar.length() + 1;
        chatLine.server = createStringfromBytes(bytes, byteCount);
        byteCount += chatLine.server.length() + 1;
        chatLine.time = createStringfromBytes(bytes, byteCount);
        byteCount += chatLine.time.length() + 1;
        chatLine.sender = createStringfromBytes(bytes, byteCount);
        byteCount += chatLine.sender.length() + 1;
        chatLine.verb = createStringfromBytes(bytes, byteCount);
        byteCount += chatLine.verb.length() + 1;
        chatLine.reciever = createStringfromBytes(bytes, byteCount);
        byteCount += chatLine.reciever.length() + 1;
        chatLine.msg = createStringfromBytes(bytes, byteCount);
        byteCount += chatLine.msg.length() + 1;
        return chatLine;
    }

    private static String createStringfromBytes(byte[] bytes, int offset) {
        byte[] strBytes = new byte[bytes[offset]];
        for (int i = 0; i < strBytes.length && (offset + i + 1 < bytes.length); i++) {
            strBytes[i] = bytes[offset + i + 1];
        }
        return new String(strBytes);
    }

    public static void main(String[] args) {
        ChatLine chatLine = new ChatLine(new LogLine(null, "(1282643830)[Tue Aug 24 11:57:10 2010] \\aPC -1 Aradya:Aradya\\/a teilt zu VME-RF-CHNL (10),\"moin satha\""));
        System.out.println("type: " + chatLine.type);
        System.out.println("sender: \"" + chatLine.sender + "\"");
        System.out.println("reciever: \"" + chatLine.reciever + "\"");
        System.out.println("msg: \"" + chatLine.msg + "\"");
    }
}
