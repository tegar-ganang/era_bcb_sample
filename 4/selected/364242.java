package org.exist.debuggee.dbgp;

import org.apache.log4j.Logger;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.exist.debuggee.DebuggeeJoint;
import org.exist.debuggee.dbgp.packets.Command;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 *
 */
public class ProtocolHandler extends IoHandlerAdapter {

    private static final Logger LOG = Logger.getLogger(ProtocolHandler.class);

    public ProtocolHandler() {
        super();
    }

    @Override
    public void sessionOpened(IoSession session) {
        session.getConfig().setIdleTime(IdleStatus.READER_IDLE, 10 * 60 * 1000);
    }

    @Override
    public void sessionClosed(IoSession session) {
        DebuggeeJoint joint = (DebuggeeJoint) session.getAttribute("joint");
        if (joint != null) joint.sessionClosed(false);
        if (LOG.isDebugEnabled()) LOG.debug("Total " + session.getReadBytes() + " byte(s) readed, " + session.getWrittenBytes() + " byte(s) writed.");
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
        if (status == IdleStatus.READER_IDLE) {
            session.close(true);
        }
    }

    @Override
    public void messageReceived(IoSession session, Object message) {
        Command command = (Command) message;
        if (LOG.isDebugEnabled()) LOG.debug("" + command.toString());
        session.write(command);
    }

    public void exceptionCaught(IoSession session, Throwable cause) {
        System.out.println(cause);
    }
}
