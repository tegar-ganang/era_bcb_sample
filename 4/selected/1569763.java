package org.mobicents.media.server.impl.rtp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.server.impl.rtp.clock.AudioClock;
import org.mobicents.media.server.spi.rtp.AVProfile;

/**
 * 
 * @author amit bhayani
 * @author baranowb
 */
public class JitterBufferTest {

    private int period = 20;

    private int jitter = 40;

    private JitterBuffer jitterBuffer;

    private RtpClock clock;

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        jitterBuffer = new JitterBuffer(jitter);
        clock = new AudioClock();
        jitterBuffer.setClock(clock);
        jitterBuffer.setFormat(AVProfile.PCMU);
    }

    @After
    public void tearDown() {
    }

    private RtpPacket createBuffer(int seq) {
        return createBuffer(seq, 0);
    }

    private RtpPacket createBuffer(int seq, int seqCycle) {
        return new RtpPacket((byte) 0, seq, (seq + seqCycle * 65535) * 160, 1, new byte[160]);
    }

    @Test
    public void testAccuracy() {
        jitterBuffer.write(createBuffer(1));
        RtpPacket p = null;
        p = jitterBuffer.read(0);
        assertEquals("Jitter Buffer not full yet", null, p);
        jitterBuffer.write(createBuffer(2));
        p = jitterBuffer.read(20);
        assertEquals("Jitter Buffer not full yet", null, p);
        jitterBuffer.write(createBuffer(3));
        p = jitterBuffer.read(40);
        assertEquals("Jitter Buffer not full yet", null, p);
        jitterBuffer.write(createBuffer(4));
        p = jitterBuffer.read(60);
        assertTrue("Jitter Buffer not full yet", p != null);
        jitterBuffer.write(createBuffer(5));
        p = jitterBuffer.read(80);
        assertTrue("Jitter Buffer should be full", p != null);
    }

    @Test
    public void testAccuracy1() {
        RtpPacket p = null;
        jitterBuffer.write(createBuffer(1));
        jitterBuffer.write(createBuffer(2));
        jitterBuffer.write(createBuffer(3));
        jitterBuffer.write(createBuffer(4));
        p = jitterBuffer.read(0);
        assertTrue("Jitter Buffer not full yet", p != null);
        jitterBuffer.write(createBuffer(5));
        p = jitterBuffer.read(20);
        assertTrue("Jitter Buffer should be full", p != null);
    }

    @Test
    public void testSilenceDetection() {
        int seq = 1;
        RtpPacket p = null;
        for (int i = 0; i < JitterBuffer.QUEUE_SIZE / 4; i++) {
            p = createBuffer(seq++);
            jitterBuffer.write(p);
        }
        RtpPacket readPacket = null;
        int readCount = 0;
        while ((readPacket = jitterBuffer.read(0)) != null) {
            readCount++;
        }
        assertEquals(24, readCount);
        long stampShift = 1600;
        int seq2 = seq;
        for (int i = 0; i < JitterBuffer.QUEUE_SIZE / 4; i++) {
            p = createBuffer(seq2++);
            p.setTimestamp(p.getTimestamp() + stampShift);
            jitterBuffer.write(p);
        }
        readPacket = jitterBuffer.read(0);
        assertEquals(20, readPacket.getDuration());
    }

    @Test
    public void testSilenceDetectionWithSeqOverlap() {
        int seq = 1;
        RtpPacket p = null;
        jitterBuffer.write(createBuffer(65533));
        jitterBuffer.write(createBuffer(65534));
        jitterBuffer.write(createBuffer(65535));
        jitterBuffer.write(createBuffer(1, 1));
        jitterBuffer.write(createBuffer(2, 1));
        RtpPacket readPacket = null;
        int readCount = 0;
        while ((readPacket = jitterBuffer.read(0)) != null) {
            readCount++;
        }
        assertEquals(4, readCount);
        long stampShift = 1600;
        int seq2 = 3;
        for (int i = 0; i < JitterBuffer.QUEUE_SIZE / 4; i++) {
            p = createBuffer(seq2++, 1);
            p.setTimestamp(p.getTimestamp() + stampShift);
            jitterBuffer.write(p);
        }
        readPacket = jitterBuffer.read(0);
        assertEquals(20, readPacket.getDuration());
    }

    @Test
    public void testSeqRollOver() {
        RtpPacket p = null;
        try {
            jitterBuffer.write(createBuffer(65533));
            jitterBuffer.write(createBuffer(65534));
            jitterBuffer.write(createBuffer(65535));
            jitterBuffer.write(createBuffer(1, 1));
            jitterBuffer.write(createBuffer(2, 1));
            p = jitterBuffer.read(0);
            assertTrue("Sequence overflow fails", p != null);
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            assertTrue("Failed, buffer threw exception when Seq number rolled over: " + e, false);
        }
    }

    @Test
    public void testComplicatedSeqRollOver() {
        RtpPacket p = null;
        int x = JitterBuffer.RTP_SEQ_MAX;
        try {
            int[] originalSeqs = new int[] { 65482, 65483, 65484, 65485, 65486, 65487, 65488, 65489, 65490, 65491, 65492, 65493, 65494, 65495, 65496, 65497, 65498, 65499, 65500, 65501, 65502, 65503, 65504, 65505, 65506, 65507, 65508, 65509, 65510, 65511, 65512, 65513, 65514, 65515, 65516, 65517, 65518, 65519, 65520, 65521, 65522, 65523, 65524, 65525, 65526, 65527, 65528, 65529, 65530, 65531, 65532, 65533, 65534, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25 };
            int seq = x - JitterBuffer.QUEUE_SIZE;
            for (int i = 0; i < JitterBuffer.QUEUE_SIZE; i++) {
                jitterBuffer.write(createBuffer(seq++));
            }
            seq = 1;
            for (int i = 0; i < JitterBuffer.QUEUE_SIZE / 4; i++) {
                jitterBuffer.write(createBuffer(seq++, 1));
            }
            jitterBuffer.write(createBuffer(seq + 20, 1));
            int index = 0;
            while ((p = jitterBuffer.read(0)) != null) {
                assertEquals("Wrong sequence!", originalSeqs[index++], p.getSeqNumber());
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            assertTrue("Failed, buffer threw exception when Seq number rolled over: " + e, false);
        }
    }

    @Test
    public void testOverflowBeforeStart() {
        for (int i = 0; i < JitterBuffer.QUEUE_SIZE + 1; i++) {
            this.jitterBuffer.write(createBuffer(i + 1));
        }
        for (int i = 0; i < JitterBuffer.QUEUE_SIZE - 1; i++) {
            long tStamp = 20 * i;
            RtpPacket p = this.jitterBuffer.read(tStamp);
            RtpPacket patternPacket = createPatternPacket(i + 2);
            makeAssertionTestRtpPacket(patternPacket, p);
        }
        this.jitterBuffer.write(createBuffer(JitterBuffer.QUEUE_SIZE + 2));
        RtpPacket p = this.jitterBuffer.read(JitterBuffer.QUEUE_SIZE * 20);
        RtpPacket patternPacket = createPatternPacket(JitterBuffer.QUEUE_SIZE + 1);
        makeAssertionTestRtpPacket(patternPacket, p);
        try {
            p = this.jitterBuffer.read(20 * (JitterBuffer.QUEUE_SIZE + 1));
            assertTrue("Buffer return packet, it should not!", p == null);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue("Failed, buffer threw exception, it should return null!: " + e, false);
        }
    }

    @Test
    public void testOverflowAfterStart() {
        int currentCseq = 1;
        for (int i = 0; i < JitterBuffer.QUEUE_SIZE; i++) {
            this.jitterBuffer.write(createBuffer(i + 1));
        }
        for (int i = 0; i < JitterBuffer.QUEUE_SIZE / 2; i++) {
            long tStamp = 20 * (currentCseq - 1);
            RtpPacket p = this.jitterBuffer.read(tStamp);
            RtpPacket patternPacket = createPatternPacket(currentCseq);
            makeAssertionTestRtpPacket(patternPacket, p);
            currentCseq++;
        }
        int packetsToWrite = (JitterBuffer.QUEUE_SIZE * 3) / 4;
        for (int i = 0; i < packetsToWrite; i++) {
            this.jitterBuffer.write(createBuffer(JitterBuffer.QUEUE_SIZE + i + 1));
        }
        currentCseq = currentCseq + (packetsToWrite - (JitterBuffer.QUEUE_SIZE - currentCseq)) - 1;
        for (int i = 0; i < JitterBuffer.QUEUE_SIZE - 1; i++) {
            long tStamp = 20 * (currentCseq - 1);
            RtpPacket p = this.jitterBuffer.read(tStamp);
            RtpPacket patternPacket = createPatternPacket(currentCseq);
            makeAssertionTestRtpPacket(patternPacket, p);
            currentCseq++;
        }
    }

    @Test
    public void testOverflowOn_w_r_nw() {
        int nextWriteCseq = prefilBuffer(70, 58);
        int nextReadCseq = 70 + 1;
        overFlowAndTest(70, 58, nextReadCseq, nextWriteCseq, 25, 1);
    }

    @Test
    public void testOverflowOn_w_r_nw_5() {
        int nextWriteCseq = prefilBuffer(70, 60);
        int nextReadCseq = 70 + 1;
        overFlowAndTest(70, 60, nextReadCseq, nextWriteCseq, 25, 5);
    }

    @Test
    public void testOverflowOn_w_r_nw_f() {
        int nextWriteCseq = prefilBuffer(95, 90);
        int nextReadCseq = 95 + 1;
        overFlowAndTest(95, 90, nextReadCseq, nextWriteCseq, 25, 1);
    }

    @Test
    public void testOverflowOn_w_r_nw_f_5() {
        int nextWriteCseq = prefilBuffer(90, 70);
        int nextReadCseq = 90 + 1;
        overFlowAndTest(90, 70, nextReadCseq, nextWriteCseq, 25, 5);
    }

    @Test
    public void testOverflowOn_r_nw_w() {
        int nextWriteCseq = prefilBuffer(JitterBuffer.QUEUE_SIZE + 10, 90);
        int nextReadCseq = JitterBuffer.QUEUE_SIZE + 10 + 90 + 1;
        overFlowAndTest(JitterBuffer.QUEUE_SIZE + 10, 90, nextReadCseq, nextWriteCseq, 25, 1);
    }

    @Test
    public void testOverflowOn_r_nw_w_5() {
        int nextWriteCseq = prefilBuffer(JitterBuffer.QUEUE_SIZE + 10, 90);
        int nextReadCseq = JitterBuffer.QUEUE_SIZE + 10 + 90 + 1;
        overFlowAndTest(JitterBuffer.QUEUE_SIZE + 10, 90, nextReadCseq, nextWriteCseq, 25, 5);
    }

    private int prefilBuffer(int targetReadPointer, int targetWritePointer) {
        int currentCseq = 1;
        if (targetReadPointer > targetWritePointer && targetReadPointer < 100) {
            int currentReadCseq = 1;
            for (int i = 0; i < JitterBuffer.QUEUE_SIZE; i++) {
                this.jitterBuffer.write(createBuffer(currentCseq++));
            }
            for (int i = 0; i < targetReadPointer; i++) {
                long tStamp = 20 * (currentReadCseq - 1);
                RtpPacket p = this.jitterBuffer.read(tStamp);
                currentReadCseq++;
            }
            for (int i = 0; i < targetWritePointer; i++) {
                this.jitterBuffer.write(createBuffer(currentCseq++));
            }
        } else {
            currentCseq = this.prefilBuffer(99, targetWritePointer);
            targetReadPointer -= 99;
            for (int i = 0; i <= targetReadPointer; i++) {
                this.jitterBuffer.write(createBuffer(currentCseq++));
            }
        }
        return currentCseq;
    }

    private void overFlowAndTest(int readCursor, int writeCursor, int nextReadCSeq, int nextWriteCseq, int writeCSeqShift, int numberOfPacketsToWrite) {
        int localNextWriteCSeq = nextWriteCseq + writeCSeqShift;
        int localNumberPacketsToWrite = numberOfPacketsToWrite;
        int localNextReadCSeq = (localNextWriteCSeq + 1) - 100;
        for (; localNumberPacketsToWrite > 0; localNumberPacketsToWrite--) {
            this.jitterBuffer.write(createBuffer(localNextWriteCSeq));
            localNextWriteCSeq -= 2;
        }
        localNumberPacketsToWrite = numberOfPacketsToWrite;
        int tStamp = localNextReadCSeq * 20;
        int desiredCSeq = (nextWriteCseq + writeCSeqShift) - 2;
        boolean reasonToLive = true;
        localNextWriteCSeq = nextWriteCseq + writeCSeqShift;
        while (reasonToLive) {
            RtpPacket p = this.jitterBuffer.read(tStamp);
            RtpPacket patternPacket = createPatternPacket(localNextReadCSeq);
            createPatternPacket(localNextReadCSeq);
            try {
                if (p.getDuration() != 20) {
                    patternPacket.setDuration(p.getDuration());
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
                throw e;
            }
            makeAssertionTestRtpPacket(patternPacket, p);
            tStamp += p.getDuration();
            if (desiredCSeq == p.getSeqNumber()) {
                return;
            } else {
                if (localNextReadCSeq >= nextWriteCseq - 1) {
                    localNumberPacketsToWrite--;
                    localNextReadCSeq = (localNextWriteCSeq - 2 * localNumberPacketsToWrite);
                    if (localNumberPacketsToWrite == 0) {
                        return;
                    }
                } else {
                    localNextReadCSeq++;
                }
            }
        }
    }

    private RtpPacket createPatternPacket(int cseq) {
        RtpPacket patternPacket = createBuffer(cseq);
        patternPacket.setDuration(20);
        patternPacket.setTime(cseq * 20);
        return patternPacket;
    }

    private void makeAssertionTestRtpPacket(RtpPacket patternPacket, RtpPacket p) {
        assertTrue("Buffer packet is null!", p != null);
        assertEquals("Duration is incorrect!", patternPacket.getDuration(), p.getDuration());
        assertEquals("Time is incorrect!", patternPacket.getTime(), p.getTime());
        assertEquals("Timestamp is incorrect!", patternPacket.getTimestamp(), p.getTimestamp());
    }

    private void printPacketData(RtpPacket p, int index) {
        if (p != null) {
            System.err.println("Packet[" + index + "], Duration: " + p.getDuration() + ", Seq: " + p.getSeqNumber() + ", Time: " + p.getTime() + ", Timestamp: " + p.getTimestamp());
        } else {
            System.err.println("packet[" + index + "]");
        }
    }
}
