package Control;

import java.io.*;
import jd2xx.JD2XX;

/**
 * @author Administrator
 *
 */
public class LP5DSMSerialTransmitter implements ISink {

    private int _serialAdapterNum = 0;

    private int _frameDelay = 8;

    private SerialIOThread _runningThread = null;

    private Channel[] _channels = null;

    public class Channel {

        public int ChannelNumber = 0x00;

        public int MinChannelValue = 0x0000;

        public int MaxChannelValue = 0x0000;

        public int CurrentChannelValue = 0x0000;

        public Channel(int channelNumber, int minChannelValue, int maxChannelValue, double defaultValue) {
            ChannelNumber = channelNumber;
            MinChannelValue = minChannelValue;
            MaxChannelValue = maxChannelValue;
            setChannelValue(defaultValue);
        }

        public void setChannelValue(double value) {
            int channelVal = (int) (((value + 0.5) * (MaxChannelValue - MinChannelValue)) + MinChannelValue);
            CurrentChannelValue = Math.max(Math.min(channelVal, MaxChannelValue), MinChannelValue);
        }

        public byte[] getBytes() {
            byte[] channelVal = new byte[2];
            channelVal[0] = (byte) (((ChannelNumber & 0x0F) << 2) + ((CurrentChannelValue & 0x03FF) >> 8));
            channelVal[1] = (byte) (CurrentChannelValue & 0xFF);
            return channelVal;
        }
    }

    private class SerialIOThread extends Thread {

        private boolean isTransmitting = false;

        private JD2XX serialOut = null;

        public SerialIOThread() {
        }

        public void run() {
            enableTransmit();
            while (isTransmitting) {
                try {
                    transmit();
                } catch (Exception e) {
                }
            }
            disableTransmit();
        }

        private void transmit() throws Exception {
            Thread.sleep(_frameDelay);
            byte[] packet = new byte[_channels.length * 2 + 2];
            packet[0] = 0;
            packet[1] = 0;
            for (int i = 0; i < _channels.length; i++) {
                byte[] channelBytes = _channels[i].getBytes();
                packet[i * 2 + 2 + 0] = channelBytes[0];
                packet[i * 2 + 2 + 1] = channelBytes[1];
            }
            serialOut.write(packet);
        }

        private void enableTransmit() {
            try {
                serialOut = new JD2XX();
                serialOut.open(_serialAdapterNum);
                serialOut.setDivisor(0x18);
                serialOut.setDataCharacteristics(8, JD2XX.STOP_BITS_1, JD2XX.PARITY_NONE);
                serialOut.setFlowControl(JD2XX.FLOW_NONE, 0, 0);
                serialOut.setTimeouts(50, 50);
            } catch (IOException e) {
                e.printStackTrace();
            }
            isTransmitting = true;
        }

        private void disableTransmit() {
            try {
                serialOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void stopTransmit() {
            isTransmitting = false;
        }

        public boolean isTransmitEnabled() {
            return isTransmitting;
        }
    }

    /**
	 * Default constructor, uses first available FT232 device
	 */
    public LP5DSMSerialTransmitter() {
        this(0);
    }

    /**
	 * Specifies the identifier of the FT232 serial device to use
	 * 
	 * @param serialAdapterNum The integer ID of the FT232 USB->Serial adapter to use
	 */
    public LP5DSMSerialTransmitter(int serialAdapterNum) {
        Channel[] channels = new Channel[6];
        channels[0] = new Channel(0, 0x0056, 0x0333, -0.5);
        channels[1] = new Channel(1, 0x0455, 0x07A9, 0);
        channels[2] = new Channel(2, 0x0877, 0x0BA0, 0);
        channels[3] = new Channel(3, 0x0C56, 0x0FAA, 0);
        channels[4] = new Channel(4, 0x1056, 0x13AA, 0);
        channels[5] = new Channel(5, 0x152A, 0x162A, 0.5);
        _serialAdapterNum = serialAdapterNum;
        _channels = channels;
    }

    public void start() {
        if (_runningThread == null) {
            _runningThread = new SerialIOThread();
        } else if (_runningThread.isTransmitting == false) {
            _runningThread = new SerialIOThread();
        }
        _runningThread.start();
    }

    public void stop() {
        if (_runningThread != null) {
            if (_runningThread.isTransmitEnabled()) {
                _runningThread.stopTransmit();
                try {
                    _runningThread.join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setPosition(int channel, double position) {
        if ((channel >= 0) && (channel < _channels.length)) {
            _channels[channel].setChannelValue(position);
        }
    }

    public void setChannelRange(int channel, int minVal, int maxVal) {
        minVal = Math.max(0, Math.min(minVal, 0x3FF));
        maxVal = Math.max(0, Math.min(maxVal, 0x3FF));
        if ((channel >= 0) && (channel < _channels.length)) {
            Channel chan = _channels[channel];
            chan.MinChannelValue = (channel * 0x400) + minVal;
            chan.MaxChannelValue = (channel * 0x400) + maxVal;
        }
    }

    public int getChannelRangeMin(int channel) {
        if ((channel >= 0) && (channel < _channels.length)) {
            Channel chan = _channels[channel];
            return (chan.MinChannelValue - (channel * 0x400));
        } else {
            return 0x00;
        }
    }

    public int getChannelRangeMax(int channel) {
        if ((channel >= 0) && (channel < _channels.length)) {
            Channel chan = _channels[channel];
            return (chan.MaxChannelValue - (channel * 0x400));
        } else {
            return 0x3FF;
        }
    }

    public void setFrameDelay(int frameDelay) {
        _frameDelay = frameDelay;
    }

    public int getFrameDelay() {
        return _frameDelay;
    }

    public void setSerialAdapterNum(int serialAdapterNum) {
        _serialAdapterNum = serialAdapterNum;
    }

    public int getSerialAdapterNum() {
        return _serialAdapterNum;
    }

    public static String[] getAvailableSerialAdapters() {
        String[] adapters = new String[] {};
        try {
            JD2XX jd = new JD2XX();
            Object[] serialNums = jd.listDevicesBySerialNumber();
            Object[] descriptions = jd.listDevicesByDescription();
            adapters = new String[serialNums.length];
            for (int i = 0; i < serialNums.length; ++i) {
                adapters[i] = serialNums[i] + ":" + descriptions[i];
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return adapters;
    }
}
