package net.kano.joscar.flap;

import junit.framework.TestCase;
import net.kano.joscar.ByteBlock;

public class SelfTest extends TestCase {

    public void testFlapHeader() {
        try {
            new FlapHeader(ByteBlock.wrap(new byte[0]));
            fail("Should not create flap header with empty data");
        } catch (IllegalArgumentException e) {
        }
        try {
            new FlapHeader(ByteBlock.wrap(new byte[] { 1, 2, 3, 4, 5, 6 }));
            fail("Should not create flap header without 0x2a header");
        } catch (IllegalArgumentException e) {
        }
        FlapHeader header = new FlapHeader(ByteBlock.wrap(new byte[] { 0x2a, 9, 0, 120, 1, 2 }));
        assertEquals(9, header.getChannel());
        assertEquals(120, header.getSeqnum());
        assertEquals(258, header.getDataLength());
    }

    public void testFlapPacket() {
        FlapHeader header = new FlapHeader(ByteBlock.wrap(new byte[] { 0x2a, 9, 0, 120, 0, 3 }));
        FlapPacket packet = new FlapPacket(header, ByteBlock.wrap(new byte[] { 1, 2, 3 }));
        assertTrue(ByteBlock.wrap(new byte[] { 1, 2, 3 }).equals(packet.getData()));
        try {
            new FlapPacket(header, ByteBlock.wrap(new byte[] { 1, 2, 3, 4, 5, 6 }));
            fail("Should not accept flap packet of greater length than header");
        } catch (IllegalArgumentException e) {
        }
        try {
            new FlapPacket(header, ByteBlock.wrap(new byte[] { 1, 2 }));
            fail("Should not accept flap packet of smaller length than header");
        } catch (IllegalArgumentException e) {
        }
        try {
            new FlapPacket(null, ByteBlock.wrap(new byte[] { 1, 2 }));
            fail("Should not accept null header");
        } catch (IllegalArgumentException e) {
        }
        try {
            new FlapPacket(header, null);
            fail("Should not accept null packet data");
        } catch (IllegalArgumentException e) {
        }
    }
}
