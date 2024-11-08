package org.eclipse.pde.internal.runtime.logview;

import java.io.*;
import java.lang.reflect.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.*;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * Displays the error log in non-Win32 platforms - see bug 55314.
 */
public final class OpenLogDialog extends Dialog {

    private File logFile;

    private IDialogSettings dialogSettings;

    private Point dialogLocation;

    private Point dialogSize;

    private int DEFAULT_WIDTH = 750;

    private int DEFAULT_HEIGHT = 800;

    public OpenLogDialog(Shell parentShell, File logFile) {
        super(parentShell);
        this.logFile = logFile;
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN | SWT.MODELESS);
    }

    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(PDERuntimeMessages.OpenLogDialog_title);
        readConfiguration();
    }

    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true);
    }

    public void create() {
        super.create();
        if (dialogLocation != null) getShell().setLocation(dialogLocation);
        if (dialogSize != null) getShell().setSize(dialogSize); else getShell().setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        getButton(IDialogConstants.CLOSE_ID).setFocus();
    }

    protected Control createDialogArea(Composite parent) {
        Composite outer = (Composite) super.createDialogArea(parent);
        Text text = new Text(outer, SWT.MULTI | SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL | SWT.NO_FOCUS | SWT.H_SCROLL);
        text.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
        gridData.grabExcessVerticalSpace = true;
        gridData.grabExcessHorizontalSpace = true;
        text.setLayoutData(gridData);
        text.setText(getLogSummary());
        return outer;
    }

    private String getLogSummary() {
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        if (logFile.length() > LogReader.MAX_FILE_LENGTH) {
            readLargeFileWithMonitor(writer);
        } else {
            readFileWithMonitor(writer);
        }
        writer.close();
        return out.toString();
    }

    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.CLOSE_ID) {
            storeSettings();
            close();
        }
        super.buttonPressed(buttonId);
    }

    /**
     * Stores the current state in the dialog settings.
     * 
     * @since 2.0
     */
    private void storeSettings() {
        writeConfiguration();
    }

    /**
     * Returns the dialog settings object used to share state between several
     * event detail dialogs.
     * 
     * @return the dialog settings to be used
     */
    private IDialogSettings getDialogSettings() {
        IDialogSettings settings = PDERuntimePlugin.getDefault().getDialogSettings();
        dialogSettings = settings.getSection(getClass().getName());
        if (dialogSettings == null) dialogSettings = settings.addNewSection(getClass().getName());
        return dialogSettings;
    }

    /**
     * Initializes itself from the dialog settings with the same state as at the
     * previous invocation.
     */
    private void readConfiguration() {
        IDialogSettings s = getDialogSettings();
        try {
            int x = s.getInt("x");
            int y = s.getInt("y");
            dialogLocation = new Point(x, y);
            x = s.getInt("width");
            y = s.getInt("height");
            dialogSize = new Point(x, y);
        } catch (NumberFormatException e) {
            dialogLocation = null;
            dialogSize = null;
        }
    }

    private void writeConfiguration() {
        IDialogSettings s = getDialogSettings();
        Point location = getShell().getLocation();
        s.put("x", location.x);
        s.put("y", location.y);
        Point size = getShell().getSize();
        s.put("width", size.x);
        s.put("height", size.y);
    }

    private void readFile(PrintWriter writer) throws FileNotFoundException, IOException {
        BufferedReader bReader = new BufferedReader(new FileReader(logFile));
        while (bReader.ready()) writer.println(bReader.readLine());
    }

    private void readLargeFile(PrintWriter writer) throws FileNotFoundException, IOException {
        RandomAccessFile random = null;
        boolean hasStarted = false;
        try {
            random = new RandomAccessFile(logFile, "r");
            random.seek(logFile.length() - LogReader.MAX_FILE_LENGTH);
            for (; ; ) {
                String line = random.readLine();
                if (line == null) break;
                line = line.trim();
                if (line.length() == 0) continue;
                if (!hasStarted && (line.startsWith("!ENTRY") || line.startsWith("!SESSION"))) hasStarted = true;
                if (hasStarted) writer.println(line);
                continue;
            }
        } finally {
            try {
                if (random != null) random.close();
            } catch (IOException e1) {
            }
        }
    }

    private void readLargeFileWithMonitor(final PrintWriter writer) {
        IRunnableWithProgress runnable = new IRunnableWithProgress() {

            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                monitor.beginTask(PDERuntimeMessages.OpenLogDialog_message, IProgressMonitor.UNKNOWN);
                try {
                    readLargeFile(writer);
                } catch (IOException e) {
                    writer.println(PDERuntimeMessages.OpenLogDialog_cannotDisplay);
                }
            }
        };
        ProgressMonitorDialog dialog = new ProgressMonitorDialog(getParentShell());
        try {
            dialog.run(true, true, runnable);
        } catch (InvocationTargetException e) {
        } catch (InterruptedException e) {
        }
    }

    private void readFileWithMonitor(final PrintWriter writer) {
        IRunnableWithProgress runnable = new IRunnableWithProgress() {

            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                monitor.beginTask(PDERuntimeMessages.OpenLogDialog_message, IProgressMonitor.UNKNOWN);
                try {
                    readFile(writer);
                } catch (IOException e) {
                    writer.println(PDERuntimeMessages.OpenLogDialog_cannotDisplay);
                }
            }
        };
        ProgressMonitorDialog dialog = new ProgressMonitorDialog(getParentShell());
        try {
            dialog.run(true, true, runnable);
        } catch (InvocationTargetException e) {
        } catch (InterruptedException e) {
        }
    }
}
