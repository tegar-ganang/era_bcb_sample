package org.tracfoundation.trac2001.primitive.file;

import org.tracfoundation.trac2001.TRAC2001;
import org.tracfoundation.trac2001.util.*;
import java.io.IOException;

/**
 * fg - Generate (create) a file
 *
 * Generate a new file on the jth channel,
 * blindly deletes any existing file with the same name.
 *
 * @author Edith Mooers, Trac Foundation http://tracfoundation.org
 * @version 1.0 (c) 2001
 */
public class FG {

    /**
     * Generate a new file on the jth channel,
     * blindly deletes any existing file with the same name.
     *
     * @param <CODE>TRAC2001</CODE> the trac process.
     */
    public static void action(TRAC2001 trac) {
        Primitive active = trac.getActivePrimitive();
        if (active.length() >= 2) {
            Channel ch = trac.getChannel(active.jGet());
            if (ch != null) {
                try {
                    ch.generate(active.getArg(1));
                } catch (Exception e) {
                    trac.zReturn(active.getArg(2));
                }
            }
        }
    }
}
