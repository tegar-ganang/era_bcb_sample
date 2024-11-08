package org.rdv.action;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.swing.JOptionPane;
import org.rdv.DataViewer;
import org.rdv.RDV;
import org.rdv.data.ASCIIDataFileWriter;
import org.rdv.data.DataChannel;
import org.rdv.data.DataFileWriter;
import org.rdv.data.MATFileWriter;
import org.rdv.data.NumericDataSample;
import org.rdv.rbnb.Channel;
import org.rdv.rbnb.RBNBController;
import org.rdv.rbnb.RBNBReader;
import org.rdv.ui.ApplicationFrame;
import org.rdv.ui.ExportDialog;
import org.rdv.ui.ProgressWindow;
import com.rbnb.sapi.SAPIException;

/**
 * An action to export data from a RBNB server to disk
 * 
 * @author Jason P. Hanley
 * @see RBNBReader
 * @see DataFileWriter
 */
public class DataExportAction extends DataViewerAction {

    /** serialization version identifier */
    private static final long serialVersionUID = 8366769228683976744L;

    public DataExportAction() {
        super("Export data channels", "Export data on the server to the local computer");
    }

    /**
   * Show the data export dialog with all channels.
   */
    public void actionPerformed(ActionEvent ae) {
        ApplicationFrame mainFrame = (ApplicationFrame) RDV.getInstance(RDV.class).getMainView().getFrame().getContentPane().getComponent(0);
        List<String> channels = mainFrame.getSelectedChannels();
        exportData(channels);
    }

    /**
   * Show the data export dialog with the specified channels.
   * 
   * @param channels
   *          the channels to export
   */
    public void exportData(List<String> channels) {
        for (int i = channels.size() - 1; i >= 0; i--) {
            Channel channel = RBNBController.getInstance().getChannel(channels.get(i));
            String mime = channel.getMetadata("mime");
            if (!mime.equals("application/octet-stream")) {
                channels.remove(i);
            }
        }
        if (channels == null || channels.isEmpty()) {
            JOptionPane.showMessageDialog(null, "There are no data channels selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<String> fileFormats = Arrays.asList(new String[] { "ASCII", "MATLAB" });
        ExportDialog dialog = ExportDialog.showDialog(channels, fileFormats);
        if (dialog == null) {
            return;
        }
        DataFileWriter writer;
        if (dialog.getFileFormat().equals("MATLAB")) {
            writer = new MATFileWriter();
        } else {
            writer = new ASCIIDataFileWriter();
        }
        exportData(dialog.getSelectedChannels(), dialog.getStartTime(), dialog.getEndTime(), dialog.getFile(), writer);
    }

    /**
   * Export the data contained in the channels, bounded by the start and end
   * time, to the file using the specified writer. This will spawn a thread to
   * do the actual exporting.
   * 
   * @param channels
   *          the channels to export
   * @param startTime
   *          the start time for the data
   * @param endTime
   *          the end time for the data
   * @param file
   *          the file to export the data to
   * @param writer
   *          the file writer to write the data to
   * @see #exportDataThread(String, List, double, double, File, DataFileWriter,
   *      ProgressWindow)
   */
    private void exportData(final List<String> channels, final double startTime, final double endTime, final File file, final DataFileWriter writer) {
        final ProgressWindow progressWindow = new ProgressWindow("Exporting data...");
        progressWindow.setStatus("Exporting data to " + file.getName());
        progressWindow.setVisible(true);
        final String rbnbServer = RBNBController.getInstance().getRBNBConnectionString();
        new Thread("Export") {

            public void run() {
                boolean error = false;
                try {
                    exportDataThread(rbnbServer, channels, startTime, endTime, file, writer, progressWindow);
                } catch (SAPIException e) {
                    error = true;
                    e.printStackTrace();
                } catch (IOException e) {
                    error = true;
                    e.printStackTrace();
                }
                progressWindow.dispose();
                if (!error) {
                    JOptionPane.showMessageDialog(null, "Export complete.", "Export complete", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "There was an error export the data to file.", "Export failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.start();
    }

    /**
   * Export the data.
   * 
   * @param rbnbServer
   *          the rbnb server to export from
   * @param channels
   *          the channels to export
   * @param startTime
   *          the start time for the data
   * @param endTime
   *          the end time for the data
   * @param file
   *          the file to export the data to
   * @param writer
   *          the file writer to write the data to
   * @param progressWindow
   *          the windows to post progress
   * @throws SAPIException
   *           if there is an error communicating with the server
   * @throws IOException
   *           if there is an error writing the data file
   */
    public void exportDataThread(String rbnbServer, List<String> channels, double startTime, double endTime, File file, DataFileWriter writer, ProgressWindow progressWindow) throws SAPIException, IOException {
        RBNBReader reader = null;
        try {
            reader = new RBNBReader(rbnbServer, channels, startTime, endTime);
            List<DataChannel> channelMetadata = RBNBController.getInstance().getMetadataManager().getChannels(channels);
            writer.init(channelMetadata, startTime, endTime, file);
            progressWindow.setProgress(0);
            NumericDataSample sample;
            while ((sample = reader.readSample()) != null) {
                writer.writeSample(sample);
                float progress = (float) ((sample.getTimestamp() - startTime) / (endTime - startTime));
                progressWindow.setProgress(progress);
                progressWindow.setStatus("Exporting data to " + file.getName() + " (" + DataViewer.formatDate(sample.getTimestamp()) + ")");
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
            writer.close();
        }
    }
}
