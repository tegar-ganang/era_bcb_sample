package net.sf.smbt.tcp.handlers;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import net.sf.smbt.tcpcmd.TcpSendCmd;
import net.sf.xqz.model.engine.EVENT_KIND;
import net.sf.xqz.model.engine.Event;
import net.sf.xqz.model.engine.impl.AbstractQxEventHandlerImpl;
import org.eclipse.core.runtime.Platform;

public class UDPCmdEventHandler extends AbstractQxEventHandlerImpl {

    public UDPCmdEventHandler() {
    }

    @Override
    public void handleQxEvent(Event event) {
        if (event.getKind().equals(EVENT_KIND.TX_CMD_ADDED)) {
            if (event.getCmd() instanceof TcpSendCmd) {
                Socket socket = (Socket) event.getQx().getEngine().getPort().getChannel();
                PrintWriter pw = null;
                try {
                    pw = new PrintWriter(socket.getOutputStream(), true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String cmd = new String(((TcpSendCmd) event.getCmd()).getStream());
                if (Platform.inDebugMode()) {
                    System.out.println("Send TCP command to [" + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + "] : '" + cmd + "'");
                }
                pw.println(cmd);
            }
        }
    }
}
