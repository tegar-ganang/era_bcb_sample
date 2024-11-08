package it.southdown.avana.ui;

import it.southdown.avana.alignment.*;
import it.southdown.avana.appcontrol.AppPreferences;
import it.southdown.avana.appcontrol.AvanaController;
import it.southdown.avana.metadata.*;
import it.southdown.avana.ui.util.*;
import it.southdown.avana.util.FileUtilities;
import it.southdown.avana.util.SequenceUtilities;
import java.io.File;
import javax.swing.*;

public class SubsetManager {

    private static AvanaGUI app = AvanaGUI.instance();

    private static AvanaController main = app.getMainController();

    /**
     * Create a subset of an existing alignment, by prompting the user to make a 
     * metadata-based selection.  The default alignment presented to the user is the
     * master alignment, but the user can change the choice.
     * 
     * @return SubsetAlignment the subset alignment derived from the user choices
     */
    public SubsetAlignment createSubsetAlignment() {
        return createSubsetAlignment(null);
    }

    /**
     * Create a subset of an existing alignment, by prompting the user to make a 
     * metadata-based selection. The default alignment presented to the user is 
     * specified as a parameter, but the user can change the choice. If the
     * alignment specified is null, then the default alignment is the master 
     * alignment.
     * 
     * @param alignment the default alignment supplying the metadata
     * @return the subset alignment derived from the user choices
     */
    public SubsetAlignment createSubsetAlignment(Alignment alignment) {
        if (alignment == null) {
            if (!main.getAlignmentManager().isAlignmentLoaded()) {
                JOptionPane.showMessageDialog(app.getWindow(), "No master alignment has been loaded", "Error creating subset", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            alignment = main.getAlignmentManager().getMasterAlignment();
        }
        MetadataValueMatchConstraint selection = getUserSelection(alignment);
        SubsetAlignment newAlignment = makeNewSubsetAlignment(selection);
        return newAlignment;
    }

    /**
     * Recreate a subset of an existing alignment, by prompting the user to make a 
     * metadata-based selection, editing previous choices.
     * 
     * @param subsetAlignment the alignment to be edited
     * @return the subset alignment derived from the user choices
     */
    public SubsetAlignment editSubsetAlignment(SubsetAlignment subsetAlignment) {
        SubsetMetadataSelection subsetSel = (SubsetMetadataSelection) subsetAlignment.getSubsetSelection();
        MetadataValueMatchConstraint selection = (MetadataValueMatchConstraint) subsetSel.getMetadataConstraint();
        SubsetAlignment newAlignment = null;
        MetadataValueMatchConstraint newSelection = getUserSelection(selection);
        if (newSelection != null) {
            newAlignment = makeNewSubsetAlignment(newSelection);
        }
        return newAlignment;
    }

    /**
     * Prompt the user for metadata selection, based on the metadata of an existing alignment.
     * The default alignment presented to the user is specified as a parameter, but the user 
     * can change the choice.
     * 
     * @param alignment the default alignment supplying the metadata
     * @return the user choices (metadata selections), specifying the alignment used as a source
     */
    private MetadataValueMatchConstraint getUserSelection(Alignment alignment) {
        return getUserSelection(new MetadataValueMatchConstraint(alignment));
    }

    /**
     * Prompt the user for metadata selection, based on the metadata of an existing alignment.
     * The dialog is initialized with defaults as specified as a parameter, but the user 
     * can change the alignment and the choices.
     * 
     * @param selection the initial metadata selection
     * @return the user choices (metadata selections), specifying the alignment used as a source
     */
    private MetadataValueMatchConstraint getUserSelection(MetadataValueMatchConstraint selection) {
        MetadataSelectorDialog dlg = new MetadataSelectorDialog();
        if (selection != null) {
            dlg.setUserSelection(selection);
        }
        dlg.pack();
        dlg.setVisible(true);
        if (dlg.isCancelled()) {
            return null;
        }
        MetadataValueMatchConstraint userSelection = dlg.getUserSelection();
        if (userSelection != null) {
            userSelection.generateNameIfNeeded();
        }
        return userSelection;
    }

    private SubsetAlignment makeNewSubsetAlignment(MetadataValueMatchConstraint selection) {
        if (selection == null) {
            return null;
        }
        SubsetMetadataSelection subsetSelection = new SubsetMetadataSelection(selection);
        if (subsetSelection.isEmpty()) {
            JOptionPane.showMessageDialog(app.getWindow(), "No sequences match the selected criteria.", "Subset cannot be created", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        SubsetAlignment newAlignment = main.getAlignmentManager().createSubsetAlignment(subsetSelection);
        return newAlignment;
    }

    public void exportAlignment(Alignment alignment) {
        if (alignment == null) {
            return;
        }
        File outFile = selectOutputFile(alignment, alignment.getDefaultOutputFilename(), new ExtensionFilter("Fasta", new String[] { "afa" }));
        if (outFile == null) {
            return;
        }
        String content = SequenceUtilities.makeFastaAlignment(alignment);
        try {
            FileUtilities.saveContent(outFile, content);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(app.getWindow(), "Error extracting subalignment", e.getMessage(), JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    public void exportMetadata(Alignment alignment) {
        if (alignment == null) {
            return;
        }
        String defaultFilename = alignment.getName() + ".meta.csv";
        File outFile = selectOutputFile(alignment, defaultFilename, new ExtensionFilter("CSV file", new String[] { "csv" }));
        if (outFile == null) {
            return;
        }
        Metadata meta = alignment.getMetadata();
        MetadataSerializer ms = new MetadataSerializer();
        String content = ms.serializeMetadata(meta);
        try {
            FileUtilities.saveContent(outFile, content);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(app.getWindow(), "Error extracting metadata", e.getMessage(), JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    private File selectOutputFile(Alignment alignment, String defaultFilename, ExtensionFilter filter) {
        File currentDir = new File(".");
        String lastFilePath = AppPreferences.store.get(AppPreferences.PROP_LAST_EXTRACT_OUTPUT_FILE, null);
        if (lastFilePath == null) {
            lastFilePath = AppPreferences.store.get(AppPreferences.PROP_LAST_ALIGNMENT_FILE, null);
        }
        if (lastFilePath != null) {
            File lastFile = new File(lastFilePath);
            if (lastFile.getParentFile().exists()) {
                currentDir = lastFile.getParentFile();
            }
        }
        File outFile = null;
        while (outFile == null) {
            File currentFile = new File(currentDir, defaultFilename);
            JFileChooser fc = new JFileChooser(currentDir);
            fc.addChoosableFileFilter(filter);
            fc.setAcceptAllFileFilterUsed(false);
            fc.setSelectedFile(currentFile);
            int retValue = fc.showSaveDialog(app.getWindow());
            if (retValue != JFileChooser.APPROVE_OPTION) {
                return null;
            }
            outFile = fc.getSelectedFile();
            if ((outFile != null) && (outFile.exists())) {
                int option = JOptionPane.showConfirmDialog(app.getWindow(), "File " + outFile + " already exists. Are you sure you want to overwrite?", "Confirm file overwrite", JOptionPane.YES_NO_OPTION);
                if (option != JOptionPane.YES_OPTION) {
                    outFile = null;
                }
            }
        }
        return outFile;
    }
}
