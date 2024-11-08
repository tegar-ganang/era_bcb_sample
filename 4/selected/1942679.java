package geovista.cartogram;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.dbf.DbaseFileWriter;
import geovista.geoviz.map.GeoMapUni;
import geovista.geoviz.visclass.VisualClassifier;
import geovista.readers.example.GeoDataGeneralizedStates;
import geovista.readers.util.MyFileFilter;

public class GuiUtils extends JPanel {

    static final Logger logger = Logger.getLogger(GuiUtils.class.getName());

    public static String chooseOutputFilename(Component comp) {
        Preferences gvPrefs = Preferences.userNodeForPackage(comp.getClass());
        try {
            LookAndFeel laf = UIManager.getLookAndFeel();
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            String defaultDir = gvPrefs.get("LastGoodOutputDirectory", "");
            JFileChooser fileChooser = new JFileChooser(defaultDir);
            MyFileFilter fileFilter = new MyFileFilter(new String[] { "shp" });
            fileChooser.setFileFilter(fileFilter);
            int returnVal = fileChooser.showOpenDialog(comp);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = null;
                file = fileChooser.getSelectedFile();
                String fileName = file.getAbsolutePath();
                gvPrefs.put("LastGoodOutputDirectory", fileName);
                UIManager.setLookAndFeel(laf);
                return fileName;
            }
            UIManager.setLookAndFeel(laf);
        } catch (Exception ex) {
            gvPrefs.put("LastGoodOutputDirectory", "");
            ex.printStackTrace();
        }
        return null;
    }

    public static String chooseInputFilename(Component comp) {
        Preferences gvPrefs = Preferences.userNodeForPackage(comp.getClass());
        try {
            LookAndFeel laf = UIManager.getLookAndFeel();
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            String defaultDir = gvPrefs.get("LastGoodInputDirectory", "");
            JFileChooser fileChooser = new JFileChooser(defaultDir);
            MyFileFilter fileFilter = new MyFileFilter(new String[] { "shp" });
            fileChooser.setFileFilter(fileFilter);
            int returnVal = fileChooser.showOpenDialog(comp);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = null;
                file = fileChooser.getSelectedFile();
                String fileName = file.getAbsolutePath();
                gvPrefs.put("LastGoodInputDirectory", fileName);
                UIManager.setLookAndFeel(laf);
                return fileName;
            }
            UIManager.setLookAndFeel(laf);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void connectMaps(GeoMapUni mapInput, GeoMapUni mapOutput) {
        mapInput.addIndicationListener(mapOutput);
        mapInput.addSelectionListener(mapOutput);
        mapOutput.addIndicationListener(mapInput);
        mapOutput.addSelectionListener(mapInput);
    }

    public static JPanel createInputPanel(GeoMapUni mapInput, JLabel inputFileNameLabel, JButton chooseInputFileButton, JTextField inputFileTextField) {
        JPanel inputPanel = new JPanel();
        JPanel pickerPanel = new JPanel();
        inputFileNameLabel = new JLabel();
        inputFileNameLabel.setText("Input File Name:");
        chooseInputFileButton.setText("Choose");
        Dimension mapSize = new Dimension(200, 150);
        mapInput.setMinimumSize(mapSize);
        Border border = BorderFactory.createLineBorder(Color.black);
        inputPanel.setBorder(border);
        pickerPanel.add(chooseInputFileButton);
        pickerPanel.add(inputFileNameLabel);
        pickerPanel.add(inputFileTextField);
        Dimension pickerPanelSize = new Dimension(200, 60);
        pickerPanel.setPreferredSize(pickerPanelSize);
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(mapInput, BorderLayout.CENTER);
        inputPanel.add(pickerPanel, BorderLayout.SOUTH);
        return inputPanel;
    }

    public static JPanel createOutputPanel(JLabel outputFileNameLabel, JButton chooseOutputFileButton, JTextField outputFileTextField, String borderTitle) {
        JPanel pickerPanel = new JPanel();
        outputFileNameLabel = new JLabel();
        chooseOutputFileButton.setText("Choose");
        Border border = BorderFactory.createTitledBorder(borderTitle);
        outputFileTextField.setText("A file name");
        pickerPanel.setBorder(border);
        pickerPanel.add(chooseOutputFileButton);
        pickerPanel.add(outputFileNameLabel);
        pickerPanel.add(outputFileTextField);
        return pickerPanel;
    }

    public static void disconnectMaps(GeoMapUni mapInput, GeoMapUni mapOutput) {
        mapInput.removeIndicationListener(mapOutput);
        mapInput.removeSelectionListener(mapOutput);
        mapOutput.removeIndicationListener(mapInput);
        mapOutput.removeSelectionListener(mapInput);
    }

    public static String writeDefaultShapefile() {
        GeoDataGeneralizedStates stateData = new GeoDataGeneralizedStates();
        String fileName = System.getProperty("user.home");
        String inputFileName = fileName + "/states48.shp";
        File newDir = new File(fileName);
        newDir.mkdir();
        MapGenFile.writeShapefile(stateData.getDataForApps().getGeneralPathData(), inputFileName);
        try {
            InputStream dbfStream = GeoDataGeneralizedStates.class.getResourceAsStream("resources/states48.dbf");
            ReadableByteChannel dChan = Channels.newChannel(dbfStream);
            DbaseFileReader dBaseReader = new DbaseFileReader(dChan, true);
            DbaseFileHeader dBaseHeader = dBaseReader.getHeader();
            WritableByteChannel out = new FileOutputStream(fileName + "/states48.dbf").getChannel();
            DbaseFileWriter dBase = new DbaseFileWriter(dBaseHeader, out);
            if (logger.isLoggable(Level.INFO)) {
                logger.info("dBase length = " + dBaseHeader.getNumRecords());
                logger.info("dbasewriter " + dBase.toString());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return inputFileName;
    }

    class VCSelectionExchanger implements ActionListener {

        VisualClassifier vcOne;

        VisualClassifier vcTwo;

        public VCSelectionExchanger(VisualClassifier vcOne, VisualClassifier vcTwo) {
            this.vcOne = vcOne;
            this.vcTwo = vcTwo;
        }

        public void actionPerformed(ActionEvent e) {
            if ((e.getSource() == vcOne) && e.getActionCommand().equals("SelectedVariable")) {
                int index = vcOne.getCurrVariableIndex();
                vcTwo.setCurrVariableIndex(index);
            } else if ((e.getSource() == vcTwo) && e.getActionCommand().equals("SelectedVariable")) {
                int index = vcTwo.getCurrVariableIndex();
                vcOne.setCurrVariableIndex(index);
            }
        }
    }
}
