package profile;

import ij.IJ;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jxl.write.WriteException;
import util.ExtensionFileFilter;
import util.SpringUtilities;
import view.IView;
import controller.ProfileController;

/**
 * This class is a JPanel that is on the left hand side of the ProfilePanel. It
 * is a view for controlling the number of MADs shown on the profile graph. It
 * also displays the mean and MAD/mean
 * 
 */
public class ProfileResultLeftPanel extends JPanel implements IView {

    private static final long serialVersionUID = 717747672086520073L;

    Profile profile;

    ProfileController controller;

    private JSpinner madSpinner;

    private SpinnerNumberModel madSpinnerModel;

    private JLabel madLabel = new JLabel("MAD");

    private JLabel meanLabel = new JLabel("Mean");

    private JLabel madMeanLabel = new JLabel("MAD/mean");

    private JFormattedTextField madMeanValueField;

    private JFormattedTextField madValueField;

    private JFormattedTextField meanValueField;

    private JButton exportToExcelButton = new JButton("Export Profile to Excel");

    private JFileChooser fileChooser = new JFileChooser();

    private ExtensionFileFilter excelFilter = new ExtensionFileFilter("Excel spreadsheet (.xls)", "xls");

    private ProfileResultFrame profileResultFrame;

    private String imageFilename;

    public ProfileResultLeftPanel(ProfileController controller, Profile profile, int startingMADs, String imageFilename, ProfileResultFrame profileResultFrame) {
        this.controller = controller;
        this.profile = profile;
        this.imageFilename = imageFilename;
        this.profileResultFrame = profileResultFrame;
        initComponents(startingMADs);
    }

    private void initComponents(int startingMADs) {
        madLabel.setLabelFor(madValueField);
        madValueField = new JFormattedTextField(new Double(profile.getProfileMAD()));
        madValueField.setEditable(false);
        meanLabel.setLabelFor(meanValueField);
        meanValueField = new JFormattedTextField(new Double(profile.getProfileMean()));
        meanValueField.setEditable(false);
        madMeanLabel.setLabelFor(madMeanValueField);
        madMeanValueField = new JFormattedTextField(new Double(profile.getProfileMAD() / profile.getProfileMean()));
        madMeanValueField.setEditable(false);
        madSpinnerModel = new SpinnerNumberModel(startingMADs, 0, 50, 1);
        madSpinner = new JSpinner(madSpinnerModel);
        madSpinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                madSpinnerStateChanged(e);
            }
        });
        this.setLayout(new SpringLayout());
        JLabel madNumLabel = new JLabel("Peak Definition Threshold (MADs)", JLabel.TRAILING);
        madNumLabel.setLabelFor(madSpinner);
        this.add(madLabel);
        this.add(madValueField);
        this.add(meanLabel);
        this.add(meanValueField);
        this.add(madMeanLabel);
        this.add(madMeanValueField);
        this.add(madNumLabel);
        this.add(madSpinner);
        this.add(exportToExcelButton);
        this.add(new JLabel());
        setupActionListeners();
        SpringUtilities.makeCompactGrid(this, 5, 2, 6, 6, 6, 6);
    }

    private void setupActionListeners() {
        exportToExcelButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                fileChooser.setFileFilter(excelFilter);
                String suggestedFilename = imageFilename.substring(0, imageFilename.indexOf("."));
                suggestedFilename += "_profile";
                fileChooser.setSelectedFile(new File(suggestedFilename));
                int returnVal = fileChooser.showSaveDialog(ProfileResultLeftPanel.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    String filename = fileChooser.getSelectedFile().toString();
                    if (!filename.endsWith(".xls")) {
                        filename += ".xls";
                    }
                    File file = new File(filename);
                    if (overwiteExistingFile(file)) {
                        try {
                            ProfileExporter exporter = new ProfileExporter(profile, imageFilename);
                            exporter.exportProfileToExcel((Integer) madSpinner.getValue(), file, profileResultFrame.getProfileChart());
                        } catch (IOException e1) {
                            IJ.error("Save file failed: " + e1.getLocalizedMessage());
                            return;
                        } catch (WriteException we) {
                            IJ.error("Unable to write to excel file: " + we.getLocalizedMessage());
                            return;
                        }
                    }
                }
            }
        });
    }

    /**
     * Checks to see if the file exists.  If the file already exists, then it prompts
     * the user that this will overwite the existing file if the user wants to proceed.
     * If the user hits the cancel button, this returns false.  If the file does not
     * exist or the user pushes OK, it will return true
     * 
     * @param file
     * @return
     */
    private boolean overwiteExistingFile(File file) {
        if (file.exists()) {
            int res = JOptionPane.showConfirmDialog(null, "File " + file.getName() + " already exists.  Click\n" + "OK to overwrite and save.", "File exists", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res == JOptionPane.CANCEL_OPTION) {
                return false;
            }
        }
        return true;
    }

    protected void madSpinnerStateChanged(ChangeEvent e) {
        controller.changeProfileMad((Integer) madSpinner.getValue());
    }

    @Override
    public void modelPropertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ProfileController.PROFILE_MAD_PROPERTY)) {
            Integer newValue = (Integer) evt.getNewValue();
            if (!madSpinner.getValue().equals(newValue)) {
                madSpinner.setValue(newValue);
            }
        }
    }
}
