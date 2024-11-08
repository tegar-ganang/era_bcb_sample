package ggc.meter.device.abbott;

import ggc.meter.data.MeterValuesEntry;
import ggc.meter.device.AbstractSerialMeter;
import ggc.meter.manager.MeterDevicesIds;
import ggc.meter.manager.company.Abbott;
import ggc.meter.util.DataAccessMeter;
import ggc.plugin.device.DeviceIdentification;
import ggc.plugin.device.PlugInBaseException;
import ggc.plugin.manager.DeviceImplementationStatus;
import ggc.plugin.manager.company.AbstractDeviceCompany;
import ggc.plugin.output.AbstractOutputWriter;
import ggc.plugin.output.ConsoleOutputWriter;
import ggc.plugin.output.OutputUtil;
import ggc.plugin.output.OutputWriter;
import ggc.plugin.protocol.SerialProtocol;
import ggc.plugin.util.DataAccessPlugInBase;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.atech.utils.data.ATechDate;
import com.atech.utils.data.TimeZoneUtil;

public abstract class FreestyleMeter extends AbstractSerialMeter {

    private static Log log = LogFactory.getLog(FreestyleMeter.class);

    protected boolean device_running = true;

    protected TimeZoneUtil tzu = TimeZoneUtil.getInstance();

    private int entries_max = 0;

    private int entries_current = 0;

    private int reading_status = 0;

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
        this(portName, writer, DataAccessMeter.getInstance());
    }

    /**
     * Constructor
     * 
     * @param portName
     * @param writer
     * @param da 
     */
    public FreestyleMeter(String portName, OutputWriter writer, DataAccessPlugInBase da) {
        super(portName, writer, da);
        this.setCommunicationSettings(19200, SerialPort.DATABITS_8, SerialPort.STOPBITS_2, SerialPort.PARITY_NONE, SerialPort.FLOWCONTROL_NONE, SerialProtocol.SERIAL_EVENT_BREAK_INTERRUPT | SerialProtocol.SERIAL_EVENT_OUTPUT_EMPTY);
        this.output_writer = writer;
        this.output_writer.getOutputUtil().setMaxMemoryRecords(this.getMaxMemoryRecords());
        this.setMeterType("Abbott", this.getName());
        this.setDeviceCompany(new Abbott());
        try {
            this.setSerialPort(portName);
            if (!this.open()) {
                this.m_status = 1;
                this.deviceDisconnected();
                return;
            }
            this.output_writer.writeHeader();
        } catch (Exception ex) {
            log.error("Exception on create:" + ex, ex);
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
    public void readDeviceDataFull() throws PlugInBaseException {
        try {
            write("mem".getBytes());
            String line;
            readInfo();
            while (((line = this.readLine()) != null) && (!isDeviceStopped(line))) {
                line = line.trim();
                processBGData(line);
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
        super.close();
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
        try {
            this.output_writer.setSubStatus(ic.getMessage("READING_SERIAL_NR_SETTINGS"));
            this.output_writer.setSpecialProgress(1);
            DeviceIdentification di = this.output_writer.getDeviceIdentification();
            this.readLineDebug();
            di.device_serial_number = this.readLineDebug();
            this.output_writer.setSpecialProgress(2);
            di.device_hardware_version = this.readLineDebug();
            this.output_writer.setSpecialProgress(3);
            this.readLineDebug();
            this.output_writer.setSpecialProgress(4);
            String entries_max_string = this.readLineDebug().trim();
            this.entries_max = Integer.parseInt(entries_max_string);
            this.output_writer.setDeviceIdentification(di);
            this.output_writer.writeDeviceIdentification();
            this.output_writer.setSpecialProgress(5);
        } catch (IOException ex) {
            throw new PlugInBaseException(ex);
        }
    }

    protected String readLineDebug() throws IOException {
        String rdl = this.readLine();
        log.debug(rdl);
        return rdl;
    }

    private boolean isDeviceStopped(String vals) {
        if ((vals == null) || ((this.reading_status == 1) && (vals.length() == 0)) || (!this.device_running) || (this.output_writer.isReadingStopped())) return true;
        return false;
    }

    /**
     * Process BG Data
     * 
     * @param entry
     */
    public void processBGData(String entry) {
        if ((entry == null) || (entry.length() == 0)) return;
        if (entry.contains("END")) {
            this.device_running = false;
            this.output_writer.setReadingStop();
            return;
        }
        MeterValuesEntry mve = new MeterValuesEntry();
        mve.setBgUnit(OutputUtil.BG_MGDL);
        String BGString = entry.substring(0, 5);
        if (BGString.contains("HI")) {
            mve.setBgValue("500");
            mve.addParameter("RESULT", "High");
        } else {
            mve.setBgValue("" + BGString.trim());
        }
        String timeString = entry.substring(5, 23);
        mve.setDateTimeObject(getDateTime(timeString));
        this.output_writer.writeData(mve);
        this.entries_current++;
        readingEntryStatus();
    }

    protected void setDeviceStopped() {
        this.device_running = false;
        this.output_writer.endOutput();
    }

    protected String getParameterValue(String val) {
        String d = val.substring(1, val.length() - 1);
        return d.trim();
    }

    private static String months_en[] = { "", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    protected ATechDate getDateTime(String datetime) {
        ATechDate dt = new ATechDate(ATechDate.FORMAT_DATE_AND_TIME_MIN);
        String mnth = datetime.substring(0, 3);
        dt.day_of_month = Integer.parseInt(datetime.substring(5, 7));
        dt.year = Integer.parseInt(datetime.substring(8, 12));
        dt.hour_of_day = Integer.parseInt(datetime.substring(13, 15));
        dt.minute = Integer.parseInt(datetime.substring(16, 18));
        for (int i = 0; i < FreestyleMeter.months_en.length; i++) {
            if (mnth.equals(FreestyleMeter.months_en[i])) {
                dt.month = i;
                break;
            }
        }
        return dt;
    }

    private void readingEntryStatus() {
        float proc_read = ((this.entries_current * 1.0f) / this.entries_max);
        float proc_total = 5 + (95 * proc_read);
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

    /**
     * getCompanyId - Get Company Id 
     * 
     * @return id of company
     */
    public int getCompanyId() {
        return MeterDevicesIds.COMPANY_ABBOTT;
    }

    /**
     * @param args
     */
    public static void main(String args[]) {
        Freestyle fm = new Freestyle();
        fm.output_writer = new ConsoleOutputWriter();
        String data[] = { "093  May  30 2005 00:46 16 0x01", "105  May  30 2005 00:42 16 0x00", "085  May  29 2005 23:52 16 0x00", "073  May  29 2005 21:13 16 0x00", "091  May  29 2005 21:11 16 0x01" };
        for (int i = 0; i < data.length; i++) {
            fm.processBGData(data[i]);
        }
    }
}
