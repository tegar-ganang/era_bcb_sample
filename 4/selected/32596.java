package org.rdv.action;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.rdv.data.DataChannel;
import org.rdv.data.DataFileReader;
import org.rdv.data.NumericDataSample;
import org.rdv.rbnb.RBNBController;
import org.rdv.rbnb.RBNBException;
import org.rdv.rbnb.RBNBSource;
import org.rdv.ui.ProgressWindow;

/**
 * An action to import data files.
 * 
 * @author Jason P. Hanley
 *
 */
public class DataImportAction extends DataViewerAction {

    /** serialization version identifier */
    private static final long serialVersionUID = 4075250858285498451L;

    /** the window to show the progress of the import */
    private ProgressWindow progressWindow;

    /** the number of samples to collect before sending the data to the server */
    private static int SAMPLES_PER_FLUSH = 50;

    public DataImportAction() {
        super("Import data file", "Import local data to RBNB server");
    }

    /** constructor with generic parameters */
    public DataImportAction(String text, String desc) {
        super(text, desc);
    }

    /**
   * Prompts the user for the data file and uploads the data to the RBNB server.
   */
    public void actionPerformed(ActionEvent ae) {
        importData();
    }

    /**
   * Prompts the user for the data file and uploads the data to the RBNB server.
   */
    public void importData() {
        File dataFile = getFile();
        if (dataFile == null) {
            return;
        }
        if (!dataFile.exists()) {
            return;
        }
        if (!dataFile.isFile()) {
            return;
        }
        importData(dataFile);
    }

    /**
   * Uploads the data to the RBNB server.
   * 
   * @param dataFile  the data file
   */
    public void importData(File dataFile) {
        importData(dataFile, null);
    }

    /**
   * Uploads the data to the RBNB server using the given source name.
   * 
   * @param dataFile    the data file
   * @param sourceName  the source name
   */
    public void importData(File dataFile, String sourceName) {
        URL dataFileURL = null;
        try {
            dataFileURL = dataFile.toURI().toURL();
        } catch (MalformedURLException e) {
        }
        importData(dataFileURL, sourceName);
    }

    /**
   * Uploads the data to the RBNB server using the given source name.
   * 
   * @param dataFile    the data file URL
   * @param sourceName  the source name
   */
    public void importData(URL dataFile, String sourceName) {
        if (sourceName == null || sourceName.length() == 0) {
            sourceName = getDefaultSourceName(dataFile);
        }
        importData(Collections.singletonList(dataFile), Collections.singletonList(sourceName));
    }

    /**
   * Uploads the data to the RBNB server.
   * 
   * @param dataFiles  the data files
   */
    public void importData(List<URL> dataFiles) {
        List<String> sourceNames = new ArrayList<String>();
        for (URL dataFile : dataFiles) {
            sourceNames.add(getDefaultSourceName(dataFile));
        }
        importData(dataFiles, sourceNames);
    }

    /**
   * Uploads the data to the RBNB using the given source names. This will happen
   * is a separate thread.
   * 
   * @param dataFiles    the data files
   * @param sourceNames  the source names
   */
    public void importData(final List<URL> dataFiles, final List<String> sourceNames) {
        progressWindow = new ProgressWindow("Importing data...");
        progressWindow.setVisible(true);
        new Thread() {

            public void run() {
                boolean error = false;
                try {
                    importDataThread(dataFiles, sourceNames);
                } catch (IOException e) {
                    error = true;
                    e.printStackTrace();
                } catch (RBNBException e) {
                    error = true;
                    e.printStackTrace();
                }
                progressWindow.dispose();
                RBNBController.getInstance().updateMetadata();
                if (!error) {
                    JOptionPane.showMessageDialog(null, "Import complete.", "Import complete", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "There was an error importing the data file.", "Import failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.start();
    }

    /**
   * The thread that does the importing.
   * 
   * @param dataFiles       the list of data files to import
   * @param sourceNames     the names of the sources for the data files
   * @throws IOException    if there is an error reading the data files
   * @throws RBNBException  if there is an error importing the data
   */
    private void importDataThread(List<URL> dataFiles, List<String> sourceNames) throws IOException, RBNBException {
        for (int i = 0; i < dataFiles.size(); i++) {
            URL dataFile = dataFiles.get(i);
            String sourceName = sourceNames.get(i);
            progressWindow.setStatus("Importing data file " + getFileName(dataFile));
            float minProgress = (float) (i) / dataFiles.size();
            float maxProgress = (float) (i + 1) / dataFiles.size();
            DataFileReader reader = new DataFileReader(dataFile);
            if (reader.getProperty("samples") == null) {
                throw new IOException("Unable to determine the number of data samples.");
            }
            int samples = Integer.parseInt(reader.getProperty("samples"));
            int archiveSize = (int) Math.ceil((double) samples / SAMPLES_PER_FLUSH);
            RBNBController rbnb = RBNBController.getInstance();
            RBNBSource source = new RBNBSource(sourceName, archiveSize, rbnb.getRBNBHostName(), rbnb.getRBNBPortNumber());
            List<DataChannel> channels = reader.getChannels();
            for (DataChannel channel : channels) {
                source.addChannel(channel.getName(), "application/octet-stream", channel.getUnit());
            }
            int currentSample = 0;
            NumericDataSample sample;
            while ((sample = reader.readSample()) != null) {
                double timestamp = sample.getTimestamp();
                Number[] values = sample.getValues();
                for (int j = 0; j < values.length; j++) {
                    if (values[j] == null) {
                        continue;
                    }
                    source.putData(channels.get(j).getName(), timestamp, values[j].doubleValue());
                }
                currentSample++;
                if (currentSample % 50 == 0) {
                    source.flush();
                }
                if (samples != -1) {
                    progressWindow.setProgress(minProgress + maxProgress * currentSample / samples);
                }
            }
            source.flush();
            source.close();
            progressWindow.setProgress(maxProgress);
        }
    }

    /**
   * Prompts the user for the file to import data from.
   * 
   * @return  the data file, or null if none is selected
   */
    private static File getFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnVal = fileChooser.showDialog(null, "Import");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        } else {
            return null;
        }
    }

    /**
   * Gets the name of the file from the URL.
   * 
   * @param file  the file URL
   * @return      the name of the file
   */
    private static String getFileName(URL file) {
        String fileName = file.getPath();
        if (fileName.endsWith("/content")) {
            fileName = fileName.substring(0, fileName.length() - 8);
        }
        int lastPathIndex = fileName.lastIndexOf('/');
        if (fileName.length() > lastPathIndex + 1) {
            fileName = fileName.substring(lastPathIndex + 1);
        }
        try {
            fileName = URLDecoder.decode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException ue) {
        }
        return fileName;
    }

    /**
   * Gets the default name of the source for the given the file. This will be
   * the name of the file without the extension.
   * 
   * @param dataFile  the data file
   * @return          the source name
   */
    private static String getDefaultSourceName(URL dataFile) {
        String sourceName = getFileName(dataFile);
        int dotIndex = sourceName.lastIndexOf('.');
        if (dotIndex != -1) {
            sourceName = sourceName.substring(0, dotIndex);
        }
        return sourceName;
    }
}
