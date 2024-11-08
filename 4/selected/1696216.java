package saci.reptil.writer;

import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import javax.swing.filechooser.FileFilter;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import saci.reptil.ReportManager;

/**
 *
 * @author  saci
 */
public class Exporter {

    private String file;

    private ReportManager reportManager;

    public Exporter(ReportManager reportManager) throws UserCancelException {
        if (reportManager == null) {
            throw new NullPointerException();
        }
        this.reportManager = reportManager;
        while (!showDialog()) {
            showDialog();
        }
    }

    public void save() throws IOException, PrinterException {
        if (file != null) {
            if (file.toUpperCase().endsWith(".PDF")) {
                new PdfExporter(reportManager, file).save();
            }
        } else {
            throw new NullPointerException();
        }
    }

    private boolean showDialog() throws UserCancelException {
        JFileChooser f = new JFileChooser();
        f.setFileFilter(new FileFilter() {

            public boolean accept(File f) {
                if (f.getPath().toUpperCase().endsWith(".PDF") || f.isDirectory()) {
                    return true;
                } else {
                    return false;
                }
            }

            public String getDescription() {
                return "PDF files (*.pdf)";
            }
        });
        f.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (f.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            FileFilter ff = f.getFileFilter();
            if (ff.getDescription().startsWith("PDF")) {
                if (!f.getSelectedFile().toString().toUpperCase().endsWith(".PDF")) {
                    f.setSelectedFile(new File(f.getSelectedFile().toString() + ".pdf"));
                }
            }
            if (!f.getSelectedFile().exists() || (f.getSelectedFile().exists() && JOptionPane.showConfirmDialog(null, "The file already exists, overwrite?", "Already exists", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)) {
                this.file = f.getSelectedFile().toString();
            }
        } else {
            throw new UserCancelException();
        }
        return this.file != null;
    }
}
