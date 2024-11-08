package jifx.connection.connector.mina.ifx;

import java.util.Date;
import jifx.commons.messages.IMessage;
import org.apache.log4j.Logger;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

public class ClientSessionHandler extends IoHandlerAdapter {

    static Logger logger = Logger.getLogger(ConnectorMinaIFX.class);

    private ConnectorMinaIFX connectorIFX;

    public ClientSessionHandler(ConnectorMinaIFX connectorIFX) {
        this.connectorIFX = connectorIFX;
    }

    public void sessionOpened(IoSession session) {
        session.setAttribute("CHANNEL_NAME", connectorIFX.getChannelName());
        if (logger.isDebugEnabled()) logger.debug(connectorIFX.getChannelName() + "| Cliente conectado: " + session.getRemoteAddress() + "|");
    }

    public void messageReceived(IoSession session, Object message) {
        IMessage im = (IMessage) message;
        if (logger.isDebugEnabled()) logger.debug(connectorIFX.getChannelName() + "|Mensaje recibido: |" + message);
        connectorIFX.messageProcessTM(im);
    }

    public void sendMessage(IoSession session, IMessage message) {
        if (logger.isDebugEnabled()) logger.debug(connectorIFX.getChannelName() + "| Enviando mensaje.|" + message);
        session.write(message);
    }

    public void sessionIdle(IoSession session, IdleStatus status) {
        if (logger.isDebugEnabled()) logger.debug(connectorIFX.getChannelName() + "| Conexi�n terminada por exceder el tiempo de espera sin actividad.|");
        session.close();
    }

    public void exceptionCaught(IoSession session, Throwable cause) {
        logger.error(connectorIFX.getChannelName() + "| " + cause.getMessage() + " |");
        connectorIFX.stopConnect();
        connectorIFX.tryConnect();
    }

    @Override
    public void sessionClosed(IoSession session) {
        if (logger.isDebugEnabled()) logger.debug(connectorIFX.getChannelName() + "| Conexi�n cerrada: " + session.getRemoteAddress() + " |");
    }
}
