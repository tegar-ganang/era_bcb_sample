package org.cdp1802.upb.adapter;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import org.cdp1802.upb.UPBDeviceI;
import org.cdp1802.upb.UPBDimmableI;
import org.cdp1802.upb.UPBLinkI;
import org.cdp1802.upb.UPBNetworkI;
import org.cdp1802.upb.UPBProductI;

/**
 * Exporter that can generate the UPStart export file for network data
 *
 * Export UPB network configuration to the export file understood by
 * the UPStart program.  
 *
 * NOTE: This exporter only partially implements the report format specified
 * as Version 5. It generates only those records that the Importer processes.
 *  
 * @author rrodgers
 */
class UPStartExporter {

    static final int BOF = 0;

    static final int EOF = 1;

    static final int LINK = 2;

    static final int ID = 3;

    static final int PRESET = 4;

    static final int ROCKER = 5;

    static final int BUTTON = 6;

    static final int INPUT = 7;

    static final int CHANNEL = 8;

    static final int VHC = 9;

    static final int INSTALLER = 10;

    static final int OWNER = 11;

    static final int DEVICE = 12;

    static final int KEYPAD = 13;

    static final int THERMOSTAT = 14;

    static final int KEYPAD_K = 1;

    static final int SWITCH_K = 2;

    static final int MODULE_K = 3;

    static final int INPUT_K = 4;

    static final int INPUT_OUTPUT_K = 5;

    static final int VPM_K = 6;

    static final int VHC_K = 7;

    static final int THERMOSTAT_K = 8;

    static final int OTHER_K = 0;

    static final int FMT_VERSION = 5;

    private File exportFile = null;

    protected String exporterName = "UPB_EXPORT";

    protected UPBNetworkI exportNetwork = null;

    private StringBuffer buf = null;

    private List<String> fields = new ArrayList<String>();

    UPStartExporter(UPBNetworkI exportNetwork) {
        this.exportNetwork = exportNetwork;
        exporterName = "UPStart_EXPORT";
    }

    /**
   *
   * Set the export file
   *
   * @param toFile file to export to
   */
    public void setExportFile(File toFile) {
        this.exportFile = toFile;
    }

    /**
   * Get the currently installed export file
   *
   * @return installed export file, if any
   */
    public File getExportFile() {
        return exportFile;
    }

    private void buildExport() {
        buf = new StringBuffer();
        writeBOF(exportNetwork);
        for (UPBLinkI link : exportNetwork.getLinks()) {
            writeLink(link);
        }
        for (UPBDeviceI device : exportNetwork.getDevices()) {
            UPBProductI product = device.getProductInfo();
            writeId(device, product);
            for (int chan = 0; chan < device.getChannelCount(); chan++) {
                writeChannel(device, chan);
                switch(product.getProductKind()) {
                    case KEYPAD_K:
                        for (int comp = 0; comp < product.getTransmitComponentCount(); comp++) {
                            writeButton(device, chan, comp);
                        }
                        break;
                    case SWITCH_K:
                    case MODULE_K:
                    case INPUT_OUTPUT_K:
                        for (int comp = 0; comp < device.getReceiveComponentCount(); comp++) {
                            writePreset(device, chan, comp);
                        }
                        for (int comp = 0; comp < product.getTransmitComponentCount(); comp++) {
                            writeButton(device, chan, comp);
                        }
                        break;
                    case OTHER_K:
                    default:
                        break;
                }
            }
        }
        writeEOF();
    }

    private void writeRecord() {
        for (String field : fields) {
            buf.append(field);
            buf.append(",");
        }
        buf.deleteCharAt(buf.length() - 1);
        buf.append("/n");
        fields.clear();
    }

    private void writeBOF(UPBNetworkI network) {
        fields.add(String.valueOf(BOF));
        fields.add(String.valueOf(FMT_VERSION));
        fields.add(String.valueOf(network.getDeviceCount()));
        fields.add(String.valueOf(network.getLinkCount()));
        fields.add(String.valueOf(network.getNetworkID()));
        fields.add(String.valueOf(network.getNetworkPassword()));
        writeRecord();
    }

    private void writeEOF() {
        fields.add(String.valueOf(EOF));
        writeRecord();
    }

    private void writeLink(UPBLinkI link) {
        fields.add(String.valueOf(LINK));
        fields.add(String.valueOf(link.getLinkID()));
        fields.add(link.getLinkName());
        writeRecord();
    }

    private void writeId(UPBDeviceI device, UPBProductI product) {
        fields.add(String.valueOf(ID));
        fields.add(String.valueOf(device.getDeviceID()));
        fields.add(String.valueOf(device.getNetworkID()));
        fields.add(String.valueOf(product.getManufacturerID()));
        fields.add(String.valueOf(product.getProductID()));
        fields.add(String.valueOf(device.getFirmwareVersion() >> 8));
        fields.add(String.valueOf(device.getFirmwareVersion() & 0xff));
        fields.add(String.valueOf(product.getProductKind()));
        fields.add(String.valueOf(device.getChannelCount()));
        fields.add(String.valueOf(product.getTransmitComponentCount()));
        fields.add(String.valueOf(device.getReceiveComponentCount()));
        fields.add(device.getRoom().getRoomName());
        fields.add(device.getDeviceName());
        fields.add("0");
        writeRecord();
    }

    private void writeChannel(UPBDeviceI device, int chan) {
        fields.add(String.valueOf(CHANNEL));
        fields.add(String.valueOf(chan));
        fields.add(String.valueOf(device.getDeviceID()));
        fields.add(device.isDimmable(chan) ? "1" : "0");
        if (device instanceof UPBDimmableI && device.isDimmable(chan)) {
            fields.add(String.valueOf(((UPBDimmableI) device).getDefaultFadeRate(chan)));
        } else {
            fields.add("0");
        }
        writeRecord();
    }

    private void writePreset(UPBDeviceI device, int chan, int comp) {
        fields.add(String.valueOf(PRESET));
        fields.add(String.valueOf(chan));
        fields.add(String.valueOf(comp));
        fields.add(String.valueOf(device.getDeviceID()));
        writeRecord();
    }

    private void writeButton(UPBDeviceI device, int chan, int comp) {
        fields.add(String.valueOf(BUTTON));
        fields.add(String.valueOf(chan));
        fields.add(String.valueOf(comp));
        fields.add(String.valueOf(device.getDeviceID()));
        writeRecord();
    }

    /**
   * Export to the configured UPStart export file
   *
   * @return true if export succeeded
   */
    public boolean startExport() {
        if ((exportFile == null) || exportFile.exists() || !exportFile.canWrite()) return false;
        FileWriter exportWriter = null;
        try {
            exportWriter = new FileWriter(exportFile);
            buildExport();
            exportWriter.write(buf.toString());
        } catch (Throwable theError) {
            theError.printStackTrace();
            return false;
        } finally {
            if (exportWriter != null) {
                try {
                    exportWriter.close();
                } catch (Exception closeError) {
                }
            }
        }
        return true;
    }
}
