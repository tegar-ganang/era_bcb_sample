package net.sourceforge.javautil.ui.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.sourceforge.javautil.common.CollectionUtil;
import net.sourceforge.javautil.common.StringUtil;
import net.sourceforge.javautil.common.ThreadUtil;
import net.sourceforge.javautil.common.ThrowableUtil;
import net.sourceforge.javautil.common.exception.ThrowableManagerRegistry;
import net.sourceforge.javautil.common.io.IVirtualDirectory;
import net.sourceforge.javautil.common.io.IVirtualFile;
import net.sourceforge.javautil.common.io.console.IIOConsole;
import net.sourceforge.javautil.common.io.impl.SystemDirectory;
import net.sourceforge.javautil.common.io.impl.SystemFile;
import net.sourceforge.javautil.common.password.IPassword;
import net.sourceforge.javautil.common.password.IPasswordLocker;
import net.sourceforge.javautil.common.password.impl.UnencryptedPassword;
import net.sourceforge.javautil.ui.UIAlertEvent;
import net.sourceforge.javautil.ui.UICommonEvent;
import net.sourceforge.javautil.ui.UICommonEventHandler;
import net.sourceforge.javautil.ui.UICommonResponse;
import net.sourceforge.javautil.ui.UIConfirmationEvent;
import net.sourceforge.javautil.ui.UIFailureException;
import net.sourceforge.javautil.ui.UserInterfaceAbstract;
import net.sourceforge.javautil.ui.UserInterfaceException;
import net.sourceforge.javautil.ui.UIAlertEvent.Severity;
import net.sourceforge.javautil.ui.UIConfirmationEvent.Answer;
import net.sourceforge.javautil.ui.cli.widget.TextTable;
import net.sourceforge.javautil.ui.command.IUICommand;
import net.sourceforge.javautil.ui.command.IUICommandContext;
import net.sourceforge.javautil.ui.command.UICommandException;
import net.sourceforge.javautil.ui.command.IUICommandSet;
import net.sourceforge.javautil.ui.model.IUIProgressMonitorSource;
import net.sourceforge.javautil.ui.model.IUITableModel;
import net.sourceforge.javautil.ui.model.impl.UITableModelDefault;

/**
 * Base for most CLI interfaces.
 *
 * @author elponderador
 * @author $Author: ponderator $
 * @version $Id: CommandLineInterfaceAbstract.java 2516 2010-11-04 03:52:40Z ponderator $
 */
public abstract class CommandLineInterfaceAbstract extends UserInterfaceAbstract implements ICommandLineInterface, CommandLinePromptController {

    protected IIOConsole console;

    protected PrintStream writer;

    protected LineNumberReader reader;

    protected IUICommandSet<? extends IUICommand, ? extends IUICommandContext> set;

    protected CommandLineContext context;

    private Severity severity = Severity.Error;

    protected boolean running = false;

    protected boolean echo = false;

    protected Thread controllingThread;

    protected CommandLinePromptController controller;

    public CommandLineInterfaceAbstract(IIOConsole console) {
        this.console = console;
        this.writer = console.getPrintStream();
        this.reader = new LineNumberReader(console.getReader());
    }

    public IIOConsole getConsole() {
        return this.console;
    }

    public PrintStream getWriter() {
        return writer;
    }

    public Reader getReader() {
        return this.reader;
    }

    public CommandLineContext getContext() {
        return this.context;
    }

    /**
	 * @return The thread that is controlling this CLI.
	 */
    public Thread getControllingThread() {
        return controllingThread;
    }

    public void initialize() {
        this.controller = this;
    }

    public void cleanup() {
    }

    public String getPrompt() {
        return "> ";
    }

    public void run() {
        this.control();
    }

    public void run(IVirtualFile batchFile) {
        if (!batchFile.isExists()) {
            info("No such file: " + batchFile);
        } else {
            try {
                this.echo = true;
                this.reader = new LineNumberReader(new InputStreamReader(batchFile.getInputStream()));
                this.run();
            } catch (IOException e) {
                throw ThrowableManagerRegistry.caught(e);
            }
        }
    }

    public void showProgressMonitor(IUIProgressMonitorSource updater) {
    }

    public void takeControl(CommandLinePromptController controller) {
        controller.control();
    }

    public void control() {
        this.set = this.createCommandSet();
        this.context = this.createContext(reader, writer);
        this.running = true;
        this.controllingThread = Thread.currentThread();
        try {
            while (running) {
                try {
                    if (this.controller != null) {
                        if (this.controller == this) {
                            try {
                                String[] commandInfo = this.captureCommand(this.getPrompt());
                                if (commandInfo == null) continue;
                                if (commandInfo.length == 0 || (commandInfo.length > 0 && "".equals(commandInfo[0]))) continue;
                                Object results = this.execute(commandInfo);
                                if (results != null) {
                                    this.handleExecutionResults(results);
                                }
                            } catch (UserInterfaceException uie) {
                                this.running = false;
                                return;
                            }
                        } else this.controller.control();
                    } else ThreadUtil.sleep(500);
                } catch (CommandLinePromptControlException e) {
                    this.controller = e.getController() == null ? this : e.getController();
                } catch (Exception e) {
                    if (!(e instanceof InterruptedException)) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } finally {
            this.cleanup();
        }
    }

    /**
	 * @param commandInfo The command line array
	 * @return The results of the command, null on errors
	 */
    public Object execute(String... commandInfo) {
        try {
            IUICommand command = set.createCommand(commandInfo[0]);
            if (command != null) {
                CommandLineArgumentsStandard cla = null;
                if (commandInfo.length > 1) {
                    if (command instanceof CommandLineArgumentParser) {
                        cla = ((CommandLineArgumentParser) command).createArguments(StringUtil.join(CollectionUtil.shift(commandInfo), ' '));
                    } else {
                        cla = new CommandLineArgumentsStandard(CollectionUtil.shift(commandInfo));
                    }
                }
                return command.execute(context, cla);
            } else {
                if ("exit".equalsIgnoreCase(commandInfo[0]) || "quit".equalsIgnoreCase(commandInfo[0])) {
                    running = false;
                    return new CommandLineExitException(this);
                } else return "Unrecognized command: " + commandInfo[0];
            }
        } catch (CommandLineExitException e) {
            return e;
        } catch (UICommandException exe) {
            return exe;
        } catch (RuntimeException e) {
            return e;
        }
    }

    /**
	 * @param results The results to handle
	 */
    public void handleExecutionResults(Object results) {
        if (results instanceof IUITableModel) {
            this.showTable((IUITableModel) results);
        } else if (results instanceof Exception) {
            if (results instanceof CommandLineExitException) {
                this.running = false;
            } else if (results instanceof UICommandException) {
                UICommandException exe = (UICommandException) results;
                ((CommandLineCommand) exe.getCommand()).showHelp(writer);
                if (exe.getCause() == null || exe.getCause() == exe) {
                    writer.println("Error executing command: " + exe.getMessage());
                } else {
                    exe.printStackTrace(writer);
                }
                writer.println();
            } else {
                writer.println("Unexpected exception:");
                ((Exception) results).printStackTrace(writer);
            }
        } else if (results != null) {
            if (results.getClass().isArray()) results = CollectionUtil.asList(results);
            if (results instanceof Collection) {
                this.showTable(new UITableModelDefault((Collection) results));
            } else if (results instanceof Map) {
                this.showTable(new UITableModelDefault((Map) results, "Key", "Value"));
            } else this.info(String.valueOf(results));
        }
    }

    public IPassword password(String msg) {
        this.info(msg);
        return new UnencryptedPassword(new String(console.getPassword()));
    }

    public IPassword password(String msg, String lockerId, IPasswordLocker... lockers) {
        IPassword pw = this.password(msg);
        IPasswordLocker locker = null;
        if (lockers.length == 1) {
            if (this.confirm("Do you wish to save this password", true)) locker = lockers[0];
        } else {
            locker = this.choose("Choose which locker to save in", null, lockers);
        }
        if (locker != null) locker.setPassword(lockerId, pw);
        return pw;
    }

    public SystemDirectory getDirectory(String prompt, SystemDirectory defaultDirectory) {
        String result = this.captureRawInput(prompt + (defaultDirectory != null ? " [default=" + defaultDirectory.getPath().toString("/") + "]" : "") + ": ");
        return "".equals(result.trim()) ? defaultDirectory : new SystemDirectory(result);
    }

    public SystemFile getFile(String prompt, SystemFile defaultFile) {
        String result = this.captureRawInput(prompt + (defaultFile != null ? "[default=" + defaultFile.getPath().toString("/") + "]" : "") + ": ");
        return "".equals(result.trim()) ? defaultFile : new SystemFile(result);
    }

    public void showTable(IUITableModel model) {
        if (model.getRowCount() > 0) new TextTable(model).render(writer);
        this.info("Results: " + model.getRowCount());
    }

    public <T> T choose(String prompt, T defaultOption, T... options) {
        StringBuffer display = new StringBuffer();
        for (int o = 0; o < options.length; o++) {
            display.append(o + 1).append(") ").append(options[o]).append("\n");
        }
        display.append(prompt == null ? "Please choose " : prompt);
        if (defaultOption != null) display.append(" [default=").append(defaultOption).append("]");
        T selected = defaultOption;
        while (true) {
            display.append(": ");
            String result = this.captureRawInput(display.toString());
            if ("".equals(result) && selected != null) break;
            if ("".equals(result)) continue;
            int choice = -1;
            try {
                choice = Integer.parseInt(result);
            } catch (NumberFormatException e) {
                writer.println("Could not understand: " + result);
                continue;
            }
            if (choice < 1 || choice > options.length) {
                writer.println("Choice out of range: " + result);
                continue;
            }
            selected = options[choice - 1];
            break;
        }
        return selected;
    }

    public Object ask(String msg, Object defaultValue) {
        String result = this.captureRawInput(msg + (defaultValue == null ? "" : " [default=" + defaultValue + "]") + ": ");
        return "".equals(result.trim()) ? defaultValue : result;
    }

    public boolean confirm(String msg, boolean defaultValue) {
        String result = this.captureRawInput(msg + (defaultValue ? " [Y/n]: " : " [y/N]: "));
        return "".equals(result.trim()) ? defaultValue : "y".equals(result);
    }

    public void writeRaw(byte[] data, int offset, int len) {
        writer.print(new String(data, offset, len));
    }

    public void info(String msg) {
        writer.println(msg);
    }

    public void error(String msg, Throwable e) {
        writer.println("ERROR: " + msg);
        if (e != null) e.printStackTrace(writer);
    }

    public void exit() {
        this.running = false;
    }

    /**
	 * @return The custom context for this CLI
	 */
    protected abstract CommandLineContext createContext(LineNumberReader input, PrintStream writer);

    /**
	 * @return The custom command set for this CLI
	 */
    protected abstract IUICommandSet<? extends IUICommand, ? extends IUICommandContext> createCommandSet();

    /**
	 * @return The next command and possible arguments from the input
	 * 
	 * @see #captureRawInput(String)
	 */
    protected String[] captureCommand(String prompt) {
        return this.captureRawInput(prompt).split(" ");
    }

    /**
	 * If there is no more input available, a UI exception will be thrown
	 * 
	 * @param prompt The prompt to show, or null if no prompt
	 * @return The data read, or null if no more input can be obtained
	 * 
	 * @throws UserInterfaceException
	 */
    protected String captureRawInput(String prompt) {
        try {
            if (prompt != null) writer.print("\n" + prompt);
            String read = reader.readLine();
            if (read == null) throw new UserInterfaceException(this, "No more input available for CLI");
            if (echo) writer.println(read);
            return read;
        } catch (IOException e) {
            throw ThrowableManagerRegistry.caught(new UserInterfaceException(this, e));
        }
    }

    public void shutdown() {
        this.running = false;
        this.cleanup();
    }

    public Severity getAlertSeverityLimit() {
        return this.severity;
    }

    public void setAlertSeverityLimit(Severity severity) {
        this.severity = severity;
    }

    public <S, R extends UICommonResponse> R handle(UICommonEvent<S, R> evt) throws UIFailureException {
        if (evt instanceof UIConfirmationEvent) {
            try {
                UIConfirmationEvent confirm = (UIConfirmationEvent) evt;
                boolean trying = true;
                while (trying) {
                    this.controller = null;
                    this.controllingThread.interrupt();
                    String options = null;
                    if (confirm.getAnswer() == null) options = "[yes/no/cancel]"; else {
                        switch(confirm.getAnswer()) {
                            case Yes:
                                options = "[YES/no/cancel]";
                                break;
                            case No:
                                options = "[yes/NO/cancel]";
                                break;
                            case Cancel:
                                options = "[yes/no/CANCEL]";
                                break;
                        }
                        options += "  [only press ENTER for default]";
                    }
                    String[] results = this.captureCommand(confirm.getMessage() + " " + options + ": ");
                    if (results.length > 0 && !"".equals(results[0].trim())) {
                        if ("yes".equalsIgnoreCase(results[0])) confirm.setAnswer(Answer.Yes); else if ("no".equalsIgnoreCase(results[1])) confirm.setAnswer(Answer.No); else if ("cancel".equalsIgnoreCase(results[2])) confirm.setAnswer(Answer.Cancel); else confirm.setAnswer(null);
                    }
                    if (confirm.getAnswer() != null) trying = false;
                }
                return (R) confirm;
            } finally {
                this.controller = this;
            }
        } else if (evt instanceof UIAlertEvent) {
            if (((UIAlertEvent) evt).getSeverity().compareTo(severity) >= 0) writer.println(((UIAlertEvent) evt).getMessage());
        }
        return null;
    }
}
