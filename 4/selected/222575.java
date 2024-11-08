package rtjdds.rtps.receive;

import java.nio.channels.DatagramChannel;

public class WaitingPacketEvent implements rtjdds.util.concurrent.lf.Event {

    private DatagramChannel _channel = null;

    private int _priority = 0;

    public WaitingPacketEvent(DatagramChannel channel, int priority) {
        _channel = channel;
        _priority = priority;
    }

    public DatagramChannel getChannel() {
        return _channel;
    }
}
