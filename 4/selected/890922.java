package org.mobicents.media.server.impl.rtp;

import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.impl.resource.dtmf.DetectorImpl;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.net.DatagramSocket;
import java.net.SocketException;
import org.mobicents.media.server.spi.dtmf.DtmfEvent;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.component.Dsp;
import org.mobicents.media.server.impl.rtp.sdp.AVProfile;
import org.mobicents.media.server.impl.PipeImpl;
import java.net.InetSocketAddress;
import org.mobicents.media.server.component.audio.Sine;
import org.mobicents.media.server.component.audio.SpectraAnalyzer;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Clock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.server.spi.format.FormatFactory;
import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class RTPEventTest implements DtmfDetectorListener {

    private Clock clock;

    private Scheduler scheduler;

    private RTPManager rtpManager;

    private UdpManager udpManager;

    private DetectorImpl detector;

    private RTPDataChannel channel;

    private PipeImpl txPipe, rxPipe;

    private DspFactoryImpl dspFactory = new DspFactoryImpl();

    private Dsp dsp11, dsp12;

    private Dsp dsp21, dsp22;

    private Sender sender;

    public RTPEventTest() {
    }

    @Before
    public void setUp() throws Exception {
        AudioFormat pcma = FormatFactory.createAudioFormat("pcma", 8000, 8, 1);
        Formats fmts = new Formats();
        fmts.add(pcma);
        Formats dstFormats = new Formats();
        dstFormats.add(FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1));
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Decoder");
        dsp11 = dspFactory.newProcessor();
        dsp12 = dspFactory.newProcessor();
        dsp21 = dspFactory.newProcessor();
        dsp22 = dspFactory.newProcessor();
        clock = new DefaultClock();
        scheduler = new Scheduler(4);
        scheduler.setClock(clock);
        scheduler.start();
        udpManager = new UdpManager(scheduler);
        udpManager.start();
        rtpManager = new RTPManager(udpManager);
        rtpManager.setBindAddress("127.0.0.1");
        rtpManager.setScheduler(scheduler);
        detector = new DetectorImpl("dtmf", scheduler);
        detector.setVolume(-35);
        detector.setDuration(40);
        detector.addListener(this);
        detector.setDsp(dsp11);
        detector.setFormats(dstFormats);
        channel = rtpManager.getChannel();
        channel.bind();
        sender = new Sender(channel.getLocalPort());
        channel.setPeer(new InetSocketAddress("127.0.0.1", 9200));
        channel.setFormatMap(AVProfile.audio);
        txPipe = new PipeImpl();
        rxPipe = new PipeImpl();
        rxPipe.connect(detector);
        rxPipe.connect(channel.getInput());
    }

    @After
    public void tearDown() {
        txPipe.stop();
        rxPipe.stop();
        channel.close();
        udpManager.stop();
        scheduler.stop();
        sender.close();
    }

    @Test
    public void testTransmission() throws Exception {
        rxPipe.start();
        new Thread(sender).start();
        Thread.sleep(5000);
        rxPipe.stop();
    }

    public void process(DtmfEvent event) {
        System.out.println("TONE=" + event.getTone());
    }

    private class Sender implements Runnable {

        private DatagramSocket socket;

        private ArrayList<byte[]> stream = new ArrayList();

        private int port;

        private InetSocketAddress dst;

        private byte[][] evt = new byte[][] { new byte[] { 0x03, 0x0a, 0x00, (byte) 0xa0 }, new byte[] { 0x03, 0x0a, 0x01, (byte) 0x40 }, new byte[] { 0x03, 0x0a, 0x01, (byte) 0xe0 } };

        private byte[][] evt1 = new byte[][] { new byte[] { 0x0b, 0x0a, 0x00, (byte) 0xa0 }, new byte[] { 0x0b, 0x0a, 0x01, (byte) 0x40 }, new byte[] { 0x0b, 0x0a, 0x01, (byte) 0xe0 }, new byte[] { 0x0b, 0x0a, 0x02, (byte) 0x80 }, new byte[] { 0x0b, 0x0a, 0x03, (byte) 0x20 }, new byte[] { 0x0b, 0x0a, 0x03, (byte) 0xc0 }, new byte[] { 0x0b, 0x0a, 0x04, (byte) 0x60 }, new byte[] { 0x0b, 0x0a, 0x05, (byte) 0x00 }, new byte[] { 0x0b, (byte) 0x8a, 0x05, (byte) 0xa0 }, new byte[] { 0x0b, (byte) 0x8a, 0x05, (byte) 0xa0 }, new byte[] { 0x0b, (byte) 0x8a, 0x05, (byte) 0xa0 } };

        public Sender(int port) throws SocketException {
            this.port = port;
            dst = new InetSocketAddress("127.0.0.1", port);
            socket = new DatagramSocket(new InetSocketAddress("127.0.0.1", 9200));
            this.generateSequence(250);
        }

        private void generateSequence(int len) {
            boolean event = false;
            boolean marker = false;
            int t = 0;
            int count = 0;
            int it = 12345;
            for (int i = 0; i < len; i++) {
                RtpPacket p = new RtpPacket(172, false);
                if (i % 50 == 0 && i > 0) {
                    event = true;
                    marker = true;
                    t = 160 * (i + 1);
                    count = 0;
                }
                if (event) {
                    p.wrap(marker, 101, i + 1, t + it, 123, evt1[count++], 0, 4);
                    marker = false;
                    if (count == evt1.length) {
                        event = false;
                    }
                } else {
                    p.wrap(marker, 8, i + 1, 160 * (i + 1) + it, 123, new byte[160], 0, 160);
                }
                byte[] data = new byte[p.getBuffer().limit()];
                p.getBuffer().get(data);
                stream.add(data);
            }
        }

        public void run() {
            while (!stream.isEmpty()) {
                byte[] data = stream.remove(0);
                try {
                    DatagramPacket p = new DatagramPacket(data, 0, data.length, dst);
                    socket.send(p);
                    Thread.sleep(20);
                } catch (Exception e) {
                }
            }
        }

        public void close() {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
