package net.sf.smbt.blinkm.cmd;

import net.sf.smbt.comm.utils.rxtx.Serial;
import net.sf.smbt.commands.BlinkMCmd;
import net.sf.smbt.commands.FLAG;
import net.sf.xqz.engine.cmd.utils.CmdUtil;
import net.sf.xqz.model.engine.EVENT_KIND;
import net.sf.xqz.model.engine.Event;
import net.sf.xqz.model.engine.impl.AbstractQxEventHandlerImpl;

/**
 * @author root
 *
 */
public class BlinkMCmdEventHandler extends AbstractQxEventHandlerImpl {

    public BlinkMCmdEventHandler() {
    }

    public void handleQxEvent(Event evt) {
        if (evt.getCmd() instanceof BlinkMCmd) {
            BlinkMCmd blinkMCmd = (BlinkMCmd) evt.getCmd();
            System.out.println(CmdUtil.INSTANCE.getCmdInfo(evt.getCmd()));
            if (evt.getKind() == EVENT_KIND.TX_CMD_REMOVED || evt.getKind() == EVENT_KIND.RX_CMD_REMOVED) {
                Object obj = evt.getQx().getEngine().getPort().getChannel();
                if (obj instanceof Serial) {
                    synchronized (evt.getQx().getEngine().getOutputInterpreter()) {
                        byte[] frame = evt.getQx().getEngine().getOutputInterpreter().cmd2ByteArray(blinkMCmd);
                        ((Serial) obj).write(frame);
                        if (blinkMCmd.getFlag() == FLAG.READ || blinkMCmd.getFlag() == FLAG.READWRITE) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            byte[] ret = new byte[blinkMCmd.getRetValues().length];
                            ((Serial) obj).readBytes(ret);
                            evt.getQx().getEngine().getOutputInterpreter().processResults(blinkMCmd, ret);
                        }
                    }
                }
            }
        }
    }
}
