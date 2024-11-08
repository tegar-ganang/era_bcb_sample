package org.jtv.frontend;

import java.io.IOException;
import java.io.OutputStream;
import java.util.SortedSet;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.jtv.backend.TvControllerMXBean;
import org.jtv.common.ConsoleTvControllerObserver;
import org.jtv.common.RecordingData;
import org.jtv.common.TvController;
import org.jtv.common.TvControllerEvent;
import org.jtv.common.TvControllerObservers;
import org.jtv.common.TvControllerResult;

public class JmxTvController implements TvController, NotificationListener, NotificationFilter {

    private static final long serialVersionUID = 1L;

    private transient JMXConnector jmxc;

    private transient TvControllerObservers observers;

    private transient TvControllerMXBean proxy;

    public JmxTvController(String server, int port) {
        super();
        this.observers = new TvControllerObservers();
        String urlString = "service:jmx:rmi:///jndi/rmi://" + server + ":" + port + "/jmxrmi";
        System.out.println("Connecting with url " + urlString);
        try {
            JMXServiceURL url = new JMXServiceURL(urlString);
            jmxc = JMXConnectorFactory.connect(url, null);
            MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
            proxy = JMX.newMXBeanProxy(mbsc, new ObjectName("tv:type=controller,name=master"), TvControllerMXBean.class);
            mbsc.addNotificationListener(new ObjectName("tv:type=controllerObserver,name=jmxObserver"), this, this, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TvControllerResult changeChannel(int tunerNumber, int channel) {
        return proxy.changeChannel(tunerNumber, channel);
    }

    public long getTime() {
        return proxy.getTime();
    }

    public void close() {
        try {
            jmxc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getNumberOfTuners() {
        return proxy.getNumberOfTuners();
    }

    public int getChannel(int tunerNumber) {
        return proxy.getChannel(tunerNumber);
    }

    public TvControllerResult scheduleRecording(int channel, long start, long end, String fileName) {
        return proxy.scheduleRecording(channel, start, end, fileName);
    }

    public TvControllerResult watch(int tunerNumber, OutputStream out) {
        return null;
    }

    public TvControllerObservers getObservers() {
        return observers;
    }

    public TvControllerResult cancelRecordingId(int id) {
        return proxy.cancelRecordingId(id);
    }

    public SortedSet<RecordingData> getRecordingsFrom(long start) {
        return proxy.getRecordingsFrom(start);
    }

    public TvControllerResult synchronizeRecordingInfo() {
        return proxy.synchronizeRecordingInfo();
    }

    public void handleNotification(Notification notification, Object handback) {
        observers.event((TvControllerEvent) notification.getUserData());
    }

    public boolean isNotificationEnabled(Notification notification) {
        return "tvControllerEvent".equals(notification.getType());
    }

    public static void main(String[] args) {
        String host;
        if (args.length > 0) {
            host = args[0];
        } else {
            host = "localhost";
        }
        int port;
        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        } else {
            port = 9999;
        }
        TvController remote = new JmxTvController(host, port);
        remote.getObservers().addObserver(new ConsoleTvControllerObserver("REMOTE", System.out));
        ConsoleTvController consoleController = new ConsoleTvController(remote);
        consoleController.repLoop(System.in);
        remote.close();
    }
}
