package roboResearch.gui;

import java.awt.*;
import java.io.*;
import javax.swing.*;

/**
 * @author Eric Simonton
 */
public class ErrorMessageHandler {

    public static void handle(IOException ex) {
        ex.printStackTrace();
        showError("I/O Error", "RoboResearch could not read or write data to a required resource");
    }

    public static void handle(Throwable ex) {
        ex.printStackTrace();
        String message = ex.getMessage();
        if (message == null) {
            message = ex.getClass().getName();
        }
        showError("Error", message);
    }

    private static void showError(String title, String message, Object... messageArgs) {
        JOptionPane.showMessageDialog(getFocusedWindow(), String.format(message, messageArgs), title, JOptionPane.ERROR_MESSAGE);
    }

    private static Window getFocusedWindow() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
    }
}
