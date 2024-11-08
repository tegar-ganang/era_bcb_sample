package barde.t4c;

import java.util.Date;
import junit.framework.TestCase;

/** * @author cbonar@free.fr * */
public class T4CMessageTest extends TestCase {

    protected Date date;

    protected T4CMessage message;

    /**	 * @see junit.framework.TestCase#setUp()	 */
    protected void setUp() throws Exception {
        this.date = new Date();
        this.message = new T4CMessage(date, "my channel", "my name", "my message");
    }

    public void testGetDate() {
        assertEquals(date, message.getDate());
    }

    public void testGetChannel() {
        assertEquals("my channel", message.getChannel());
    }

    public void testGetCharacter() {
        assertEquals("my name", message.getAvatar());
    }

    public void testGetMessage() {
        assertEquals("my message", message.getContent());
    }
}
