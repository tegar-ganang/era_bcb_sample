package acide.process.externalCommand;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import acide.gui.mainWindow.AcideMainWindow;
import acide.language.AcideLanguageManager;
import acide.log.AcideLog;
import acide.process.console.AcideConsoleInputProcess;

/**
 * ACIDE - A Configurable IDE external command process.
 * 
 * @version 0.8
 * @see Thread
 */
public class AcideExternalCommandProcess extends Thread {

    /**
	 * Shell path.
	 */
    private final String _shellPath;

    /**
	 * Shell directory.
	 */
    private final String _shellDirectory;

    /**
	 * Text component.
	 */
    private JTextPane _textComponent;

    /**
	 * Creates a new ACIDE - A Configurable IDE external command process.
	 * 
	 * @param shellPath
	 *            shell path.
	 * @param shellDirectory
	 *            shell directory.
	 * @param textComponent
	 *            text component.
	 */
    public AcideExternalCommandProcess(String shellPath, String shellDirectory, JTextPane textComponent) {
        _shellPath = shellPath;
        _shellDirectory = shellDirectory;
        _textComponent = textComponent;
    }

    /**
	 * Main method of the ACIDE - A Configurable IDE console process.
	 */
    public synchronized void run() {
        try {
            Process _process = Runtime.getRuntime().exec(_shellPath, null, new File(_shellDirectory));
            BufferedWriter _writer = new BufferedWriter(new OutputStreamWriter(_process.getOutputStream()));
            AcideConsoleInputProcess inputThread = new AcideConsoleInputProcess(_writer, System.in);
            AcideOutputProcess errorGobbler = new AcideOutputProcess(_process.getErrorStream(), _textComponent);
            AcideOutputProcess outputGobbler = new AcideOutputProcess(_process.getInputStream(), _textComponent);
            errorGobbler.start();
            outputGobbler.start();
            inputThread.start();
            try {
                _process.waitFor();
            } catch (InterruptedException exception) {
                AcideLog.getLog().error(exception.getMessage());
                exception.printStackTrace();
            }
        } catch (Exception exception) {
            AcideLog.getLog().error(exception.getMessage());
            JOptionPane.showMessageDialog(AcideMainWindow.getInstance(), AcideLanguageManager.getInstance().getLabels().getString("s1017"), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
	 * Executes a command in the console given as a parameter.
	 * 
	 * @param shell
	 *            shell in which the command is going to be executed.
	 * @param shellPath
	 *            shell path.
	 * @param command
	 *            command to execute.
	 * @param exitCommand
	 *            exit command.
	 * @param consolePanel
	 *            console panel in which the result of the execution will be
	 *            displayed.
	 */
    public void executeCommand(String shell, String shellPath, String command, String exitCommand, JTextPane consolePanel) {
        Process process = null;
        String pathOutput = shellPath;
        try {
            File filePath = new File(pathOutput);
            process = Runtime.getRuntime().exec(shell, null, filePath);
        } catch (Exception exception) {
            AcideLog.getLog().error(exception.getMessage());
            exception.printStackTrace();
        } finally {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            AcideOutputProcess errorGobbler = new AcideOutputProcess(process.getErrorStream(), consolePanel);
            AcideOutputProcess outputGobbler = new AcideOutputProcess(process.getInputStream(), consolePanel);
            errorGobbler.start();
            outputGobbler.start();
            try {
                writer.write(command + '\n');
                writer.flush();
                writer.write(exitCommand + '\n');
                writer.flush();
            } catch (IOException exception) {
                AcideLog.getLog().error(exception.getMessage());
                exception.printStackTrace();
            }
        }
    }
}
