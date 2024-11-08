package pl.msystems.mqprobe.bean;

import java.util.TreeSet;
import javax.jms.JMSException;
import javax.jms.QueueConnectionFactory;
import org.jdom.Element;
import com.ibm.mq.jms.MQQueueConnectionFactory;

/**
 * Object representing MQ Queue MAnager Connection data.
 * 
 * @author Marek Godlewski
 * 
 */
public class MQQueueManagerData implements Comparable<MQQueueManagerData> {

    /**
	 * Queue Manager Name
	 */
    private String name;

    /**
	 * Queue Manager Host
	 */
    private String host;

    /**
	 * Queue Manager Port
	 */
    private int port = 1414;

    /**
	 * Queue Manager CCSID
	 */
    private int ccsid = 1208;

    /**
	 * Queue Manager Server Connection Channel Name
	 */
    private String channel = "SYSTEM.DEF.SVRCONN";

    /**
	 * Queues located at Queue Manager
	 */
    private TreeSet<String> queues = new TreeSet<String>();

    /**
	 * Default constructor.
	 */
    public MQQueueManagerData() {
    }

    /**
	 * Creates a new MQQueueManagerData using specific parameters
	 * 
	 * @param name name of queue manager
	 * @param host queue manager host name
	 * @param port queue manager listener port
	 * @param channel queue manager channel
	 * @param ccsid queue manager ccsid.
	 */
    public MQQueueManagerData(String name, String host, int port, String channel, int ccsid) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.channel = channel;
        this.ccsid = ccsid;
    }

    /**
	 * Adds queue to list of queues
	 * 
	 * @param queueName
	 */
    public void addQueue(String queueName) {
        queues.add(queueName);
    }

    /**
	 * Returns Queue Connection Factory object for this queue manager.
	 * 
	 * @return Queue Connection Factory object for this queue manager.
	 * @throws JMSException
	 */
    public QueueConnectionFactory getQueueConnectionFactory() throws JMSException {
        MQQueueConnectionFactory qcf = new MQQueueConnectionFactory();
        qcf.setQueueManager(name);
        qcf.setHostName(host);
        qcf.setPort(port);
        qcf.setChannel(channel);
        qcf.setTransportType(com.ibm.mq.jms.JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);
        qcf.setCCSID(ccsid);
        return qcf;
    }

    /**
	 * Returns JDOM Element representing this MQQueueManagerData object.
	 * 
	 * @return JDOM Element representing this MQQueueManagerData object.
	 */
    public Element asJDOMElement() {
        Element mqData = new Element("mqQueueManager");
        mqData.setAttribute("name", name);
        mqData.setAttribute("host", host);
        mqData.setAttribute("port", Integer.toString(port));
        mqData.setAttribute("ccsid", Integer.toString(ccsid));
        mqData.setAttribute("channel", channel);
        for (String queueName : queues) {
            Element queue = new Element("queue");
            queue.setText(queueName);
            mqData.addContent(queue);
        }
        return mqData;
    }

    /**
	 * Method implementation.
	 * 
	 * @param o
	 * @return
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
    public int compareTo(MQQueueManagerData o) {
        String thisAsString = toString() + queues.size();
        String oAsString = o.toString() + o.getQueues().size();
        return thisAsString.compareTo(oAsString);
    }

    /**
	 * Method implementation.
	 * 
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MQQueueManagerData) {
            MQQueueManagerData other = (MQQueueManagerData) obj;
            return toString().equals(other.toString());
        }
        return super.equals(obj);
    }

    /**
	 * Method implementation.
	 * 
	 * @return
	 * @see java.lang.Object#toString()
	 */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(host).append(port);
        sb.append(channel).append(ccsid);
        return sb.toString();
    }

    /**
	 * Returns name.
	 * 
	 * @return Returns name.
	 */
    public String getName() {
        return this.name;
    }

    /**
	 * Sets name.
	 * 
	 * @param name New value of name.
	 */
    public void setName(String name) {
        this.name = name;
    }

    /**
	 * Returns host.
	 * 
	 * @return Returns host.
	 */
    public String getHost() {
        return this.host;
    }

    /**
	 * Sets host.
	 * 
	 * @param host New value of host.
	 */
    public void setHost(String host) {
        this.host = host;
    }

    /**
	 * Returns port.
	 * 
	 * @return Returns port.
	 */
    public int getPort() {
        return this.port;
    }

    /**
	 * Sets port.
	 * 
	 * @param port New value of port.
	 */
    public void setPort(int port) {
        this.port = port;
    }

    /**
	 * Returns ccsid.
	 * 
	 * @return Returns ccsid.
	 */
    public int getCcsid() {
        return this.ccsid;
    }

    /**
	 * Sets ccsid.
	 * 
	 * @param ccsid New value of ccsid.
	 */
    public void setCcsid(int ccsid) {
        this.ccsid = ccsid;
    }

    /**
	 * Returns channel.
	 * 
	 * @return Returns channel.
	 */
    public String getChannel() {
        return this.channel;
    }

    /**
	 * Sets channel.
	 * 
	 * @param channel New value of channel.
	 */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
	 * Returns queues.
	 * 
	 * @return Returns queues.
	 */
    public TreeSet<String> getQueues() {
        return this.queues;
    }
}
