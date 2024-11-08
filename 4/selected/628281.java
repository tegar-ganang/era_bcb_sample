package acme.view;

import acme.model.BoardFeature;
import acme.model.Corral;
import acme.model.Fence;
import acme.model.Grid;
import acme.model.Grid.Direction;
import acme.model.Obstacle;
import java.awt.Component;
import java.io.File;
import javax.swing.JOptionPane;

public class Dialogs {

    public static Grid.Direction getFenceDirection(Component parent) {
        String[] options = new String[] { "Up", "Down", "Left", "Right" };
        String result = (String) JOptionPane.showInputDialog(parent, "Select the fence direction:", "Fence Properties (1/2)", JOptionPane.PLAIN_MESSAGE, null, options, "Up");
        if (result == null) return Direction.none; else if (result == options[0]) return Direction.up; else if (result == options[1]) return Direction.down; else if (result == options[2]) return Direction.left; else return Direction.right;
    }

    public static int getFenceLength(Component parent) {
        int length = 0;
        while (length <= 0) {
            String lengthString = (String) JOptionPane.showInputDialog(parent, "Select the fence length:", "Fence Properties (2/2)", JOptionPane.PLAIN_MESSAGE, null, null, null);
            if (lengthString == null) return 0;
            try {
                length = Integer.parseInt(lengthString);
                if (length == 0) JOptionPane.showMessageDialog(parent, "Can't make a fence of zero length!", "Error", JOptionPane.ERROR_MESSAGE); else if (length < 0) JOptionPane.showMessageDialog(parent, "Can't make a fence of negative length!", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(parent, "Length of fence must be an integer!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return length;
    }

    public static void fenceEndOutsideGridError(Component parent) {
        JOptionPane.showMessageDialog(parent, "Can't make a fence which ends outside the grid!", "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void fenceNotStraightError(Component parent) {
        JOptionPane.showMessageDialog(parent, "Fence must be a horizontal or vertical straight line!", "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirmFeatureDelete(BoardFeature feature, Component parent) {
        String featureName = "Feature";
        if (feature instanceof Fence) {
            featureName = "Fence";
        } else if (feature instanceof Corral) {
            featureName = "Corral";
        } else if (feature instanceof Obstacle) {
            featureName = "Obstacle";
        }
        int result = JOptionPane.showConfirmDialog(parent, featureName + " in the way - delete it?");
        if (result == 0) return true; else return false;
    }

    public static boolean confirmOverwrite(File file, Component parent) {
        int result = JOptionPane.showConfirmDialog(parent, "File \"" + file.getName() + "\" already exists - overwrite?");
        if (result == 0) return true; else return false;
    }

    public static void fileWriteError(Component parent) {
        JOptionPane.showMessageDialog(parent, "Oh dear. Something went wrong while writing to file.", "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static String getMapName(Component parent) {
        String nameString = null;
        while (true) {
            nameString = (String) JOptionPane.showInputDialog(parent, "Please give your map an alphanumeric name:", "Name Your Map", JOptionPane.PLAIN_MESSAGE, null, null, null);
            if (nameString == null) break; else if (nameString.matches("\\p{Alnum}+$")) {
                break;
            } else {
                JOptionPane.showMessageDialog(parent, "Map name must be alphanumeric!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return nameString;
    }

    public static void missingCorralError(Component parent) {
        JOptionPane.showMessageDialog(parent, "Can't export map - not enough corrals", "Error", JOptionPane.ERROR_MESSAGE);
    }
}
