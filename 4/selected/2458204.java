package org.matsim.withinday.trafficmanagement;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import org.matsim.controler.Controler;
import org.matsim.utils.io.IOUtils;

/**
 * @author dgrether
 *
 */
public class VDSSignOutput {

    private String spreadsheetFileName;

    private VDSSignSpreadSheetWriter spreadSheetWriter;

    public void setSpreadsheetFile(final String filename) {
        this.spreadsheetFileName = filename;
    }

    public void addMeasurement(final double time, final double measuredTTMainRoute, final double measuredTTAltRoute, final double nashTime) {
        try {
            this.spreadSheetWriter.writeLine(time, measuredTTMainRoute, measuredTTAltRoute, nashTime);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init() throws IOException {
        String fileName = Controler.getIterationFilename(this.spreadsheetFileName);
        File spreadsheetFile = new File(fileName);
        if (spreadsheetFile.exists()) {
            boolean ret = IOUtils.renameFile(fileName, fileName + ".old");
            if (!ret) {
                spreadsheetFile.delete();
            }
        }
        spreadsheetFile.createNewFile();
        BufferedWriter spreadFileWriter = IOUtils.getBufferedWriter(fileName);
        this.spreadSheetWriter = new VDSSignSpreadSheetWriter(spreadFileWriter);
        this.spreadSheetWriter.writeHeader();
    }

    public void close() {
        try {
            this.spreadSheetWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
