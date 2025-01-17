package websiteschema.common.amqp;

import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.AlreadyClosedException;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import websiteschema.common.base.Function;
import org.apache.log4j.Logger;
import java.io.IOException;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import static websiteschema.utils.PojoMapper.*;

/**
 *
 * @author ray
 */
public class RabbitQueue<T> {

    private ConnectionFactory factory = new ConnectionFactory();

    private Connection connection;

    private String queueName;

    private String host = "localhost";

    private int port = -1;

    private boolean connStateOk = false;

    private String charset = "UTF-8";

    private Logger l = Logger.getLogger(RabbitQueue.class);

    public RabbitQueue(String host, String queueName) {
        this(host, -1, queueName);
    }

    public RabbitQueue(String host, int port, String queueName) {
        this.host = host;
        this.port = port;
        this.queueName = queueName;
        try {
            reset();
        } catch (Exception ex) {
            l.error(ex.getMessage(), ex);
        }
    }

    private void reset() throws IOException {
        connection = null;
        factory.setHost(host);
        factory.setRequestedHeartbeat(10);
        factory.setConnectionTimeout(30000);
        if (port > 0) {
            factory.setPort(port);
        }
        connection = factory.newConnection();
        if (null != connection) {
            l.debug("created new connection.");
            connStateOk = true;
        } else {
            l.debug("can not create new connection.");
        }
    }

    public Channel getChannel() throws Exception {
        if (null != connection) {
            Channel chnl = connection.createChannel();
            chnl.queueDeclare(queueName, true, false, false, null);
            return chnl;
        } else {
            l.debug("connection is null");
            checkConnection();
            if (null != connection) {
                l.debug("retry create channel.");
                Channel chnl = connection.createChannel();
                chnl.queueDeclare(queueName, true, false, false, null);
                return chnl;
            }
        }
        return null;
    }

    private QueueingConsumer getQueueingConsumer(Channel chnl) throws IOException {
        if (null != chnl) {
            chnl.basicQos(1);
            QueueingConsumer c = new QueueingConsumer(chnl);
            chnl.basicConsume(queueName, false, c);
            return c;
        }
        return null;
    }

    public synchronized void checkConnection() {
        if (!connStateOk) {
            close();
            try {
                reset();
            } catch (Exception ex) {
                l.error(ex.getMessage(), ex);
            }
        }
    }

    public boolean offer(T msg) {
        boolean ret = false;
        Channel channel = null;
        try {
            channel = getChannel();
            if (null != channel) {
                String json = toJson(msg);
                channel.basicPublish("", queueName, MessageProperties.PERSISTENT_TEXT_PLAIN, json.getBytes(charset));
                l.debug(" [x] Sent '" + json + "'");
                ret = true;
            } else {
                l.debug("channel can not be initialized.");
            }
        } catch (Exception ex) {
            processException(ex);
        } finally {
            try {
                if (null != channel) {
                    l.debug("close channel.");
                    channel.close();
                    l.debug("close channel successfully.");
                }
            } catch (Exception ex) {
                l.error(ex.getMessage(), ex);
            }
        }
        return ret;
    }

    public void processException(Exception ex) {
        if (ex instanceof JsonGenerationException) {
            l.error("json error", ex);
        } else if (ex instanceof JsonMappingException) {
            l.error("json error", ex);
        } else if (ex instanceof IOException) {
            l.error("io error, trying reconnect...", ex);
            connStateOk = false;
            checkConnection();
        } else if (ex instanceof AlreadyClosedException) {
            l.error("server already shutdown, trying reconnect sec...", ex);
            connStateOk = false;
            checkConnection();
        } else {
            l.error(ex.getMessage(), ex);
        }
    }

    public boolean offer(Channel channel, T msg) {
        boolean ret = false;
        try {
            if (null != channel) {
                String json = toJson(msg);
                channel.basicPublish("", queueName, MessageProperties.PERSISTENT_TEXT_PLAIN, json.getBytes(charset));
                l.debug(" [x] Sent '" + json + "'");
                ret = true;
            } else {
                l.debug("channel can not be initialized.");
            }
        } catch (Exception ex) {
            processException(ex);
        }
        return ret;
    }

    public boolean offer(T[] msgs) {
        boolean ret = false;
        Channel channel = null;
        try {
            channel = getChannel();
            if (null != channel) {
                for (T msg : msgs) {
                    String json = toJson(msg);
                    channel.basicPublish("", queueName, MessageProperties.PERSISTENT_TEXT_PLAIN, json.getBytes(charset));
                    l.debug(" [x] Sent '" + json + "'");
                }
                ret = true;
                l.debug("sent all message successfully.");
            } else {
                l.debug("channel can not be initialized.");
            }
        } catch (Exception ex) {
            processException(ex);
        } finally {
            try {
                if (null != channel) {
                    l.debug("close channel.");
                    channel.close();
                    l.debug("close channel successfully.");
                }
            } catch (Exception ex) {
                l.error(ex.getMessage(), ex);
            }
        }
        return ret;
    }

    public T poll(Class<T> clazz) {
        return poll(clazz, -1, null);
    }

    public T poll(Class<T> clazz, int timeout) {
        return poll(clazz, timeout, null);
    }

    public T poll(Class<T> clazz, Function<T> worker) {
        return poll(clazz, -1, worker);
    }

    public T poll(Class<T> clazz, int timeout, Function<T> worker) {
        Channel channel = null;
        QueueingConsumer.Delivery delivery = null;
        T msg = null;
        try {
            channel = getChannel();
            QueueingConsumer consumer = getQueueingConsumer(channel);
            if (null != consumer) {
                if (timeout > 0) {
                    delivery = consumer.nextDelivery(timeout);
                } else {
                    delivery = consumer.nextDelivery();
                }
                if (null != delivery) {
                    String message = new String(delivery.getBody(), charset);
                    l.debug(" [x] Received from " + queueName + ": '" + message + "'");
                    msg = fromJson(message, clazz);
                    if (null != worker) {
                        worker.invoke(msg);
                    }
                    l.debug(" [x] Done");
                } else {
                    l.debug("rabbitmq timeout " + timeout);
                }
            }
        } catch (Exception ex) {
            if (ex instanceof JsonGenerationException) {
                l.error("json error", ex);
            } else if (ex instanceof JsonMappingException) {
                l.error("json error", ex);
            } else if (ex instanceof IOException) {
                l.error("io error, trying reconnect...", ex);
                connStateOk = false;
                checkConnection();
            } else if (ex instanceof ShutdownSignalException) {
                l.error("server shutdown, trying reconnect after 30 sec...", ex);
                connStateOk = false;
                sleep(30000);
                checkConnection();
            } else {
                l.error(ex.getMessage(), ex);
            }
        } finally {
            try {
                if (null != channel) {
                    if (null != delivery) {
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    }
                    l.debug("close channel.");
                    channel.close();
                    l.debug("close channel successfully.");
                }
            } catch (Exception ex) {
                l.error(ex.getMessage(), ex);
            }
        }
        return msg;
    }

    private void sleep(long milsec) {
        try {
            Thread.sleep(milsec);
        } catch (Exception ex) {
            l.error(ex.getMessage(), ex);
        }
    }

    public void close() {
        try {
            if (null != connection) {
                l.debug("closed connection.");
                connection.close();
                connection = null;
            }
        } catch (Exception ex) {
            l.error(ex.getMessage(), ex);
        }
    }

    public String getQueueName() {
        return queueName;
    }
}
