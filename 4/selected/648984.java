package MotionTool.Interface;

import MotionTool.MotionDef.ServoKeyFrames;
import MotionTool.MotionDef.Motion;
import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * The event Handler for any button of the interface clicked
 */
class JButtonEventHandler extends java.awt.event.MouseAdapter {

    /** The parent graphical interface */
    private MotionInterface motionInterface;

    /**
     * Returns a JButtonEventHandler Object
     * @param theInterface the parent interface
     */
    public JButtonEventHandler(MotionInterface motionInterface, MotionInterface theInterface) {
        this.motionInterface = motionInterface;
    }

    /**
     * The event handler for a mouse click over a button
     * @param e: The Mouse event
     */
    public void mouseClicked(MouseEvent e) {
        JButton button = (JButton) e.getSource();
        String buttonName = button.getName();
        System.out.println("Clicked on button " + buttonName);
        if (buttonName == "Delete") {
            if (button.isEnabled()) {
                System.out.println("Deleting at pos " + motionInterface.deletKeyFramePos);
                button.setEnabled(false);
                motionInterface.drawingBoard.removeKeyFrame(motionInterface.actualDrawingMotor, motionInterface.deletKeyFramePos);
                motionInterface.resultBoard.removeKeyFrame(motionInterface.actualDrawingMotor, motionInterface.deletKeyFramePos);
                motionInterface.frameIsClicked = false;
            } else System.out.println("Delete is  not actif");
        } else if (buttonName == "newMotion") {
            String motionName = JOptionPane.showInputDialog("Please input a new Motion Name");
            if (motionName != null && !motionName.equals("")) {
                motionInterface.listModel.addElement(motionName);
                ServoKeyFrames keyFrame = new ServoKeyFrames(0);
                Motion newMotion = new Motion();
                newMotion.appendServoKeyFrames(keyFrame);
                newMotion.motionName = motionName;
                motionInterface.motionVector.addElement(newMotion);
                motionInterface.changeBoardsToNewMotion(motionInterface.motionVector.size() - 1, true);
                motionInterface.motionList.setSelectedIndex(motionInterface.actualMotionIndex);
            }
        } else if (buttonName == "go") {
            System.out.println("lets go MotionInterface= " + motionInterface.letsGo);
            if (motionInterface.letsGo == 0) {
                motionInterface.jButtonGo.setBackground(Color.red);
                motionInterface.jButtonGo.setText("STOP");
                ServoKeyFrames newDrawing = motionInterface.drawingBoard.getTransFormedServoKeyFrames(motionInterface.actualDrawingMotor);
                motionInterface.actualMotion.appendServoKeyFrames(newDrawing);
                motionInterface.actualMotion.updatenumberOfFramesInMotion();
                float smoothFactor = Float.parseFloat(motionInterface.jTextSmoothFactor.getText());
                if (smoothFactor < 0 || smoothFactor > 200) {
                    smoothFactor = 0;
                    motionInterface.jTextSmoothFactor.setText("0");
                }
                motionInterface.actualMotion.setSmoothFactorMotionTransition(smoothFactor);
                motionInterface.letsGo = 1;
                motionInterface.resultBoard.redrawBoard();
            } else {
                motionInterface.jButtonGo.setBackground(Color.green);
                motionInterface.jButtonGo.setText("GO");
                motionInterface.letsGo = 3;
            }
        } else if (buttonName == "load") {
            openFileChooser();
        } else if (buttonName == "save") {
            saveFileChooser();
        } else if (buttonName == "editMotion") {
            int pos = motionInterface.motionList.getMinSelectionIndex();
            if (pos >= 0) {
                String inputValue = JOptionPane.showInputDialog("Please input a new Motion Name");
                if (inputValue != null && !inputValue.equals("")) {
                    System.out.println("You have chosen a new motion Name : " + inputValue + " for position " + pos);
                    motionInterface.listModel.setElementAt(inputValue, pos);
                    ((Motion) motionInterface.motionVector.elementAt(pos)).motionName = inputValue;
                }
            }
        } else if (buttonName == "deleteMotion") {
            int pos = motionInterface.motionList.getMinSelectionIndex();
            String motionName = (String) motionInterface.listModel.elementAt(pos);
            Object[] options = { "Delete", "Cancel" };
            int cancel = JOptionPane.showOptionDialog(motionInterface, "Are you shure you want to remove Motion " + motionName, "Remove Motion ?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (cancel == 0) {
                System.out.println("List size is " + motionInterface.motionList.getSize() + " Trying to erase position " + pos);
                motionInterface.listModel.remove(pos);
                motionInterface.motionVector.removeElementAt(pos);
                if (motionInterface.motionVector.size() < 1) {
                    motionInterface.listModel.addElement("NewMotion");
                    ServoKeyFrames servoTemp = new ServoKeyFrames(0);
                    Motion motTemp = new Motion();
                    motTemp.appendServoKeyFrames(servoTemp);
                    motTemp.motionName = "NewMotion";
                    motionInterface.motionVector.add(motTemp);
                }
                motionInterface.changeBoardsToNewMotion(0, false);
            }
        }
    }

    /**
      * Opens a file chooser
      */
    public void openFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new XMLFilter());
        chooser.setCurrentDirectory(new File("D:/Programme/EPFL/Webots/controllers/Judoka0"));
        int returnVal = chooser.showOpenDialog(motionInterface);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String fileName = chooser.getSelectedFile().getAbsolutePath();
            System.out.println("You chose to open this file: " + fileName);
            try {
                FileReader File = new FileReader(fileName);
                if (motionInterface.loadFileToBoard(fileName) < 0) {
                    JOptionPane.showMessageDialog(motionInterface, "There was an error Parsing the File. Please view the errors in the errorLog.txt / debugLog.txt files", "Parser Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException ex) {
                System.out.println("Caught access file IOException: " + ex.toString());
                JOptionPane.showMessageDialog(motionInterface, "The File you've choosen dosn't exist", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * opens a save file chooser
     */
    public void saveFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new XMLFilter());
        chooser.setCurrentDirectory(new File("D:/Programme/EPFL/Webots/controllers/Judoka0"));
        int returnVal = chooser.showOpenDialog(motionInterface);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String fileName = chooser.getSelectedFile().getAbsolutePath();
            System.out.println("You chose to open this file: " + fileName);
            try {
                FileReader File = new FileReader(fileName);
                Object[] options = { "Overwrite", "Cancel" };
                int cancel = JOptionPane.showOptionDialog(motionInterface, "The file you have chosen already exists, do you want to overwrite it?", "File already Exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                if (cancel == 0) {
                    motionInterface.saveBoardToFile(fileName);
                }
            } catch (IOException ex) {
                System.out.println("File doesn't exist creating it.. ");
                motionInterface.saveBoardToFile(fileName);
            }
        }
    }
}
