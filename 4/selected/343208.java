package com.google.code.shell4eclipse.console;

import java.io.IOException;
import java.io.OutputStreamWriter;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;
import com.google.code.shell4eclipse.IShell4EclipseConstants;
import com.google.code.shell4eclipse.Shell4EclipsePlugin;
import com.google.code.shell4eclipse.console.AnsiTermTokenizer.Token;
import com.google.code.shell4eclipse.process.IProcess;
import com.google.code.shell4eclipse.process.IProcessListener;
import com.google.code.shell4eclipse.process.IStreamReader;
import com.google.code.shell4eclipse.process.IProcess.IStreamListener;

/**
 * 
 *
 * @author Kamen Petroff 
 * @since Mar 2, 2007
 */
public class Shell4EclipseConsole extends IOConsole implements IProcessListener {

    private IProcess process;

    private IOConsoleOutputStream outputStream;

    private IOConsoleOutputStream errorStream;

    public Shell4EclipseConsole(IProcess process) {
        super("Shell4Eclipse", "shell", Shell4EclipsePlugin.getImageDescriptor(IShell4EclipseConstants.IMG_S4E_CONSOLE), true);
        this.process = process;
        this.process.setProcessListener(this);
        this.outputStream = super.newOutputStream();
        this.errorStream = super.newOutputStream();
        this.process.setOutputStreamListener(new StreamListener(outputStream, 1024 * 128));
        this.process.setErrorStreamListener(new StreamListener(errorStream, 1024 * 32));
        setColor(outputStream, SWT.COLOR_DARK_GREEN);
        setColor(errorStream, SWT.COLOR_DARK_RED);
        new InputStreamThread().start();
    }

    public void setColor(final IOConsoleOutputStream consoleStream, final int color) {
        final Display display = Shell4EclipsePlugin.getDefault().getWorkbench().getDisplay();
        display.asyncExec(new Runnable() {

            public void run() {
                consoleStream.setColor(display.getSystemColor(color));
            }
        });
    }

    private class InputStreamThread extends Thread {

        @Override
        public void run() {
            IOConsoleInputStream inputStream = getInputStream();
            byte[] buffer = new byte[1024];
            try {
                for (; process != null; ) {
                    int read = inputStream.read(buffer);
                    if (read > 0) {
                        process.writeStdIn(buffer, read);
                    }
                }
            } catch (IOException ex) {
                Shell4EclipsePlugin.log(IStatus.ERROR, "Failed to write subprocess's STDIN", ex);
            }
        }
    }

    /**
	 * Clean-up created fonts.
	 */
    public void shutdown() {
    }

    public void show() {
        IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
        IConsole[] existing = manager.getConsoles();
        boolean exists = false;
        for (int i = 0; i < existing.length; i++) {
            if (existing[i] == this) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            manager.addConsoles(new IConsole[] { this });
        }
        manager.showConsoleView(this);
    }

    public void close() {
        process.kill();
        IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
        manager.removeConsoles(new IConsole[] { this });
        process = null;
    }

    private class StreamListener implements IStreamListener {

        private byte[] buffer;

        private IOConsoleOutputStream stream;

        private AnsiTermTokenizer tokenizer = new AnsiTermTokenizer();

        protected StreamListener(IOConsoleOutputStream stream, int bufferLength) {
            this.stream = stream;
            this.buffer = new byte[bufferLength];
        }

        public void onOutputAvailable(IStreamReader reader) {
            int read = reader.read(buffer);
            try {
                while (read > 0) {
                    write(buffer, read);
                    read = reader.read(buffer);
                }
                flush();
            } catch (IOException ex) {
                Shell4EclipsePlugin.log(IStatus.ERROR, "Failed writing to the Console View", ex);
            }
        }

        protected void write(byte[] buffer, int lenght) throws IOException {
            if (false) {
                stream.write(buffer, 0, lenght);
            } else {
                Token token = tokenizer.next(buffer, lenght);
                while (token != null) {
                    writeToken(token);
                    token = tokenizer.next(null, 0);
                }
            }
        }

        private void writeToken(Token token) throws IOException {
            if (token.getToken() != null) {
                stream.write(token.getToken());
            }
        }

        protected void flush() throws IOException {
            stream.flush();
        }
    }

    public void onProcessExited(int exitValue) {
        OutputStreamWriter writer = new OutputStreamWriter(errorStream);
        try {
            writer.append("\n\n\t----=== PROCESS EXITED ===----\n");
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            Shell4EclipsePlugin.log(IStatus.WARNING, "failed to write to IOConsole after process exited", ex);
        }
    }
}
