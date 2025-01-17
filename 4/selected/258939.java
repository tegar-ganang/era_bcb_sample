package org.openmobster.core.services.channel;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.text.DateFormat;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openmobster.core.common.database.HibernateManager;
import org.openmobster.core.common.transaction.TransactionHelper;
import org.openmobster.core.common.errors.ErrorHandler;
import org.openmobster.core.common.bus.Bus;
import org.openmobster.core.common.bus.BusMessage;
import org.openmobster.core.common.XMLUtilities;
import org.openmobster.core.services.event.ChannelEvent;
import org.openmobster.core.security.device.Device;
import org.openmobster.core.security.device.DeviceController;
import org.openmobster.core.security.identity.Identity;

/**
 * The ChannelDaemon monitors any new data updates on its respective Channel. When an Update is detected, 
 * it sends an Event about this to interested Subscribers
 * 
 * @author openmobster@gmail.com
 */
public final class ChannelDaemon {

    private static Logger log = Logger.getLogger(ChannelDaemon.class);

    private Timer timer;

    private HibernateManager hibernateManager;

    private DeviceController deviceController;

    /**
	 * The channel being monitored
	 */
    private ChannelRegistration channelRegistration;

    public ChannelDaemon(HibernateManager hibernateManager, DeviceController deviceController, ChannelRegistration channelRegistration) {
        this.channelRegistration = channelRegistration;
        this.hibernateManager = hibernateManager;
        this.deviceController = deviceController;
    }

    public ChannelRegistration getChannelRegistration() {
        return this.channelRegistration;
    }

    public void start() {
        String channel = this.channelRegistration.getUri();
        Bus.startBus(channel);
        this.timer = new Timer(this.getClass().getName(), true);
        TimerTask checkForUpdates = new CheckForUpdates(this.hibernateManager, this.deviceController, this.channelRegistration);
        long startDelay = 5000;
        long howOftenShouldICheck = channelRegistration.getUpdateCheckInterval();
        this.timer.schedule(checkForUpdates, startDelay, howOftenShouldICheck);
        log.info("-----------------------------------------------------");
        log.info("Channel Daemon (" + this.channelRegistration.getUri() + ") started. Update Interval: " + howOftenShouldICheck + "(ms)");
        log.info("-----------------------------------------------------");
    }

    public void stop() {
        String channel = this.channelRegistration.getUri();
        this.timer.cancel();
        this.timer.purge();
        Bus.stopBus(channel);
    }

    private static class CheckForUpdates extends TimerTask {

        private ChannelRegistration channelRegistration;

        private HibernateManager hibernateManager;

        private DeviceController deviceController;

        private CheckForUpdates(HibernateManager hibernateManager, DeviceController deviceController, ChannelRegistration channelRegistration) {
            this.channelRegistration = channelRegistration;
            this.hibernateManager = hibernateManager;
            this.deviceController = deviceController;
        }

        public void run() {
            boolean isStartedHere = TransactionHelper.startTx();
            try {
                Date timestamp = new Date();
                List<ChannelBeanMetaData> allUpdates = new ArrayList<ChannelBeanMetaData>();
                List<Device> allDevices = this.deviceController.readAll();
                if (allDevices != null) {
                    for (Device device : allDevices) {
                        if (!device.getIdentity().isActive()) {
                            continue;
                        }
                        this.scan(device, timestamp, allUpdates);
                    }
                }
                if (allUpdates != null && !allUpdates.isEmpty()) {
                    this.sendChannelEvent(allUpdates);
                }
                if (isStartedHere) {
                    TransactionHelper.commitTx();
                }
            } catch (Exception e) {
                log.error(this, e);
                if (isStartedHere) {
                    TransactionHelper.rollbackTx();
                }
                ErrorHandler.getInstance().handle(e);
            }
        }

        private void scan(Device device, Date timestamp, List<ChannelBeanMetaData> allUpdates) {
            try {
                LastScanTimestamp lastScanTimestamp = this.findScanTimestamp(device);
                Identity identity = device.getIdentity();
                String[] added = this.channelRegistration.getChannel().scanForNew(device, lastScanTimestamp.getTimestamp());
                String[] updated = this.channelRegistration.getChannel().scanForUpdates(device, lastScanTimestamp.getTimestamp());
                String[] deleted = this.channelRegistration.getChannel().scanForDeletions(device, lastScanTimestamp.getTimestamp());
                if (added != null) {
                    for (String beanId : added) {
                        ChannelBeanMetaData cour = new ChannelBeanMetaData();
                        cour.setChannel(this.channelRegistration.getUri());
                        cour.setBeanId(beanId);
                        cour.setDeviceId(device.getIdentifier());
                        cour.setUpdateType(ChannelUpdateType.ADD);
                        cour.setPrincipal(identity.getPrincipal());
                        allUpdates.add(cour);
                    }
                }
                if (updated != null) {
                    for (String beanId : updated) {
                        ChannelBeanMetaData cour = new ChannelBeanMetaData();
                        cour.setChannel(this.channelRegistration.getUri());
                        cour.setBeanId(beanId);
                        cour.setDeviceId(device.getIdentifier());
                        cour.setUpdateType(ChannelUpdateType.REPLACE);
                        cour.setPrincipal(identity.getPrincipal());
                        allUpdates.add(cour);
                    }
                }
                if (deleted != null) {
                    for (String beanId : deleted) {
                        ChannelBeanMetaData cour = new ChannelBeanMetaData();
                        cour.setChannel(this.channelRegistration.getUri());
                        cour.setBeanId(beanId);
                        cour.setDeviceId(device.getIdentifier());
                        cour.setUpdateType(ChannelUpdateType.DELETE);
                        cour.setPrincipal(identity.getPrincipal());
                        allUpdates.add(cour);
                    }
                }
                lastScanTimestamp.setTimestamp(timestamp);
                this.save(lastScanTimestamp);
            } catch (Exception e) {
                ErrorHandler.getInstance().handle(e);
                DateFormat dateFormat = DateFormat.getDateTimeInstance();
                Exception ex = new Exception("Device:" + device.getIdentifier() + ",Identity:" + device.getIdentity().getPrincipal() + "Channel: " + this.channelRegistration.getUri() + "Scan Time: " + dateFormat.format(new Date()));
                ErrorHandler.getInstance().handle(e);
            }
        }

        private void sendChannelEvent(List<ChannelBeanMetaData> allUpdates) {
            String channel = this.channelRegistration.getUri();
            BusMessage message = new BusMessage();
            message.setBusUri(channel);
            message.setSenderUri(channel);
            ChannelEvent event = new ChannelEvent();
            event.setChannel(channel);
            event.setAttribute(ChannelEvent.metadata, allUpdates);
            message.setAttribute(ChannelEvent.event, XMLUtilities.marshal(event));
            Bus.sendMessage(message);
        }

        private LastScanTimestamp findScanTimestamp(Device device) throws Exception {
            LastScanTimestamp scanTimestamp = null;
            scanTimestamp = this.read(this.channelRegistration.getUri(), device.getIdentifier());
            if (scanTimestamp == null) {
                scanTimestamp = new LastScanTimestamp();
                scanTimestamp.setTimestamp(new Date());
                scanTimestamp.setChannel(this.channelRegistration.getUri());
                scanTimestamp.setClientId(device.getIdentifier());
            }
            return scanTimestamp;
        }

        private LastScanTimestamp read(String channel, String clientId) throws Exception {
            Session session = null;
            Transaction tx = null;
            try {
                LastScanTimestamp lastScanTimestamp = null;
                session = this.hibernateManager.getSessionFactory().getCurrentSession();
                tx = session.beginTransaction();
                lastScanTimestamp = (LastScanTimestamp) session.createQuery("from LastScanTimestamp where channel=? and clientId=?").setString(0, channel).setString(1, clientId).uniqueResult();
                tx.commit();
                return lastScanTimestamp;
            } catch (Exception e) {
                if (tx != null) {
                    tx.rollback();
                }
                throw e;
            }
        }

        private void save(LastScanTimestamp lastScanTimestamp) throws Exception {
            Session session = null;
            Transaction tx = null;
            try {
                session = this.hibernateManager.getSessionFactory().getCurrentSession();
                tx = session.beginTransaction();
                session.saveOrUpdate(lastScanTimestamp);
                tx.commit();
            } catch (Exception e) {
                if (tx != null) {
                    tx.rollback();
                }
                throw e;
            }
        }
    }
}
