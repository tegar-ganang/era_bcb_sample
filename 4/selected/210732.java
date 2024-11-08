package org.tracfoundation.trac2001.primitive.file;

import org.tracfoundation.trac2001.TRAC2001;
import org.tracfoundation.trac2001.util.*;
import java.io.IOException;

/**
 * fpb - position from beginning.
 *
 * Postions the file pointer as an offset from the
 * beginning of the file.
 *
 *
 * @author Edith Mooers, Trac Foundation http://tracfoundation.org
 * @version 1.0 (c) 2001
 */
public class FPB {

    /**
     * Postions the file pointer as an offset from the
     * beginning of the file.
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
                    ch.repositionFromStart(pos);
                } catch (Exception e) {
                    trac.zReturn(active.getArg(2));
                }
            }
        }
    }
}
