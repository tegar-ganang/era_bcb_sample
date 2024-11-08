package org.nexopenframework.management.monitor.channels;

import java.io.Serializable;
import java.util.List;
import javax.management.ObjectName;
import org.nexopenframework.management.module.Module;
import org.nexopenframework.management.module.model.NodeInfo;

/**
 * <p>NexOpen Framework</p>
 * 
 * <p>Main component for sending information to notification listener channels. It carries information about
 * information/warning or problem, threshold value of monitor, gauge value which has produced this notification
 * and in which module and node has been produced.</p>
 * 
 * @see org.nexopenframework.management.module.Module
 * @see org.nexopenframework.management.module.model.NodeInfo
 * @author Francesc Xavier Magdaleno
 * @version $Revision ,$Date 01/05/2009 21:13:23
 * @since 1.0.0.m1
 */
public class ChannelNotificationInfo implements Serializable, Comparable<ChannelNotificationInfo> {

    /**serialization stuff*/
    private static final long serialVersionUID = 1L;

    public static enum Severity {

        /**<p><code>INFO</code> indicates just information of some events in your application.</p>*/
        INFO, /**
		 * <p><code>WARN</code> indicates something that it can cause problems to your application.</p>
		 * @since 1.0.0.m2
		 */
        WARN, /**<p><code>ERROR</code> indicates and internal error/problem in your application.</p>*/
        ERROR, /**<p><code>FATAL</code> indicates and internal dangerous problem in your application.</p>*/
        FATAL
    }

    public static enum ChannelMedia {

        /**Synchronous invocation media*/
        SYNC, /**Asynchronous invocation media*/
        ASYNC
    }

    /**which severity must be applied*/
    private final Severity severity;

    /**a message to be notified*/
    private final String message;

    /**when this info has been created*/
    private final long timestamp;

    /**name of monitor*/
    private String monitorName;

    /**JMX ObjectName of monitor*/
    private ObjectName objectName;

    /**threshold value of monitor*/
    private Number threshold;

    /**Gauge value which has produced this notification*/
    private Number derivedGauge;

    /**current thread involved in invocation*/
    private final String threadName;

    /**media where this information has been created [sync or async media]*/
    private final ChannelMedia media;

    /**Current module involved or default {@link Module#SYSTEM}*/
    private Module module = Module.SYSTEM;

    /**Current module involved*/
    private NodeInfo nodeInfo;

    /**
	 * <p>Optional information to be shown in channels</p>
	 * @since 1.0.0.m3
	 */
    private List<? extends Serializable> optionalInfo;

    /**
	 * <p>Constructor which builds info needed for channels to notify problems</p>
	 * 
	 * @param severity
	 * @param message
	 * @param timestamp
	 */
    public ChannelNotificationInfo(final Severity severity, final String message, final long timestamp, final String threadName) {
        this(severity, message, timestamp, threadName, ChannelMedia.SYNC);
    }

    /**
	 * <p></p>
	 * 
	 * @param severity
	 * @param message
	 * @param timestamp
	 * @param threadName
	 * @param module
	 * @since 1.0.0.m2
	 */
    public ChannelNotificationInfo(final Severity severity, final String message, final long timestamp, final String threadName, final Module module) {
        this(severity, message, timestamp, threadName, ChannelMedia.SYNC, module);
    }

    /**
	 * <p></p>
	 * 
	 * @param severity
	 * @param message
	 * @param timestamp
	 * @param threadName
	 * @param media
	 */
    public ChannelNotificationInfo(final Severity severity, final String message, final long timestamp, final String threadName, final ChannelMedia media) {
        super();
        this.severity = severity;
        this.message = message;
        this.timestamp = timestamp;
        this.threadName = threadName;
        this.media = media;
    }

    /**
	 * <p></p>
	 * 
	 * @param severity
	 * @param message
	 * @param timestamp
	 * @param threadName
	 * @param media
	 * @param module
	 * @since 1.0.0.m2
	 */
    public ChannelNotificationInfo(final Severity severity, final String message, final long timestamp, final String threadName, final ChannelMedia media, final Module module) {
        this(severity, message, timestamp, threadName, media);
        this.module = module;
    }

    public String getMessage() {
        return message;
    }

    public Severity getSeverity() {
        return severity;
    }

    public final long getTimestamp() {
        return this.timestamp;
    }

    /**
	 * <p>Name of thread where notification has been produced</p>
	 * 
	 * @return
	 */
    public final String getThreadName() {
        return this.threadName;
    }

    public final ChannelMedia getChannelMedia() {
        return this.media;
    }

    /**
	 * <p>Module where notification has been produced</p>
	 * 
	 * @return the module
	 */
    public Module getModule() {
        return module;
    }

    /**
	 * <p>Added module involved</p>
	 * 
	 * @param module the module to set
	 */
    public void setModule(final Module module) {
        if (module != null) {
            this.module = module;
        }
    }

    /**
	 * @return the nodeInfo
	 */
    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }

    /**
	 * @param nodeInfo the nodeInfo to set
	 */
    public void setNodeInfo(final NodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    /**
	 * <p></p>
	 * 
	 * @since 1.0.0.m2
	 * @return the threshold
	 */
    public Number getThreshold() {
        return threshold;
    }

    /**
	 * <p></p>
	 * 
	 * @since 1.0.0.m2
	 * @param threshold the threshold to set
	 */
    public void setThreshold(final Number threshold) {
        this.threshold = threshold;
    }

    /**
	 * <p></p>
	 * 
	 * @since 1.0.0.m2
	 * @return the derivedGauge
	 */
    public Number getDerivedGauge() {
        return derivedGauge;
    }

    /**
	 * <p></p>
	 * 
	 * @since 1.0.0.m2
	 * @param derivedGauge the derivedGauge to set
	 */
    public void setDerivedGauge(final Number derivedGauge) {
        this.derivedGauge = derivedGauge;
    }

    /**
	 * <p></p>
	 * 
	 * @since 1.0.0.m2
	 * @return the monitorName
	 */
    public String getMonitorName() {
        return monitorName;
    }

    /**
	 * <p></p>
	 * 
	 * @since 1.0.0.m2
	 * @param monitorName the monitorName to set
	 */
    public void setMonitorName(final String monitorName) {
        this.monitorName = monitorName;
    }

    /**
	 * <p>JMX {@link ObjectName} of this monitor</p>
	 * 
	 * @since 1.0.0.m2
	 * @return the objectName
	 */
    public ObjectName getObjectName() {
        return objectName;
    }

    /**
	 * <p></p>
	 * 
	 * @since 1.0.0.m2
	 * @param objectName the objectName to set
	 */
    public void setObjectName(final ObjectName objectName) {
        this.objectName = objectName;
    }

    public List<? extends Serializable> getOptionalInfo() {
        return this.optionalInfo;
    }

    public void setOptionalInfo(final List<? extends Serializable> optionalInfo) {
        this.optionalInfo = optionalInfo;
    }

    /**
	 * <p>Free resources for GC</p>
	 */
    public void clearOptionalInfo() {
        if (this.optionalInfo != null) {
            this.optionalInfo.clear();
        }
    }

    /**
	 * <p></p>
	 * 
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
    public int compareTo(final ChannelNotificationInfo o) {
        return 0;
    }
}
