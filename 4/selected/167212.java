package de.genodeftest.k8055_old;

import java.awt.AWTEvent;
import java.awt.Component;

/**
 * @author Chris
 */
public abstract class IOEvent extends AWTEvent {

    private static final long serialVersionUID = -8055969067773495674L;

    public static final int ID_ANALOG = AWTEvent.RESERVED_ID_MAX + 1;

    public static final int ID_ANALOG_ALL = AWTEvent.RESERVED_ID_MAX + 2;

    public static final int ID_DIGITAL = AWTEvent.RESERVED_ID_MAX + 3;

    public static final int ID_DIGITAL_ALL = AWTEvent.RESERVED_ID_MAX + 4;

    public static final int ID_COUNTER = AWTEvent.RESERVED_ID_MAX + 5;

    public static final int ID_COUNTER_ALL = AWTEvent.RESERVED_ID_MAX + 6;

    public final long eventMoment;

    public IOEvent(Component target, int ID, long eventMoment) {
        super(target, ID);
        this.eventMoment = eventMoment;
    }

    public static class AnalogEvent extends IOEvent {

        private static final long serialVersionUID = 9208622722768990772L;

        private final int value;

        public AnalogEvent(Component target, int value, long eventMoment) {
            super(target, IOEvent.ID_ANALOG, eventMoment);
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static class AnalogAllEvent extends IOEvent {

        private static final long serialVersionUID = 9208622722768990772L;

        public final short[] values;

        public AnalogAllEvent(Component target, short[] values, long eventMoment) {
            super(target, IOEvent.ID_ANALOG_ALL, eventMoment);
            if (values.length != IOChannels.ANALOG.getChannelCount()) throw new IllegalArgumentException();
            this.values = values;
        }
    }

    public static class CounterEvent extends IOEvent {

        private static final long serialVersionUID = 9208622722768990772L;

        private final long value;

        public CounterEvent(Component target, long value, long eventMoment) {
            super(target, IOEvent.ID_COUNTER, eventMoment);
            this.value = value;
        }

        public long getValue() {
            return value;
        }
    }

    public static class CounterAllEvent extends IOEvent {

        private static final long serialVersionUID = 9208622722768990772L;

        public final long[] values;

        public CounterAllEvent(Component target, long[] values, long eventMoment) {
            super(target, IOEvent.ID_COUNTER_ALL, eventMoment);
            if (values.length != IOChannels.COUNTER.getChannelCount()) throw new IllegalArgumentException("wrong array size");
            this.values = values;
        }
    }

    public static class DigitalEvent extends IOEvent {

        private static final long serialVersionUID = 9208622722768990772L;

        private final boolean value;

        public DigitalEvent(Component target, boolean value, long eventMoment) {
            super(target, IOEvent.ID_DIGITAL, eventMoment);
            this.value = value;
        }

        public boolean getValue() {
            return value;
        }
    }

    public static class DigitalAllEvent extends IOEvent {

        private static final long serialVersionUID = 9208622722768990772L;

        public final boolean[] values;

        public DigitalAllEvent(Component target, boolean[] values, long eventMoment) {
            super(target, IOEvent.ID_DIGITAL_ALL, eventMoment);
            if (values.length != IOChannels.DIGITAL.getChannelCount()) throw new IllegalArgumentException("wrong array size");
            this.values = values;
        }
    }
}
