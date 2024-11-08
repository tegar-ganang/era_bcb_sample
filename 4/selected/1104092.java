package jifx.connection.configurator.ejb;

import java.util.GregorianCalendar;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.management.AttributeChangeNotification;
import javax.management.AttributeChangeNotificationFilter;
import javax.management.ListenerNotFoundException;
import javax.management.NotificationListener;
import jifx.commons.jboss.ServiceSupport;
import jifx.commons.link.ILink;
import jifx.connection.configurator.filters.FilterFactory;
import jifx.connection.configurator.filters.IFilter;
import jifx.connection.configurator.link.Link;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @jmx:mbean name="mbean:service=Connection-Configurator"
 *            extends="org.jboss.system.ServiceMBean"
 * @author Juan Pablo Bochard
 */
public class Configurator extends ServiceSupport implements ConfiguratorMBean {

    static Logger logger = Logger.getLogger(Configurator.class);

    private Element configuration;

    private ChannelState channelState;

    private String channelName;

    private int timeout;

    private int timeoutLink;

    private int flushTime;

    private int idxLink;

    private String jndi;

    private String incomingTopic;

    private String outgoingTopic;

    private ScheduledThreadPoolExecutor scheduler;

    public Configurator() {
        super();
    }

    public void startService() {
        if (configuration != null) {
            try {
                FilterFactory ff = new FilterFactory();
                IFilter filter = null;
                IFilter prevFilter = null;
                Node node = null;
                Element element = null;
                String[] names = this.getServiceName().getCanonicalName().split("=");
                channelName = names[names.length - 1];
                if (channelName == null) {
                    logger.error("| Nos se pudo obtener el nombre del canal!!!! " + names + "|");
                    return;
                }
                if (jndi == null || jndi.trim().equals("")) jndi = "localhost:1099";
                logger.info(channelName + "| Iniciando connection...|");
                logger.info(channelName + "| Servicio jndi: " + jndi + "|");
                AttributeChangeNotificationFilter nf = new AttributeChangeNotificationFilter();
                nf.enableAttribute("ChannelState");
                incomingTopic = configuration.getAttribute("incomingTopic");
                outgoingTopic = configuration.getAttribute("outgoingTopic");
                idxLink = 0;
                NodeList nodes = configuration.getChildNodes();
                for (int i = 0; i < nodes.getLength(); i++) {
                    node = nodes.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        element = (Element) node;
                        filter = ff.createFilter(channelName, jndi, timeout, flushTime, incomingTopic, outgoingTopic, element);
                        if (filter == null) return;
                        this.addNotificationListener((NotificationListener) filter, nf, this);
                        if (prevFilter != null) prevFilter.setFilterTM(filter);
                        filter.setFilterTC(prevFilter);
                        prevFilter = filter;
                    }
                }
                channelState = ChannelState.ACTIVE;
                Timer timer = new Timer();
                scheduler = new ScheduledThreadPoolExecutor(1);
                scheduler.scheduleAtFixedRate(timer, flushTime, flushTime, TimeUnit.SECONDS);
                scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
                logger.info(channelName + "| Canal iniciado.|");
            } catch (Throwable t) {
                logger.error(channelName + "| No se pudo levantar el canal: " + t.getMessage() + "|");
                t.printStackTrace();
            }
        }
    }

    public void stopService() {
        if (configuration != null) {
            sendNotification(new AttributeChangeNotification(this, this.getNextNotificationSequenceNumber(), new GregorianCalendar().getTimeInMillis(), "Detener connection", "ChannelState", "jifx.connection.configurator.ejb.ConfiguratorMBean.ChannelState", channelState, ChannelState.STOPED));
            scheduler.shutdown();
            channelState = ChannelState.STOPED;
            logger.info(channelName + "|Canal detenido.|");
        }
    }

    public ILink getLinkChannel() {
        ILink link = new Link(channelName, jndi, idxLink, timeoutLink, incomingTopic, outgoingTopic);
        synchronized (this) {
            idxLink++;
        }
        return link;
    }

    public void registerLinkChannel(ILink link) {
        if (logger.isDebugEnabled()) logger.debug(channelName + "| registerLinkChannel - link " + link + " registrado. |");
        AttributeChangeNotificationFilter nf = new AttributeChangeNotificationFilter();
        nf.disableAllAttributes();
        nf.enableAttribute("ChannelState");
        this.addNotificationListener((NotificationListener) link, nf, this);
    }

    public void deregisterLinkChannel(ILink link) {
        try {
            this.removeNotificationListener((NotificationListener) link);
            if (logger.isDebugEnabled()) logger.debug(channelName + "| deregisterLinkChannel - link " + link + " deregistrado. |");
        } catch (ListenerNotFoundException ignore) {
        }
    }

    private void executeFlush() {
        channelState = ChannelState.FLUSH;
        sendNotification(new AttributeChangeNotification(this, this.getNextNotificationSequenceNumber(), new GregorianCalendar().getTimeInMillis(), "Flush chanal", "ChannelState", "jifx.connection.configurator.ejb.ConfiguratorMBean.ChannelState", channelState, ChannelState.FLUSH));
        channelState = ChannelState.ACTIVE;
        logger.info(channelName + "| Realizando Flush en el canal.|");
    }

    /**
	 * @jmx:managed-attribute
	 */
    public String getChannelName() {
        return channelName;
    }

    /**
	 * @jmx:managed-attribute
	 */
    public ChannelState getChannelState() {
        return channelState;
    }

    /**
	 * @jmx:managed-attribute
	 */
    public Element getConfiguration() {
        return configuration;
    }

    /**
	 * @jmx:managed-attribute
	 */
    public void setConfiguration(Element configuration) {
        this.configuration = configuration;
    }

    /**
	 * @jmx:managed-attribute
	 */
    public int getFlushTime() {
        return flushTime;
    }

    /**
	 * @jmx:managed-attribute
	 */
    public void setFlushTime(int flushTime) {
        this.flushTime = flushTime;
    }

    /**
	 * @jmx:managed-attribute
	 */
    public int getTimeout() {
        return timeout;
    }

    /**
	 * @jmx:managed-attribute
	 */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeoutLink() {
        return timeoutLink;
    }

    public void setTimeoutLink(int timeoutLink) {
        this.timeoutLink = timeoutLink;
    }

    public void create() throws Exception {
    }

    class Timer extends Thread {

        @Override
        public void run() {
            executeFlush();
        }
    }

    /**
	 * @jmx:managed-attribute
	 */
    public String getJndi() {
        return jndi;
    }

    /**
	 * @jmx:managed-attribute
	 */
    public void setJndi(String jndi) {
        this.jndi = jndi;
    }
}
