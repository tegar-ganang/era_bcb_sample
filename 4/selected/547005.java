package org.tracfoundation.trac2001.primitive.file;

import org.tracfoundation.trac2001.TRAC2001;
import org.tracfoundation.trac2001.util.*;
import java.io.IOException;

/**
 * fw - Write to a file sequentially
 *
 * Write text to the file open on the jth channel.
 *
 * @author Edith Mooers, Trac Foundation http://tracfoundation.org
 * @version 1.0 (c) 2001
 */
public class FW {

    /**
     * Write text to the file open on the jth channel.
     *
     * @param <CODE>TRAC2001</CODE> the trac process.
     */
    public static void action(TRAC2001 trac) {
        Primitive active = trac.getActivePrimitive();
        if (active.length() >= 1) {
            Channel ch = trac.getChannel(active.jGet());
            if (ch != null) {
                try {
                    ch.write(active.getArg(1));
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }
}
