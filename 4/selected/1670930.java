package be.lassi.lanbox.commands;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;
import be.lassi.lanbox.domain.ChannelChanges;
import be.lassi.support.ObjectBuilder;
import be.lassi.support.ObjectTest;
import be.lassi.util.Hex;

/**
 * Tests class <code>CueSceneRead</code>.
 */
public class CueSceneReadTestCase {

    @Test
    public void encodeDecode() {
        CueSceneRead command1 = new CueSceneRead(7, 3);
        CueSceneRead command2 = new CueSceneRead(command1.getRequest());
        assertEquals(command1, command2);
    }

    @Test
    public void processResponse() {
        byte[] bytes = new byte[19];
        Hex.set2(bytes, 5, 2);
        Hex.set4(bytes, 7, 10);
        Hex.set2(bytes, 11, 20);
        Hex.set4(bytes, 13, 11);
        Hex.set2(bytes, 17, 21);
        CueSceneRead command = new CueSceneRead(1, 1);
        command.processResponse(bytes);
        ChannelChanges cc = command.getChannelChanges();
        assertEquals(cc.getString(), "10[20] 11[21] ");
    }

    @Test
    public void object() {
        ObjectBuilder b = new ObjectBuilder() {

            public Object getObject1() {
                return new CueSceneRead(7, 3);
            }

            public Object getObject2() {
                return new CueSceneRead(7, 4);
            }
        };
        ObjectTest.test(b);
    }
}
