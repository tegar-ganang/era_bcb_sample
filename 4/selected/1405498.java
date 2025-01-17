package net.sourceforge.toscanaj.gui.action;

import java.awt.Frame;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import net.sourceforge.toscanaj.controller.fca.DiagramController;
import net.sourceforge.toscanaj.controller.fca.DiagramHistory;
import net.sourceforge.toscanaj.gui.dialog.DiagramExportSettingsPanel;
import net.sourceforge.toscanaj.gui.dialog.ErrorDialog;
import net.sourceforge.toscanaj.gui.dialog.ExtensionFileFilter;
import net.sourceforge.toscanaj.view.diagram.DiagramView;
import org.tockit.canvas.imagewriter.DiagramExportSettings;
import org.tockit.canvas.imagewriter.GraphicFormat;
import org.tockit.canvas.imagewriter.GraphicFormatRegistry;
import org.tockit.canvas.imagewriter.ImageGenerationException;
import org.tockit.canvas.imagewriter.ImageWriter;

public class ExportDiagramAction extends KeyboardMappedAction {

    private final DiagramExportSettings diagramExportSettings;

    private final DiagramView diagramView;

    private final DiagramExportSettingsPanel exportSettingsPanel;

    /**
     * If you don't want to specify mnemonics then use the other constructor.
     */
    public ExportDiagramAction(final Frame frame, final DiagramExportSettings diagExpSettings, final DiagramView diagramView) {
        super(frame, "Export Diagram...");
        this.diagramExportSettings = diagExpSettings;
        this.diagramView = diagramView;
        this.exportSettingsPanel = new DiagramExportSettingsPanel(this.diagramExportSettings);
    }

    public ExportDiagramAction(final Frame frame, final DiagramExportSettings diagExpSettings, final DiagramView diagramView, final int mnemonic, final KeyStroke keystroke) {
        super(frame, "Export Diagram...", mnemonic, keystroke);
        this.diagramExportSettings = diagExpSettings;
        this.diagramView = diagramView;
        this.exportSettingsPanel = new DiagramExportSettingsPanel(this.diagramExportSettings);
    }

    public void actionPerformed(final ActionEvent e) {
        exportImage();
    }

    public void exportImage() {
        final CustomJFileChooser saveDialog;
        final File lastImageExportFile = this.diagramExportSettings.getLastImageExportFile();
        if (lastImageExportFile != null) {
            saveDialog = new CustomJFileChooser(lastImageExportFile.getParentFile());
        } else {
            saveDialog = new CustomJFileChooser(null);
        }
        FileFilter defaultFilter = saveDialog.getFileFilter();
        final Iterator formatIterator = GraphicFormatRegistry.getIterator();
        while (formatIterator.hasNext()) {
            final GraphicFormat graphicFormat = (GraphicFormat) formatIterator.next();
            final ExtensionFileFilter fileFilter = new ExtensionFileFilter(graphicFormat.getExtensions(), graphicFormat.getName());
            saveDialog.addChoosableFileFilter(fileFilter);
            if (graphicFormat == this.diagramExportSettings.getGraphicFormat()) {
                defaultFilter = fileFilter;
            }
        }
        saveDialog.setFileFilter(defaultFilter);
        saveDialog.setAccessory(this.exportSettingsPanel);
        boolean formatDefined;
        do {
            formatDefined = true;
            final int rv = saveDialog.showSaveDialog(this.frame);
            if (rv == JFileChooser.APPROVE_OPTION) {
                File selectedFile = saveDialog.getSelectedFile();
                final ExtensionFileFilter extFileFilter = (ExtensionFileFilter) saveDialog.getFileFilter();
                if (selectedFile.getName().indexOf('.') == -1) {
                    final String[] extensions = extFileFilter.getExtensions();
                    selectedFile = new File(selectedFile.getAbsolutePath() + "." + extensions[0]);
                }
                final GraphicFormat gFormat = GraphicFormatRegistry.getTypeByName(extFileFilter.getFileTypeName());
                if (gFormat != null) {
                    this.diagramExportSettings.setGraphicFormat(gFormat);
                } else {
                    if (selectedFile.getName().indexOf('.') != -1) {
                        JOptionPane.showMessageDialog(this.frame, "Sorry, no type with this extension known.\n" + "Please use either another extension or try\n" + "manual settings.", "Export failed", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this.frame, "No extension given.\n" + "Please give an extension or pick a file type\n" + "from the options.", "Export failed", JOptionPane.ERROR_MESSAGE);
                    }
                    formatDefined = false;
                }
                if (formatDefined) {
                    exportImage(selectedFile);
                }
            }
        } while (formatDefined == false);
    }

    private void exportImage(final File selectedFile) {
        try {
            String title = "";
            String description = "";
            final String lineSeparator = System.getProperty("line.separator");
            final DiagramController diagramController = DiagramController.getController();
            final DiagramHistory diagramHistory = diagramController.getDiagramHistory();
            if (diagramHistory.getNumberOfCurrentDiagrams() != 0) {
                final int numCurDiag = diagramHistory.getNumberOfCurrentDiagrams();
                final int firstCurrentPos = diagramHistory.getFirstCurrentDiagramPosition();
                for (int i = 0; i < numCurDiag; i++) {
                    title += diagramHistory.getElementAt(i + firstCurrentPos).toString();
                    if (i < numCurDiag - 1) {
                        title += " / ";
                    }
                    if ((i == numCurDiag - 1) && numCurDiag > 1) {
                        title += " ( Outer diagram / Inner diagram )";
                    }
                }
                description = diagramController.getDiagramHistory().getTextualDescription();
            } else {
                title = this.diagramView.getDiagram().getTitle();
            }
            final Properties metadata = new Properties();
            metadata.setProperty("title", title);
            metadata.setProperty("description", description.trim());
            final ImageWriter writer = this.diagramExportSettings.getGraphicFormat().getWriter();
            writer.exportGraphic(this.diagramView, this.diagramExportSettings, selectedFile, metadata);
            if (this.diagramExportSettings.getSaveCommentsToFile() == true) {
                try {
                    final PrintWriter out = new PrintWriter(new FileWriter(new File(selectedFile.getAbsolutePath() + ".txt")));
                    out.println("The diagram(s) you have viewed for the resulting image: " + lineSeparator + selectedFile.getAbsolutePath());
                    final DateFormat dateFormatter = DateFormat.getDateTimeInstance();
                    out.println("as at " + dateFormatter.format(new Date(System.currentTimeMillis())) + " is(are): ");
                    out.println();
                    out.println(description);
                    out.close();
                } catch (final IOException e) {
                    ErrorDialog.showError(this.frame, e, "Exporting text file error");
                }
            }
            if (this.diagramExportSettings.getSaveCommentToClipboard() == true) {
                final DateFormat dateFormatter = DateFormat.getDateTimeInstance();
                final String header = "The diagram(s) you have viewed for the resulting image:\n" + selectedFile.getAbsolutePath() + "\n" + "as at " + dateFormatter.format(new Date(System.currentTimeMillis())) + " is(are): \n";
                final StringSelection comments = new StringSelection(header + "\n" + description);
                final Clipboard systemClipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
                systemClipboard.setContents(comments, null);
            }
        } catch (final ImageGenerationException e) {
            ErrorDialog.showError(this.frame, e, "Exporting image error");
        } catch (final OutOfMemoryError e) {
            ErrorDialog.showError(this.frame, e, "Out of memory", "Not enough memory available to export\n" + "the diagram in this size");
        }
        this.diagramExportSettings.setLastImageExportFile(selectedFile);
    }

    /**
     * The custom file chooser will check whether the file exists and shows the
     * appropriate warning message (if applicable)
     * 
     */
    private class CustomJFileChooser extends JFileChooser {

        private CustomJFileChooser(final File selectedFile) {
            super(selectedFile);
        }

        @Override
        public void approveSelection() {
            ExportDiagramAction.this.exportSettingsPanel.saveSettings();
            File selectedFile = getSelectedFile();
            if (selectedFile.getName().indexOf('.') == -1) {
                final FileFilter filter = getFileFilter();
                if (filter instanceof ExtensionFileFilter) {
                    final ExtensionFileFilter extFileFilter = (ExtensionFileFilter) filter;
                    final String[] extensions = extFileFilter.getExtensions();
                    selectedFile = new File(selectedFile.getAbsolutePath() + "." + extensions[0]);
                    setSelectedFile(selectedFile);
                }
            }
            if (selectedFile.exists()) {
                String warningMessage = "The image file '" + selectedFile.getName() + "' already exists.\nDo you want to overwrite the existing file?";
                if (ExportDiagramAction.this.diagramExportSettings.getSaveCommentsToFile() == true) {
                    final File textFile = new File(selectedFile.getAbsoluteFile() + ".txt");
                    if (textFile.exists()) {
                        warningMessage = "The files '" + selectedFile.getName() + "' and '" + textFile.getName() + "' already exist.\nDo you want to overwrite the existing files?";
                    }
                }
                final int response = JOptionPane.showOptionDialog(this, warningMessage, "File Export Warning: File exists", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[] { "Yes", "No" }, "No");
                if (response != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            if (!selectedFile.exists() && ExportDiagramAction.this.diagramExportSettings.getSaveCommentsToFile() == true) {
                final File textFile = new File(selectedFile.getAbsoluteFile() + ".txt");
                if (textFile.exists()) {
                    final int response = JOptionPane.showOptionDialog(this, "The text file '" + textFile.getName() + "' already exists.\n" + "Do you want to overwrite the existing file?", "File Export Warning: File exists", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[] { "Yes", "No" }, "No");
                    if (response != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
            }
            super.approveSelection();
        }
    }
}
