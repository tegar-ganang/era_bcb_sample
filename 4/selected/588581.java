package jifx.connection.connector.mina.iso;

import jifx.commons.messages.IMessage;
import org.apache.log4j.Logger;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

public class ClientSessionHandler extends IoHandlerAdapter {

    static Logger logger = Logger.getLogger(ConnectorMinaISO.class);

    private ConnectorMinaISO connectorISO;

    public ClientSessionHandler(ConnectorMinaISO connectorISO) {
        this.connectorISO = connectorISO;
    }

    public void sessionOpened(IoSession session) {
        session.setIdleTime(IdleStatus.BOTH_IDLE, 0);
        session.setAttribute("CHANNEL_NAME", connectorISO.getChannelName());
        if (logger.isDebugEnabled()) logger.debug(connectorISO.getChannelName() + "| Cliente conectado: " + session.getRemoteAddress() + "|");
    }

    public void messageReceived(IoSession session, Object message) {
        IMessage im = (IMessage) message;
        if (logger.isDebugEnabled()) logger.debug(connectorISO.getChannelName() + "|Mensaje recibido: |" + message);
        connectorISO.messageProcessTM(im);
    }

    public void sendMessage(IoSession session, IMessage message) {
        if (logger.isDebugEnabled()) logger.debug(connectorISO.getChannelName() + "| Enviando mensaje. |" + message);
        session.write(message);
    }

    public void sessionIdle(IoSession session, IdleStatus status) {
        if (logger.isDebugEnabled()) logger.debug(connectorISO.getChannelName() + "| Conexi�n terminada por exceder el tiempo de espera sin actividad.|");
        session.close();
    }

    public void exceptionCaught(IoSession session, Throwable cause) {
        logger.error(connectorISO.getChannelName() + "| " + cause.getMessage() + " |");
        connectorISO.stopConnect();
        connectorISO.tryConnect();
    }

    @Override
    public void sessionClosed(IoSession session) {
        if (logger.isDebugEnabled()) logger.debug(connectorISO.getChannelName() + "| Conexi�n cerrada: " + session.getRemoteAddress() + " |");
    }
}
