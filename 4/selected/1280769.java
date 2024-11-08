package com.varaneckas.hawkscope.util;

import java.net.URLEncoder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import com.varaneckas.hawkscope.Constants;
import com.varaneckas.hawkscope.Version;

/**
 * Registers uncaught exception handler that displays error message inside
 * Swing dialog
 *
 * @author Tomas Varaneckas
 * @version $Id: UncaughtExceptionHandler.java 421 2009-03-12 17:06:38Z tomas.varaneckas $
 */
public class UncaughtExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler {

    /**
     * Logger
     */
    private static final Log log = LogFactory.getLog(UncaughtExceptionHandler.class);

    /**
     * Handles the uncaught {@link Exception}
     */
    public void uncaughtException(final Thread t, final Throwable e) {
        final Display display = Display.getCurrent();
        final Shell shell = new Shell(display);
        final MessageBox message = new MessageBox(shell, SWT.OK | SWT.CANCEL | SWT.ICON_ERROR);
        message.setText("Hawkscope Error");
        message.setMessage(e.getMessage() + "\nSubmit Hawkscope Error Report to Issue Tracker?");
        log.error("Uncaught exception", e);
        if (message.open() == SWT.OK) {
            IOUtils.copyToClipboard(Version.getBugReport(e));
            try {
                Program.launch(Constants.HAWKSCOPE_URL_ROOT + "issues/entry?comment=" + URLEncoder.encode("Please paste the Hawkscope Error " + "Report here. It's currently copied to your " + "clipboard. Thank you for your support!", Constants.ENCODING));
            } catch (final Exception e1) {
                Program.launch(Constants.HAWKSCOPE_URL_ROOT + "issues/entry");
            }
        }
        shell.dispose();
    }
}
