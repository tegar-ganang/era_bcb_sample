package charismata.broadcast;

import java.util.ArrayList;
import java.util.List;

public class Container {

    protected Channel getChannel(String channelName) {
        ChannelRepository repository = ChannelRepository.getInstance();
        return repository.getChannel(channelName);
    }

    public void deregisterBroadcast(String channelName, ChannelProgram channelProgram) {
        Channel channel = getChannel(channelName);
        channel.removeBroadcaster(this, channelProgram);
    }

    public void deregisterListener(String channelName, ChannelProgram channelProgram) {
        Channel channel = getChannel(channelName);
        channel.removeListener(this, channelProgram);
    }

    public void registerListener(String channelName, ChannelProgram channelProgram) {
        Channel channel = getChannel(channelName);
        channel.addListener(this, channelProgram);
    }

    public void registerBroadcast(String channelName, ChannelProgram channelProgram) {
        Channel channel = getChannel(channelName);
        channel.addBroadcaster(this, channelProgram);
    }

    public void broadcast(String channel, Object broadcastObj) {
        BroadcastInfo bi = new BroadcastInfo(channel, broadcastObj);
        broadcast(bi);
    }

    public void broadcast(BroadcastInfo bi) {
        Channel channel = bi.getChannel();
        if (channel.hasListener()) {
            ChannelQueue channelQueue = ChannelQueue.getInstance();
            channelQueue.broadCast(bi);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    /**
	 * 
	 * The following code is used to test the Broadcaster Framework
	 * ===============================================================
	 *
	 */
    static class GenerateEvents extends Thread {

        List<Container> containerList;

        String channel;

        public GenerateEvents() {
            containerList = new ArrayList();
            channel = "MONITOR";
        }

        public synchronized void addContainer(Container container) {
            containerList.add(container);
        }

        public synchronized void removeContainer(Container container) {
            boolean removed = containerList.remove(container);
            System.out.println("?removed container " + removed);
        }

        public synchronized void service() {
            for (Container container : containerList) {
                BroadcastInfo bi = new BroadcastInfo(channel, null);
                container.broadcast(bi);
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(2000);
                    System.out.println("Raising Events");
                    service();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class GenerateEvents2 extends GenerateEvents {

        public GenerateEvents2() {
            containerList = new ArrayList();
            channel = "MONITOR2";
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(50);
                    System.out.println("Raising Events");
                    service();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void test1() {
        final GenerateEvents ge = new GenerateEvents();
        ge.start();
        Container containerBroadcaster = new Container();
        Container containerListener = new Container();
        System.out.println("Registering Broadcaster");
        containerBroadcaster.registerBroadcast("MONITOR", new ChannelProgramAdaptor() {

            public void hook(Container container, Channel channel) {
                System.out.println("ChannelProgramAdaptor Hooking container for monitor1");
                ge.addContainer(container);
            }

            @Override
            public void unhook(Container container, Channel channel) {
                System.out.println("ChannelProgramAdaptor UnHooking container for monitor1");
                ge.removeContainer(container);
            }
        });
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Registering listener");
        ChannelProgram listenerChannelProgram = new ChannelProgramAdaptor() {

            @Override
            public void listen(Container container, Channel channel, BroadcastInfo info) {
                System.out.println("Receive Broadcasted from Channel " + channel.getChannelName());
            }
        };
        containerListener.registerListener("MONITOR", listenerChannelProgram);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("DeRegistering Listener");
        containerListener.deregisterListener("MONITOR", listenerChannelProgram);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Registering listener");
        containerListener.registerListener("MONITOR", listenerChannelProgram);
        final GenerateEvents2 ge2 = new GenerateEvents2();
        ge2.start();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("DeRegistering Listener");
        containerListener.deregisterListener("MONITOR", listenerChannelProgram);
        System.out.println("Registering listener");
        containerListener.registerListener("MONITOR2", listenerChannelProgram);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("STEP 10 : Registering Broadcaster");
        containerBroadcaster.registerBroadcast("MONITOR2", new ChannelProgramAdaptor() {

            public void hook(Container container, Channel channel) {
                System.out.println("ChannelProgramAdaptor Hooking container for monitor2");
                ge2.addContainer(container);
            }

            @Override
            public void unhook(Container container, Channel channel) {
                System.out.println("ChannelProgramAdaptor UnHooking container for monitor2");
                ge2.removeContainer(container);
            }
        });
    }

    public static void main(String argv[]) {
        test1();
    }
}
