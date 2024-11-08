package net.saim.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * 
 * @author Konrad Höffner
 *
 */
public class MainFrameActionListener implements ActionListener {

    MainFrame mainFrame;

    public MainFrameActionListener(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    private static void browseIt() {
        try {
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            java.net.URI uri;
            uri = new java.net.URI("http://aksw.org/KonradHoeffner/LinkedGeoData/Aufgabe6");
            desktop.browse(uri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadXML() {
        JFileChooser chooser = new JFileChooser("config");
        int returnVal = chooser.showOpenDialog(mainFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION) try {
            mainFrame.loadXML(chooser.getSelectedFile());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void saveXML() {
        JFileChooser chooser = new JFileChooser("config");
        int returnVal = chooser.showSaveDialog(mainFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (chooser.getSelectedFile().exists()) {
                if (JOptionPane.showConfirmDialog(mainFrame, "File already exists. Overwrite?") == JOptionPane.YES_OPTION) {
                    mainFrame.saveXML(chooser.getSelectedFile());
                }
            } else {
                mainFrame.saveXML(chooser.getSelectedFile());
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == mainFrame.openDevelopementPlanItem) browseIt();
        if (event.getSource() == mainFrame.loadXMLItem) loadXML();
        if (event.getSource() == mainFrame.saveXMLItem) saveXML();
        if (event.getSource() == mainFrame.matchButton) mainFrame.match();
        if (event.getSource() == mainFrame.previewButton) mainFrame.preview();
    }
}
