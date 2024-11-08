package ggc.meter.device.freestyle;

import ggc.meter.data.MeterValuesEntry;
import ggc.meter.device.AbstractSerialMeter;
import ggc.meter.manager.company.LifeScan;
import ggc.meter.util.DataAccessMeter;
import ggc.plugin.device.DeviceIdentification;
import ggc.plugin.device.PlugInBaseException;
import ggc.plugin.manager.DeviceImplementationStatus;
import ggc.plugin.manager.company.AbstractDeviceCompany;
import ggc.plugin.output.AbstractOutputWriter;
import ggc.plugin.output.OutputWriter;
import ggc.plugin.protocol.SerialProtocol;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import java.util.StringTokenizer;
import com.atech.utils.ATechDate;
import com.atech.utils.TimeZoneUtil;

public abstract class FreestyleMeter extends AbstractSerialMeter {

    /**
     * 
     */
    public static final int LIFESCAN_COMPANY = 3;

    /**
     * 
     */
    public static final int METER_LIFESCAN_ONE_TOUCH_ULTRA = 30001;

    /**
     * 
     */
    public static final int METER_LIFESCAN_ONE_TOUCH_ULTRA_2 = 30002;

    /**
     * 
     */
    public static final int METER_LIFESCAN_ONE_TOUCH_ULTRASMART = 30003;

    /**
     * 
     */
    public static final int METER_LIFESCAN_ONE_TOUCH_ULTRALINK = 30004;

    /**
     * 
     */
    public static final int METER_LIFESCAN_ONE_TOUCH_SELECT = 30005;

    /**
     * 
     */
    public static final int METER_LIFESCAN_INDUO = 30006;

    /**
     * 
     */
    public static final int METER_LIFESCAN_ONE_TOUCH_BASIC = 30007;

    /**
     * 
     */
    public static final int METER_LIFESCAN_ONE_TOUCH_SURESTEP = 30008;

    /**
     * 
     */
    public static final int METER_LIFESCAN_ONE_TOUCH_FASTTAKE = 30009;

    /**
     * 
     */
    public static final int METER_LIFESCAN_ONE_TOUCH_PROFILE = 30010;

    /**
     * 
     */
    public static final int METER_LIFESCAN_ONE_TOUCH_II = 30011;

    /**
     * 
     */
    public static final int METER_LIFESCAN_ONE_TOUCH_ULTRA_MINI = 30050;

    /**
     * 
     */
    public static final int METER_LIFESCAN_ONE_TOUCH_ULTRA_EASY = 30050;

    protected boolean device_running = true;

    protected TimeZoneUtil tzu = TimeZoneUtil.getInstance();

    private int entries_max = 0;

    private int entries_current = 0;

    private int reading_status = 0;

    private int info_tokens;

    private String date_order;

    /**
     * Constructor
     */
    public FreestyleMeter() {
    }

    /**
     * Constructor for device manager
     * 
     * @param cmp
     */
    public FreestyleMeter(AbstractDeviceCompany cmp) {
        super(cmp);
    }

    /**
     * Constructor
     * 
     * @param portName
     * @param writer
     */
    public FreestyleMeter(String portName, OutputWriter writer) {
        super(DataAccessMeter.getInstance());
        this.setCommunicationSettings(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE, SerialPort.FLOWCONTROL_NONE, SerialProtocol.SERIAL_EVENT_BREAK_INTERRUPT | SerialProtocol.SERIAL_EVENT_OUTPUT_EMPTY);
        this.output_writer = writer;
        this.output_writer.getOutputUtil().setMaxMemoryRecords(this.getMaxMemoryRecords());
        this.setMeterType("LifeScan", this.getName());
        this.setDeviceCompany(new LifeScan());
        try {
            this.setSerialPort(portName);
            if (!this.open()) {
                this.m_status = 1;
                this.deviceDisconnected();
                return;
            }
            this.output_writer.writeHeader();
        } catch (Exception ex) {
        }
    }

    /** 
     * getComment
     */
    public String getComment() {
        return null;
    }

    /** 
     * getImplementationStatus
     */
    public int getImplementationStatus() {
        return DeviceImplementationStatus.IMPLEMENTATION_TESTING;
    }

    /** 
     * getInstructions
     */
    public String getInstructions() {
        return null;
    }

    /** 
     * readDeviceDataFull
     */
    public void readDeviceDataFull() {
        try {
            write("D".getBytes());
            waitTime(100);
            write("M".getBytes());
            waitTime(100);
            write("?".getBytes());
            waitTime(100);
            String line;
            while ((line = this.readLine()) == null) {
                System.out.println("Serial Number1: " + line);
            }
            System.out.println("Serial Number2: " + line);
            write("D".getBytes());
            waitTime(100);
            write("M".getBytes());
            waitTime(100);
            write("P".getBytes());
            waitTime(100);
            while (((line = this.readLine()) != null) && (!isDeviceStopped(line))) {
                processEntry(line);
                if (line == null) break;
            }
            this.output_writer.setSpecialProgress(100);
            this.output_writer.setSubStatus(null);
        } catch (Exception ex) {
            System.out.println("Exception: " + ex);
            ex.printStackTrace();
        }
        if (this.isDeviceFinished()) {
            this.output_writer.endOutput();
        }
        System.out.println("Reading finsihed");
    }

    private boolean isDeviceFinished() {
        return (this.entries_current == this.entries_max);
    }

    /**
     * This is method for reading partitial data from device. All reading from actual device should be done from 
     * here. Reading can be done directly here, or event can be used to read data.
     */
    public void readDeviceDataPartitial() throws PlugInBaseException {
    }

    /** 
     * This is method for reading configuration
     * 
     * @throws PlugInBaseException
     */
    public void readConfiguration() throws PlugInBaseException {
    }

    /**
     * This is for reading device information. This should be used only if normal dump doesn't retrieve this
     * information (most dumps do). 
     * @throws PlugInBaseException
     */
    public void readInfo() throws PlugInBaseException {
    }

    private boolean isDeviceStopped(String vals) {
        if ((vals == null) || ((this.reading_status == 1) && (vals.length() == 0)) || (!this.device_running) || (this.output_writer.isReadingStopped())) return true;
        return false;
    }

    private void processEntry(String entry) {
        if ((entry == null) || (entry.length() == 0)) return;
        StringTokenizer strtok = new StringTokenizer(entry, ",");
        if (strtok.countTokens() == this.info_tokens) {
            if (this.reading_status == 0) this.readInfo(strtok); else setDeviceStopped();
        } else if (strtok.countTokens() == 5) {
            this.readBGEntry(strtok, entry);
        } else {
            setDeviceStopped();
        }
    }

    protected void setDeviceStopped() {
        this.device_running = false;
        this.output_writer.endOutput();
    }

    private void readInfo(StringTokenizer strtok) {
        this.output_writer.setSubStatus(ic.getMessage("READING_SERIAL_NR_SETTINGS"));
        String num_x = strtok.nextToken();
        num_x = num_x.substring(2).trim();
        this.entries_max = Integer.parseInt(num_x);
        String dev = strtok.nextToken();
        DeviceIdentification di = this.output_writer.getDeviceIdentification();
        di.device_serial_number = dev;
        this.output_writer.setDeviceIdentification(di);
        this.output_writer.writeDeviceIdentification();
        this.output_writer.setSpecialProgress(2);
        reading_status = 1;
        this.output_writer.setSpecialProgress(4);
    }

    protected String getParameterValue(String val) {
        String d = val.substring(1, val.length() - 1);
        return d.trim();
    }

    protected void readBGEntry(StringTokenizer strtok, String entry) {
        try {
            strtok.nextToken();
            String date = strtok.nextToken();
            String time = strtok.nextToken();
            String res = this.getParameterValue(strtok.nextToken());
            this.output_writer.setSubStatus(ic.getMessage("READING_PROCESSING_ENTRY") + (this.entries_current + 1));
            if ((res.startsWith("C")) || (res.startsWith("!")) || (res.startsWith("I")) || (res.startsWith("K"))) {
                this.entries_current++;
            } else {
                this.entries_current++;
                MeterValuesEntry mve = new MeterValuesEntry();
                mve.setBgUnit(DataAccessMeter.BG_MGDL);
                mve.setDateTimeObject(getDateTime(date, time));
                if (res.contains("HIGH")) {
                    mve.setBgValue("600");
                    mve.addParameter("RESULT", "High");
                } else {
                    if (res.contains("?")) {
                        res = m_da.replaceExpression(res, "?", " ").trim();
                        mve.addParameter("RESULT", "Suspect Entry");
                    }
                    if (res.contains("MM")) {
                        res = m_da.replaceExpression(res, "MM", " ").trim();
                        try {
                            mve.setBgValue("" + m_da.getBGValueByType(DataAccessMeter.BG_MMOL, DataAccessMeter.BG_MGDL, res));
                        } catch (Exception ex) {
                        }
                    } else mve.setBgValue(res);
                }
                this.output_writer.writeData(mve);
                readingEntryStatus();
            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex);
            System.out.println("Entry: " + entry);
            ex.printStackTrace();
        }
    }

    private ATechDate getDateTime(String date, String time) {
        date = this.getParameterValue(date);
        time = this.getParameterValue(time);
        String dt = "";
        StringTokenizer st = new StringTokenizer(date, "/");
        String m = "", d = "", y = "";
        if (this.date_order.equals("MDY")) {
            m = st.nextToken();
            d = st.nextToken();
            y = st.nextToken();
        } else {
            d = st.nextToken();
            m = st.nextToken();
            y = st.nextToken();
        }
        try {
            int year = Integer.parseInt(y);
            if (year < 100) {
                if (year > 70) dt += "19" + m_da.getLeadingZero(year, 2); else dt += "20" + m_da.getLeadingZero(year, 2);
            } else dt += year;
        } catch (Exception ex) {
        }
        dt += m;
        dt += d;
        if (time.contains(" ")) {
            st = new StringTokenizer(time, ":");
            String hr = st.nextToken();
            String min = st.nextToken();
            int hr_s = Integer.parseInt(hr);
            if (time.contains("AM")) {
                if (hr_s == 12) {
                    dt += "00";
                } else {
                    dt += m_da.getLeadingZero(hr_s, 2);
                }
            } else {
                if (hr_s == 12) {
                    dt += "12";
                } else {
                    hr_s += 12;
                    dt += m_da.getLeadingZero(hr_s, 2);
                }
            }
            dt += min;
        } else {
            st = new StringTokenizer(time, ":");
            dt += st.nextToken();
            dt += st.nextToken();
        }
        return tzu.getCorrectedDateTime(new ATechDate(Long.parseLong(dt)));
    }

    private void readingEntryStatus() {
        float proc_read = ((this.entries_current * 1.0f) / this.entries_max);
        float proc_total = 4 + (96 * proc_read);
        this.output_writer.setSpecialProgress((int) proc_total);
    }

    /**
     * hasSpecialProgressStatus - in most cases we read data directly from device, in this case we have 
     *    normal progress status, but with some special devices we calculate progress through other means.
     * @return true is progress status is special
     */
    public boolean hasSpecialProgressStatus() {
        return true;
    }

    /**
     * Returns short name for meter (for example OT Ultra, would return "Ultra")
     * 
     * @return short name of meter
     */
    public abstract String getShortName();

    /**
     * We don't use serial event for reading data, because process takes too long, we use serial event just 
     * to determine if device is stopped (interrupted) 
     */
    @Override
    public void serialEvent(SerialPortEvent event) {
        switch(event.getEventType()) {
            case SerialPortEvent.BI:
                System.out.println("recievied break");
                this.output_writer.setStatus(AbstractOutputWriter.STATUS_STOPPED_DEVICE);
                break;
            case SerialPortEvent.CD:
                System.out.println("recievied cd");
                break;
            case SerialPortEvent.CTS:
                System.out.println("recievied cts");
                break;
            case SerialPortEvent.DSR:
                System.out.println("recievied dsr");
                break;
            case SerialPortEvent.FE:
                System.out.println("recievied fe");
                break;
            case SerialPortEvent.OE:
                System.out.println("recievied oe");
                System.out.println("Output Empty");
                break;
            case SerialPortEvent.PE:
                System.out.println("recievied pe");
                break;
            case SerialPortEvent.RI:
                System.out.println("recievied ri");
                break;
        }
    }
}
