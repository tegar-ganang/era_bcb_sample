package uk.azdev.openfire.net.attrvalues;

import static org.junit.Assert.*;
import java.nio.ByteBuffer;
import org.junit.Before;
import org.junit.Test;
import uk.azdev.openfire.common.SessionId;
import uk.azdev.openfire.net.attrvalues.SessionIdAttributeValue;
import uk.azdev.openfire.net.util.IOUtil;

public class SessionIdAttributeValueTest {

    private static final SessionId INITIAL_SID = new SessionId(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });

    private static final SessionId SAMPLE_ATTR = new SessionId(new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F });

    private SessionIdAttributeValue value;

    @Before
    public void setUp() {
        value = new SessionIdAttributeValue();
    }

    @Test
    public void testSetValue() {
        assertEquals("initial value incorrect", INITIAL_SID, value.getValue());
        value.setSessionId(SAMPLE_ATTR);
        assertNotSame(value.getValue().getBytes(), SAMPLE_ATTR);
        assertEquals("set value incorrect", SAMPLE_ATTR, value.getValue());
    }

    @Test
    public void testGetSize() {
        assertEquals(16, value.getSize());
    }

    @Test
    public void testGetTypeId() {
        assertEquals(SessionIdAttributeValue.TYPE_ID, value.getTypeId());
    }

    @Test
    public void testReadValue() {
        ByteBuffer buffer = IOUtil.createBuffer(SAMPLE_ATTR.getBytes().length);
        buffer.put(SAMPLE_ATTR.getBytes());
        buffer.flip();
        value.readValue(buffer);
        assertEquals("read SID does not match", SAMPLE_ATTR, value.getValue());
    }

    @Test
    public void testWriteValue() {
        value.setSessionId(SAMPLE_ATTR);
        ByteBuffer buffer = IOUtil.createBuffer(value.getSize());
        value.writeValue(buffer);
        buffer.flip();
        SessionIdAttributeValue readValue = new SessionIdAttributeValue();
        readValue.readValue(buffer);
        assertNotSame(readValue.getValue(), SAMPLE_ATTR);
        assertEquals("read after write does not match", SAMPLE_ATTR, readValue.getValue());
    }

    @Test
    public void testNewInstance() {
        assertTrue(value.newInstance().getClass() == SessionIdAttributeValue.class);
    }

    @Test
    public void testToString() {
        value.setSessionId(SAMPLE_ATTR);
        assertEquals("SID:<00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F>", value.toString());
    }
}
