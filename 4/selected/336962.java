package com.googlecode.acpj.tests;

import java.net.URI;
import java.util.Random;
import java.util.UUID;
import com.googlecode.acpj.actors.ActorFactory;
import com.googlecode.acpj.channels.Channel;
import com.googlecode.acpj.channels.ChannelException;
import com.googlecode.acpj.channels.ChannelFactory;
import com.googlecode.acpj.channels.ChannelMonitorOutput;
import com.googlecode.acpj.channels.ChannelPoisonedException;
import com.googlecode.acpj.channels.ChannelRegistry;
import com.googlecode.acpj.channels.PortArity;
import com.googlecode.acpj.channels.ReadPort;
import com.googlecode.acpj.channels.WritePort;
import com.googlecode.acpj.patterns.ChannelFilter;
import com.googlecode.acpj.patterns.ChannelOperation;
import com.googlecode.acpj.patterns.Request;
import com.googlecode.acpj.patterns.RequestWithCallbackPattern;
import com.googlecode.acpj.services.LogService;
import com.googlecode.acpj.services.WatchdogService;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * <p>
 * </p>
 * 
 * @author Simon Johnston (simon@johnstonshome.org)
 * @since 0.1.0
 * 
 */
public class SimpleChannelTests extends TestCase {

    public class UuidServer implements Runnable {

        public void run() {
            Channel<Request<UUID>> channel = ChannelFactory.getInstance().createAnyToOneChannel();
            ChannelRegistry.getInstance().register(channel, "test.uuid.server", false);
            ReadPort<Request<UUID>> readPort = channel.getReadPort(true);
            while (true) {
                try {
                    Request<UUID> value = readPort.read();
                    WritePort<UUID> callback = value.getCallbackPort();
                    callback.claim();
                    callback.write(UUID.randomUUID());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class ChannelReader<T> implements Runnable {

        private Channel<T> channel = null;

        public ChannelReader(Channel<T> channel) {
            this.channel = channel;
        }

        public void run() {
            System.out.println("ChannelReader " + this.channel.getName() + ", about to read");
            try {
                T value = this.channel.getReadPort(true).read();
                System.out.println("ChannelReader " + this.channel.getName() + ", read " + value.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class ChannelWriter<T> implements Runnable {

        private Channel<T> channel = null;

        private T value = null;

        public ChannelWriter(Channel<T> channel, T value) {
            this.channel = channel;
            this.value = value;
        }

        public void run() {
            System.out.println("ChannelWriter " + this.channel.getName() + ", about to write " + this.value.toString());
            try {
                this.channel.getWritePort(true).write(this.value);
                System.out.println("ChannelWriter " + this.channel.getName() + ", done.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class ChannelConsumer implements Runnable {

        private Channel<String> channel = null;

        public ChannelConsumer(Channel<String> channel) {
            this.channel = channel;
        }

        public void run() {
            try {
                ReadPort<String> readPort = this.channel.getReadPort(true);
                final String name = readPort.getOwningActor().getName();
                System.out.println("ChannelConsumer " + name + ", about to read.");
                while (true) {
                    try {
                        String value = readPort.read();
                        System.out.println("ChannelConsumer " + name + ", read '" + value.toString() + "'");
                    } catch (ChannelPoisonedException e) {
                        break;
                    }
                }
                System.out.println("ChannelConsumer " + name + " done.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class ChannelProducer implements Runnable {

        private Channel<String> channel = null;

        private int limit;

        public ChannelProducer(Channel<String> channel, int limit) {
            this.channel = channel;
            this.limit = limit;
        }

        public void run() {
            try {
                WritePort<String> writePort = this.channel.getWritePort(true);
                final String name = writePort.getOwningActor().getName();
                final int count = new Random(System.currentTimeMillis()).nextInt(this.limit) + 1;
                System.out.println("ChannelProducer " + name + ", writing " + count + " messages.");
                for (int i = 0; i < count; i++) {
                    try {
                        writePort.write("This is actor " + name + " calling.");
                    } catch (ChannelPoisonedException e) {
                        break;
                    }
                }
                System.out.println("ChannelProducer " + name + ", done.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class RandomNumberGenerator implements Runnable {

        private WritePort<Integer> writePort = null;

        private int count = 0;

        public RandomNumberGenerator(WritePort<Integer> writePort, int count) {
            this.writePort = writePort;
            this.count = count;
        }

        public void run() {
            this.writePort.claim();
            Random random = new Random();
            for (int i = 0; i < this.count; i++) {
                this.writePort.write(new Integer(random.nextInt(100)));
            }
            this.writePort.poison();
        }
    }

    public class NumberFilter implements ChannelOperation<Integer, Integer> {

        public Integer process(Integer message) {
            if (message.intValue() < 50) {
                return null;
            } else {
                return message;
            }
        }
    }

    public class NumberPrinter implements Runnable {

        private ReadPort<Integer> readPort = null;

        public NumberPrinter(ReadPort<Integer> readPort) {
            this.readPort = readPort;
        }

        public void run() {
            try {
                this.readPort.claim();
                while (true) {
                    try {
                        Integer number = this.readPort.read();
                        System.out.println("Received " + number);
                    } catch (ChannelPoisonedException e) {
                        break;
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public void test001_CreateBadChannels() throws Exception {
        System.out.println(String.format("===== %s =====", getName()));
        try {
            ChannelFactory.getInstance().createChannel("test", null, 1, PortArity.ONE, 1);
            fail();
        } catch (IllegalArgumentException e) {
            ;
        }
        try {
            ChannelFactory.getInstance().createChannel("test", PortArity.ONE, 1, null, 1);
            fail();
        } catch (IllegalArgumentException e) {
            ;
        }
        try {
            ChannelFactory.getInstance().createChannel("test", PortArity.ONE, 0, PortArity.ONE, 1);
            fail();
        } catch (IllegalArgumentException e) {
            ;
        }
        try {
            ChannelFactory.getInstance().createChannel("test", PortArity.ONE, 1, PortArity.ONE, 0);
            fail();
        } catch (IllegalArgumentException e) {
            ;
        }
        Thread.sleep(1000);
    }

    public void test002_CreateSimpleChannel() throws Exception {
        System.out.println(String.format("===== %s =====", getName()));
        Channel<Integer> channel = ChannelFactory.getInstance().createOneToOneChannel();
        Assert.assertNotNull(channel);
        URI name = new URI(channel.getName());
        Assert.assertEquals(name.getScheme(), "channel");
        String id = name.getPath().substring(1);
        new Long(id);
    }

    public void test003_SimpleChannelReadWrite() throws Exception {
        System.out.println(String.format("===== %s =====", getName()));
        Channel<Integer> channel = ChannelFactory.getInstance().createOneToOneChannel();
        Assert.assertNotNull(channel);
        ActorFactory.getInstance().createActor(new ChannelReader<Integer>(channel));
        Thread.sleep(500);
        ActorFactory.getInstance().createActor(new ChannelWriter<Integer>(channel, new Integer(101)));
    }

    public void test004_SimpleChannelPoison() throws Exception {
        System.out.println(String.format("===== %s =====", getName()));
        Channel<Integer> channel = ChannelFactory.getInstance().createOneToOneChannel();
        Assert.assertNotNull(channel);
        ActorFactory.getInstance().createActor(new ChannelReader<Integer>(channel));
        Thread.sleep(2000);
        channel.poison();
    }

    public void test005_SimpleRequestResponseTest() throws Exception {
        System.out.println(String.format("===== %s =====", getName()));
        ActorFactory.getInstance().createActor(new UuidServer());
        Thread.sleep(500);
        Channel<Request<UUID>> requestChannel = ChannelRegistry.getInstance().lookupOrNull("test.uuid.server");
        WritePort<Request<UUID>> requestPort = requestChannel.getWritePort(true);
        RequestWithCallbackPattern<Integer, UUID> guidRequestPattern = new RequestWithCallbackPattern<Integer, UUID>();
        for (int i = 0; i < 5; i++) {
            UUID uuid = guidRequestPattern.requestWithCallback(requestPort);
            System.out.println("Got new UUID: " + uuid);
        }
    }

    public void test006_ChannelFilterTest() throws Exception {
        System.out.println(String.format("===== %s =====", getName()));
        new LogService().start();
        new WatchdogService().start();
        Channel<Integer> rawChannel = ChannelFactory.getInstance().createOneToOneChannel("RandomNumbers");
        Channel<Integer> filteredChannel = ChannelFactory.getInstance().createOneToOneChannel("FilteredNumbers");
        ActorFactory.getInstance().createActor(new NumberPrinter(filteredChannel.getReadPort(false)), "NumberPrinter");
        ActorFactory.getInstance().createActor(new ChannelFilter<Integer, Integer>(rawChannel.getReadPort(false), filteredChannel.getWritePort(false), new NumberFilter(), true), "ChannelFilter");
        ActorFactory.getInstance().createActor(new RandomNumberGenerator(rawChannel.getWritePort(false), 500), "RandomNumberGenerator");
        ChannelMonitorOutput.writeDOT(ChannelFactory.getInstance().getChannelMonitor(), System.err);
        Thread.sleep(1000);
    }

    public void test007_OneToOneConsumer() throws Exception {
        System.out.println(String.format("===== %s =====", getName()));
        Channel<String> channel = ChannelFactory.getInstance().createOneToOneChannel();
        Assert.assertNotNull(channel);
        ActorFactory.getInstance().createActor(new ChannelConsumer(channel));
        Thread.sleep(500);
        ActorFactory.getInstance().createActor(new ChannelProducer(channel, 10));
        Thread.sleep(1000);
    }

    public void test008_OneToAnyConsumer() throws Exception {
        System.out.println(String.format("===== %s =====", getName()));
        Channel<String> channel = ChannelFactory.getInstance().createOneToAnyChannel();
        Assert.assertNotNull(channel);
        ActorFactory.getInstance().createActor(new ChannelConsumer(channel));
        ActorFactory.getInstance().createActor(new ChannelConsumer(channel));
        ActorFactory.getInstance().createActor(new ChannelConsumer(channel));
        Thread.sleep(500);
        ActorFactory.getInstance().createActor(new ChannelProducer(channel, 10));
        Thread.sleep(1000);
    }

    public void test009_AnyToOneConsumer() throws Exception {
        System.out.println(String.format("===== %s =====", getName()));
        Channel<String> channel = ChannelFactory.getInstance().createAnyToOneChannel();
        Assert.assertNotNull(channel);
        ActorFactory.getInstance().createActor(new ChannelConsumer(channel));
        Thread.sleep(500);
        ActorFactory.getInstance().createActor(new ChannelProducer(channel, 10));
        ActorFactory.getInstance().createActor(new ChannelProducer(channel, 10));
        ActorFactory.getInstance().createActor(new ChannelProducer(channel, 10));
        Thread.sleep(1000);
    }

    public void test010_AnyToAnyConsumer() throws Exception {
        System.out.println(String.format("===== %s =====", getName()));
        Channel<String> channel = ChannelFactory.getInstance().createAnyToAnyChannel();
        Assert.assertNotNull(channel);
        ActorFactory.getInstance().createActor(new ChannelConsumer(channel));
        ActorFactory.getInstance().createActor(new ChannelConsumer(channel));
        Thread.sleep(500);
        ActorFactory.getInstance().createActor(new ChannelProducer(channel, 10));
        ActorFactory.getInstance().createActor(new ChannelProducer(channel, 10));
        Thread.sleep(1000);
    }

    public void test011_OneToOneRules() throws Exception {
        System.out.println(String.format("===== %s =====", getName()));
        Channel<String> channel = ChannelFactory.getInstance().createOneToOneChannel();
        Assert.assertNotNull(channel);
        channel.getReadPort(false);
        try {
            channel.getReadPort(false);
            fail("Port Arity rule check failed.");
        } catch (ChannelException e) {
            ;
        }
        channel.getWritePort(false);
        try {
            channel.getWritePort(false);
            fail("Port Arity rule check failed.");
        } catch (ChannelException e) {
            ;
        }
    }

    public void test012_AnyToOneRules() throws Exception {
        System.out.println(String.format("===== %s =====", getName()));
        Channel<String> channel = ChannelFactory.getInstance().createAnyToOneChannel();
        Assert.assertNotNull(channel);
        channel.getReadPort(false);
        try {
            channel.getReadPort(false);
            fail("Port Arity rule check failed.");
        } catch (ChannelException e) {
            ;
        }
        channel.getWritePort(false);
        channel.getWritePort(false);
        channel.getWritePort(false);
        channel.getWritePort(false);
        channel.getWritePort(false);
        channel.getWritePort(false);
    }

    public void test013_OneToAnyRules() throws Exception {
        System.out.println(String.format("===== %s =====", getName()));
        Channel<String> channel = ChannelFactory.getInstance().createOneToAnyChannel();
        Assert.assertNotNull(channel);
        channel.getReadPort(false);
        channel.getReadPort(false);
        channel.getReadPort(false);
        channel.getReadPort(false);
        channel.getReadPort(false);
        channel.getWritePort(false);
        try {
            channel.getWritePort(false);
            fail("Port Arity rule check failed.");
        } catch (ChannelException e) {
            ;
        }
    }

    public void test014_AnyToAnyRules() throws Exception {
        System.out.println(String.format("===== %s =====", getName()));
        Channel<String> channel = ChannelFactory.getInstance().createAnyToAnyChannel();
        Assert.assertNotNull(channel);
        channel.getReadPort(false);
        channel.getReadPort(false);
        channel.getReadPort(false);
        channel.getReadPort(false);
        channel.getReadPort(false);
        channel.getWritePort(false);
        channel.getWritePort(false);
        channel.getWritePort(false);
        channel.getWritePort(false);
        channel.getWritePort(false);
    }

    public void test011_PortClosedRules() throws Exception {
        System.out.println(String.format("===== %s =====", getName()));
        Channel<String> channel = ChannelFactory.getInstance().createOneToOneChannel();
        Assert.assertNotNull(channel);
        ReadPort<String> readPort = channel.getReadPort(true);
        try {
            channel.getReadPort(true);
            fail("Port Arity rule check failed.");
        } catch (ChannelException e) {
            ;
        }
        assertFalse(readPort.isClosed());
        readPort.close();
        assertTrue(readPort.isClosed());
        readPort = channel.getReadPort(true);
        assertFalse(readPort.isClosed());
        try {
            channel.getReadPort(true);
            fail("Port Arity rule check failed.");
        } catch (ChannelException e) {
            ;
        }
    }
}
