package de.evaluationtool.gui;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import org.apache.commons.io.FileUtils;
import org.jfree.ui.FilesystemFilter;
import de.evaluationtool.EvaluationTool;
import de.evaluationtool.Link;
import de.evaluationtool.Reference;
import de.evaluationtool.format.ReferenceFormat;
import de.evaluationtool.format.ReferenceFormats;
import de.evaluationtool.gui.EvaluationFrame.SaveXMLMode;

/** @author Konrad Höffner */
class EvaluationFrameActionListener implements ActionListener {

    final EvaluationFrame frame;

    private File geoFile = null;

    public EvaluationFrameActionListener(EvaluationFrame frame) {
        this.frame = frame;
    }

    private void saveXML() {
        JFileChooser chooser = new JFileChooser("Save evaluation result as alignment XML");
        chooser.setCurrentDirectory(frame.defaultDirectory);
        int returnVal = chooser.showSaveDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (chooser.getSelectedFile().exists() && (JOptionPane.showConfirmDialog(frame, "File already exists. Overwrite?") != JOptionPane.YES_OPTION)) {
                return;
            }
            frame.saveXML(chooser.getSelectedFile(), SaveXMLMode.SAVE_EVERYTHING);
        }
    }

    private void saveCSV() {
        JFileChooser chooser = new JFileChooser("Save evaluation result as CSV");
        chooser.setCurrentDirectory(frame.defaultDirectory);
        int returnVal = chooser.showSaveDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (chooser.getSelectedFile().exists() && (JOptionPane.showConfirmDialog(frame, "File already exists. Overwrite?") != JOptionPane.YES_OPTION)) {
                return;
            }
            frame.saveCSV(chooser.getSelectedFile());
        }
    }

    private void saveReferenceXML() {
        JFileChooser chooser = new JFileChooser("Save as alignment xml format. YOUR EVALUATION WILL NOT BE SAVED, ONLY A COPY OF THE INPUT.");
        chooser.setCurrentDirectory(new File("."));
        int returnVal = chooser.showSaveDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (chooser.getSelectedFile().exists() && (JOptionPane.showConfirmDialog(frame, "File already exists. Overwrite?") != JOptionPane.YES_OPTION)) {
                return;
            }
            frame.saveReferenceXML(chooser.getSelectedFile(), true);
        }
    }

    private void savePositiveNegativeNT() throws IOException {
        JFileChooser chooser = new JFileChooser("Save as multiple nt files. Please choose a directory");
        chooser.setCurrentDirectory(frame.defaultDirectory);
        if (geoFile != null) {
            chooser.setCurrentDirectory(geoFile);
        }
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        int returnVal = chooser.showSaveDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            frame.savePositiveNegativeNT(chooser.getSelectedFile());
        }
    }

    private void loadPositiveNegativeNT() throws IOException {
        JFileChooser chooser = new JFileChooser("Load multiple nt files. Please choose a directory");
        chooser.setCurrentDirectory(frame.defaultDirectory);
        if (geoFile != null) {
            chooser.setCurrentDirectory(geoFile);
        }
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        int returnVal = chooser.showSaveDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            System.out.print("Loading...");
            frame.loadPositiveNegativeNT(chooser.getSelectedFile());
            System.out.println("loading finished.");
        }
    }

    private ReferenceFormat formatChooser(Collection<ReferenceFormat> formats) {
        ReferenceFormat[] formatsArray = formats.toArray(new ReferenceFormat[0]);
        String[] options = new String[formats.size()];
        for (int i = 0; i < formats.size(); i++) {
            options[i] = formatsArray[i].getDescription();
        }
        int result = JOptionPane.showOptionDialog(frame, "Please choose a reference format.", "Choose a reference format", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        if (result == JOptionPane.CLOSED_OPTION) return null;
        return formatsArray[result];
    }

    private void loadReference() throws Exception {
        JFileChooser chooser = new JFileChooser("Please choose a reference file or directory.");
        chooser.setCurrentDirectory(frame.defaultDirectory);
        chooser.setFileSelectionMode(ReferenceFormats.REFERENCE_FORMATS.directoryFormats.isEmpty() ? JFileChooser.FILES_ONLY : JFileChooser.FILES_AND_DIRECTORIES);
        for (ReferenceFormat format : ReferenceFormats.REFERENCE_FORMATS.readableFormats) {
            chooser.addChoosableFileFilter(new FilesystemFilter(format.getFileExtension(), format.getDescription()));
        }
        chooser.setAcceptAllFileFilterUsed(true);
        int returnVal = chooser.showOpenDialog(frame);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        ReferenceFormat format = null;
        System.out.print("Loading...");
        frame.setTitle("Loading...");
        File f = chooser.getSelectedFile();
        Collection<ReferenceFormat> formats;
        if (f.isDirectory()) {
            formats = ReferenceFormats.REFERENCE_FORMATS.directoryFormats;
        } else {
            formats = ReferenceFormats.REFERENCE_FORMATS.extensionToFormats.get(f.getName().substring(f.getName().lastIndexOf(".") + 1));
        }
        if (formats.isEmpty()) {
            throw new Exception("No format available that can read this.");
        }
        if (formats.size() == 1) {
            format = formats.iterator().next();
        } else {
            format = formatChooser(formats);
        }
        if (format == null) {
            return;
        }
        Reference reference = format.readReference(chooser.getSelectedFile(), true, frame.loadLimit);
        if (!reference.links.isEmpty()) {
            Link firstLink = reference.links.iterator().next();
            frame.dataSourceName1 = EvaluationFrame.getProbableDatasourceName(firstLink.uris.first);
            frame.dataSourceName2 = EvaluationFrame.getProbableDatasourceName(firstLink.uris.second);
        }
        frame.setReference(reference);
        System.out.println("loading finished, " + reference.links.size() + " links loaded.");
    }

    private void saveReference(boolean mustSupportEvaluation, boolean includeEvaluation) throws FileNotFoundException {
        Set<ReferenceFormat> formats = mustSupportEvaluation ? ReferenceFormats.REFERENCE_FORMATS.evaluationIncludingFormats : ReferenceFormats.REFERENCE_FORMATS.formats;
        formats.retainAll(ReferenceFormats.REFERENCE_FORMATS.writeableFormats);
        ReferenceFormat format = formatChooser(formats);
        if (format == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser("Save reference. Please choose a file.");
        chooser.setCurrentDirectory(frame.defaultDirectory);
        chooser.setFileSelectionMode(format.readsDirectory() ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
        if (format.getFileExtension() != null) {
            chooser.addChoosableFileFilter(new FilesystemFilter(format.getFileExtension(), format.getDescription()));
        } else {
            chooser.setAcceptAllFileFilterUsed(true);
        }
        int returnVal = chooser.showSaveDialog(frame);
        if (returnVal != JFileChooser.APPROVE_OPTION) return;
        System.out.print("Saving...");
        format.writeReference(frame.reference, chooser.getSelectedFile(), includeEvaluation);
        System.out.println("saving finished.");
    }

    private void changeLoadLimit() {
        try {
            String input = JOptionPane.showInputDialog("Change the load limit to (0 means unlimited)", frame.getLoadLimit());
            if (input == null) return;
            frame.setLoadLimit(Integer.valueOf(input));
            FileUtils.writeStringToFile(frame.loadLimitFile, String.valueOf(frame.getLoadLimit()));
        } catch (NumberFormatException e) {
            changeLoadLimit();
        } catch (IOException e) {
            JOptionPane.showConfirmDialog(frame, e);
        }
    }

    private void changeAutoEvalDistance() {
        try {
            String input = JOptionPane.showInputDialog("Change the auto eval distance to ", frame.getAutoEvalDistance());
            if (input == null) return;
            frame.setAutoEvalDistance(Integer.valueOf(input));
            FileUtils.writeStringToFile(frame.autoEvalDistanceFile, String.valueOf(frame.getAutoEvalDistance()));
        } catch (NumberFormatException e) {
            changeLoadLimit();
        } catch (IOException e) {
            JOptionPane.showConfirmDialog(frame, e);
        }
    }

    private void browse(String url) throws URISyntaxException, IOException {
        java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
        java.net.URI uri = null;
        uri = new java.net.URI(url);
        desktop.browse(uri);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        try {
            if (event.getSource() == frame.loadReferenceItem) {
                loadReference();
            }
            if (event.getSource() == frame.saveReferenceOnlyItem) {
                saveReference(false, false);
            }
            if (event.getSource() == frame.saveReferenceAndEvaluationItem) {
                saveReference(true, true);
            }
            if (event.getSource() == frame.removeAllUnderAutoEvalDistanceItem) {
                frame.removeAllUnderAutoEvalDistance();
            }
            if (event.getSource() == frame.reloadLabelsItem) {
                frame.startLabelThread(false, true);
            }
            if (event.getSource() == frame.autoEvalItem) {
                frame.autoEvaluate();
            }
            if (event.getSource() == frame.sortByCorrectnessItem) {
                frame.sortByCorrectness();
            }
            if (event.getSource() == frame.evaluateItem) {
                frame.evaluate();
            }
            if (event.getSource() == frame.evaluateAlignItem) {
                frame.evaluateAlign();
            }
            if (event.getSource() == frame.changeAutoEvalDistanceItem) {
                changeAutoEvalDistance();
            }
            if (event.getSource() == frame.changeLoadLimitItem) {
                changeLoadLimit();
            }
            if (event.getSource() == frame.shrinkToLoadLimitItem) {
                frame.shrinkToLoadLimit();
            }
            if (event.getSource() == frame.javadocMenuItem) {
                browse("file://" + System.getProperty("user.dir") + "/doc/" + EvaluationTool.class.toString().split(" ")[1].replace('.', '/') + ".html");
            }
            if (event.getSource() == frame.manualMenuItem) {
                browse("file:///home/konrad/projekte/www/evaluationtool/index.html");
            }
            if (event.getSource() == frame.saveReferenceXMLItem) {
                saveReferenceXML();
            }
            if (event.getSource() == frame.precisionButton) {
                frame.showPrecision();
            }
            if (event.getSource() == frame.editNameSourceFileItem) {
                editNameSourceFile();
            }
            if (event.getSource() == frame.reloadNameSourceFileItem) {
                reloadNameSourceFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Exception: " + e);
        }
    }

    private void editNameSourceFile() throws IOException, InterruptedException {
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.edit(new File("config/namesources.csv"));
        } catch (Exception e) {
            final Process p = Runtime.getRuntime().exec("gedit config/namesources.csv");
            if (p.waitFor() != 0) {
                final Process q = Runtime.getRuntime().exec("edit config/namesources.csv");
            }
        }
    }

    private void reloadNameSourceFile() {
        frame.stopLabelThread();
        frame.reloadNamesources();
        frame.startLabelThread();
    }
}
