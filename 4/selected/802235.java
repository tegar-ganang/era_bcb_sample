package org.tracfoundation.trac2001.primitive.file;

import org.tracfoundation.trac2001.TRAC2001;
import org.tracfoundation.trac2001.util.*;
import java.io.IOException;

/**
 * fpc - Position from current
 *
 * Positions the pointer at a offset measured from the 
 * current pointer location.
 *
 * @author Edith Mooers, Trac Foundation http://tracfoundation.org
 * @version 1.0 (c) 2001
 */
public class FPC {

    /**
     * Positions the pointer at a offset measured from the 
     * current pointer location.
     *
     * @param <CODE>TRAC2001</CODE> the trac process.
     */
    public static void action(TRAC2001 trac) {
        Primitive active = trac.getActivePrimitive();
        if (active.length() >= 2) {
            Channel ch = trac.getChannel(active.jGet());
            if (ch != null) {
                try {
                    byte[] pos = active.getArg(1);
                    ch.repositionFromCurrent(pos);
                } catch (Exception e) {
                    trac.zReturn(active.getArg(2));
                }
            }
        }
    }
}
