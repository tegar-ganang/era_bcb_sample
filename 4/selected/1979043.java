package annone.engine;

public class Pipeline implements Runnable {

    private Channel channel;

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void run() {
    }
}
