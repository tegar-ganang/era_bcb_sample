package com.sun.javame.sensor;

import java.util.Hashtable;
import javax.microedition.sensor.SensorInfo;
import javax.microedition.sensor.DataListener;
import com.sun.midp.events.Event;
import com.sun.midp.events.EventListener;
import com.sun.midp.events.EventQueue;
import com.sun.midp.events.EventTypes;
import com.sun.midp.events.NativeEvent;
import com.sun.midp.security.*;

/**
 * This class is a bridge between native event callbacks and
 * {@link AvailabilityListener} callbacks. The current implementation of
 * SensorDevice does not know anything about SensorInfo to which it belongs to,
 * so we must register them in the {@link Configurator} factory.
 */
final class NativeSensorRegistry {

    /** Maps native sensor id to the SensorInfo. */
    private static final Hashtable ID_INFO_MAP = new Hashtable();

    /** Maps native sensor id to the AvailabilityListener. */
    private static final Hashtable ID_LISTENER_MAP = new Hashtable();

    /** Event queue instance. */
    private static final EventQueue eventQueue;

    /** Code operation table: smallest code. */
    static final int EVENT_SMALLEST_CODE = 1;

    /** Code operation table: aviability listener code. */
    static final int EVENT_AV_LISTENER_CODE = EVENT_SMALLEST_CODE;

    /** Code operation table: data collect code. */
    static final int EVENT_DATA_COLLECT_CODE = EVENT_SMALLEST_CODE + 1;

    /** Code operation table: data collect code. */
    static final int EVENT_CONDITIOIN_MET = EVENT_SMALLEST_CODE + 2;

    /** Code operation table: start of data collection of channel. */
    static final int EVENT_CHANNEL_MESSAGE = EVENT_SMALLEST_CODE + 3;

    /** Code operation table: start of data collection of channel. */
    static final int EVENT_SENSOR_MESSAGE = EVENT_SMALLEST_CODE + 4;

    /** Code operation table: start of data collection of channel. */
    static final int EVENT_SENSOR_DATA_RECEIVED = EVENT_SMALLEST_CODE + 5;

    /** Code operation table: highest code. */
    static final int EVENT_HIGHEST_CODE = EVENT_SENSOR_DATA_RECEIVED;

    /** Native event listener. */
    private static final EventListener EVENT_LISTENER = new EventListener() {

        public boolean preprocess(Event evtNew, Event evtOld) {
            boolean retV = (evtNew instanceof NativeEvent);
            if (retV) {
                int codeOp = ((NativeEvent) evtNew).intParam1;
                retV = (EVENT_SMALLEST_CODE <= codeOp && codeOp <= EVENT_HIGHEST_CODE);
            }
            return retV;
        }

        public void process(Event evt) {
            if (evt instanceof NativeEvent) {
                NativeEvent nativeEvt = (NativeEvent) evt;
                int codeOp = nativeEvt.intParam1;
                switch(codeOp) {
                    case EVENT_AV_LISTENER_CODE:
                        processAvListEvent(nativeEvt);
                        break;
                    case EVENT_DATA_COLLECT_CODE:
                        processDataCollectEvent(nativeEvt);
                        break;
                    case EVENT_CONDITIOIN_MET:
                        processConditionMetEvent(nativeEvt);
                        break;
                    case EVENT_CHANNEL_MESSAGE:
                        processChannelMessageQueue(nativeEvt);
                        break;
                    case EVENT_SENSOR_MESSAGE:
                        processSensorMessageQueue(nativeEvt);
                        break;
                    case EVENT_SENSOR_DATA_RECEIVED:
                        processDataListener(nativeEvt);
                        break;
                }
            }
        }

        private void processAvListEvent(NativeEvent nativeEvt) {
            Integer sensorType = new Integer(nativeEvt.intParam2);
            boolean available = (nativeEvt.intParam3 == 1);
            AvailabilityListener listener;
            synchronized (ID_LISTENER_MAP) {
                listener = (AvailabilityListener) ID_LISTENER_MAP.get(sensorType);
            }
            if (listener != null) {
                SensorInfo info;
                synchronized (ID_INFO_MAP) {
                    info = (SensorInfo) ID_INFO_MAP.get(sensorType);
                }
                listener.notifyAvailability(info, available);
            }
        }

        private void processDataCollectEvent(NativeEvent nativeEvt) {
            Sensor sensor = SensorRegistry.getSensor(nativeEvt.intParam2);
            if (sensor != null) {
                ChannelDevice device = sensor.getChannelDevice(nativeEvt.intParam3);
                if (device != null) {
                    ValueListener listener;
                    if ((listener = device.getListener()) != null) {
                        int errorCode = device.measureData();
                        if (errorCode == ValueListener.DATA_READ_OK) {
                            listener.valueReceived(nativeEvt.intParam3, device.getData(), device.getUncertainty(), device.getValidity());
                        } else {
                            listener.dataReadError(nativeEvt.intParam3, errorCode);
                        }
                    }
                }
            }
        }

        private void processConditionMetEvent(NativeEvent nativeEvt) {
            Sensor sensor = SensorRegistry.getSensor(nativeEvt.intParam2);
            if (sensor != null) {
                ChannelImpl channel = (ChannelImpl) sensor.getChannelInfos()[nativeEvt.intParam3];
                if (channel != null) {
                    ConditionListenerPair pair;
                    while ((pair = channel.getCondPair()) != null) {
                        try {
                            pair.getListener().conditionMet(sensor, pair.getData(), pair.getCondition());
                        } catch (Exception exc) {
                        }
                    }
                }
            }
        }

        private void processChannelMessageQueue(NativeEvent nativeEvt) {
            Sensor sensor = SensorRegistry.getSensor(nativeEvt.intParam2);
            if (sensor != null) {
                ChannelImpl channel = (ChannelImpl) sensor.getChannelInfos()[nativeEvt.intParam3];
                if (channel != null) {
                    channel.processMessage();
                }
            }
        }

        private void processSensorMessageQueue(NativeEvent nativeEvt) {
            Sensor sensor = SensorRegistry.getSensor(nativeEvt.intParam2);
            if (sensor != null) {
                sensor.processMessage();
            }
        }

        private void processDataListener(NativeEvent nativeEvt) {
            Sensor sensor = SensorRegistry.getSensor(nativeEvt.intParam2);
            if (sensor != null) {
                sensor.callDataListener();
            }
        }
    };

    /**
     * Inner class to request security token from SecurityInitializer.
     * SecurityInitializer should be able to check this inner class name.
     */
    private static class SecurityTrusted implements ImplicitlyTrustedClass {
    }

    ;

    static {
        SecurityToken classSecurityToken = SecurityInitializer.requestToken(new SecurityTrusted());
        eventQueue = EventQueue.getEventQueue(classSecurityToken);
        eventQueue.registerEventListener(EventTypes.SENSOR_EVENT, EVENT_LISTENER);
    }

    /** Prevents instantiation. */
    private NativeSensorRegistry() {
    }

    /**
     * Register the {@link SensorDevice} with {@link SensorInfo}.
     *
     * @param device SensorDevice instance
     * @param info SensorInfo instance
     */
    static void register(SensorDevice device, SensorInfo info) {
        if (device.sensorType != DeviceFactory.SENSOR_OTHER) {
            synchronized (ID_INFO_MAP) {
                ID_INFO_MAP.put(new Integer(device.sensorType), info);
            }
        }
    }

    /**
     * Posts an event to queue.
     *
     * @param codeOp code operation
     * @param param1 parameter 1
     * @param param2 parameter 2
     * @param param3 parameter 3
     */
    static void postSensorEvent(int codeOp, int param1, int param2, int param3) {
        NativeEvent event = new NativeEvent(EventTypes.SENSOR_EVENT);
        event.intParam1 = codeOp;
        event.intParam2 = param1;
        event.intParam3 = param2;
        event.intParam4 = param3;
        eventQueue.post(event);
    }

    /**
     * Start monitoring the activity change callbacks from native layer.
     *
     * @param sensorType id of the native sensor
     * @param listener AvailabilityListener callback listener
     */
    static void startMonitoringAvailability(int sensorType, AvailabilityListener listener) {
        synchronized (ID_LISTENER_MAP) {
            ID_LISTENER_MAP.put(new Integer(sensorType), listener);
        }
        doStartMonitoringAvailability(sensorType);
    }

    /**
     * Stop monitoring the activity change callbacks from native layer.
     *
     * @param sensorType the native sensor id
     */
    static void stopMonitoringAvailability(int sensorType) {
        doStopMonitoringAvailability(sensorType);
        synchronized (ID_LISTENER_MAP) {
            ID_LISTENER_MAP.remove(new Integer(sensorType));
        }
    }

    /**
     * Start monitoring the activity change events in native layer.
     * <p>
     * <i>calls javacall_sensor_start_monitor_availability(sensor)</i>
     *
     * @param sensorType the native sensor id
     * @return true on success false otherwise
     */
    private static native boolean doStartMonitoringAvailability(int sensorType);

    /**
     * Stop monitoring the activity change events in native layer.
     * <p>
     * <i>calls javacall_sensor_stop_monitor_availability(sensor)</i>
     *
     * @param sensorType the native sensor id
     * @return true on success false otherwise
     */
    private static native boolean doStopMonitoringAvailability(int sensorType);
}
