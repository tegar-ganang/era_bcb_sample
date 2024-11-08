package handlers;

import gui.ExportDialog;
import gui.NewPlotDialog;
import gui.PlotFileFilter;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import main.OpenPlotTool;
import plot.Plot;
import plot.PlotFactory;
import utils.FileUtils;
import view.PlotView;
import view.ViewManager;

public class FileHandler {

    private static FileHandler instance = null;

    public static FileHandler getInstance() {
        if (instance == null) {
            instance = new FileHandler();
        }
        return instance;
    }

    protected FileHandler() {
    }

    public void closeAllPlots() {
        ViewManager.closeAllViews();
    }

    public void closePlot() {
        if (ViewManager.getSelectedPlot().isEdited()) {
            int n = JOptionPane.showConfirmDialog(OpenPlotTool.getInstance().getMainFrame(), "This file has changes that have not been saved.\n" + "Are you sure you still wish to close?", "Close Plot?", JOptionPane.YES_NO_OPTION);
            if (n == JOptionPane.NO_OPTION) {
                return;
            }
        }
        ViewManager.closeView();
    }

    public void exportPlot() {
        if (ViewManager.getSelectedPlot() != null) {
            ExportDialog exportDialog = new ExportDialog();
            exportDialog.setVisible(true);
            if (exportDialog.getPressed() == ExportDialog.EXPORT_PRESSED) {
                Plot selectedPlot = ViewManager.getSelectedPlot();
                File file = exportDialog.getFile();
                String format = exportDialog.getFormat();
                if (file != null) {
                    int width = selectedPlot.getWidth();
                    int height = selectedPlot.getHeight();
                    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2 = image.createGraphics();
                    selectedPlot.paint(g2);
                    g2.dispose();
                    try {
                        ImageIO.write(image, format, file);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(OpenPlotTool.getInstance().getMainFrame(), "Error while exporting to file.\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(OpenPlotTool.getInstance().getMainFrame(), "Invalid export file location.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public void newPlot() {
        NewPlotDialog newPlotDialog = new NewPlotDialog();
        newPlotDialog.setVisible(true);
        if (newPlotDialog.getPressed() == NewPlotDialog.OK_PRESSED) {
            String plotType = newPlotDialog.getSelected();
            Plot newPlot = PlotFactory.getPlot(plotType);
            if (newPlot != null) {
                PlotView newView = new PlotView(newPlot);
                ViewManager.addView(newView);
            } else {
                System.out.println("Invalid plot type.");
            }
        }
    }

    public void openPlot() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.addChoosableFileFilter(new PlotFileFilter());
        int returnValue = fileChooser.showOpenDialog(OpenPlotTool.getInstance().getMainFrame());
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.isFile()) {
                try {
                    ObjectInputStream objectInput = new ObjectInputStream(new FileInputStream(selectedFile));
                    Plot loadedPlot = (Plot) objectInput.readObject();
                    loadedPlot.setPlotFile(selectedFile);
                    PlotView newView = new PlotView(loadedPlot);
                    ViewManager.addView(newView);
                    ViewManager.updateTabText();
                } catch (FileNotFoundException e) {
                    JOptionPane.showMessageDialog(OpenPlotTool.getInstance().getMainFrame(), "Failed to open plot:\n" + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(OpenPlotTool.getInstance().getMainFrame(), "Failed to open plot:\n" + e.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
                } catch (ClassNotFoundException e) {
                    JOptionPane.showMessageDialog(OpenPlotTool.getInstance().getMainFrame(), "Failed to open plot:\n" + e.getMessage(), "Class Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(OpenPlotTool.getInstance().getMainFrame(), "Failed to open plot: Invalid File\n", "Class Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void printPlot() {
    }

    public void savePlot() {
        if (ViewManager.getSelectedPlot() != null) {
            Plot plot = ViewManager.getSelectedPlot();
            File saveFile = ViewManager.getSelectedPlot().getPlotFile();
            if (saveFile != null) {
                try {
                    ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream(saveFile));
                    objectOutput.writeObject(plot);
                    objectOutput.close();
                    plot.setEdited(false);
                } catch (FileNotFoundException e) {
                    JOptionPane.showMessageDialog(OpenPlotTool.getInstance().getMainFrame(), "Failed to save plot:\n" + e.getMessage(), "File Not Found Error", JOptionPane.ERROR_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(OpenPlotTool.getInstance().getMainFrame(), "Failed to save plot:\n" + e.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                savePlotAs();
            }
        }
    }

    public void savePlotAs() {
        if (ViewManager.getSelectedPlot() != null) {
            Plot plot = ViewManager.getSelectedPlot();
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.addChoosableFileFilter(new PlotFileFilter());
            int returnValue = fileChooser.showSaveDialog(OpenPlotTool.getInstance().getMainFrame());
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File saveFile = fileChooser.getSelectedFile();
                if (FileUtils.getExtension(saveFile).equals("")) {
                    saveFile = new File(saveFile.getAbsoluteFile() + ".opf");
                }
                if (saveFile.exists()) {
                    int n = JOptionPane.showConfirmDialog(OpenPlotTool.getInstance().getMainFrame(), "File already exists.\n Are you sure you wish to overwrite it?", "Overwrite", JOptionPane.YES_NO_OPTION);
                    if (n == JOptionPane.NO_OPTION) {
                        return;
                    }
                }
                try {
                    ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream(saveFile));
                    objectOutput.writeObject(plot);
                    objectOutput.close();
                    plot.setPlotFile(saveFile);
                    ViewManager.updateTabText();
                    plot.setEdited(false);
                } catch (FileNotFoundException e) {
                    JOptionPane.showMessageDialog(OpenPlotTool.getInstance().getMainFrame(), "Failed to save plot:\n" + e.getMessage(), "File Not Found Error", JOptionPane.ERROR_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(OpenPlotTool.getInstance().getMainFrame(), "Failed to save plot:\n" + e.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}
