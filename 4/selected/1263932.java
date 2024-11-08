package ch.superj.midi;

import javax.sound.midi.ShortMessage;

public class AxiomRawData {

    private Pad[] _pads = new Pad[8];

    private Rotary[] _rotary = new Rotary[16];

    private Key _key;

    private Modulation _modulation;

    private AxiomController _currentController = null;

    public synchronized void receiveShortMessage(ShortMessage shortMessage) {
        int status = shortMessage.getStatus();
        _currentController = null;
        if (status == ShortMessage.NOTE_ON) {
            _key = new Key();
            _key.setNote(shortMessage.getData1());
            _key.setVelocity(shortMessage.getData2());
            _key.setNoteOn(true);
            _currentController = _key;
        } else if (status == ShortMessage.CHANNEL_PRESSURE) {
            if (_key != null) {
                _key.setChannelPressure(shortMessage.getData1());
                _key.setNoteOn(false);
                _currentController = _key;
            }
        } else if (status == ShortMessage.CONTROL_CHANGE) {
            if (shortMessage.getData1() == 30) {
                _modulation = new Modulation(shortMessage.getData2());
                _currentController = _modulation;
            }
            Rotary rotary = null;
            if (shortMessage.getData1() > -1 && shortMessage.getData1() < _rotary.length) {
                rotary = new Rotary(shortMessage.getData2());
                rotary.setNumber(shortMessage.getData1() - 1);
                _rotary[shortMessage.getData1()] = rotary;
                _currentController = rotary;
            }
        } else if (status == ShortMessage.PITCH_BEND) {
            PitchBend pitchBend = new PitchBend(shortMessage.getData2());
            _currentController = pitchBend;
        } else if (status == 185) {
            Pad pad = null;
            if (shortMessage.getData1() - 20 > -1 && shortMessage.getData1() - 20 < _pads.length) {
                pad = new Pad(shortMessage.getData2());
                pad.setNumber(shortMessage.getData1() - 20);
                _pads[shortMessage.getData1() - 20] = pad;
            }
            _currentController = pad;
        }
    }

    public synchronized Pad getPad(int number) {
        Pad pad = _pads[number];
        if (pad == null) return new Pad(0);
        return (Pad) pad.clone();
    }

    public synchronized Rotary getRotary(int number) {
        Rotary rotary = _rotary[number];
        if (rotary == null) return new Rotary(0);
        return (Rotary) rotary.clone();
    }

    public synchronized Key getKey() {
        if (_key == null) return new Key();
        return _key.clone();
    }

    public static class Rotary extends Controller {

        public Rotary(int value) {
            super(value);
        }
    }

    public static class Pad extends Controller {

        public Pad(int value) {
            super(value);
        }
    }

    public static class Modulation extends Controller {

        public Modulation(int value) {
            super(value);
        }
    }

    public static class PitchBend extends Controller {

        public PitchBend(int value) {
            super(value);
        }
    }

    public static class Key implements Cloneable, AxiomController {

        private int _note;

        private int _velocity;

        private int _channelPressure;

        private boolean _noteOn = false;

        public synchronized boolean isNoteOn() {
            return _noteOn;
        }

        public synchronized void setNoteOn(boolean on) {
            _noteOn = on;
        }

        public int getValue() {
            return _note;
        }

        public float getNormValue() {
            return AxiomRawData.calcNormValue(_note);
        }

        public int getNote() {
            return _note;
        }

        public void setNote(int _note) {
            this._note = _note;
        }

        public int getVelocity() {
            return _velocity;
        }

        public float getNormVelocity() {
            return AxiomRawData.calcNormValue(_velocity);
        }

        public void setVelocity(int _velocity) {
            this._velocity = _velocity;
        }

        public int getChannelPressure() {
            return _channelPressure;
        }

        public float getNormChannelPressure() {
            return AxiomRawData.calcNormValue(_channelPressure);
        }

        public void setChannelPressure(int pressure) {
            _channelPressure = pressure;
        }

        public Key clone() {
            try {
                return (Key) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class: " + this.getClass());
            sb.append("\n");
            sb.append("note: " + _note);
            sb.append("\n");
            sb.append("velocity: " + _velocity);
            sb.append("\n");
            sb.append("channelPressure: " + _channelPressure);
            sb.append("\n");
            return sb.toString();
        }
    }

    public static float calcNormValue(int value) {
        return value / 127f;
    }

    public static class Controller implements Cloneable, AxiomController {

        private int _number;

        private int _value;

        public Controller(int value) {
            _value = value;
        }

        public int getValue() {
            return _value;
        }

        public float getNormValue() {
            return AxiomRawData.calcNormValue(_value);
        }

        public Controller clone() {
            try {
                return (Controller) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        public synchronized int getNumber() {
            return _number;
        }

        public synchronized void setNumber(int number) {
            _number = number;
        }
    }

    public synchronized AxiomController getCurrentController() {
        return _currentController;
    }
}
