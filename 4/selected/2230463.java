package gov.sns.apps.lossviewer2.signals;

import gov.sns.ca.*;
import java.util.*;
import java.util.logging.*;

public class ChargeNormalizer extends AbstractSignal implements ConnectionListener {

    public void connectionMade(Channel channel) {
        if (channel == machineModeChannel) {
            connectMachineMode();
        } else {
            connectBCM(channel);
        }
    }

    private String currentBCM = "";

    private void connectBCM(final Channel channel) {
        Monitor m = bcmMonitors.get(channel);
        if (m == null) {
            try {
                m = channel.addMonitorValTime(new IEventSinkValTime() {

                    public void eventValue(ChannelTimeRecord record, Channel chan) {
                        boolean amIcurrentBCM = false;
                        synchronized (currentBCM) {
                            amIcurrentBCM = currentBCM.equals(channel.channelName());
                        }
                        if (amIcurrentBCM) {
                            long tst = (long) (record.getTimestamp().getSeconds() * 1000);
                            double v = record.doubleValue();
                            dispatcher.processNewValue(new ScalarSignalValue(ChargeNormalizer.this, tst, v));
                        }
                    }
                }, 1);
                bcmMonitors.put(channel, m);
            } catch (MonitorException e) {
            } catch (ConnectionException e) {
            }
        }
    }

    private void connectMachineMode() {
        Logger.getLogger(this.getClass().getCanonicalName()).log(Level.INFO, ("Machine mode connected"));
        if (machineModeMonitor == null) {
            try {
                machineModeMonitor = machineModeChannel.addMonitorValTime(new IEventSinkValTime() {

                    public void eventValue(ChannelTimeRecord record, Channel chan) {
                        synchronized (currentBCM) {
                            String mode = record.stringValue();
                            currentBCM = bcmChannelNames.get(mode);
                            Logger.getLogger(ChargeNormalizer.this.getClass().getCanonicalName()).log(Level.INFO, ("Normalization BCM changed to " + currentBCM));
                        }
                    }
                }, 1);
            } catch (MonitorException e) {
            } catch (ConnectionException e) {
            }
        }
    }

    public void connectionDropped(Channel channel) {
        Logger.getLogger(this.getClass().getCanonicalName()).log(Level.WARNING, ("Connection dropped " + channel.channelName()));
    }

    Channel machineModeChannel;

    Monitor machineModeMonitor;

    Map<String, Channel> bcmChannels = new HashMap<String, Channel>();

    Map<Channel, Monitor> bcmMonitors = new HashMap<Channel, Monitor>();

    Map<String, String> bcmChannelNames;

    String machineModeName;

    public ChargeNormalizer(String machineModeName, Map<String, String> bcmNames) {
        this.machineModeName = machineModeName;
        bcmChannelNames = bcmNames;
        setName("Charge");
    }

    public void close() {
        machineModeChannel.disconnect();
        machineModeChannel.flushIO();
        for (String bcmName : bcmChannels.keySet()) {
            bcmChannels.get(bcmName).disconnect();
            bcmChannels.get(bcmName).flushIO();
        }
    }

    /**
     * Method start
     *
     */
    public void start() {
        ChannelFactory cf = ChannelFactory.defaultFactory();
        machineModeChannel = cf.getChannel(machineModeName);
        machineModeChannel.addConnectionListener(this);
        machineModeChannel.requestConnection();
        for (String bcmName : bcmChannelNames.keySet()) {
            Channel ch = cf.getChannel(bcmChannelNames.get(bcmName));
            ch.addConnectionListener(this);
            ch.requestConnection();
            bcmChannels.put(bcmName, ch);
            bcmMonitors.put(ch, null);
        }
    }

    public double doubleValue() {
        return 1.0;
    }

    public boolean setValue(double v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
