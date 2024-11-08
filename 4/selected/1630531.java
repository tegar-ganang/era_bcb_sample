package handlers;

import gui.ErrorBox;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import main.OpenPlotTool;
import net.smplmathparser.MathParserException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import plot.PlotPage;
import xml.CartesianXMLBuilder;
import xml.CartesianXMLLoader;
import xml.PieChartXMLBuilder;
import xml.PieChartXMLLoader;
import xml.XMLFileHandler;

public class PageFileHandler {

    private static String getFileExtension(File file) {
        String fileName = file.getName();
        String extension = (fileName.lastIndexOf(".") == -1) ? "" : fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
        return extension;
    }

    public static void openPageFile() {
        JFileChooser filechooser = new JFileChooser();
        filechooser.showOpenDialog(OpenPlotTool.getMainFrame());
        File selectedFile = filechooser.getSelectedFile();
        if (selectedFile != null) {
            boolean openFile = true;
            if (PageHandler.pageFileCheck(selectedFile)) {
                int chosen = JOptionPane.showConfirmDialog(OpenPlotTool.getMainFrame(), "The file you are trying to open is already open.\n" + "Do you wish to open it again?", "File already open", JOptionPane.YES_NO_OPTION);
                if (chosen == JOptionPane.NO_OPTION) {
                    openFile = false;
                }
            }
            if (openFile) {
                PlotPage loadedPage = performOpen(selectedFile);
                if (loadedPage != null) {
                    PageHandler.addPlotPage(loadedPage);
                }
            }
        }
    }

    private static PlotPage performOpen(File file) {
        Document xmlDocument = null;
        boolean tryPlain = false;
        try {
            xmlDocument = XMLFileHandler.openCompressedFile(file);
        } catch (IOException e) {
            if (e.getMessage().equals("Not in GZIP format")) {
                tryPlain = true;
            } else {
                JOptionPane.showMessageDialog(OpenPlotTool.getMainFrame(), "IO error while reading file.", "Failed to open file", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        } catch (SAXException e) {
            JOptionPane.showMessageDialog(OpenPlotTool.getMainFrame(), "SAX error while reading file.\n" + "GZIP compressed non XML file", "Failed to open file", JOptionPane.ERROR_MESSAGE);
            return null;
        } catch (ParserConfigurationException e) {
            JOptionPane.showMessageDialog(OpenPlotTool.getMainFrame(), "XML Parser error while reading file.", "Failed to open file", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (xmlDocument == null && tryPlain) {
            try {
                xmlDocument = XMLFileHandler.openPlainFile(file);
            } catch (SAXException e) {
                JOptionPane.showMessageDialog(OpenPlotTool.getMainFrame(), "SAX error while reading file.\n" + "Non XML file", "Failed to open file", JOptionPane.ERROR_MESSAGE);
                return null;
            } catch (IOException e) {
                JOptionPane.showMessageDialog(OpenPlotTool.getMainFrame(), "IO error while reading file.", "Failed to open file", JOptionPane.ERROR_MESSAGE);
                return null;
            } catch (ParserConfigurationException e) {
                JOptionPane.showMessageDialog(OpenPlotTool.getMainFrame(), "XML Parser error while reading file.", "Failed to open file", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        if (xmlDocument != null) {
            if (XMLFileHandler.getFileVersion(xmlDocument) == -1) {
                JOptionPane.showMessageDialog(OpenPlotTool.getMainFrame(), "Document format error while reading file.\n" + "XML document is not in correct format.", "Failed to open file", JOptionPane.ERROR_MESSAGE);
                return null;
            } else if (XMLFileHandler.getFileVersion(xmlDocument) != 0.3) {
                int chosen = JOptionPane.showConfirmDialog(OpenPlotTool.getMainFrame(), "The file you are trying to open is in an old format.\n" + "Do you want to try open the file anyway?", "Old file version", JOptionPane.YES_NO_OPTION);
                if (chosen == JOptionPane.NO_OPTION) {
                    return null;
                }
            }
            String plotType = XMLFileHandler.getPlotType(xmlDocument);
            PlotPage loadedPage = null;
            if (plotType.equals("cartesianxyplot")) {
                try {
                    loadedPage = CartesianXMLLoader.loadFile(xmlDocument);
                } catch (MathParserException e) {
                    JOptionPane.showMessageDialog(OpenPlotTool.getMainFrame(), "Math Parser error while creating page.", "Failed to create page", JOptionPane.ERROR_MESSAGE);
                    return loadedPage;
                }
            } else if (plotType.equals("piechartplot")) {
                loadedPage = PieChartXMLLoader.loadFile(xmlDocument);
            }
            if (loadedPage != null) {
                loadedPage.setPageFile(file);
                loadedPage.setType(plotType);
                return loadedPage;
            } else {
                JOptionPane.showMessageDialog(OpenPlotTool.getMainFrame(), "Error while creating page.", "Failed to create page", JOptionPane.ERROR_MESSAGE);
                return loadedPage;
            }
        } else {
            JOptionPane.showMessageDialog(OpenPlotTool.getMainFrame(), "Unknown error while reading file.", "Failed to open file", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private static void performSave(PlotPage page, File saveFile) {
        StringBuffer xmlDocumentBuffer = null;
        if (page.getType().equals("cartesianxyplot")) {
            xmlDocumentBuffer = CartesianXMLBuilder.buildXML(page);
        } else if (page.getType().equals("piechartplot")) {
            xmlDocumentBuffer = PieChartXMLBuilder.buildXML(page);
        }
        try {
            if (getFileExtension(saveFile).equals("opc")) {
                XMLFileHandler.saveCompressedFile(xmlDocumentBuffer, saveFile);
            } else if (getFileExtension(saveFile).equals("opp")) {
                XMLFileHandler.savePlainFile(xmlDocumentBuffer, saveFile);
            } else if (PreferenceHandler.getSettings().useCompressedFiles()) {
                XMLFileHandler.saveCompressedFile(xmlDocumentBuffer, saveFile);
            } else {
                XMLFileHandler.savePlainFile(xmlDocumentBuffer, saveFile);
            }
        } catch (FileNotFoundException e) {
            ErrorBox errorDialog = new ErrorBox(OpenPlotTool.getMainFrame(), "Failed to save page", e);
            errorDialog.setVisible(true);
        } catch (IOException e) {
            ErrorBox errorDialog = new ErrorBox(OpenPlotTool.getMainFrame(), "Failed to save page", e);
            errorDialog.setVisible(true);
        }
    }

    public static void revertPageFile() {
        PlotPage page = (PlotPage) OpenPlotTool.getMainFrame().getPlotPanel().getSelectedComponent();
        int index = OpenPlotTool.getMainFrame().getPlotPanel().getSelectedIndex();
        if (page != null) {
            if (page.getPageFile() != null) {
                PlotPage loadedPage = performOpen(page.getPageFile());
                if (loadedPage != null) {
                    PageHandler.replacePage(loadedPage, index);
                } else {
                }
            } else {
                JOptionPane.showMessageDialog(OpenPlotTool.getMainFrame(), "Page has not been saved to a file.", "Failed to revert page", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void saveAsPageFile() {
        PlotPage page = (PlotPage) OpenPlotTool.getMainFrame().getPlotPanel().getSelectedComponent();
        int index = OpenPlotTool.getMainFrame().getPlotPanel().getSelectedIndex();
        if (page != null) {
            JFileChooser fc = new JFileChooser();
            fc.showSaveDialog(OpenPlotTool.getMainFrame());
            File saveFile = fc.getSelectedFile();
            if (saveFile != null) {
                String fileExtension = PreferenceHandler.getSettings().useCompressedFiles() ? "opc" : "opp";
                if (fileExtension.equals(getFileExtension(saveFile))) {
                } else if (getFileExtension(saveFile).equals("") && PreferenceHandler.getSettings().isAddFileExtensions()) {
                    saveFile = new File(saveFile.getAbsoluteFile() + "." + fileExtension);
                } else if (PreferenceHandler.getSettings().isCheckFileExtensions()) {
                    int chosen = JOptionPane.showConfirmDialog(OpenPlotTool.getMainFrame(), "The file extension you have used is not the recommened one.\n" + "Do you want to save with that extension anyway?", "Wrong File Extension", JOptionPane.YES_NO_OPTION);
                    if (chosen == JOptionPane.NO_OPTION) {
                        return;
                    }
                }
                if (saveFile.exists() && !(page.getPageFile().equals(saveFile))) {
                    int chosen = JOptionPane.showConfirmDialog(OpenPlotTool.getMainFrame(), "A file with the name you have given alreadt exists.\n" + "Do you wish to overwrite this file?", "Overwrite File", JOptionPane.YES_NO_OPTION);
                    if (chosen == JOptionPane.NO_OPTION) {
                        return;
                    }
                }
                performSave(page, saveFile);
                page.setPageFile(saveFile);
                OpenPlotTool.getMainFrame().getPlotPanel().setTitleAt(index, saveFile.getName());
                OpenPlotTool.getMainFrame().getPlotPanel().setToolTipTextAt(index, saveFile.getAbsolutePath());
                PageHandler.updatePageChange();
            }
        }
    }

    public static void savePageFile() {
        PlotPage page = (PlotPage) OpenPlotTool.getMainFrame().getPlotPanel().getSelectedComponent();
        if (page != null) {
            File saveFile = page.getPageFile();
            if (saveFile == null) {
                saveAsPageFile();
            } else {
                performSave(page, saveFile);
            }
        }
    }
}
