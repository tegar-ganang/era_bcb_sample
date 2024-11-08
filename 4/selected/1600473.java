package jifx.connection.connector.server.atm;

import java.util.Map.Entry;
import jifx.commons.TimeOutException;
import jifx.commons.TimeOutMap;
import jifx.commons.TimeoutHashMap;
import jifx.commons.messages.IMessage;
import jifx.commons.messages.MessageID;
import jifx.commons.messages.MessageIDInfo;
import jifx.commons.messages.MessageIdException;
import org.apache.log4j.Logger;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.TransportType;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;

public class ServerSessionHandler extends IoHandlerAdapter {

    static Logger logger = Logger.getLogger(ConnectorSrvATM.class);

    private TimeOutMap<MessageID, IoSession> table;

    private String channelName;

    private int numClient;

    private int numTransaction;

    private int idleTime;

    private int bufferSize;

    private MessageIDInfo messageIDInfo;

    private ConnectorSrvATM server;

    public ServerSessionHandler(ConnectorSrvATM server, int idleTime, int bufferSize, MessageIDInfo messageIDInfo) {
        this.server = server;
        this.channelName = server.getChannelName();
        this.messageIDInfo = messageIDInfo;
        this.numClient = 0;
        this.numTransaction = 1;
        this.idleTime = idleTime;
        this.bufferSize = bufferSize;
        this.table = new TimeoutHashMap<MessageID, IoSession>(server.getTimeout());
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        session.setIdleTime(IdleStatus.BOTH_IDLE, idleTime);
        if (session.getTransportType() == TransportType.SOCKET) {
            ((SocketSessionConfig) session.getConfig()).setReceiveBufferSize(bufferSize);
            ((SocketSessionConfig) session.getConfig()).setSendBufferSize(bufferSize);
        }
    }

    @Override
    public void sessionOpened(IoSession session) {
        session.setAttachment(server.getChannelName() + "-" + numClient);
        numClient++;
        logger.info(session.getAttachment() + "| Cliente conectado: " + session.getRemoteAddress() + "|");
    }

    @Override
    public void messageReceived(IoSession session, Object message) {
        try {
            IMessage im = (IMessage) message;
            im.setElement("BankSvcRq.DebitAddRq.MsgRqHdr.ClientTerminalSeqId", String.valueOf(numTransaction++));
            im.setElement("BankSvcRq.DebitAddRq.MsgRqHdr.NetworkTrnInfo.TerminalId", "22340");
            MessageID msgId = new MessageID(messageIDInfo, im);
            registerSession(msgId, session);
            if (logger.isDebugEnabled()) logger.debug(session.getAttachment() + "|Mensaje recibido: |" + message);
            server.messageProcessTM(im);
        } catch (MessageIdException e) {
            logger.error(channelName + "| Mensaje descartado. Falta key: " + e.getKey() + " |" + message);
        }
    }

    public void sendMessage(IMessage message) throws Exception {
        MessageID msgId = new MessageID(messageIDInfo, message);
        IoSession session = null;
        synchronized (this) {
            session = table.remove(msgId);
        }
        if (session != null) {
            if (logger.isDebugEnabled()) logger.debug(session.getAttachment() + "| Enviando mensaje.|" + message);
            session.write(message);
        } else {
            logger.error(session.getAttachment() + "|Mensaje para cliente desconocido.|" + message);
        }
    }

    public void flush() {
        if (logger.isDebugEnabled()) logger.debug(channelName + "| Flush de canal.|");
        synchronized (this) {
            table.flush();
        }
    }

    public void sessionIdle(IoSession session, IdleStatus status) {
        if (logger.isDebugEnabled()) logger.debug(session.getAttachment() + "| Conexi�n terminada por exceder el tiempo de espera sin actividad.|");
        session.close();
    }

    public void exceptionCaught(IoSession session, Throwable cause) {
        logger.error(session.getAttachment() + "| " + cause.getMessage() + " |");
        session.close();
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        super.sessionClosed(session);
        removeSession(session);
        logger.info(session.getAttachment() + "| Conexi�n cerrada: " + session.getRemoteAddress() + " |");
    }

    private synchronized void registerSession(MessageID msgId, IoSession session) {
        table.put(msgId, session);
        logger.debug(table);
    }

    private synchronized void removeSession(IoSession session) {
        for (Entry<MessageID, IoSession> ses : table.entrySet()) {
            if (ses.getValue().equals(session)) {
                try {
                    table.remove(ses.getKey());
                } catch (TimeOutException e) {
                }
                break;
            }
        }
    }
}
