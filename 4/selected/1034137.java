package org.mobicents.media.server.test.rtp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.PipeImpl;
import org.mobicents.media.server.impl.rtp.RTPManager;
import org.mobicents.media.server.impl.rtp.RTPChannel;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.format.EncodingName;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;

/**
 *
 * @author kulikov
 */
public class RtpPerformance {

    private Clock clock = new DefaultClock();

    private static Scheduler scheduler;

    private UdpManager udpTransport;

    private RTPManager rtpManager;

    private Format fmt = FormatFactory.createAudioFormat(new EncodingName("test"));

    private RTPFormat rtpFormat = new RTPFormat(3, fmt);

    private long PERIOD = 20000000L;

    private static final int jitter = 60;

    private static InetAddress localAddress;

    private int localPort = 9201;

    private ArrayList<Conversation> conversations = new ArrayList();

    public void setup() throws Exception {
        localAddress = InetAddress.getByName("localhost");
        scheduler = new Scheduler(4);
        scheduler.setClock(clock);
        scheduler.start();
        udpTransport = new UdpManager(scheduler);
        scheduler.submit(udpTransport, 0);
        scheduler.submit(udpTransport, 1);
        scheduler.submit(udpTransport, 2);
        scheduler.submit(udpTransport, 3);
        rtpFormat.setClockRate(8000);
        rtpManager = new RTPManager(udpTransport);
        rtpManager.setBindAddress("localhost");
        rtpManager.setScheduler(scheduler);
        rtpManager.setJitter(jitter);
        rtpManager.start();
    }

    public void createConversations(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            conversations.add(new Conversation("conversation[" + i + "]"));
        }
    }

    @SuppressWarnings("static-access")
    public void startTest(int duration) {
        for (Conversation conversation : conversations) {
            conversation.start();
        }
        try {
            Thread.currentThread().sleep(duration);
        } catch (Exception e) {
        }
    }

    public void stopTest() {
        for (Conversation conversation : conversations) {
            conversation.stop();
        }
        conversations.clear();
    }

    @SuppressWarnings("static-access")
    public void relax(int amount) {
        try {
            Thread.currentThread().sleep(amount);
        } catch (Exception e) {
        }
    }

    public void report() {
        for (Conversation conversation : conversations) {
            conversation.report();
        }
    }

    public void shutDown() {
        System.out.println("Shut down!!!!");
        rtpManager.stop();
        scheduler.stop();
    }

    private class User {

        private String name;

        private Generator gen;

        private Detector det;

        private RTPChannel channel;

        private long start;

        private long finish;

        private long duration;

        public User(String name) throws Exception {
            this.name = name;
            channel = rtpManager.getChannel();
            channel.bind();
            channel.addFormat(rtpFormat);
            gen = new Generator("generator[" + name + "]", scheduler);
            det = new Detector("detector[" + name + "]", scheduler);
            PipeImpl rxPipe = new PipeImpl();
            PipeImpl txPipe = new PipeImpl();
            rxPipe.connect(det);
            rxPipe.connect(channel.getInput());
            txPipe.connect(gen);
            txPipe.connect(channel.getOutput());
        }

        public int getPort() {
            return channel.getLocalPort();
        }

        public void setPeer(User user) throws IOException {
            channel.setPeer(new InetSocketAddress(localAddress, user.getPort()));
        }

        public void start() {
            gen.start();
            det.start();
            channel.getInput().start();
            channel.getOutput().start();
            start = System.currentTimeMillis();
        }

        public void stop() {
            gen.stop();
            det.stop();
            channel.close();
            System.out.println("Socket released");
        }

        public void report() {
            System.out.println("Det jitter=" + det.maxJitter + " at=" + det.psn);
            System.out.println("Miss rate=" + det.missrate + ", packets=" + det.count + ", sent" + gen.count);
            System.out.println("tx=" + channel.getPacketsTransmitted() + ", rx=" + channel.getPacketsReceived() + ", lost=" + channel.getPacketsLost());
        }
    }

    private class Conversation {

        private User alice;

        private User bob;

        public Conversation(String name) throws Exception {
            alice = new User(name + "-A");
            bob = new User(name + "-B");
            alice.setPeer(bob);
            bob.setPeer(alice);
        }

        public void start() {
            alice.start();
            bob.start();
        }

        public void stop() {
            alice.stop();
            bob.stop();
        }

        public void report() {
            alice.report();
            bob.report();
        }
    }

    private class Generator extends AbstractSource {

        private long count;

        public Generator(String name, Scheduler scheduler) {
            super(name, scheduler);
        }

        @Override
        public Frame evolve(long timestamp) {
            count++;
            Frame frame = Memory.allocate(160);
            frame.setOffset(0);
            frame.setLength(160);
            frame.setEOM(false);
            frame.setTimestamp(timestamp);
            frame.setDuration(PERIOD);
            frame.setFormat(fmt);
            return frame;
        }

        @Override
        public Formats getNativeFormats() {
            return null;
        }
    }

    private class Detector extends AbstractSink {

        private volatile boolean receiving;

        private long timestamp;

        private long maxJitter;

        private int missrate;

        private long psn;

        private long count;

        public Detector(String name, Scheduler scheduler) {
            super(name, scheduler);
        }

        @Override
        public void onMediaTransfer(Frame frame) throws IOException {
            count++;
            if (!receiving) {
                timestamp = System.nanoTime();
                receiving = true;
                return;
            }
            long now = System.nanoTime();
            long j = now - timestamp - PERIOD;
            if (frame.getSequenceNumber() > 1 && Math.abs(j) > Math.abs(maxJitter)) {
                maxJitter = j;
                psn = frame.getSequenceNumber();
            }
            if (frame.getSequenceNumber() > 1 && Math.abs(j) > jitter * 1000000) missrate++;
            timestamp = now;
            frame.recycle();
        }

        @Override
        public Formats getNativeFormats() {
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        RtpPerformance test = new RtpPerformance();
        System.out.println("Creating test");
        test.setup();
        try {
            System.out.println("Creating conversations");
            test.createConversations(1);
            test.relax(2000);
            System.out.println("Starting conversations");
            test.startTest(10000);
            test.report();
            System.out.println("----------------------");
            test.stopTest();
            System.out.println("Scheduler miss rate: " + scheduler.getMissRate());
            System.out.println("Worst execution time: " + scheduler.getWorstExecutionTime());
        } finally {
            test.shutDown();
        }
    }
}
