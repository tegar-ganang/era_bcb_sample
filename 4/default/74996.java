import junit.framework.*;
import tw.edu.shu.im.iccio.*;
import tw.edu.shu.im.iccio.datatype.*;
import tw.edu.shu.im.iccio.tagtype.*;

public class CurveStructureTest extends TestCase {

    private byte[] ba = new byte[] { (byte) 0x53, (byte) 0x74, (byte) 0x61, (byte) 0x41, (byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x7f, (byte) 0xff, (byte) 0, (byte) 0, (byte) 0x80, (byte) 0, (byte) 0, (byte) 0 };

    private void compareBytes(byte[] expected, byte[] result) {
        assertEquals("two byte array not same size", expected.length, result.length);
        for (int i = 0; i < result.length; i++) assertEquals("byte " + i, expected[i], result[i]);
    }

    private void compareBytes(byte[] expected, int offset, int len, byte[] result) {
        for (int i = 0; i < len; i++) assertEquals("byte " + i, expected[i + offset], result[i]);
    }

    public void testConstructors() {
        try {
            CurveStructure empty = new CurveStructure();
            CurveStructure inst1 = new CurveStructure(ba, 0, 1);
            byte[] bac = inst1.toByteArray();
        } catch (ICCProfileException e) {
            assertFalse(e.getMessage(), true);
        }
    }

    public void testFromByteArray() {
        CurveStructure data = new CurveStructure();
        try {
            data.fromByteArray(ba, 0, 1);
            assertEquals(0x53746141, data.getMeasureUnitSignature().intValue());
            UInt32Number[] cms = data.getChannelMeasures();
            assertEquals(1, cms.length);
            assertEquals(1, cms[0].intValue());
            XYZNumber[] patches = data.getPatchMeasures();
            assertEquals(1, patches.length);
            assertEquals(1.0, patches[0].getCIEX().doubleValue());
            assertEquals(1.0, patches[0].getCIEY().doubleValue());
            assertEquals(1.0, patches[0].getCIEZ().doubleValue());
            Response16Number[] ar = data.getResponseArray();
            assertEquals(1, ar.length);
            byte[] b = ar[0].toByteArray();
            compareBytes(ba, 20, b.length, b);
        } catch (ICCProfileException e) {
            assertFalse(e.getMessage(), true);
        }
        try {
            data.fromByteArray(ba, -1, ba.length);
            assertFalse("index out of bounds, should raise exception", true);
        } catch (ICCProfileException e) {
            assertEquals(e.getMessage(), ICCProfileException.IndexOutOfBoundsException, e.getType());
        }
        try {
            data.fromByteArray(ba, ba.length, ba.length);
            assertFalse("index out of bounds, should raise exception", true);
        } catch (ICCProfileException e) {
            assertEquals(e.getMessage(), ICCProfileException.IndexOutOfBoundsException, e.getType());
        }
        try {
            data.fromByteArray(null, 0, 0);
            assertFalse("should raise null pointer exception", true);
        } catch (ICCProfileException e) {
            assertEquals(e.getMessage(), ICCProfileException.NullPointerException, e.getType());
        }
    }

    public void testToByteArray() {
        try {
            CurveStructure ct = new CurveStructure(ba, 0, 1);
            byte[] bac = ct.toByteArray();
            compareBytes(ba, bac);
        } catch (ICCProfileException e) {
            assertFalse(e.getMessage(), true);
        }
        try {
            CurveStructure ct = new CurveStructure();
            byte[] bac = ct.toByteArray();
            assertFalse("should raise ICCProfileException.InvalidDataValueException", true);
        } catch (ICCProfileException e) {
            assertTrue(e.getMessage(), ICCProfileException.InvalidDataValueException == e.getType());
        }
    }
}
