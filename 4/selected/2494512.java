package acide.process.console;

import acide.configuration.project.AcideProjectConfiguration;
import acide.gui.consolePanel.AcideConsolePanel;
import acide.gui.mainWindow.AcideMainWindow;
import java.io.*;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import acide.language.AcideLanguageManager;
import acide.log.AcideLog;
import acide.resources.AcideResourceManager;

/**
 * ACIDE - A Configurable IDE console process.
 * 
 * @version 0.8
 * @see Thread
 */
public class AcideConsoleProcess extends Thread {

    /**
	 * ACIDE - A Configurable IDE console process buffered writer.
	 */
    private static BufferedWriter _writer = null;

    /**
	 * ACIDE - A Configurable IDE console process shell process to execute.
	 */
    public Process _process = null;

    /**
	 * Creates a new ACIDE - A Configurable IDE console process.
	 */
    public AcideConsoleProcess() {
    }

    /**
	 * Main method of the ACIDE - A Configurable IDE console process.
	 */
    public synchronized void run() {
        String shellPath = null;
        String shellDirectory = null;
        try {
            shellPath = AcideResourceManager.getInstance().getProperty("consolePanel.shellPath");
            shellDirectory = AcideResourceManager.getInstance().getProperty("consolePanel.shellDirectory");
            File shellPathFile = new File(shellPath);
            File shellDirectoryFile = new File(shellDirectory);
            if ((shellPathFile.exists() && shellPathFile.isFile()) && (shellDirectoryFile.exists() && shellDirectoryFile.isDirectory())) {
                File path = new File(shellDirectory);
                _process = Runtime.getRuntime().exec(shellPath, null, path);
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        AcideMainWindow.getInstance().getConsolePanel().getTextPane().setEditable(true);
                    }
                });
                _writer = new BufferedWriter(new OutputStreamWriter(_process.getOutputStream()));
                AcideConsoleInputProcess inputThread = new AcideConsoleInputProcess(_writer, System.in);
                AcideConsoleOutputProcess errorGobbler = new AcideConsoleOutputProcess(_process.getErrorStream(), AcideMainWindow.getInstance().getConsolePanel());
                AcideConsoleOutputProcess outputGobbler = new AcideConsoleOutputProcess(_process.getInputStream(), AcideMainWindow.getInstance().getConsolePanel());
                errorGobbler.start();
                outputGobbler.start();
                inputThread.start();
                try {
                    _process.waitFor();
                } catch (InterruptedException exception) {
                    AcideLog.getLog().error(exception.getMessage());
                    exception.printStackTrace();
                }
            } else {
                setDefaultConfiguration();
            }
        } catch (Exception exception) {
            AcideLog.getLog().error(exception.getMessage());
            JOptionPane.showMessageDialog(AcideMainWindow.getInstance(), AcideLanguageManager.getInstance().getLabels().getString("s1017"), "Error", JOptionPane.ERROR_MESSAGE);
            AcideProjectConfiguration.getInstance().setIsModified(true);
            setDefaultConfiguration();
        }
    }

    /**
	 * Sets the console default configuration.
	 */
    private void setDefaultConfiguration() {
        AcideProjectConfiguration.getInstance().setShellPath("");
        AcideResourceManager.getInstance().setProperty("consolePanel.shellPath", "");
        AcideProjectConfiguration.getInstance().setShellDirectory("");
        AcideResourceManager.getInstance().setProperty("consolePanel.shellDirectory", "");
        AcideProjectConfiguration.getInstance().setExitCommand("");
        AcideResourceManager.getInstance().setProperty("consolePanel.exitCommand", "");
        AcideProjectConfiguration.getInstance().setIsEchoCommand(false);
        AcideResourceManager.getInstance().setProperty("consolePanel.isEchoCommand", "false");
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                AcideMainWindow.getInstance().getConsolePanel().getTextPane().setText("");
                AcideMainWindow.getInstance().getConsolePanel().validate();
                AcideMainWindow.getInstance().getConsolePanel().repaint();
                AcideMainWindow.getInstance().getConsolePanel().getTextPane().setEditable(false);
            }
        });
    }

    /**
	 * Returns the buffered writer.
	 * 
	 * @return the buffered writer.
	 */
    public BufferedWriter getWriter() {
        return _writer;
    }

    /**
	 * Returns the process to execute.
	 * 
	 * @return the process to execute.
	 */
    public Process getProcess() {
        return _process;
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
    public void executeCommand(String shell, String shellPath, String command, String exitCommand, AcideConsolePanel consolePanel) {
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
            AcideConsoleOutputProcess errorGobbler = new AcideConsoleOutputProcess(process.getErrorStream(), consolePanel);
            AcideConsoleOutputProcess outputGobbler = new AcideConsoleOutputProcess(process.getInputStream(), consolePanel);
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
