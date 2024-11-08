package net.sf.jga.swing.spreadsheet;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import javax.jnlp.FileContents;
import javax.jnlp.FileOpenService;
import javax.jnlp.FileSaveService;
import javax.jnlp.ServiceManager;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * An JNLP-based application wrapper for Spreadsheet, providing a main method to
 * allow for standalone use.  This version supplies input and output stream
 * services based on the WebStart API.
 * <p>
 * Copyright &copy; 2004-2005  David A. Hall
 * @author <a href="mailto:davidahall@users.sf.net">David A. Hall</a>
 */
public class WebStart extends Application {

    private FileContents _fc;

    /**
     * Loads the file via the JNLP FileOpen service
     */
    public int loadFile(Spreadsheet sheet) {
        final Controller controller = getController();
        try {
            FileOpenService fos = (FileOpenService) ServiceManager.lookup("javax.jnlp.FileOpenService");
            if (fos == null) {
                return Controller.CANCEL_OPTION;
            }
            _fc = fos.openFileDialog(null, null);
            if (_fc == null) return Controller.CANCEL_OPTION;
            sheet.readSpreadsheet(_fc.getInputStream());
            controller.setSheetSource(new URL("file:///" + _fc.getName()));
            controller.setSheetDirty(false);
            return Controller.YES_OPTION;
        } catch (Exception x) {
            displayError(x);
            return Controller.CANCEL_OPTION;
        }
    }

    /**
     * Writes the file via the JNLP FileSave service
     */
    public int saveFile(Spreadsheet sheet, boolean promptForName) {
        PipedInputStream pipeIn = null;
        PipedOutputStream pipeOut = null;
        try {
            FileSaveService fss = (FileSaveService) ServiceManager.lookup("javax.jnlp.FileSaveService");
            if (fss == null) {
                return Controller.CANCEL_OPTION;
            }
            pipeIn = new PipedInputStream();
            pipeOut = new PipedOutputStream(pipeIn);
            Thread writeThread = new Thread(new Writer(sheet, pipeOut));
            writeThread.setDaemon(true);
            writeThread.start();
            Controller controller = getController();
            String hint = getClue(_fc, controller.getSheetSource());
            if (promptForName && _fc != null) {
                _fc = fss.saveAsFileDialog(hint, new String[] { "hwks" }, _fc);
            } else {
                _fc = fss.saveFileDialog(hint, new String[] { "hwks" }, pipeIn, hint);
            }
            if (_fc == null) {
                return Controller.CANCEL_OPTION;
            }
            controller.setSheetSource(new URL("file:///" + _fc.getName()));
            controller.setSheetDirty(false);
            pipeIn.close();
            return Controller.YES_OPTION;
        } catch (Exception x) {
            displayError(x);
            return Controller.CANCEL_OPTION;
        }
    }

    /**
     */
    private String getClue(FileContents fc, URL source) {
        if (fc != null) try {
            return fc.getName();
        } catch (IOException x) {
        }
        if (source != null) return source.getPath();
        return "worksheet.hwks";
    }

    /**
     * Writes the spreadsheet contents
     */
    private class Writer implements Runnable {

        private PipedOutputStream _os;

        private Spreadsheet _sheet;

        public Writer(Spreadsheet sheet, PipedOutputStream os) {
            _os = os;
            _sheet = sheet;
        }

        public void run() {
            try {
                _sheet.writeSpreadsheet(_os);
            } catch (Exception x) {
                displayError(x);
            }
            try {
                _os.close();
            } catch (IOException x) {
                displayError(x);
            }
        }
    }

    /**
     * Displays an error message to the user, via whatever method the Controller
     * has been configured with.
     */
    private synchronized void displayError(final Exception x) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    displayError(x);
                }
            });
        } else {
            Controller controller = getController();
            String msg = x.getMessage();
            if (msg == null || msg.length() == 0) msg = Controller.getExceptionName(x);
            controller.notify(msg, Controller.getExceptionName(x));
        }
    }

    public static void main(String[] args) {
        printStartupHeader();
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception x) {
            System.err.println("Error loading L&F:" + x);
        }
        new WebStart();
    }

    private static void printStartupHeader() {
        System.out.println("");
        System.out.println("/**");
        System.out.println(" * A Java Hacker's Worksheet: JNLP Edition");
        System.out.println(" * Copyright (c) 2005  David A. Hall");
        System.out.println(" */");
        System.out.println("");
    }
}
