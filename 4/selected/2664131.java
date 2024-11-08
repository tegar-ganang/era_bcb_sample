package org.dcm4chex.cdw.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.dcm4cheri.util.StringUtils;
import org.jboss.logging.Logger;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 4142 $ $Date: 2004-07-08 17:40:43 -0400 (Thu, 08 Jul 2004) $
 * @since 08.07.2004
 */
public class Executer {

    private final String cmd;

    private final Logger log;

    private final Process child;

    public Executer(Logger log, String[] cmdarray, OutputStream stdout, OutputStream stderr) throws IOException {
        this.log = log;
        this.cmd = StringUtils.toString(cmdarray, ' ');
        log.debug("invoke: " + cmd);
        this.child = Runtime.getRuntime().exec(cmdarray);
        startCopy(child.getInputStream(), stdout);
        startCopy(child.getErrorStream(), stderr);
    }

    public final String cmd() {
        return cmd;
    }

    public int waitFor() throws InterruptedException {
        log.debug("wait for: " + cmd);
        int exit = child.waitFor();
        log.debug("exit[" + exit + "]: " + cmd);
        return exit;
    }

    private void startCopy(final InputStream in, final OutputStream out) {
        new Thread(new Runnable() {

            public void run() {
                try {
                    int len;
                    byte[] buf = new byte[512];
                    while ((len = in.read(buf)) != -1) if (out != null) out.write(buf, 0, len);
                } catch (IOException e) {
                    log.warn("i/o error reading stdout/stderr of " + cmd, e);
                }
            }
        }).start();
    }
}
