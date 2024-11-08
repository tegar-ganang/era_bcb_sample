package ch.unizh.ini.jaer.projects.opticalflow.usbinterface;

import javax.swing.JPanel;
import net.sf.jaer.biasgen.*;
import net.sf.jaer.biasgen.Biasgen;
import net.sf.jaer.biasgen.VDAC.*;
import net.sf.jaer.hardwareinterface.*;
import net.sf.jaer.util.*;
import ch.unizh.ini.jaer.projects.opticalflow.*;
import ch.unizh.ini.jaer.projects.opticalflow.mdc2d.MDC2D;
import ch.unizh.ini.jaer.projects.opticalflow.mdc2d.MotionDataMDC2D;
import ch.unizh.ini.jaer.projects.opticalflow.motion18.Motion18;
import de.thesycon.usbio.*;
import de.thesycon.usbio.PnPNotifyInterface;
import de.thesycon.usbio.UsbIoErrorCodes;
import de.thesycon.usbio.structs.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Servo motor controller using USBIO driver access to SiLabsC8051F320 device.
 *
 * @author tobi
 * 
 * changelog by andstein
 *   - moved various hardware specific parts from other code into this class
 * 
 * TODO
 *   - when the hardware interface is closed, it cannot be properly reopened
 *     because the MotionReader-thread cannot be reset in the open() method
 */
public class SiLabsC8051F320_OpticalFlowHardwareInterface implements MotionChipInterface, UsbIoErrorCodes, PnPNotifyInterface {

    Logger log = Logger.getLogger("SiLabsC8051F320_USBIO_ServoController");

    /** A "magic byte" marking the start of each frame */
    public static final byte FRAME_START_MARKER = (byte) 0xac;

    static final int MAX_POTS = 64;

    int interfaceNumber = 0;

    /** driver GUID (Globally unique ID, for this USB driver instance */
    public static final String GUID = "{2013DFAA-ED13-4775-9967-8C3FEC412E2C}";

    /** Optical flow board vendor ID (This is Thesycon's VID) */
    public static final short VID = (short) 0x0547;

    /**OpticalFlowBoard Product ID */
    public static final short PID = (short) 0x8760;

    public static final short CONFIG_INDEX = 0;

    public static final short CONFIG_NB_OF_INTERFACES = 1;

    public static final short CONFIG_INTERFACE = 0;

    public static final short CONFIG_ALT_SETTING = 0;

    public static final int CONFIG_TRAN_SIZE = 64;

    public static final byte ENDPOINT_OUT = (byte) 0x02;

    public static final byte ENDPOINT_IN = (byte) 0x81;

    public static final byte VENDOR_REQUEST_START_STREAMING = 0x1a;

    public static final byte VENDOR_REQUEST_STOP_STREAMING = 0x1b;

    public static final byte VENDOR_REQUEST_SEND_BIASES = 0x1c;

    public static final byte VENDOR_REQUEST_SEND_BIAS = 0x1f;

    public static final byte VENDOR_REQUEST_SET_DATA_TO_SEND = 0x1d;

    public static final byte VENDOR_REQUEST_REQUEST_FRAME = 0x1e;

    public static final byte VENDOR_REQUEST_SEND_ONCHIP_BIAS = 0x20;

    public static final byte VENDOR_REQUEST_SET_POWERDOWN_STATE = 0x21;

    public static final byte VENDOR_REQUEST_SET_DATA_TO_SEND_MDC2D = 0x22;

    PnPNotify pnp = null;

    private boolean isOpened;

    UsbIoPipe outPipe = null;

    UsbIoPipe inPipe = null;

    private MotionReader reader = null;

    private int[] vpotValues = null;

    private int[] ipotValues = null;

    private static final long DATA_TIMEOUT_MS = 50000;

    private static final int MOTION_BUFFER_LENGTH = 1 << 14;

    private Chip2DMotion chip = new MDC2D();

    private MotionData lastbuffer;

    /**
     * Creates a new instance of SiLabsC8051F320_USBIO_ServoController
     * @param n the number of the interface, in range returned by OpticalFlowHardwareInterfaceFactory.getNumInterfacesAvailable().
     *
     */
    public SiLabsC8051F320_OpticalFlowHardwareInterface(int n) {
        interfaceNumber = n;
    }

    private void generateMotionData() {
        initialEmptyBuffer = chip.getEmptyMotionData();
        initialFullBuffer = chip.getEmptyMotionData();
        currentBuffer = initialFullBuffer;
    }

    public void setChip(Chip2DMotion chip) {
        this.chip = chip;
        generateMotionData();
    }

    public void onAdd() {
        log.info("device added");
    }

    public void onRemove() {
        log.info("device removed");
    }

    /** Closes the device. Never throws an exception.
     */
    public synchronized void close() {
        if (!isOpened) {
            log.warning("close(): not open");
            return;
        }
        if (reader != null) {
            reader.shutdownThread();
            reader.unbind();
            reader.close();
            reader = null;
        }
        gUsbIo.close();
        UsbIo.destroyDeviceList(gDevList);
        log.info("USBIOInterface.close(): device closed");
        isOpened = false;
    }

    /** the first USB string descriptor (Vendor name) (if available) */
    protected USB_STRING_DESCRIPTOR stringDescriptor1 = new USB_STRING_DESCRIPTOR();

    /** the second USB string descriptor (Product name) (if available) */
    protected USB_STRING_DESCRIPTOR stringDescriptor2 = new USB_STRING_DESCRIPTOR();

    /** the third USB string descriptor (Serial number) (if available) */
    protected USB_STRING_DESCRIPTOR stringDescriptor3 = new USB_STRING_DESCRIPTOR();

    protected int numberOfStringDescriptors = 2;

    /** returns number of string descriptors
     * @return number of string descriptors: 2 for TmpDiff128, 3 for MonitorSequencer */
    public int getNumberOfStringDescriptors() {
        return numberOfStringDescriptors;
    }

    /** the USBIO device descriptor */
    protected USB_DEVICE_DESCRIPTOR deviceDescriptor = new USB_DEVICE_DESCRIPTOR();

    /** the UsbIo interface to the device. This is assigned on construction by the
     * factory which uses it to open the device. here is used for all USBIO access
     * to the device*/
    protected UsbIo gUsbIo = null;

    /** the devlist handle for USBIO */
    protected long gDevList;

    /** checks if device has a string identifier that is a non-empty string
     *@return false if not, true if there is one
     */
    protected boolean hasStringIdentifier() {
        int status = gUsbIo.getStringDescriptor(stringDescriptor1, (byte) 1, 0);
        if (status != USBIO_ERR_SUCCESS) {
            return false;
        } else {
            if (stringDescriptor1.Str.length() > 0) return true;
        }
        return false;
    }

    /** constrcuts a new USB connection, opens it.
     */
    public void open() {
        try {
            openUsbIo();
        } catch (HardwareInterfaceException e) {
            e.printStackTrace();
            close();
        }
    }

    public synchronized void openUsbIo() throws HardwareInterfaceException {
        if (isOpened) {
            log.warning("CypressFX2.openUsbIo(): already opened interface and setup device");
            return;
        }
        int status;
        gUsbIo = new UsbIo();
        gDevList = UsbIo.createDeviceList(GUID);
        status = gUsbIo.open(interfaceNumber, gDevList, GUID);
        if (status != USBIO_ERR_SUCCESS) {
            UsbIo.destroyDeviceList(gDevList);
            throw new HardwareInterfaceException("can't open USB device: " + UsbIo.errorText(status));
        }
        status = gUsbIo.getDeviceDescriptor(deviceDescriptor);
        if (status != USBIO_ERR_SUCCESS) {
            gUsbIo.resetDevice();
            UsbIo.destroyDeviceList(gDevList);
            throw new HardwareInterfaceException("CypressFX2.openUsbIo(): getDeviceDescriptor: " + UsbIo.errorText(status));
        } else {
            log.info("getDeviceDescriptor: Vendor ID (VID) " + HexString.toString((short) deviceDescriptor.idVendor) + " Product ID (PID) " + HexString.toString((short) deviceDescriptor.idProduct));
        }
        try {
            int status2;
            status2 = gUsbIo.unconfigureDevice();
            if (status2 != USBIO_ERR_SUCCESS) {
                UsbIo.destroyDeviceList(gDevList);
                throw new HardwareInterfaceException("unconfigureDevice: " + UsbIo.errorText(status2));
            }
        } catch (HardwareInterfaceException e) {
            log.warning("can't unconfigure,will try simulated disconnect");
            int cycleStatus = gUsbIo.cyclePort();
            if (cycleStatus != USBIO_ERR_SUCCESS) {
                throw new HardwareInterfaceException("Error cycling port: " + UsbIo.errorText(cycleStatus));
            }
            throw new HardwareInterfaceException("couldn't unconfigure device");
        }
        USBIO_SET_CONFIGURATION Conf = new USBIO_SET_CONFIGURATION();
        Conf.ConfigurationIndex = CONFIG_INDEX;
        Conf.NbOfInterfaces = CONFIG_NB_OF_INTERFACES;
        Conf.InterfaceList[0].InterfaceIndex = CONFIG_INTERFACE;
        Conf.InterfaceList[0].AlternateSettingIndex = CONFIG_ALT_SETTING;
        Conf.InterfaceList[0].MaximumTransferSize = CONFIG_TRAN_SIZE;
        status = gUsbIo.setConfiguration(Conf);
        if (status != USBIO_ERR_SUCCESS) {
            log.warning("setting configuration: " + UsbIo.errorText(status));
        }
        status = gUsbIo.getDeviceDescriptor(deviceDescriptor);
        isOpened = true;
        if (reader == null) {
            reader = new MotionReader();
            reader.startThread(3);
        } else {
            log.warning("MotionReader was still running !");
            reader.resetPipe();
        }
    }

    /** return the string USB descriptors for the device
     *@return String[] of length 2 of USB descriptor strings.
     */
    public String[] getStringDescriptors() {
        if (stringDescriptor1 == null) {
            log.warning("USBAEMonitor: getStringDescriptors called but device has not been opened");
            String[] s = new String[numberOfStringDescriptors];
            for (int i = 0; i < numberOfStringDescriptors; i++) {
                s[i] = "";
            }
            return s;
        }
        String[] s = new String[numberOfStringDescriptors];
        s[0] = stringDescriptor1.Str;
        s[1] = stringDescriptor2.Str;
        if (numberOfStringDescriptors == 3) {
            s[2] = stringDescriptor3.Str;
        }
        return s;
    }

    /** return the USB VID/PID of the interface
     *@return int[] of length 2 containing the Vendor ID (VID) and Product ID (PID) of the device. First element is VID, second element is PID.
     */
    public int[] getVIDPID() {
        if (deviceDescriptor == null) {
            log.warning("USBAEMonitor: getVIDPID called but device has not been opened");
            return new int[2];
        }
        int[] n = new int[2];
        n[0] = deviceDescriptor.idVendor;
        n[1] = deviceDescriptor.idProduct;
        return n;
    }

    public short getVID() {
        if (deviceDescriptor == null) {
            log.warning("USBAEMonitor: getVID called but device has not been opened");
            return 0;
        }
        return (short) deviceDescriptor.idVendor;
    }

    public short getPID() {
        if (deviceDescriptor == null) {
            log.warning("USBAEMonitor: getPID called but device has not been opened");
            return 0;
        }
        return (short) deviceDescriptor.idProduct;
    }

    /** @return bcdDevice (the binary coded decimel device version */
    public short getDID() {
        return (short) deviceDescriptor.bcdDevice;
    }

    /** reports if interface is {@link #open}.
     * @return true if already open
     */
    public boolean isOpen() {
        return isOpened;
    }

    /** sends a vender request without any data. Thread safe.
     *@param request the vendor request byte, identifies the request on the device
     *@param value the value of the request (bValue USB field)
     *@param index the "index" of the request (bIndex USB field)
     */
    public synchronized void sendVendorRequest(byte request, short value, short index) throws HardwareInterfaceException {
        sendVendorRequest(request, value, index, null);
    }

    /** sends a vender request with data. Thread-safe.
     *@param request the vendor request byte, identifies the request on the device
     *@param value the value of the request (bValue USB field)
     *@param index the "index" of the request (bIndex USB field)
     *@param dataBuffer the data which is to be transmitted to the device
     */
    public synchronized void sendVendorRequest(byte request, short value, short index, USBIO_DATA_BUFFER dataBuffer) throws HardwareInterfaceException {
        if (!isOpen()) {
            throw new HardwareInterfaceException("Tried to send vendor request but device not open");
        }
        USBIO_CLASS_OR_VENDOR_REQUEST VendorRequest = new USBIO_CLASS_OR_VENDOR_REQUEST();
        int status;
        VendorRequest.Flags = UsbIoInterface.USBIO_SHORT_TRANSFER_OK;
        VendorRequest.Type = UsbIoInterface.RequestTypeVendor;
        VendorRequest.Recipient = UsbIoInterface.RecipientDevice;
        VendorRequest.RequestTypeReservedBits = 0;
        VendorRequest.Request = request;
        VendorRequest.Index = index;
        VendorRequest.Value = value;
        if (dataBuffer == null) {
            dataBuffer = new USBIO_DATA_BUFFER(0);
            dataBuffer.setNumberOfBytesToTransfer(dataBuffer.Buffer().length);
        } else {
            dataBuffer.setNumberOfBytesToTransfer(dataBuffer.Buffer().length);
        }
        status = gUsbIo.classOrVendorOutRequest(dataBuffer, VendorRequest);
        if (status != USBIO_ERR_SUCCESS) {
            throw new HardwareInterfaceException("Unable to send vendor request " + request + ": " + UsbIo.errorText(status));
        }
        HardwareInterfaceException.clearException();
    }

    /** the concurrent object to exchange data between rendering and the MotionReader capture thread */
    Exchanger<MotionData> exchanger = new Exchanger();

    MotionData initialEmptyBuffer = chip.getEmptyMotionData();

    MotionData initialFullBuffer = chip.getEmptyMotionData();

    MotionData currentBuffer = initialFullBuffer;

    @Override
    public JPanel getConfigPanel() {
        return null;
    }

    @Override
    public int getRawDataIndex(int bit) {
        switch(bit) {
            case MotionDataMDC2D.PHOTO:
                return 0;
            case MotionDataMDC2D.LMC1:
                return 1;
            case MotionDataMDC2D.LMC2:
                return 2;
            case MotionDataMDC2D.ON_CHIP_ADC:
                return 3;
            default:
                return -1;
        }
    }

    @Override
    public void setChannel(int bit, boolean onChip) throws HardwareInterfaceException {
        if (onChip && bit == MotionDataMDC2D.PHOTO) sendVendorRequest(VENDOR_REQUEST_SET_DATA_TO_SEND, (short) 0x0d, (short) 0);
        if (onChip && bit == MotionDataMDC2D.LMC1) sendVendorRequest(VENDOR_REQUEST_SET_DATA_TO_SEND, (short) 0x0b, (short) 0);
        if (onChip && bit == MotionDataMDC2D.LMC2) sendVendorRequest(VENDOR_REQUEST_SET_DATA_TO_SEND, (short) 0x07, (short) 0);
    }

    /**
     * This reader reads data from the motion chip
     */
    protected class MotionReader extends UsbIoReader implements UsbIoErrorCodes {

        MotionData currentBuffer;

        int sequenceNumber = 0;

        private int NUM_MOTION_BUFFERS = 2;

        public MotionReader() throws HardwareInterfaceException {
            int status;
            status = bind(0, ENDPOINT_IN, gDevList, GUID);
            if (status != USBIO_ERR_SUCCESS) {
                throw new HardwareInterfaceException("can't bind pipe: " + UsbIo.errorText(status));
            }
            USBIO_PIPE_PARAMETERS pipeParams = new USBIO_PIPE_PARAMETERS();
            pipeParams.Flags = UsbIoInterface.USBIO_SHORT_TRANSFER_OK;
            status = setPipeParameters(pipeParams);
            if (status != USBIO_ERR_SUCCESS) {
                throw new HardwareInterfaceException("can't set pipe parameters: " + UsbIo.errorText(status));
            }
            allocateBuffers(MOTION_BUFFER_LENGTH, NUM_MOTION_BUFFERS);
            currentBuffer = initialEmptyBuffer;
        }

        /** Starts the thread running with some tolerated error count and sends a vendor request to start streaming data
         @param i the number of errors before throwing exception
         */
        public void startThread(int i) {
            super.startThread(i);
            log.info("started MotionReader thread");
            try {
                sendVendorRequest(VENDOR_REQUEST_START_STREAMING, (short) 0, (short) 0);
            } catch (HardwareInterfaceException e) {
                log.warning(e.getMessage());
            }
        }

        /** called to prepare buffer for capture */
        public void processBuffer(UsbIoBuf usbIoBuf) {
            usbIoBuf.NumberOfBytesToTransfer = usbIoBuf.Size;
            usbIoBuf.BytesTransferred = 0;
            usbIoBuf.OperationFinished = false;
            lastbuffer = currentBuffer.clone();
            try {
                currentBuffer = exchanger.exchange(currentBuffer);
                requestData();
                lastbuffer.setLastMotionData(lastbuffer);
                currentBuffer.setPastMotionData(lastbuffer.getPastMotionData());
            } catch (InterruptedException ex) {
            }
        }

        /** called when xfer is finished */
        public void processData(UsbIoBuf usbIoBuf) {
            unpackData(usbIoBuf, currentBuffer);
        }

        int[] buf;

        /** unpacks ADC data into float values ranging 0-1 */
        void unpackData(UsbIoBuf usbBuf, MotionData motionBuf) {
            if (buf == null || buf.length != usbBuf.BytesTransferred * 2) {
                buf = new int[usbBuf.BytesTransferred * 2];
            }
            int count = 0;
            int i = 0;
            byte bitOffset = 0;
            int a, b = 0;
            int posX = 0, posY = 0;
            byte packetDescriptor = usbBuf.BufferMem[1];
            try {
                if (usbBuf.BufferMem[0] != FRAME_START_MARKER) {
                    log.warning("Frame start marker does not match, unpacking failed");
                    return;
                }
                motionBuf.setContents(packetDescriptor);
                motionBuf.setSequenceNumber(sequenceNumber++);
                motionBuf.setTimeCapturedMs(System.currentTimeMillis());
                for (i = 2; i < usbBuf.BytesTransferred; i++) {
                    a = usbBuf.BufferMem[i];
                    b = usbBuf.BufferMem[i + 1];
                    if (a < 0) {
                        a = (a & 0x7F) + 0x80;
                    }
                    if (b < 0) {
                        b = (b & 0x7F) + 0x80;
                    }
                    buf[count] = ((a << (2 + bitOffset)) & 0x3FF) | ((b >>> (6 - bitOffset)) & 0xFF);
                    count++;
                    bitOffset += 2;
                    if (bitOffset == 8) {
                        bitOffset = 0;
                        i++;
                    }
                    if (buf[count] < 0) {
                        log.warning("sign error while unpacking");
                    }
                }
                i = 0;
                float[] globalRaw = motionBuf.getRawDataGlobal();
                for (int j = 0; j < motionBuf.getNumGlobalChannels(); j++) {
                    globalRaw[i] = chip.convert10bitToFloat(buf[i]);
                    i++;
                }
                motionBuf.setRawDataGlobal(globalRaw);
                posX = 0;
                posY = 0;
                float[][][] pixelRaw = motionBuf.getRawDataPixel();
                while (i < count) {
                    for (int j = 0; j < motionBuf.getNumLocalChannels(); j++) {
                        pixelRaw[j][posY][posX] = chip.convert10bitToFloat(buf[i]);
                        i++;
                    }
                    posX++;
                    if (posX == chip.NUM_COLUMNS) {
                        posX = 0;
                        posY++;
                        if (posY == chip.NUM_ROWS && i < count) log.warning("position y too big while unpacking");
                    }
                }
                motionBuf.setRawDataPixel(pixelRaw);
            } catch (ArrayIndexOutOfBoundsException e) {
                log.warning(e.getMessage());
            }
            motionBuf.collectMotionInfo();
        }

        @Override
        public void bufErrorHandler(UsbIoBuf usbIoBuf) {
            log.warning("bufferError: " + UsbIo.errorText(usbIoBuf.Status));
        }

        @Override
        public void onThreadExit() {
            try {
                sendVendorRequest(VENDOR_REQUEST_STOP_STREAMING, (short) 0, (short) 0);
            } catch (HardwareInterfaceException e) {
                log.warning(e.getMessage());
            }
        }

        void requestData() {
        }
    }

    /** returns MotionData from the device
     * @return MotionData for one frame
     @throws TimeOutException when request for exchange with Reader thread times out. Timeout duration is set by DATA_TIMEOUT_MS.
     */
    @Override
    public MotionData getData() throws java.util.concurrent.TimeoutException {
        try {
            currentBuffer = exchanger.exchange(currentBuffer, DATA_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            return currentBuffer;
        } catch (InterruptedException e) {
            return null;
        } catch (java.util.concurrent.TimeoutException to) {
            throw new TimeoutException("didn't get data after " + DATA_TIMEOUT_MS + " ms");
        }
    }

    @Override
    public void setPowerDown(boolean powerDown) throws HardwareInterfaceException {
        int pd;
        if (powerDown) {
            pd = 1;
        } else {
            pd = 0;
        }
        this.sendVendorRequest(VENDOR_REQUEST_SET_POWERDOWN_STATE, (short) pd, (short) 0);
    }

    /** sends the pot values, but uses a local cache to only send those values that have changed
     * @param biasgen the biasgen we are sending for
     */
    @Override
    public void sendConfiguration(Biasgen biasgen) throws HardwareInterfaceException {
        PotArray potArray = biasgen.getPotArray();
        if (vpotValues == null) {
            vpotValues = new int[MAX_POTS];
            for (int i = 0; i < vpotValues.length; i++) {
                vpotValues[i] = -1;
            }
        }
        for (short i = 0; i < biasgen.getNumPots(); i++) {
            VPot vpot = (VPot) potArray.getPotByNumber(i);
            int chan = vpot.getChannel();
            if (vpotValues[chan] != vpot.getBitValue()) {
                sendVendorRequest(VENDOR_REQUEST_SEND_BIAS, (short) vpot.getBitValue(), (short) chan);
                vpotValues[chan] = vpot.getBitValue();
                log.info("set VPot value " + vpot.getBitValue() + " (" + vpot.getPhysicalValue() + vpot.getPhysicalValueUnits() + ") for channel " + chan);
            }
        }
        if (ipotValues == null) {
            ipotValues = new int[38];
            for (int i = 0; i < ipotValues.length; i++) {
                ipotValues[i] = -1;
            }
        }
        PotArray ipots = ((MDC2D.MDC2DBiasgen) biasgen).getIPotArray();
        for (short i = 0; i < ipots.getNumPots(); i++) {
            IPot ipot = (IPot) ipots.getPotByNumber(i);
            int chan = ipot.getShiftRegisterNumber();
            if (ipotValues[chan] != ipot.getBitValue()) {
                ipotValues[chan] = ipot.getBitValue();
                byte[] bin = ipot.getBinaryRepresentation();
                byte request = VENDOR_REQUEST_SEND_ONCHIP_BIAS;
                short value = (short) (((chan << 8) & 0xFF00) | ((bin[0]) & 0x00FF));
                short index = (short) (((bin[1] << 8) & 0xFF00) | (bin[2] & 0x00FF));
                sendVendorRequest(request, value, index);
                log.info("set IPot value " + ipot.getBitValue() + " (" + ipot.getPhysicalValue() + ipot.getPhysicalValueUnits() + ") into SR pos " + chan);
            }
        }
    }

    @Override
    public void flashConfiguration(Biasgen biasgen) throws HardwareInterfaceException {
        log.warning("not implemented yet");
    }

    @Override
    public void setCaptureMode(int mode) throws HardwareInterfaceException {
        generateMotionData();
    }

    @Override
    public byte[] formatConfigurationBytes(Biasgen biasgen) {
        return null;
    }

    /** get text name of interface, e.g. "CypressFX2" or "SiLabsC8051F320" */
    @Override
    public String getTypeName() {
        return "SiLabsC8051F320";
    }
}
