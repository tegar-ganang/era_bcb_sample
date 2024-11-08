package org.skycastle.scratchpad.sketch.actions;

import org.skycastle.scratchpad.sketch.SketchController;
import org.skycastle.scratchpad.sketch.formats.Format;
import org.skycastle.scratchpad.sketch.model.Sketch;
import org.skycastle.util.ParameterChecker;
import org.skycastle.util.command.AbstractCommand;
import org.skycastle.util.command.Command;
import org.skycastle.util.command.CommandAction;
import org.skycastle.util.command.CommandStack;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Saves the current sketch with a selected filename.
 *
 * @author Hans Haggstrom
 */
public final class SaveAsAction extends CommandAction {

    private final List<Format> myFormats;

    private Frame myMainFrame;

    private File myMostRecentDirectory = null;

    private SketchController mySketchController;

    private Format myFormat = null;

    private JComboBox myFormatComboBox = null;

    private JPanel myFormatOptionsPanel = null;

    private JTextArea myFormatDescriptionArea;

    private static final Logger LOGGER = Logger.getLogger(SaveAsAction.class.getName());

    private static final int BORDER_SIZE = 12;

    public SaveAsAction(final SketchController sketchController, final CommandStack commandStack, final Frame mainFrame, final List<? extends Format> formats) {
        super(commandStack, "Save as", "Saves the currect sketch in a specified file format");
        ParameterChecker.checkNotNull(sketchController, "sketchController");
        ParameterChecker.checkNotNull(mainFrame, "mainFrame");
        ParameterChecker.checkNotNull(formats, "formats");
        mySketchController = sketchController;
        myMainFrame = mainFrame;
        myFormats = new ArrayList<Format>();
        for (Format format : formats) {
            if (format.isSaveSupported()) {
                myFormats.add(format);
            }
        }
        ParameterChecker.checkNotEmpty(myFormats, "formats that can be saved");
        myFormat = myFormats.get(0);
    }

    /**
     * @return the currently selected format.
     */
    public Format getFormat() {
        return myFormat;
    }

    public final void setFormat(final Format format) {
        if (myFormat != format) {
            myFormat = format;
            myFormatComboBox.setSelectedItem(format);
            myFormatOptionsPanel.removeAll();
            myFormatOptionsPanel.add(format.getSaveOptionsUi(), BorderLayout.CENTER);
            myFormatOptionsPanel.validate();
            myFormatOptionsPanel.repaint();
            myFormatDescriptionArea.setText(format.getDescription());
        }
    }

    @Override
    protected Command createCommand() {
        return new AbstractCommand("Save as", false, false) {

            public void doCommand() {
                boolean okToSave = false;
                while (!okToSave) {
                    okToSave = true;
                    final BufferedImage snapshot;
                    try {
                        snapshot = mySketchController.getView().createSnapshot();
                    } catch (Exception e) {
                        showSaveError(null, e, "Problem creating a snapshot of the current view", "Could not create a snapshot of the view");
                        return;
                    }
                    JFileChooser chooser = new JFileChooser(myMostRecentDirectory);
                    chooser.setAccessory(createFormatSelectionUi());
                    int returnVal = chooser.showSaveDialog(myMainFrame);
                    myMostRecentDirectory = chooser.getCurrentDirectory();
                    if (returnVal == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                        File destinationFile = chooser.getSelectedFile();
                        destinationFile = appendExtensionIfMissing(destinationFile);
                        if (destinationFile.exists()) {
                            switch(askForOverwrite(destinationFile)) {
                                case JOptionPane.YES_OPTION:
                                    okToSave = true;
                                    break;
                                case JOptionPane.NO_OPTION:
                                    okToSave = false;
                                    break;
                                case JOptionPane.CANCEL_OPTION:
                                    return;
                                default:
                                    throw new IllegalStateException();
                            }
                        }
                        if (okToSave) {
                            save(destinationFile, snapshot);
                        }
                    } else {
                        return;
                    }
                }
            }
        };
    }

    private File appendExtensionIfMissing(File destinationFile) {
        final String extension = "." + getFormat().getExtension();
        if (!destinationFile.getName().endsWith(extension)) {
            destinationFile = new File(destinationFile.getPath() + extension);
        }
        return destinationFile;
    }

    private JPanel createFormatSelectionUi() {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(300, 200));
        panel.add(createFormatSelectionComboPanel(), BorderLayout.NORTH);
        myFormatOptionsPanel = new JPanel(new BorderLayout());
        panel.add(myFormatOptionsPanel, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(0, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
        return panel;
    }

    private JPanel createFormatSelectionComboPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel("Format:"), BorderLayout.WEST);
        myFormatComboBox = new JComboBox(myFormats.toArray());
        p.add(myFormatComboBox, BorderLayout.CENTER);
        myFormatComboBox.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                setFormat((Format) myFormatComboBox.getSelectedItem());
            }
        });
        myFormatDescriptionArea = createInformationTextArea(getFormat().getDescription(), p.getBackground());
        p.add(myFormatDescriptionArea, BorderLayout.SOUTH);
        myFormatComboBox.setBorder(BorderFactory.createEmptyBorder(0, BORDER_SIZE, 0, 0));
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, BORDER_SIZE, 0));
        return p;
    }

    private JTextArea createInformationTextArea(final String text, final Color backgroundColor) {
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(backgroundColor);
        textArea.setFocusable(false);
        return textArea;
    }

    private void save(final File destinationFile, final BufferedImage snapshot) {
        final Sketch sketch = mySketchController.getSketch();
        Format format = getFormat();
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(destinationFile));
            format.save(out, sketch, mySketchController, snapshot);
        } catch (FileNotFoundException e) {
            showSaveError(destinationFile, e, "Could not create the output file ", "Could not save sketch");
        } catch (IOException e) {
            showSaveError(destinationFile, e, "Problem when saving the output file ", "Problem when saveing the sketch");
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error when closing output file '" + destinationFile + "': " + e.getMessage(), e);
                }
            }
        }
    }

    private void showSaveError(final File destinationFile, final Exception e, final String message, final String title) {
        JOptionPane.showMessageDialog(myMainFrame, message + "'" + destinationFile.getName() + "':  " + e.getClass() + ": " + e.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }

    private int askForOverwrite(final File destinationFile) {
        return JOptionPane.showConfirmDialog(myMainFrame, "The file '" + destinationFile.getName() + "' already exists.  Do you want to overwrite it?", "Overwrite file?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
    }
}
