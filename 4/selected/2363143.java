package wing.message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import wing.Block;

public class YellMessage extends Message {

    private static final Pattern p = Pattern.compile("(\\d+):(.*):\\s+(.*)");

    private int channelNo;

    private String name;

    private String text;

    public YellMessage(Block block) {
        super(block);
        channelNo = 0;
        name = "";
        text = "";
        if (block.messageSize() == 1) {
            Matcher m = p.matcher(block.get(0));
            if (m.matches()) {
                channelNo = Integer.parseInt(m.group(1));
                name = m.group(2);
                text = m.group(3);
                System.out.println("YellParser:" + channelNo + ":" + name + ":" + text);
            } else {
                System.err.println("YellParser: parse fail :" + block.get(0));
            }
        } else {
            System.err.println("YellParser:size() != 1");
        }
    }

    public int getChannelNo() {
        return channelNo;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }
}
