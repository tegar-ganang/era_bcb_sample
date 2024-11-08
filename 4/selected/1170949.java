package jgnash.message;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import java.io.CharArrayWriter;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jgnash.engine.Account;
import jgnash.engine.CommodityNode;
import jgnash.engine.Engine;
import jgnash.engine.EngineFactory;
import jgnash.engine.ExchangeRate;
import jgnash.engine.Transaction;
import jgnash.engine.budget.Budget;
import jgnash.engine.recurring.Reminder;
import jgnash.net.ConnectionFactory;
import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remote message bus client
 * 
 * @author Craig Cavanaugh
 * @version $Id: MessageBusRemoteClient.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
class MessageBusRemoteClient {

    private String host = "localhost";

    private int port = 0;

    private static final Logger logger = LoggerFactory.getLogger(MessageBusRemoteClient.class);

    private IoSession session;

    private XStream xstream;

    static {
        IoBuffer.setUseDirectBuffer(false);
        IoBuffer.setAllocator(new SimpleBufferAllocator());
    }

    public MessageBusRemoteClient(final String host, final int port) {
        this.host = host;
        this.port = port;
        xstream = new XStream(new StaxDriver());
        xstream.alias("Message", Message.class);
        xstream.alias("MessagePropery", MessageProperty.class);
    }

    private static int getConnectionTimeout() {
        return ConnectionFactory.getConnectionTimeout();
    }

    public boolean connectToServer() {
        boolean result = false;
        SocketConnector connector = new NioSocketConnector();
        connector.setConnectTimeoutMillis(getConnectionTimeout() * 1000);
        connector.setHandler(new ClientSessionHandler());
        connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));
        try {
            ConnectFuture future = connector.connect(new InetSocketAddress(host, port));
            future.awaitUninterruptibly();
            session = future.getSession();
            result = true;
            logger.info("Connected to remote message server");
        } catch (RuntimeIoException e) {
            logger.error("Failed to connect to remote message bus");
            if (session != null) {
                session.close(true);
            }
        }
        return result;
    }

    public void disconnectFromServer() {
        session.close(true);
    }

    public synchronized void sendRemoteMessage(final Message message) {
        logger.info("begin");
        CharArrayWriter writer = new CharArrayWriter();
        xstream.marshal(message, new CompactWriter(writer));
        session.write(writer.toString());
        logger.info("sent: " + writer.toString());
    }

    private class ClientSessionHandler extends IoHandlerAdapter {

        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        /**
         * Time in milliseconds to force an update latency to ensure client container is current before processing the
         * message
         */
        private static final int FORCED_LATENCY = 2000;

        @Override
        public void sessionOpened(IoSession s) {
        }

        @Override
        public void messageReceived(final IoSession s, final Object object) {
            logger.info("messageReceived: " + object.toString());
            if (object instanceof String && ((String) object).startsWith("<Message")) {
                final Message message = (Message) xstream.fromXML((String) object);
                if (!EngineFactory.getEngine(EngineFactory.DEFAULT).getUuid().equals(message.getSource())) {
                    scheduler.schedule(new Runnable() {

                        @Override
                        public void run() {
                            processRemoteMessage(message);
                        }
                    }, FORCED_LATENCY, TimeUnit.MILLISECONDS);
                }
            } else {
                logger.error("Unknown message: " + object.toString());
            }
        }

        @Override
        public void exceptionCaught(final IoSession s, final Throwable cause) {
            logger.error("Error", cause);
            s.close(true);
        }

        @Override
        public void sessionClosed(final IoSession s) throws Exception {
            s.close(true);
        }
    }

    /**
     * Takes a remote message and forces remote updates before sending the message to the MessageBus to notify UI
     * components of changes.
     * 
     * @param message Message to process and send
     */
    private synchronized void processRemoteMessage(final Message message) {
        logger.info("processing a remote message");
        Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        if (message.getChannel() == MessageChannel.ACCOUNT) {
            final Account account = (Account) message.getObject(MessageProperty.ACCOUNT);
            switch(message.getEvent()) {
                case ACCOUNT_ADD:
                case ACCOUNT_REMOVE:
                    engine.refreshAccount(account);
                    message.setObject(MessageProperty.ACCOUNT, engine.getStoredObjectByUuid(account.getUuid()));
                    engine.refreshAccount(account.getParent());
                    break;
                case ACCOUNT_MODIFY:
                case ACCOUNT_SECURITY_ADD:
                case ACCOUNT_SECURITY_REMOVE:
                case ACCOUNT_VISIBILITY_CHANGE:
                    engine.refreshAccount(account);
                    message.setObject(MessageProperty.ACCOUNT, engine.getStoredObjectByUuid(account.getUuid()));
                    break;
                default:
                    break;
            }
        }
        if (message.getChannel() == MessageChannel.BUDGET) {
            final Budget budget = (Budget) message.getObject(MessageProperty.BUDGET);
            switch(message.getEvent()) {
                case BUDGET_ADD:
                case BUDGET_UPDATE:
                case BUDGET_REMOVE:
                case BUDGET_GOAL_UPDATE:
                    engine.refreshBudget(budget);
                    message.setObject(MessageProperty.BUDGET, engine.getStoredObjectByUuid(budget.getUuid()));
                    break;
                default:
                    break;
            }
        }
        if (message.getChannel() == MessageChannel.COMMODITY) {
            switch(message.getEvent()) {
                case CURRENCY_MODIFY:
                case COMMODITY_HISTORY_ADD:
                case COMMODITY_HISTORY_REMOVE:
                    final CommodityNode node = (CommodityNode) message.getObject(MessageProperty.COMMODITY);
                    engine.refreshCommodity(node);
                    message.setObject(MessageProperty.COMMODITY, engine.getStoredObjectByUuid(node.getUuid()));
                    break;
                case EXCHANGERATE_ADD:
                case EXCHANGERATE_REMOVE:
                    final ExchangeRate rate = (ExchangeRate) message.getObject(MessageProperty.EXCHANGERATE);
                    engine.refreshExchangeRate(rate);
                    message.setObject(MessageProperty.EXCHANGERATE, engine.getStoredObjectByUuid(rate.getUuid()));
                    break;
                default:
                    break;
            }
        }
        if (message.getChannel() == MessageChannel.REMINDER) {
            switch(message.getEvent()) {
                case REMINDER_ADD:
                case REMINDER_REMOVE:
                    final Reminder reminder = (Reminder) message.getObject(MessageProperty.REMINDER);
                    engine.refreshReminder(reminder);
                    message.setObject(MessageProperty.REMINDER, engine.getStoredObjectByUuid(reminder.getUuid()));
                    break;
                default:
                    break;
            }
        }
        if (message.getChannel() == MessageChannel.TRANSACTION) {
            switch(message.getEvent()) {
                case TRANSACTION_ADD:
                case TRANSACTION_REMOVE:
                    final Account account = (Account) message.getObject(MessageProperty.ACCOUNT);
                    engine.refreshAccount(account);
                    message.setObject(MessageProperty.ACCOUNT, engine.getStoredObjectByUuid(account.getUuid()));
                    final Transaction transaction = (Transaction) message.getObject(MessageProperty.TRANSACTION);
                    engine.refreshTransaction(transaction);
                    message.setObject(MessageProperty.TRANSACTION, engine.getStoredObjectByUuid(transaction.getUuid()));
                    break;
                default:
                    break;
            }
        }
        message.setRemote(true);
        logger.info("fire remote message");
        MessageBus.getInstance().fireEvent(message);
    }
}
