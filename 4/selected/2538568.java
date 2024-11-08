package com.hypermine.ultrasonic.ui.commons;

import java.io.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.swt.widgets.*;

/**
 * 
 * @author wschwitzer
 * @author $Author: wschwitzer $
 * @version $Rev: 152 $
 * @levd.rating GREEN Rev: 152
 */
public class FileDialogUtils {

    /** The filter extension used in the file dialog for projects. */
    public static final String[] FILTER_EXTENSIONS = { "*.usp" };

    /** The filter names used in the file dialog for projects. */
    public static final String[] FILTER_NAMES = { "UltraSonic Projects (*.usp)" };

    /**
	 * Returns the name of a file the user has selected in a {@link FileDialog}
	 * that has the given shell as parent and the given text as title text.
	 * Returns <code>null</code> if the user canceled the file dialog.
	 */
    public static String showFileDialog(Shell shell, int style, String text) {
        FileDialog dialog = new FileDialog(shell, style);
        dialog.setFilterExtensions(FILTER_EXTENSIONS);
        dialog.setFilterNames(FILTER_NAMES);
        dialog.setText(text);
        return dialog.open();
    }

    /**
	 * Returns <code>true</code> if either the given does not exist or the
	 * file exists and the user has confirmed a message dialog with OK.
	 * <p>
	 * The message dialog has the given shell as parent.
	 */
    public static boolean confirmOverwrite(Shell shell, File file) {
        if (file.exists()) {
            return MessageDialog.openConfirm(shell, "Confirm", "File \"" + file + "\" already exists. Continue and overwrite?");
        }
        return true;
    }

    /**
	 * Returns <code>true</code> if the user has confirmed a message dialog
	 * asking to discard unsaved changes with OK.
	 * <p>
	 * The message dialog has the given shell as parent.
	 */
    public static boolean confirmDiscard(Shell shell) {
        return MessageDialog.openConfirm(shell, "Confirm", "Current project has unsaved changes. Continue and discard unsaved changes?");
    }
}
