package org.apache.jk.common;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.apache.jk.core.Msg;
import org.apache.jk.core.MsgContext;
import org.apache.tomcat.util.IntrospectionUtils;

/** Shm implementation using JDK1.4 nio.
 *
 *
 * @author Costin Manolache
 */
public class Shm14 extends Shm {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(Shm14.class);

    MappedByteBuffer bb;

    public void init() {
        try {
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            FileChannel fc = f.getChannel();
            bb = fc.map(FileChannel.MapMode.READ_WRITE, 0, f.length());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void dumpScoreboard(String file) {
        bb.force();
    }

    public void resetScoreboard() throws IOException {
    }

    public int invoke(Msg msg, MsgContext ep) throws IOException {
        if (log.isDebugEnabled()) log.debug("ChannelShm14.invoke: " + ep);
        return 0;
    }

    public void initCli() {
    }

    public static void main(String args[]) {
        try {
            Shm14 shm = new Shm14();
            if (args.length == 0 || ("-?".equals(args[0]))) {
                shm.setHelp(true);
                return;
            }
            IntrospectionUtils.processArgs(shm, args);
            shm.execute();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
