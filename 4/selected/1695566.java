package com.unicont.cardio.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.IllegalFormatCodePointException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.logging.Logger;
import javax.comm.SerialPort;
import javax.comm.SerialPortEvent;
import javax.comm.SerialPortEventListener;
import javax.comm.UnsupportedCommOperationException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import com.unicont.cardio.model.Metadata;

public class SerialCardioPagerConduit extends GenericConduit {

    public static final int TWO_LEADS_EXPECTED_SIZE = 0xfffe;

    private SerialPort serialPort;

    private OutputStream outputStream;

    private int baudRate = 115200;

    private long handshakeTime = 15000;

    public static final int DELAY = 2000;

    private Logger logger = Logger.getLogger("cardio.terminal.pager");

    public static final String PING_CMD = "*";

    public static final String POWEROFF_CMD = "q";

    public static final String GET_I_II_CMD = "g";

    public static final String GET_V1_V2_CMD = "G";

    public static final String GET_I_II_FILTER_CMD = "f";

    public static final String GET_V1_V2_FILTER_CMD = "F";

    public static final String GET_RTC_CMD = "t";

    public static final String GET_TWOLEADS_FROM_FLASH_CMD = "L";

    public static final String GET_ECGTIME_FROM_FLASH_CMD = "lt";

    public static final String GET_SYSINFO_CMD = "r";

    public static final String GET_ECG_CAPACITY_CMD = "k";

    public static final String SET_FLASH_ERASE_CMD = "e";

    public static final String GET_ID_CMD = "n";

    public static final String wkDays[] = { "Mon", "Tue", "Wnd", "Ths", "Fri", "Sat", "Sun" };

    public static final String PAGER_VERSION_SAMPLE = "CardioJ";

    public static final int PAGER_VERSION_LENGTH = 37;

    public static final int I_II = 0;

    public static final int V1_2 = 1;

    public static final int V3_4 = 2;

    public static final int V5_6 = 3;

    private InputStream inputStream;

    private CardioPagerListener cardioPagerListener;

    private List<PagerStateListener> stateListeners = new LinkedList<PagerStateListener>();

    private long lastSync = -1;

    private boolean checkLength = false;

    private PagerStates currentState = PagerStates.OFFLINE;

    SerialCardioPagerConduit(SerialPort serialPort) {
        super();
        this.serialPort = serialPort;
    }

    void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    int getBaudRate() {
        return baudRate;
    }

    public void setHandshakeTime(long handshakeTime) {
        this.handshakeTime = handshakeTime;
    }

    public long getHandshakeTime() {
        return handshakeTime;
    }

    void init() throws IOException, SerialCardioPagerConduitException {
        try {
            outputStream = serialPort.getOutputStream();
            inputStream = serialPort.getInputStream();
            serialPort.setSerialPortParams(getBaudRate(), SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            serialPort.notifyOnDataAvailable(true);
            serialPort.notifyOnBreakInterrupt(true);
        } catch (UnsupportedCommOperationException e1) {
            throw new SerialCardioPagerConduitException(e1);
        }
        serialPort.setInputBufferSize(16384);
        cardioPagerListener = new CardioPagerListener();
        try {
            serialPort.addEventListener(cardioPagerListener);
            cardioPagerListener.setBytesToExpect(PAGER_VERSION_LENGTH);
            receiveFromBuffer(PAGER_VERSION_LENGTH, getHandshakeTime());
            ByteBuffer bb = cardioPagerListener.getReceivedData();
            String pagerIdent = buffer2string(bb);
            if (!(pagerIdent.indexOf("Car") >= 0)) throw new SerialCardioPagerConduitException("Pager was not detected!");
            logger.fine("Pager found [" + pagerIdent + "]");
            breakOperationAndFlushReceiverBuffer();
            currentState = PagerStates.ONLINE;
        } catch (TooManyListenersException e) {
            throw new SerialCardioPagerConduitException(e);
        }
    }

    public void addPagerStateListener(PagerStateListener aListener) {
        synchronized (stateListeners) {
            stateListeners.add(aListener);
        }
    }

    public void removePagerStateListener(PagerStateListener aListener) {
        synchronized (stateListeners) {
            stateListeners.remove(aListener);
        }
    }

    protected void firePagerStateChanged(PagerStates aState) {
        currentState = aState;
        for (Iterator stateListener = stateListeners.iterator(); stateListener.hasNext(); ) {
            PagerStateListener l = (PagerStateListener) stateListener.next();
            l.onChangedState(aState);
        }
    }

    void close() throws IOException {
        if (outputStream != null) outputStream.close();
        if (inputStream != null) inputStream.close();
    }

    public boolean isCheckLength() {
        return checkLength;
    }

    public void setCheckLength(boolean checkLength) {
        this.checkLength = checkLength;
    }

    void ping() throws IOException {
        sendToPager(PING_CMD);
    }

    void powerOff() throws IOException {
        try {
            if (serialPort != null) {
                serialPort.notifyOnDataAvailable(false);
                if (currentState == PagerStates.ONLINE) {
                    sendToPager(POWEROFF_CMD);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    void getFromPager(String aCmd, int expected) throws SerialCardioPagerConduitException, IOException {
        getFromPager(aCmd, -1, expected);
    }

    void getFromPager(String aCmd, int timeout, int expected, byte[] term) throws SerialCardioPagerConduitException, IOException {
        breakOperationAndFlushReceiverBuffer();
        SerialCardioTerminalImpl.writeToLog("expect to receive:" + expected + " timeout:" + timeout + " terminator:" + term.length + " bytes");
        cardioPagerListener.setBytesToExpect(expected, term);
        SerialCardioTerminalImpl.writeToLog("Sending " + aCmd);
        if (timeout > 0) sendToPager(aCmd, timeout); else sendToPager(aCmd);
        receiveFromBuffer(expected, DELAY);
    }

    void getFromPager(String aCmd, int timeout, int expected) throws SerialCardioPagerConduitException, IOException {
        breakOperationAndFlushReceiverBuffer();
        SerialCardioTerminalImpl.writeToLog("expect to receive:" + expected + " timeout:" + timeout);
        cardioPagerListener.setBytesToExpect(expected);
        SerialCardioTerminalImpl.writeToLog("Sending " + aCmd);
        if (timeout > 0) sendToPager(aCmd, timeout); else sendToPager(aCmd);
        receiveFromBuffer(expected, DELAY);
    }

    void receiveFromBuffer(int expected, long receiveTime) throws SerialCardioPagerConduitException, IOException {
        SerialCardioTerminalImpl.writeToLog("Waiting for " + receiveTime + " millis...");
        while (!cardioPagerListener.isCompleted()) {
            synchronized (this) {
                try {
                    wait(receiveTime);
                } catch (InterruptedException e) {
                    SerialCardioTerminalImpl.writeToLog("Received data from device!");
                }
            }
            if (System.currentTimeMillis() - lastSync >= receiveTime) break;
        }
        if (!cardioPagerListener.isCompleted()) cardioPagerListener.setCompleted(true);
        SerialCardioTerminalImpl.writeToLog("Waking up...");
        if (cardioPagerListener.getThrowable() != null) {
            SerialCardioTerminalImpl.writeToLog("Got problem:" + cardioPagerListener.getThrowable().getMessage());
            throw new SerialCardioPagerConduitException(cardioPagerListener.getThrowable());
        }
        if (isCheckLength() && cardioPagerListener.getReceivedBytes() < expected) {
            throw new SerialCardioPagerConduitException(Messages.getSoleArgString("SerialCardioPagerConduit.not.enough.data.retreived", "getFromPager"));
        }
    }

    private void setLastSync(long lastSync) {
        synchronized (this) {
            this.lastSync = lastSync;
        }
    }

    Metadata getSystemInfo() throws SerialCardioPagerConduitException, IOException {
        int pagerId = (int) getPagerId();
        int capacity = getPagerCapacity();
        logger.finest("Metadata: pager capacity:" + capacity);
        String ecgtime = null;
        List<Date> ecgTimeList = new LinkedList<Date>();
        for (int j = 0; j < capacity; j++) {
            try {
                ecgtime = getEcgTime(I_II, j);
                if (ecgtime == null) break;
                ecgTimeList.add(CardioPager.parseTime(ecgtime));
                logger.finest("Metadata: ECG[" + j + "] time: [" + ecgtime + "]");
            } catch (ParseException e) {
                logger.severe(e.getMessage());
            }
        }
        int nEcgFound = 0;
        Metadata m = new Metadata(ecgTimeList.size(), pagerId);
        for (Date date : ecgTimeList) {
            m.setEcgData(nEcgFound++, date.getTime());
        }
        logger.finest("Metadata: there are [" + nEcgFound + "] ECG in pager [" + pagerId + "]");
        m.setLastAccessTimeDate(ecgTimeList.get(nEcgFound - 1).toString());
        return m;
    }

    String getRtc() throws SerialCardioPagerConduitException, IOException {
        String rtc = "no rtc";
        getFromPager(GET_RTC_CMD, 200, 7);
        if (cardioPagerListener.getReceivedBytes() >= 7) {
            ByteBuffer bb = cardioPagerListener.getReceivedData();
            byte[] rtcData = bcd2bin(bb);
            rtc = getValLessThen(rtcData[2], 24, "{0}", "??");
            rtc += getValLessThen(rtcData[1], 60, ":{0}", "??");
            rtc += getValLessThen(rtcData[0], 60, ":{0} / ", "??");
            rtc += getValLessThen(rtcData[3], 32, "{0}(", "??");
            rtc += rtcData[4] < wkDays.length ? wkDays[rtcData[4] > 0 ? rtcData[4] - 1 : rtcData[4]] : "??";
            rtc += ").";
            rtc += getValLessThen(rtcData[5], 13, "{0}.", "??");
            rtc += getValLessThen(rtcData[6], 100, "{0}", "?? ");
        } else {
            return Messages.getSoleArgString("SerialCardioPagerConduit.not.enough.data.retreived", "getRtc");
        }
        return rtc;
    }

    private String buffer2string(ByteBuffer bb) {
        String str = "";
        bb.position(0);
        for (int j = 0; j < bb.limit(); j++) {
            str += (char) bb.get(j);
        }
        return str;
    }

    private String buffer2number(ByteBuffer bb) {
        String str = "";
        bb.position(0);
        for (int j = 0; j < bb.limit(); j++) {
            char ch = (char) bb.get(j);
            if (ch >= '0' && ch <= '9') str += ch;
        }
        return str;
    }

    long getPagerId() throws SerialCardioPagerConduitException, IOException {
        long _id = -1;
        String pagerId = "";
        getFromPager(GET_ID_CMD, 100, 8, new byte[] { '\r', '\n' });
        ByteBuffer bb = cardioPagerListener.getReceivedData();
        pagerId = buffer2number(bb);
        if (!"".equals(pagerId)) {
            try {
                _id = Long.parseLong(pagerId);
            } catch (NumberFormatException e) {
                String msg = MessageFormat.format("PagerId: [{0}] is not numeric.", new Object[] { pagerId });
                logger.severe(msg);
                SerialCardioTerminalImpl.writeToLog(msg);
            }
        }
        return _id;
    }

    private Signal[] getTwoLeadsData(String aCmd, int size) throws SerialCardioPagerConduitException, IOException {
        boolean oldCheckLength = isCheckLength();
        setCheckLength(false);
        getFromPager(aCmd, 200, size);
        ByteBuffer bb = cardioPagerListener.getReceivedData();
        Signal[] signal = new SignalImpl[2];
        signal[0] = new SignalImpl();
        signal[1] = new SignalImpl();
        for (int j = 0; j < cardioPagerListener.getReceivedBytes(); j++) {
            ComData comData = new ComData(bb.get(j));
            if (!comData.isChannelSet()) signal[0].addData(comData); else signal[1].addData(comData);
        }
        setCheckLength(oldCheckLength);
        return signal;
    }

    /**
	 * Gets signal for I and II leads
	 * @return array of two {@link Signal} instances where I lead data at 0 index and IInd at 1 
	 * @throws SerialCardioPagerConduitException
	 * @throws IOException
	 */
    public Signal[] getLeadData_I_II() throws SerialCardioPagerConduitException, IOException {
        return getTwoLeadsData(GET_I_II_CMD, TWO_LEADS_EXPECTED_SIZE);
    }

    public Signal[] getLeadData_V1_V2() throws SerialCardioPagerConduitException, IOException {
        return getTwoLeadsData(GET_V1_V2_CMD, TWO_LEADS_EXPECTED_SIZE);
    }

    public Signal[] getLeadData(int leadPair, int ecgNum) throws SerialCardioPagerConduitException, IOException {
        return getTwoLeadsData(GET_TWOLEADS_FROM_FLASH_CMD + (char) bin2bcd((byte) ecgNum) + (char) bin2bcd((byte) leadPair), TWO_LEADS_EXPECTED_SIZE);
    }

    public int getPagerCapacity() throws SerialCardioPagerConduitException, IOException {
        int capacity = 0;
        getFromPager(GET_ECG_CAPACITY_CMD, 100, 3, new byte[] { '\r', '\n' });
        if (cardioPagerListener.getReceivedBytes() > 0) {
            try {
                capacity = Integer.parseInt(buffer2number(cardioPagerListener.getReceivedData()));
            } catch (NumberFormatException e) {
                SerialCardioTerminalImpl.writeToLog("ERROR: Pager capacity is not a number!");
            }
        }
        return capacity;
    }

    public String getEcgTime(int leadPair, int ecgNum) throws SerialCardioPagerConduitException, IOException {
        String ecgTime = null;
        getFromPager(GET_ECGTIME_FROM_FLASH_CMD + (char) bin2bcd((byte) ecgNum) + (char) bin2bcd((byte) leadPair), 100, 6, new byte[] { 'E', 'm', 'p' });
        if (cardioPagerListener.getTerminatorFound()) return null;
        if (cardioPagerListener.getReceivedBytes() == 6) {
            ByteBuffer bb = cardioPagerListener.getReceivedData();
            byte[] timeData = bcd2bin(bb);
            ecgTime = "" + timeData[2] + ":" + timeData[1] + ":" + timeData[0] + " " + timeData[3] + "." + timeData[4] + "." + timeData[5];
        }
        return ecgTime;
    }

    public void getLeadFilteredData_I_II() throws SerialCardioPagerConduitException, IOException {
        throw new NotImplementedException();
    }

    public void getLeadFilteredData_V1_V2() throws SerialCardioPagerConduitException, IOException {
        throw new NotImplementedException();
    }

    private final String getValLessThen(int val, int lessThen, String frm, String ow) {
        String r = val < lessThen ? "" + val : ow;
        return MessageFormat.format(frm, new Object[] { r });
    }

    private void breakOperationAndFlushReceiverBuffer() throws IOException {
        sendToPager(PING_CMD, 300);
        int c = 0;
        while (inputStream.available() > 0) {
            inputStream.read();
            c++;
        }
        SerialCardioTerminalImpl.writeToLog("Trashed buffer: read [" + c + "] bytes");
    }

    void sendToPager(String aCmd) throws IOException {
        byte[] data = aCmd.getBytes();
        SerialCardioTerminalImpl.writeToLog("Command:" + aCmd + ", bytes to send:" + data.length);
        for (int j = 0; j < aCmd.getBytes().length; j++) {
            outputStream.write(aCmd.getBytes()[j]);
            outputStream.flush();
        }
    }

    void sendToPager(String aCmd, int timeout) throws IOException {
        sendToPager(aCmd);
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            ;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    private byte bin2bcd(byte data) {
        if (data < 100) {
            int _data = ((data / 10) << 4) | (data % 10);
            data = (byte) _data;
        }
        return data;
    }

    private byte bcd2bin(byte data) {
        int _d = (data & 0xF) + ((data & 0xF0) >> 4) * 10;
        return (byte) _d;
    }

    private byte[] bcd2bin(ByteBuffer a) {
        byte[] ra = new byte[a.limit()];
        for (int j = 0; j < a.limit(); j++) {
            byte n = a.get(j);
            ra[j] = bcd2bin(n);
        }
        return ra;
    }

    class SysInfo {

        ByteBuffer bb;

        public SysInfo(ByteBuffer bb) {
            this.bb = bb;
        }

        String getTimeAndDate() {
            bb.position(0);
            String fmt = "{0,number,integer}:{1,number,integer}:{2,number,integer} {3,number,integer}.{4,number,integer}.{5,number,integer}";
            return MessageFormat.format(fmt, new Object[] { getInt(bb, 8), getInt(bb, 4), getInt(bb, 0), getInt(bb, 12), getInt(bb, 16), getInt(bb, 20) });
        }

        Metadata.PagerState getPagerState() {
            Metadata.PagerState ps = null;
            bb.position(0);
            ps = new Metadata().new PagerState(getInt(bb, 24), getInt(bb, 28), getInt(bb, 32));
            return ps;
        }

        /**
		 * @deprecated
		 * @return
		 */
        int getPagerId() {
            bb.position(0);
            return (getInt(bb, 267) << 24) | (getInt(bb, 266) << 16) | (getInt(bb, 265) << 8) | getInt(bb, 264);
        }

        int getNumberOfEkg() {
            bb.position(0);
            int b271 = getInt(bb, 271);
            int b270 = getInt(bb, 270);
            int b269 = getInt(bb, 269);
            int b268 = getInt(bb, 268);
            return (b271 << 24) | (b270 << 16) | (b269 << 8) | b268;
        }
    }

    private final int getInt(ByteBuffer bb, int num) {
        return new Byte(bb.get(num)).intValue();
    }

    class CardioPagerListener implements SerialPortEventListener {

        private int bytesToExpect = 0;

        private int receivedBytes;

        private ByteBuffer buffer;

        private Throwable throwable;

        private boolean completed = false;

        private byte[] terminator = null;

        private boolean terminatorFound = false;

        public Throwable getThrowable() {
            return throwable;
        }

        public int getBytesToExpect() {
            return bytesToExpect;
        }

        public void setBytesToExpect(int bytesToExpect) {
            setBytesToExpect(bytesToExpect, null);
        }

        public void setBytesToExpect(int bytesToExpect, byte[] terminator) {
            this.bytesToExpect = bytesToExpect;
            this.terminator = terminator;
            receivedBytes = 0;
            buffer = null;
            throwable = null;
            completed = false;
            buffer = ByteBuffer.allocate(bytesToExpect);
            terminatorFound = false;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        public int getReceivedBytes() {
            return receivedBytes;
        }

        ByteBuffer getReceivedData() {
            return buffer;
        }

        public void serialEvent(SerialPortEvent event) {
            switch(event.getEventType()) {
                case SerialPortEvent.BI:
                    SerialCardioTerminalImpl.writeToLog("BREAK INTERRUPT!");
                    if (isCompleted()) firePagerStateChanged(PagerStates.OFFLINE);
                case SerialPortEvent.OE:
                case SerialPortEvent.FE:
                case SerialPortEvent.PE:
                case SerialPortEvent.CD:
                case SerialPortEvent.CTS:
                case SerialPortEvent.DSR:
                case SerialPortEvent.RI:
                case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                    break;
                case SerialPortEvent.DATA_AVAILABLE:
                    SerialCardioTerminalImpl.writeToLog("received DATA_AVAILABLE event");
                    if (isCompleted()) return;
                    try {
                        SerialCardioTerminalImpl.writeToLog("Available " + inputStream.available() + " bytes, need to read:" + buffer.limit());
                        int av = inputStream.available();
                        if (buffer.limit() - receivedBytes < av) {
                            av = buffer.limit() - receivedBytes;
                        }
                        byte ar[] = new byte[av];
                        receivedBytes += inputStream.read(ar);
                        buffer.put(ar);
                        SerialCardioTerminalImpl.writeToLog("Read " + receivedBytes + " bytes, Remaining " + (buffer.limit() - receivedBytes));
                    } catch (IOException e) {
                        throwable = e;
                        return;
                    } catch (NullPointerException e) {
                        throwable = e;
                        return;
                    } finally {
                        if (terminatorIsNullAndExpectMoreToReceive()) {
                            SerialCardioPagerConduit.this.setLastSync(System.currentTimeMillis());
                            return;
                        } else if (checkTerminatorVsMoreToReceive()) {
                            if (canCompareTerminatorWithBuffer()) {
                                if (!isTerminatorFound(terminator)) {
                                    SerialCardioPagerConduit.this.setLastSync(System.currentTimeMillis());
                                    return;
                                } else {
                                    terminatorFound = true;
                                }
                            } else {
                                SerialCardioPagerConduit.this.setLastSync(System.currentTimeMillis());
                                return;
                            }
                        }
                        if (isTerminatorFound(terminator)) {
                            terminatorFound = true;
                        }
                        completed = true;
                        if (buffer != null && buffer.array() != null) receivedBytes = buffer.array().length; else receivedBytes = 0;
                        synchronized (SerialCardioPagerConduit.this) {
                            SerialCardioTerminalImpl.writeToLog("Notifying waiting SerialCardioPagerConduit thread about data received...");
                            SerialCardioPagerConduit.this.notify();
                        }
                    }
                    break;
            }
        }

        public boolean getTerminatorFound() {
            return terminatorFound;
        }

        private boolean isTerminatorFound(byte[] terminator) {
            if (terminator == null || buffer == null) return false;
            return CardioPager.isIntersect(buffer.array(), terminator);
        }

        private boolean canCompareTerminatorWithBuffer() {
            return terminator.length <= buffer.array().length;
        }

        private boolean checkTerminatorVsMoreToReceive() {
            return terminator != null && receivedBytes < getBytesToExpect();
        }

        private boolean terminatorIsNullAndExpectMoreToReceive() {
            return terminator == null && receivedBytes < getBytesToExpect();
        }

        private void printOut(byte[] ar) {
            for (int j = 0; j < ar.length; j++) {
                System.out.printf("%02x ", ar[j]);
            }
            System.out.println();
            for (int j = 0; j < ar.length; j++) {
                try {
                    System.out.printf("%c ", ar[j]);
                } catch (IllegalFormatCodePointException e) {
                    System.out.printf("\\%02x ", ar[j]);
                }
            }
            System.out.println();
        }
    }
}
