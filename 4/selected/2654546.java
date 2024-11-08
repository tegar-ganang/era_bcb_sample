package ioio.lib.impl;

import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.SpiMaster;
import ioio.lib.api.TwiMaster.Rate;
import ioio.lib.api.Uart;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import android.util.Log;

public class IOIOProtocol {

    static final int HARD_RESET = 0x00;

    static final int ESTABLISH_CONNECTION = 0x00;

    static final int SOFT_RESET = 0x01;

    static final int CHECK_INTERFACE = 0x02;

    static final int CHECK_INTERFACE_RESPONSE = 0x02;

    static final int SET_PIN_DIGITAL_OUT = 0x03;

    static final int SET_DIGITAL_OUT_LEVEL = 0x04;

    static final int REPORT_DIGITAL_IN_STATUS = 0x04;

    static final int SET_PIN_DIGITAL_IN = 0x05;

    static final int REPORT_PERIODIC_DIGITAL_IN_STATUS = 0x05;

    static final int SET_CHANGE_NOTIFY = 0x06;

    static final int REGISTER_PERIODIC_DIGITAL_SAMPLING = 0x07;

    static final int SET_PIN_PWM = 0x08;

    static final int SET_PWM_DUTY_CYCLE = 0x09;

    static final int SET_PWM_PERIOD = 0x0A;

    static final int SET_PIN_ANALOG_IN = 0x0B;

    static final int REPORT_ANALOG_IN_STATUS = 0x0B;

    static final int SET_ANALOG_IN_SAMPLING = 0x0C;

    static final int REPORT_ANALOG_IN_FORMAT = 0x0C;

    static final int UART_CONFIG = 0x0D;

    static final int UART_STATUS = 0x0D;

    static final int UART_DATA = 0x0E;

    static final int SET_PIN_UART = 0x0F;

    static final int UART_REPORT_TX_STATUS = 0x0F;

    static final int SPI_CONFIGURE_MASTER = 0x10;

    static final int SPI_STATUS = 0x10;

    static final int SPI_MASTER_REQUEST = 0x11;

    static final int SPI_DATA = 0x11;

    static final int SET_PIN_SPI = 0x12;

    static final int SPI_REPORT_TX_STATUS = 0x12;

    static final int I2C_CONFIGURE_MASTER = 0x13;

    static final int I2C_STATUS = 0x13;

    static final int I2C_WRITE_READ = 0x14;

    static final int I2C_RESULT = 0x14;

    static final int I2C_REPORT_TX_STATUS = 0x15;

    static final int ICSP_SIX = 0x16;

    static final int ICSP_REPORT_RX_STATUS = 0x16;

    static final int ICSP_REGOUT = 0x17;

    static final int ICSP_RESULT = 0x17;

    static final int ICSP_PROG_ENTER = 0x18;

    static final int ICSP_PROG_EXIT = 0x19;

    static final int ICSP_CONFIG = 0x1A;

    static final int INCAP_CONFIGURE = 0x1B;

    static final int INCAP_STATUS = 0x1B;

    static final int SET_PIN_INCAP = 0x1C;

    static final int INCAP_REPORT = 0x1C;

    static final int[] SCALE_DIV = new int[] { 0x1F, 0x1E, 0x1D, 0x1C, 0x1B, 0x1A, 0x17, 0x16, 0x15, 0x14, 0x13, 0x12, 0x0F, 0x0E, 0x0D, 0x0C, 0x0B, 0x0A, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01 };

    enum PwmScale {

        SCALE_1X(1, 0), SCALE_8X(8, 3), SCALE_64X(64, 2), SCALE_256X(256, 1);

        public final int scale;

        private final int encoding;

        PwmScale(int scale, int encoding) {
            this.scale = scale;
            this.encoding = encoding;
        }
    }

    private byte[] buf_ = new byte[128];

    private int pos_ = 0;

    private void writeByte(int b) {
        assert (b >= 0 && b < 256);
        buf_[pos_++] = (byte) b;
    }

    private void flush() throws IOException {
        try {
            out_.write(buf_, 0, pos_);
        } finally {
            pos_ = 0;
        }
    }

    private void writeTwoBytes(int i) throws IOException {
        writeByte(i & 0xFF);
        writeByte(i >> 8);
    }

    private void writeThreeBytes(int i) throws IOException {
        writeByte(i & 0xFF);
        writeByte((i >> 8) & 0xFF);
        writeByte((i >> 16) & 0xFF);
    }

    public synchronized void hardReset() throws IOException {
        writeByte(HARD_RESET);
        writeByte('I');
        writeByte('O');
        writeByte('I');
        writeByte('O');
        flush();
    }

    public synchronized void softReset() throws IOException {
        writeByte(SOFT_RESET);
        flush();
    }

    public synchronized void checkInterface(byte[] interfaceId) throws IOException {
        if (interfaceId.length != 8) {
            throw new IllegalArgumentException("interface ID must be exactly 8 bytes long");
        }
        writeByte(CHECK_INTERFACE);
        for (int i = 0; i < 8; ++i) {
            writeByte(interfaceId[i]);
        }
        flush();
    }

    public synchronized void setDigitalOutLevel(int pin, boolean level) throws IOException {
        writeByte(SET_DIGITAL_OUT_LEVEL);
        writeByte(pin << 2 | (level ? 1 : 0));
        flush();
    }

    public synchronized void setPinPwm(int pin, int pwmNum, boolean enable) throws IOException {
        writeByte(SET_PIN_PWM);
        writeByte(pin & 0x3F);
        writeByte((enable ? 0x80 : 0x00) | (pwmNum & 0x0F));
        flush();
    }

    public synchronized void setPwmDutyCycle(int pwmNum, int dutyCycle, int fraction) throws IOException {
        writeByte(SET_PWM_DUTY_CYCLE);
        writeByte(pwmNum << 2 | fraction);
        writeTwoBytes(dutyCycle);
        flush();
    }

    public synchronized void setPwmPeriod(int pwmNum, int period, PwmScale scale) throws IOException {
        writeByte(SET_PWM_PERIOD);
        writeByte(((scale.encoding & 0x02) << 6) | (pwmNum << 1) | (scale.encoding & 0x01));
        writeTwoBytes(period);
        flush();
    }

    public synchronized void setPinIncap(int pin, int incapNum, boolean enable) throws IOException {
        writeByte(SET_PIN_INCAP);
        writeByte(pin);
        writeByte(incapNum | (enable ? 0x80 : 0x00));
        flush();
    }

    public synchronized void incapClose(int incapNum) throws IOException {
        writeByte(INCAP_CONFIGURE);
        writeByte(incapNum);
        writeByte(0x00);
        flush();
    }

    public synchronized void incapConfigure(int incapNum, boolean double_prec, int mode, int clock) throws IOException {
        writeByte(INCAP_CONFIGURE);
        writeByte(incapNum);
        writeByte((double_prec ? 0x80 : 0x00) | (mode << 3) | clock);
        flush();
    }

    public synchronized void i2cWriteRead(int i2cNum, boolean tenBitAddr, int address, int writeSize, int readSize, byte[] writeData) throws IOException {
        writeByte(I2C_WRITE_READ);
        writeByte(((address >> 8) << 6) | (tenBitAddr ? 0x20 : 0x00) | i2cNum);
        writeByte(address & 0xFF);
        writeByte(writeSize);
        writeByte(readSize);
        for (int i = 0; i < writeSize; ++i) {
            writeByte(((int) writeData[i]) & 0xFF);
        }
        flush();
    }

    public synchronized void setPinDigitalOut(int pin, boolean value, DigitalOutput.Spec.Mode mode) throws IOException {
        writeByte(SET_PIN_DIGITAL_OUT);
        writeByte((pin << 2) | (mode == DigitalOutput.Spec.Mode.OPEN_DRAIN ? 0x01 : 0x00) | (value ? 0x02 : 0x00));
        flush();
    }

    public synchronized void setPinDigitalIn(int pin, DigitalInput.Spec.Mode mode) throws IOException {
        int pull = 0;
        if (mode == DigitalInput.Spec.Mode.PULL_UP) {
            pull = 1;
        } else if (mode == DigitalInput.Spec.Mode.PULL_DOWN) {
            pull = 2;
        }
        writeByte(SET_PIN_DIGITAL_IN);
        writeByte((pin << 2) | pull);
        flush();
    }

    public synchronized void setChangeNotify(int pin, boolean changeNotify) throws IOException {
        writeByte(SET_CHANGE_NOTIFY);
        writeByte((pin << 2) | (changeNotify ? 0x01 : 0x00));
        flush();
    }

    public synchronized void registerPeriodicDigitalSampling(int pin, int freqScale) throws IOException {
    }

    public synchronized void setPinAnalogIn(int pin) throws IOException {
        writeByte(SET_PIN_ANALOG_IN);
        writeByte(pin);
        flush();
    }

    public synchronized void setAnalogInSampling(int pin, boolean enable) throws IOException {
        writeByte(SET_ANALOG_IN_SAMPLING);
        writeByte((enable ? 0x80 : 0x00) | (pin & 0x3F));
        flush();
    }

    public synchronized void uartData(int uartNum, int numBytes, byte data[]) throws IOException {
        if (numBytes > 64) {
            throw new IllegalArgumentException("A maximum of 64 bytes can be sent in one uartData message. Got: " + numBytes);
        }
        writeByte(UART_DATA);
        writeByte((numBytes - 1) | uartNum << 6);
        for (int i = 0; i < numBytes; ++i) {
            writeByte(((int) data[i]) & 0xFF);
        }
        flush();
    }

    public synchronized void uartConfigure(int uartNum, int rate, boolean speed4x, Uart.StopBits stopbits, Uart.Parity parity) throws IOException {
        int parbits = parity == Uart.Parity.EVEN ? 1 : (parity == Uart.Parity.ODD ? 2 : 0);
        writeByte(UART_CONFIG);
        writeByte((uartNum << 6) | (speed4x ? 0x08 : 0x00) | (stopbits == Uart.StopBits.TWO ? 0x04 : 0x00) | parbits);
        writeTwoBytes(rate);
        flush();
    }

    public synchronized void uartClose(int uartNum) throws IOException {
        writeByte(UART_CONFIG);
        writeByte(uartNum << 6);
        writeTwoBytes(0);
        flush();
    }

    public synchronized void setPinUart(int pin, int uartNum, boolean tx, boolean enable) throws IOException {
        writeByte(SET_PIN_UART);
        writeByte(pin);
        writeByte((enable ? 0x80 : 0x00) | (tx ? 0x40 : 0x00) | uartNum);
        flush();
    }

    public synchronized void spiConfigureMaster(int spiNum, SpiMaster.Config config) throws IOException {
        writeByte(SPI_CONFIGURE_MASTER);
        writeByte((spiNum << 5) | SCALE_DIV[config.rate.ordinal()]);
        writeByte((config.sampleOnTrailing ? 0x00 : 0x02) | (config.invertClk ? 0x01 : 0x00));
        flush();
    }

    public synchronized void spiClose(int spiNum) throws IOException {
        writeByte(SPI_CONFIGURE_MASTER);
        writeByte(spiNum << 5);
        writeByte(0x00);
        flush();
    }

    public synchronized void setPinSpi(int pin, int mode, boolean enable, int spiNum) throws IOException {
        writeByte(SET_PIN_SPI);
        writeByte(pin);
        writeByte((1 << 4) | (mode << 2) | spiNum);
        flush();
    }

    public synchronized void spiMasterRequest(int spiNum, int ssPin, byte data[], int dataBytes, int totalBytes, int responseBytes) throws IOException {
        final boolean dataNeqTotal = (dataBytes != totalBytes);
        final boolean resNeqTotal = (responseBytes != totalBytes);
        writeByte(SPI_MASTER_REQUEST);
        writeByte((spiNum << 6) | ssPin);
        writeByte((dataNeqTotal ? 0x80 : 0x00) | (resNeqTotal ? 0x40 : 0x00) | totalBytes - 1);
        if (dataNeqTotal) {
            writeByte(dataBytes);
        }
        if (resNeqTotal) {
            writeByte(responseBytes);
        }
        for (int i = 0; i < dataBytes; ++i) {
            writeByte(((int) data[i]) & 0xFF);
        }
        flush();
    }

    public synchronized void i2cConfigureMaster(int i2cNum, Rate rate, boolean smbusLevels) throws IOException {
        int rateBits = (rate == Rate.RATE_1MHz ? 3 : (rate == Rate.RATE_400KHz ? 2 : 1));
        writeByte(I2C_CONFIGURE_MASTER);
        writeByte((smbusLevels ? 0x80 : 0) | (rateBits << 5) | i2cNum);
        flush();
    }

    public synchronized void i2cClose(int i2cNum) throws IOException {
        writeByte(I2C_CONFIGURE_MASTER);
        writeByte(i2cNum);
        flush();
    }

    public void icspOpen() throws IOException {
        writeByte(ICSP_CONFIG);
        writeByte(0x01);
        flush();
    }

    public void icspClose() throws IOException {
        writeByte(ICSP_CONFIG);
        writeByte(0x00);
        flush();
    }

    public void icspEnter() throws IOException {
        writeByte(ICSP_PROG_ENTER);
        flush();
    }

    public void icspExit() throws IOException {
        writeByte(ICSP_PROG_EXIT);
        flush();
    }

    public void icspSix(int instruction) throws IOException {
        writeByte(ICSP_SIX);
        writeThreeBytes(instruction);
        flush();
    }

    public void icspRegout() throws IOException {
        writeByte(ICSP_REGOUT);
        flush();
    }

    public interface IncomingHandler {

        public void handleEstablishConnection(byte[] hardwareId, byte[] bootloaderId, byte[] firmwareId);

        public void handleConnectionLost();

        public void handleSoftReset();

        public void handleCheckInterfaceResponse(boolean supported);

        public void handleSetChangeNotify(int pin, boolean changeNotify);

        public void handleReportDigitalInStatus(int pin, boolean level);

        public void handleRegisterPeriodicDigitalSampling(int pin, int freqScale);

        public void handleReportPeriodicDigitalInStatus(int frameNum, boolean values[]);

        public void handleAnalogPinStatus(int pin, boolean open);

        public void handleReportAnalogInStatus(int pins[], int values[]);

        public void handleUartOpen(int uartNum);

        public void handleUartClose(int uartNum);

        public void handleUartData(int uartNum, int numBytes, byte data[]);

        public void handleUartReportTxStatus(int uartNum, int bytesRemaining);

        public void handleSpiOpen(int spiNum);

        public void handleSpiClose(int spiNum);

        public void handleSpiData(int spiNum, int ssPin, byte data[], int dataBytes);

        public void handleSpiReportTxStatus(int spiNum, int bytesRemaining);

        public void handleI2cOpen(int i2cNum);

        public void handleI2cClose(int i2cNum);

        public void handleI2cResult(int i2cNum, int size, byte[] data);

        public void handleI2cReportTxStatus(int spiNum, int bytesRemaining);

        void handleIcspOpen();

        void handleIcspClose();

        void handleIcspReportRxStatus(int bytesRemaining);

        void handleIcspResult(int size, byte[] data);

        public void handleIncapReport(int incapNum, int size, byte[] data);

        public void handleIncapClose(int incapNum);

        public void handleIncapOpen(int incapNum);
    }

    class IncomingThread extends Thread {

        private int[] analogFramePins_ = new int[0];

        private Set<Integer> removedPins_ = new HashSet<Integer>(Constants.NUM_ANALOG_PINS);

        private Set<Integer> addedPins_ = new HashSet<Integer>(Constants.NUM_ANALOG_PINS);

        private void findDelta(int[] newPins) {
            removedPins_.clear();
            addedPins_.clear();
            for (int i : analogFramePins_) {
                removedPins_.add(i);
            }
            for (int i : newPins) {
                addedPins_.add(i);
            }
            for (Iterator<Integer> it = removedPins_.iterator(); it.hasNext(); ) {
                Integer current = it.next();
                if (addedPins_.contains(current)) {
                    it.remove();
                    addedPins_.remove(current);
                }
            }
        }

        private int readByte() throws IOException {
            try {
                int b = in_.read();
                if (b == -1) throw new IOException("Unexpected stream closure");
                return b;
            } catch (IOException e) {
                Log.i("IOIOProtocol", "IOIO disconnected");
                throw e;
            }
        }

        private void readBytes(int size, byte[] buffer) throws IOException {
            for (int i = 0; i < size; ++i) {
                buffer[i] = (byte) readByte();
            }
        }

        @Override
        public void run() {
            super.run();
            setPriority(MAX_PRIORITY);
            int arg1;
            int arg2;
            int numPins;
            int size;
            byte[] data = new byte[256];
            try {
                while (true) {
                    switch(arg1 = readByte()) {
                        case ESTABLISH_CONNECTION:
                            if (readByte() != 'I' || readByte() != 'O' || readByte() != 'I' || readByte() != 'O') {
                                throw new IOException("Bad establish connection magic");
                            }
                            byte[] hardwareId = new byte[8];
                            byte[] bootloaderId = new byte[8];
                            byte[] firmwareId = new byte[8];
                            readBytes(8, hardwareId);
                            readBytes(8, bootloaderId);
                            readBytes(8, firmwareId);
                            handler_.handleEstablishConnection(hardwareId, bootloaderId, firmwareId);
                            break;
                        case SOFT_RESET:
                            handler_.handleSoftReset();
                            break;
                        case REPORT_DIGITAL_IN_STATUS:
                            arg1 = readByte();
                            handler_.handleReportDigitalInStatus(arg1 >> 2, (arg1 & 0x01) == 1);
                            break;
                        case SET_CHANGE_NOTIFY:
                            arg1 = readByte();
                            handler_.handleSetChangeNotify(arg1 >> 2, (arg1 & 0x01) == 1);
                            break;
                        case REGISTER_PERIODIC_DIGITAL_SAMPLING:
                            break;
                        case REPORT_PERIODIC_DIGITAL_IN_STATUS:
                            break;
                        case REPORT_ANALOG_IN_FORMAT:
                            numPins = readByte();
                            int[] newFormat = new int[numPins];
                            for (int i = 0; i < numPins; ++i) {
                                newFormat[i] = readByte();
                            }
                            findDelta(newFormat);
                            for (Integer i : removedPins_) {
                                handler_.handleAnalogPinStatus(i, false);
                            }
                            for (Integer i : addedPins_) {
                                handler_.handleAnalogPinStatus(i, true);
                            }
                            analogFramePins_ = newFormat;
                            break;
                        case REPORT_ANALOG_IN_STATUS:
                            numPins = analogFramePins_.length;
                            int header = 0;
                            int[] values = new int[numPins];
                            for (int i = 0; i < numPins; ++i) {
                                if (i % 4 == 0) {
                                    header = readByte();
                                }
                                values[i] = (readByte() << 2) | (header & 0x03);
                                header >>= 2;
                            }
                            handler_.handleReportAnalogInStatus(analogFramePins_, values);
                            break;
                        case UART_REPORT_TX_STATUS:
                            arg1 = readByte();
                            arg2 = readByte();
                            handler_.handleUartReportTxStatus(arg1 & 0x03, (arg1 >> 2) | (arg2 << 6));
                            break;
                        case UART_DATA:
                            arg1 = readByte();
                            for (int i = 0; i < (arg1 & 0x3F) + 1; ++i) {
                                data[i] = (byte) readByte();
                            }
                            handler_.handleUartData(arg1 >> 6, (arg1 & 0x3F) + 1, data);
                            break;
                        case UART_STATUS:
                            arg1 = readByte();
                            if ((arg1 & 0x80) != 0) {
                                handler_.handleUartOpen(arg1 & 0x03);
                            } else {
                                handler_.handleUartClose(arg1 & 0x03);
                            }
                            break;
                        case SPI_DATA:
                            arg1 = readByte();
                            arg2 = readByte();
                            for (int i = 0; i < (arg1 & 0x3F) + 1; ++i) {
                                data[i] = (byte) readByte();
                            }
                            handler_.handleSpiData(arg1 >> 6, arg2 & 0x3F, data, (arg1 & 0x3F) + 1);
                            break;
                        case SPI_REPORT_TX_STATUS:
                            arg1 = readByte();
                            arg2 = readByte();
                            handler_.handleSpiReportTxStatus(arg1 & 0x03, (arg1 >> 2) | (arg2 << 6));
                            break;
                        case SPI_STATUS:
                            arg1 = readByte();
                            if ((arg1 & 0x80) != 0) {
                                handler_.handleSpiOpen(arg1 & 0x03);
                            } else {
                                handler_.handleSpiClose(arg1 & 0x03);
                            }
                            break;
                        case I2C_STATUS:
                            arg1 = readByte();
                            if ((arg1 & 0x80) != 0) {
                                handler_.handleI2cOpen(arg1 & 0x03);
                            } else {
                                handler_.handleI2cClose(arg1 & 0x03);
                            }
                            break;
                        case I2C_RESULT:
                            arg1 = readByte();
                            arg2 = readByte();
                            if (arg2 != 0xFF) {
                                for (int i = 0; i < arg2; ++i) {
                                    data[i] = (byte) readByte();
                                }
                            }
                            handler_.handleI2cResult(arg1 & 0x03, arg2, data);
                            break;
                        case I2C_REPORT_TX_STATUS:
                            arg1 = readByte();
                            arg2 = readByte();
                            handler_.handleI2cReportTxStatus(arg1 & 0x03, (arg1 >> 2) | (arg2 << 6));
                            break;
                        case CHECK_INTERFACE_RESPONSE:
                            arg1 = readByte();
                            handler_.handleCheckInterfaceResponse((arg1 & 0x01) == 1);
                            break;
                        case ICSP_REPORT_RX_STATUS:
                            arg1 = readByte();
                            arg2 = readByte();
                            handler_.handleIcspReportRxStatus(arg1 | (arg2 << 8));
                            break;
                        case ICSP_RESULT:
                            data[0] = (byte) readByte();
                            data[1] = (byte) readByte();
                            handler_.handleIcspResult(2, data);
                            break;
                        case ICSP_CONFIG:
                            arg1 = readByte();
                            if ((arg1 & 0x01) == 1) {
                                handler_.handleIcspOpen();
                            } else {
                                handler_.handleIcspClose();
                            }
                            break;
                        case INCAP_STATUS:
                            arg1 = readByte();
                            if ((arg1 & 0x80) != 0) {
                                handler_.handleIncapOpen(arg1 & 0x0F);
                            } else {
                                handler_.handleIncapClose(arg1 & 0x0F);
                            }
                            break;
                        case INCAP_REPORT:
                            arg1 = readByte();
                            size = arg1 >> 6;
                            if (size == 0) {
                                size = 4;
                            }
                            readBytes(size, data);
                            handler_.handleIncapReport(arg1 & 0x0F, size, data);
                            break;
                        default:
                            in_.close();
                            IOException e = new IOException("Received unexpected command: 0x" + Integer.toHexString(arg1));
                            Log.e("IOIOProtocol", "Protocol error", e);
                            throw e;
                    }
                }
            } catch (IOException e) {
                handler_.handleConnectionLost();
            }
        }
    }

    private final InputStream in_;

    private final OutputStream out_;

    private final IncomingHandler handler_;

    private final IncomingThread thread_ = new IncomingThread();

    public IOIOProtocol(InputStream in, OutputStream out, IncomingHandler handler) {
        in_ = in;
        out_ = out;
        handler_ = handler;
        thread_.start();
    }
}
