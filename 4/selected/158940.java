package org.tracfoundation.trac2001.primitive.file;

import org.tracfoundation.trac2001.TRAC2001;
import org.tracfoundation.trac2001.util.*;
import java.io.IOException;

/**
 * fr - Read from a file sequentially
 *
 * 
 *
 * @author Edith Mooers, Trac Foundation http://tracfoundation.org
 * @version 1.0 (c) 2001
 */
public class FR {

    /**
     * Read from the file open on the jth channel.
     *
     * @param <CODE>TRAC2001</CODE> the trac process.
     */
    public static void action(TRAC2001 trac) {
        Primitive active = trac.getActivePrimitive();
        if (active.length() >= 3) {
            Channel ch = trac.getChannel(active.jGet());
            if (ch != null) {
                try {
                    active.addValue(ch.read(active.getArg(1), active.getArg(2)));
                } catch (Exception e) {
                    trac.zReturn(active.getArg(3));
                }
            }
        }
    }
}
