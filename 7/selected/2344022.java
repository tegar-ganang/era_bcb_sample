package org.charvolant.tmsnet.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.apache.log4j.xml.DOMConfigurator;
import org.charvolant.tmsnet.model.EventDescription;
import org.charvolant.tmsnet.model.ReservationType;
import org.charvolant.tmsnet.model.ServiceType;
import org.charvolant.tmsnet.model.TimerDescription;
import org.charvolant.tmsnet.protocol.TMSNetInterface;
import org.charvolant.tmsnet.protocol.TestServer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A console for sending commands to the TMSCommander TAP and showing the results.
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
public class TestConsole implements PropertyChangeListener, DisposeListener {

    private static final Logger logger = LoggerFactory.getLogger(TestConsole.class);

    /** The logging configuration to use */
    public static final String LOGGING_CONFIG = "log4j.xml";

    private static final Command[] COMMAND_TABLE = { new Command("connect", null, "Connect to the server at host/port", "doConnect"), new Command("changechannel", null, "Change the currrent channel", "doChangeChannel"), new Command("closefile", null, "Close a file", "doCloseFile"), new Command("createdirectory", null, "Create a directory", "doCreateDirectory"), new Command("createfile", null, "Create a file", "doCreateFile"), new Command("deletefile", null, "Delete a file", "doDeleteFile"), new Command("getchannellist", null, "Get information about the TV/RADIO channels for a specific type", "doGetChannelList"), new Command("getcurrentchannel", null, "Get information about the current channel", "doGetCurrentChannel"), new Command("getdisksizes", null, "Get storage information", "doGetDiskSizes"), new Command("getfile", null, "Get a segement of file data", "doGetFile"), new Command("getfileinfos", null, "Get information files for a recording", "doGetFileInfos"), new Command("getonedaysevents", null, "Get one days worth of events", "doGetOneDaysEvents"), new Command("getplayinfo", null, "Get current playing information", "doGetPlayInfo"), new Command("getrecinfo", null, "Get current recording information", "doGetRecInfo"), new Command("getsysinfo", null, "Get system information", "doGetSysInfo"), new Command("gettimerslist", null, "Get  a list of the current timers", "doGetTimersList"), new Command("addtimer", null, "Add a timer", "doAddTimer"), new Command("deletetimer", null, "Delete a timer", "doDeleteTimer"), new Command("modifytimer", null, "Modify a timer", "doModifyTimer"), new Command("listfiles", null, "List files", "doListFiles"), new Command("openfile", null, "Open a file", "doOpenFile"), new Command("playts", null, "Play recording", "doPlayTS"), new Command("renamefile", null, "Rename a file", "doRenameFile"), new Command("startrecord", null, "Start recording", "doStartRecord"), new Command("stoprecord", null, "Stop a recording", "doStopRecord"), new Command("setrecordduration", null, "Set the recording duration", "doSetRecordDuration"), new Command("stopts", null, "Stop playing recording", "doStopTS"), new Command("help", "h", "Show help", "doHelp"), new Command("ping", "p", "Ping the server", "doPing"), new Command("quit", "q", "Exit the console", "doQuit"), new Command("server", null, "Toggle the test server on/off", "doServer"), new Command("status", null, "Show the status of the console", "doStatus"), new Command("stop", null, "Stop the connection to the server", "doStop") };

    /** The interface */
    private TMSNetInterface intf;

    /** The test server, if needed */
    private TestServer testServer;

    /** The xml representation context */
    private JAXBContext context;

    /** The display for this console */
    private Display display;

    /** The shell to use */
    private Shell shell;

    /** The command widget */
    private Text command;

    /** The result widget */
    private Text result;

    /** The label contents */
    private String contents;

    /**
   * Construct a test console
   * 
   * @throws Exception if unable to construct the context or the UI
   */
    public TestConsole() throws Exception {
        this.context = JAXBContext.newInstance("org.charvolant.tmsnet.command:org.charvolant.tmsnet.model:org.charvolant.tmsnet.protocol");
        this.buildUI();
    }

    protected void buildUI() {
        GridLayout layout = new GridLayout(1, false);
        GridData data;
        this.display = new Display();
        this.shell = new Shell(this.display);
        this.shell.setLayout(layout);
        this.shell.setText("Test Console");
        this.shell.addDisposeListener(this);
        this.command = new Text(this.shell, SWT.SINGLE | SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        data.widthHint = 400;
        this.command.setLayoutData(data);
        this.command.addListener(SWT.DefaultSelection, new Listener() {

            @Override
            public void handleEvent(Event event) {
                TestConsole.this.executeCommand();
            }
        });
        this.result = new Text(this.shell, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
        data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.widthHint = 400;
        data.heightHint = 300;
        this.result.setLayoutData(data);
        this.result.setText("Enter help for help");
        this.shell.pack();
        this.shell.open();
    }

    /**
   * Send a command for execution
   * 
   * @param command The command
   */
    protected void sendCommand(Object command) {
        if (this.intf == null) {
            this.result.setText("Not connected");
            return;
        }
        this.intf.queue(command);
        this.result.setText("Sent:\n" + this.displayObject(command));
    }

    protected void executeCommand() {
        String[] cmd = this.command.getText().split("[ \t]+");
        String verb = cmd[0];
        this.command.setText("");
        for (int i = 0; i < this.COMMAND_TABLE.length; i++) if (verb.equals(this.COMMAND_TABLE[i].getAbbrev()) || verb.equals(this.COMMAND_TABLE[i].getCommand())) {
            this.COMMAND_TABLE[i].execute(this, cmd);
            return;
        }
        this.doError("Unknown command '" + verb + "'");
    }

    /**
   * Print a help description.
   */
    protected void doHelp() {
        StringBuilder builder = new StringBuilder(256);
        for (int i = 0; i < this.COMMAND_TABLE.length; i++) {
            builder.append(this.COMMAND_TABLE[i].getHelpLine());
            builder.append('\n');
        }
        this.result.setText(builder.toString());
    }

    /**
   * Connect the interface to a specific server
   * 
   * @param cmd The command arguments
   */
    protected void doConnect(String host, int port) {
        InetSocketAddress addr;
        if (this.intf != null) {
            this.doError("Already connected to " + this.intf.getServer());
            return;
        }
        try {
            addr = new InetSocketAddress(host, port);
            this.intf = new TMSNetInterface(addr);
            this.intf.start();
            this.intf.addPropertyChangeListener(this);
            this.result.setText("Connected to " + addr);
        } catch (IllegalArgumentException ex) {
            this.doError("Invalid host/port combination");
            return;
        }
    }

    /**
   * Get the TV channel list
   */
    protected void doGetChannelList(int type) {
        ServiceType ct = type == 0 ? ServiceType.TV : ServiceType.RADIO;
        this.sendCommand(new org.charvolant.tmsnet.command.GetChannelList(ct));
    }

    /**
   * Get the timers list
   */
    protected void doGetTimersList() {
        this.sendCommand(new org.charvolant.tmsnet.command.GetTimersList());
    }

    /**
   * Change the channel
   */
    protected void doChangeChannel(int channel) {
        this.sendCommand(new org.charvolant.tmsnet.command.ChangeChannel(channel));
    }

    /**
   * Change the channel
   */
    protected void doGetOneDaysEvents() {
        this.sendCommand(new org.charvolant.tmsnet.command.GetOneDaysEvents());
    }

    /**
   * Get the current channel
   */
    protected void doGetCurrentChannel() {
        this.sendCommand(new org.charvolant.tmsnet.command.GetCurrentChannel());
    }

    /**
   * Get storage information
   */
    protected void doGetDiskSizes() {
        this.sendCommand(new org.charvolant.tmsnet.command.GetDiskSizes());
    }

    /**
   * Get player information
   */
    protected void doGetPlayInfo() {
        this.sendCommand(new org.charvolant.tmsnet.command.GetPlayInfo());
    }

    /**
   * Get recording information
   */
    protected void doGetRecInfo() {
        this.sendCommand(new org.charvolant.tmsnet.command.GetRecInfo());
    }

    /**
   * Get system information
   */
    protected void doGetSysInfo() {
        this.sendCommand(new org.charvolant.tmsnet.command.GetSysInfo());
    }

    /**
   * Ping the server
   */
    protected void doPing() {
        this.sendCommand(new org.charvolant.tmsnet.command.Ping());
    }

    /**
   * Quit the application
   */
    protected void doQuit() {
        this.shell.close();
    }

    /**
   * Add a timer
   */
    protected void doAddTimer(int channel) {
        EventDescription event = new EventDescription();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.HOUR, 1);
        event.setDuration(30);
        event.setEventId(100);
        event.setStart(calendar.getTime());
        calendar.add(Calendar.MINUTE, event.getDuration());
        event.setFinish(calendar.getTime());
        event.setRunning(false);
        event.setServiceId(channel);
        event.setTitle("Test");
        event.setTransportStreamId(0);
        this.sendCommand(new org.charvolant.tmsnet.command.AddTimer(new org.charvolant.tmsnet.model.Event()));
    }

    /**
   * Modify a timer
   */
    protected void doModifyTimer(int slot, int channel) {
        TimerDescription timer = new TimerDescription();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MINUTE, 10);
        timer.setDuration(30);
        timer.setFileName("ATestTimer.mpg");
        timer.setFixFileName(true);
        timer.setRecording(true);
        timer.setReservationType(ReservationType.ONE_TIME);
        timer.setServiceId(channel);
        timer.setServiceType(ServiceType.TV);
        timer.setSlot(slot);
        timer.setStart(calendar.getTime());
        timer.setTuner(3);
        this.sendCommand(new org.charvolant.tmsnet.command.ModifyTimer(timer));
    }

    /**
   * Delete a timer
   */
    protected void doDeleteTimer(int slot) {
        TimerDescription timer = new TimerDescription();
        timer.setSlot(slot);
        this.sendCommand(new org.charvolant.tmsnet.command.DeleteTimer(timer));
    }

    /**
   * Get file information
   */
    protected void doGetFileInfos(String path) {
        this.sendCommand(new org.charvolant.tmsnet.command.GetFileInfos(path));
    }

    /**
   * List files
   */
    protected void doListFiles(String dir) {
        this.sendCommand(new org.charvolant.tmsnet.command.ListFiles(dir));
    }

    /**
   * Play recording
   */
    protected void doPlayTS(String file) {
        this.sendCommand(new org.charvolant.tmsnet.command.PlayTS(file));
    }

    /**
   * Stop playing a recording
   */
    protected void doStopTS() {
        this.sendCommand(new org.charvolant.tmsnet.command.StopTS());
    }

    /**
   * Start recording
   */
    protected void doStartRecord() {
        this.sendCommand(new org.charvolant.tmsnet.command.StartRecord());
    }

    /**
   * Stop recording
   */
    protected void doStopRecord(int slot) {
        this.sendCommand(new org.charvolant.tmsnet.command.StopRecord(slot));
    }

    /**
   * Set the recording duration
   */
    protected void doSetRecordDuration(int slot, int duration) {
        this.sendCommand(new org.charvolant.tmsnet.command.SetRecordDuration(slot, duration));
    }

    /**
   * Create a directory
   */
    protected void doCreateDirectory(String path) {
        this.sendCommand(new org.charvolant.tmsnet.command.CreateDirectory(path));
    }

    /**
   * Delete a file
   */
    protected void doDeleteFile(String path) {
        this.sendCommand(new org.charvolant.tmsnet.command.DeleteFile(path));
    }

    /**
   * Rename a file
   */
    protected void doRenameFile(String oldPath, String newPath) {
        this.sendCommand(new org.charvolant.tmsnet.command.RenameFile(oldPath, newPath));
    }

    /**
   * Create a file
   */
    protected void doCreateFile(String path) {
        this.sendCommand(new org.charvolant.tmsnet.command.CreateFile(path));
    }

    /**
   * Open a file
   */
    protected void doOpenFile(String path) {
        this.sendCommand(new org.charvolant.tmsnet.command.OpenFile(path));
    }

    /**
   * Close a file
   */
    protected void doCloseFile(int fd) {
        this.sendCommand(new org.charvolant.tmsnet.command.CloseFile(fd));
    }

    /**
   * Get from a file
   */
    protected void doGetFile(int fd) {
        this.sendCommand(new org.charvolant.tmsnet.command.GetFile(fd));
    }

    /**
   * Toggle the test server
   */
    protected void doServer() throws Exception {
        if (this.testServer == null) {
            this.testServer = new TestServer(new Date(), new Locale("en", "AU"));
            try {
                this.testServer.start();
            } catch (Exception ex) {
                this.result.setText("Unable to start server: " + ex.getMessage());
                try {
                    this.testServer.stop();
                } catch (Exception ex1) {
                }
                this.testServer = null;
            }
            this.result.setText("Started server on " + this.testServer.getAddress());
        } else {
            try {
                this.testServer.stop();
            } catch (Exception ex) {
                this.result.setText("Unable to stop server: " + ex.getMessage());
            }
            this.testServer = null;
            this.result.setText("Stopped server");
        }
    }

    /**
   * Get a status report
   */
    protected void doStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Status:\n");
        if (this.intf != null) {
            status.append("Connected to " + this.intf.getServer() + "\n");
        }
        if (this.testServer != null) {
            status.append("Test server running at " + this.testServer.getAddress() + "\n");
        }
        this.result.setText(status.toString());
    }

    /**
   * Stop the interface
   */
    protected void doStop() {
        if (this.intf == null) {
            this.result.setText("Interface not running");
            return;
        }
        this.intf.removePropertyChangeListener(this);
        this.intf.stop();
        this.intf = null;
        this.result.setText("Interface stopped");
    }

    /**
   * Report an error.
   * 
   * @param message The error message
   */
    protected void doError(String message) {
        this.result.setText(message + "\n" + "Use 'help' for help");
    }

    /**
   * Run the main dispatch loop for the system.
   */
    public void mainLoop() {
        while (!this.shell.isDisposed()) if (!this.display.readAndDispatch()) this.display.sleep();
        this.display.dispose();
    }

    /**
   * Build an XML representation of an object, if we can.
   * <p>
   * If we can't then return a {@link Object#toString()} representation
   * 
   * @param object The object
   * @return The XML reprsentation
   */
    protected String displayObject(Object object) {
        try {
            Marshaller marshaller = this.context.createMarshaller();
            StringWriter writer = new StringWriter(128);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(object, writer);
            return writer.toString();
        } catch (Exception ex) {
            this.logger.info("Unable to marshal " + object, ex);
            return object.toString();
        }
    }

    /**
   * Receive a notification from the server.
   *
   * @param evt The event
   * 
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        this.contents = this.displayObject(evt.getNewValue());
        this.display.asyncExec(new Runnable() {

            @SuppressWarnings("synthetic-access")
            public void run() {
                TestConsole.this.result.setText(TestConsole.this.contents);
            }
        });
    }

    /**
   * Shut everything down when we quit.
   * 
   * @param e The event
   */
    @Override
    public void widgetDisposed(DisposeEvent e) {
        try {
            if (this.testServer != null) this.testServer.stop();
            this.testServer = null;
            if (this.intf != null) this.intf.stop();
            this.intf = null;
        } catch (Exception ex) {
            this.logger.error("Unable to shut down", ex);
        }
    }

    public static void testLogConfiguration() throws Exception {
        DOMConfigurator.configure(TestConsole.class.getResource(LOGGING_CONFIG));
    }

    /**
   * Create a test console
   */
    public static void main(String[] args) throws Exception {
        TestConsole console = new TestConsole();
        testLogConfiguration();
        console.mainLoop();
    }

    private static class Command {

        /** The command name */
        private String command;

        /** The command abbreviation */
        private String abbrev;

        /** The command description */
        private String desc;

        /** The command method */
        private Method method;

        /**
     * Construct a command description
     * 
     * @param command
     * @param abbrev
     * @param desc
     * @param method
     */
        @SuppressWarnings("synthetic-access")
        public Command(String command, String abbrev, String desc, String method) {
            super();
            this.command = command;
            this.abbrev = abbrev;
            this.desc = desc;
            for (Method m : TestConsole.class.getDeclaredMethods()) if (m.getName().equals(method)) {
                this.method = m;
                return;
            }
            TestConsole.logger.warn("No method called " + method);
        }

        /**
     * @return the command
     */
        public String getCommand() {
            return this.command;
        }

        /**
     * @return the abbrev
     */
        public String getAbbrev() {
            return this.abbrev;
        }

        @SuppressWarnings("synthetic-access")
        public void execute(TestConsole console, String[] cmd) {
            Class<?>[] params = this.method.getParameterTypes();
            Object[] args = new Object[params.length];
            if (params.length > cmd.length - 1) {
                console.result.setText("Invalid number of command arguments");
                return;
            }
            for (int i = 0; i < params.length; i++) {
                if (params[i] == String.class) args[i] = cmd[i + 1]; else if (params[i] == int.class) {
                    try {
                        args[i] = Integer.parseInt(cmd[i + 1]);
                    } catch (NumberFormatException ex) {
                        console.result.setText("Value " + cmd[i + 1] + " is not a number");
                        return;
                    }
                } else {
                    console.logger.warn("Unable to parse class " + params[i]);
                    args[i] = null;
                }
            }
            try {
                this.method.invoke(console, args);
            } catch (Exception ex) {
                console.logger.error("Unable to call method", ex);
            }
        }

        public String getHelpLine() {
            StringBuilder builder = new StringBuilder(64);
            int tab = 24;
            builder.append(this.command);
            if (this.abbrev != null) {
                builder.append('/');
                builder.append(this.abbrev);
            }
            for (Class<?> param : this.method.getParameterTypes()) {
                builder.append(' ');
                if (param == String.class) builder.append("string"); else if (param == int.class) builder.append("integer"); else builder.append(param.toString());
            }
            tab -= builder.length();
            while (tab-- > 0) builder.append(' ');
            builder.append(this.desc);
            return builder.toString();
        }
    }
}
