package slojj.dotsbox.task;

import slojj.dotsbox.parser.Channel;
import slojj.dotsbox.core.TaskBase;
import slojj.dotsbox.parser.ChannelBuilder;
import slojj.dotsbox.parser.ChannelBuilderException;

public class LoadChannelTask extends TaskBase {

    private final Channel channel;

    private final String link;

    public LoadChannelTask(Channel channel) {
        super(channel);
        this.channel = channel;
        this.link = channel.getLink();
        status = "[Spider] loading " + link;
    }

    @Override
    protected Object internalRun() {
        try {
            ChannelBuilder cb = new ChannelBuilder(link);
            Channel channel = cb.getChannel();
            status = "[Spider] Successfully load " + link;
            return channel;
        } catch (ChannelBuilderException e) {
            e.printStackTrace();
            status = "[Spider] Fail to load " + link;
        }
        return null;
    }

    public String getId() {
        return "Spider#loadChannel@" + link;
    }
}
