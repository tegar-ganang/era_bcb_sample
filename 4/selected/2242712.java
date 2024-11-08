package test.lf;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import javax.realtime.ImmortalMemory;
import javax.realtime.LTMemory;
import javax.realtime.MemoryArea;
import javax.realtime.NoHeapRealtimeThread;
import javax.realtime.RealtimeThread;
import jtools.time.HighResClock;
import jtools.time.HighResTimer;
import rtjdds.util.GlobalProperties;
import rtjdds.util.Logger;
import rtjdds.util.concurrent.lf.Event;
import rtjdds.util.concurrent.lf.EventHandler;
import rtjdds.util.concurrent.lf.FIFOLeaderFollowerTP;
import rtjdds.util.concurrent.lf.LIFOLeaderFollowerTP;
import rtjdds.util.concurrent.lf.LeaderFollowerTP;

public class TestDatagramChannel {

    public static void main(String[] args) throws InterruptedException {
        Launcher trampoline = new Launcher(ImmortalMemory.instance());
        trampoline.start();
        trampoline.join();
    }

    static class Launcher extends NoHeapRealtimeThread {

        public Launcher(MemoryArea mem) {
            super(null, mem);
        }

        public void run() {
            long memSize = 1024 * 1024;
            EventHandler _theHandler = new TestDatagramChannel.ConcreteHandler(memSize);
            System.out.println("here");
            LeaderFollowerTP _thePool = new FIFOLeaderFollowerTP(10, 10, false, _theHandler);
            System.out.println("there");
            _thePool.start();
            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static class ConcreteEvent implements Event {

        DatagramChannel _channel;

        public ConcreteEvent(DatagramChannel channel) {
            _channel = channel;
        }

        public DatagramChannel getChannel() {
            return _channel;
        }
    }

    static class ConcreteHandler extends EventHandler {

        Selector _selector = null;

        SelectionKey _channelKey = null;

        SelectionKey key = null;

        SocketAddress sender = null;

        DatagramChannel channel = null;

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        public ConcreteHandler(long memSize) {
            super(memSize);
            try {
                _selector = Selector.open();
                DatagramChannel channelOne = DatagramChannel.open();
                channelOne.socket().bind(new InetSocketAddress("127.0.0.1", 8888));
                channelOne.configureBlocking(false);
                _channelKey = channelOne.register(_selector, SelectionKey.OP_READ);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void handleEvent(Event ev) {
            if (ev != null) {
                buffer.rewind();
                try {
                    sender = ((ConcreteEvent) ev)._channel.receive(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                buffer.rewind();
                System.out.println(". Ricevuto msg no. " + buffer.get() + " from " + sender);
                System.out.println(". In service...");
                for (int k = 0; k < 10000; k++) {
                }
                System.out.println(". Ex. time = " + HighResClock.getTime());
            }
        }

        public Event pollEvent() {
            GlobalProperties.logger.log(Logger.INFO, getClass(), "pollEvent()", " Polling");
            int selectedKeys = 0;
            SelectionKey currentKey = null;
            while (true) {
                try {
                    selectedKeys = _selector.select();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (selectedKeys > 0) {
                    GlobalProperties.logger.log(Logger.INFO, getClass(), "pollEvent()", " Selecting Keys");
                    GlobalProperties.logger.log(Logger.PEDANTIC, getClass(), "pollEvent()", "Selected keys are:");
                    Iterator it = _selector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        currentKey = (SelectionKey) it.next();
                        GlobalProperties.logger.log(Logger.PEDANTIC, getClass(), "pollEvent()", "key (isBlocking): " + key.channel().isBlocking());
                        if (key.equals(_channelKey) && key.isReadable()) {
                            it.remove();
                            return new ConcreteEvent((DatagramChannel) _channelKey.channel());
                        }
                    }
                } else {
                    GlobalProperties.logger.log(Logger.PEDANTIC, getClass(), "pollEvent()", "select() BUG? select() returned " + selectedKeys + " BUT selected keyring size=" + _selector.selectedKeys().size());
                }
            }
        }

        public void unblockPolling() {
        }
    }
}
