package com.google.code.cubeirc.ui;

import java.util.ArrayList;
import java.util.Iterator;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Level;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.wb.swt.SWTResourceManager;
import com.google.code.cubeirc.Main;
import com.google.code.cubeirc.base.Base;
import com.google.code.cubeirc.basequeue.BaseQueue;
import com.google.code.cubeirc.common.ApplicationInfo;
import com.google.code.cubeirc.common.HTTPClient;
import com.google.code.cubeirc.config.Configuration;
import com.google.code.cubeirc.config.ConnectionSettings;
import com.google.code.cubeirc.connection.Connection;
import com.google.code.cubeirc.connection.MessagesFormat;
import com.google.code.cubeirc.connection.data.ChannelErrorResponse;
import com.google.code.cubeirc.connection.data.GenericChannelResponse;
import com.google.code.cubeirc.connection.data.GenericTargetResponse;
import com.google.code.cubeirc.connection.data.GenericUserResponse;
import com.google.code.cubeirc.connection.data.PrivateMessageResponse;
import com.google.code.cubeirc.dialogs.AlertBox;
import com.google.code.cubeirc.dialogs.InputDialog;
import com.google.code.cubeirc.editor.HistoryAdapter;
import com.google.code.cubeirc.editor.EditorManager;
import com.google.code.cubeirc.queue.MessageQueue;
import com.google.code.cubeirc.queue.MessageQueueEnum;
import com.google.code.cubeirc.queue.MessageQueueEvent;
import com.google.code.cubeirc.room.RoomManager;
import com.google.code.cubeirc.scripting.ScriptManager;
import com.google.code.cubeirc.tab.TabManager;

public class MainForm extends Base {

    public MainForm(String name) {
        super(name);
    }

    @Getter
    protected Shell shell;

    @Getter
    @Setter
    private Connection connection;

    @Getter
    @Setter
    private RoomManager roommanager;

    @Getter
    @Setter
    private EditorManager editor;

    @Getter
    @Setter
    private EditorManager InputEditor;

    private MenuItem mntmConfiguration;

    private MenuItem mntmDebug;

    private Menu menu_5;

    private MenuItem mntmShowDebugger;

    private Menu mServers;

    private ToolItem tltmDisconnect;

    private MenuItem mntmAbout;

    private MenuItem mntmChannels;

    private Menu menu_7;

    private MenuItem mntmJoinInC;

    private ToolItem tltmJoinInChannel;

    private MenuItem mntmDownloads;

    public void open() {
        Display display = Display.getDefault();
        createContents();
        shell.open();
        shell.layout();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    /**
	 * Create contents of the window.
	 * @wbp.parser.entryPoint
	 */
    @SuppressWarnings("unused")
    protected void createContents() {
        shell = new Shell();
        shell.addDisposeListener(new DisposeListener() {

            public void widgetDisposed(DisposeEvent arg0) {
                int result = SWT.YES;
                if (getConnection().isConnected()) {
                    AlertBox ab = new AlertBox(getShell(), ApplicationInfo.APP_FULL, "You are connected to server!\nAre you sure to exit?", SWT.ICON_QUESTION, SWT.YES | SWT.NO);
                    result = ab.go();
                }
                if (result == SWT.YES) BaseQueue.Close();
            }
        });
        shell.setImage(SWTResourceManager.getImage(MainForm.class, ApplicationInfo.APP_ICON));
        shell.setSize(764, 518);
        shell.setText(ApplicationInfo.APP_FULL);
        shell.setLayout(new FormLayout());
        Main.setShell(shell);
        Menu menu = new Menu(shell, SWT.BAR);
        shell.setMenuBar(menu);
        MenuItem mntmfile = new MenuItem(menu, SWT.CASCADE);
        mntmfile.setText("&File");
        Menu menu_1 = new Menu(mntmfile);
        mntmfile.setMenu(menu_1);
        final MenuItem mntmServers = new MenuItem(menu_1, SWT.CASCADE);
        mntmServers.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_servers.gif"));
        mntmServers.setText("&Servers");
        mServers = new Menu(mntmServers);
        mntmServers.setMenu(mServers);
        final MenuItem mntmAddremoveServers = new MenuItem(mServers, SWT.NONE);
        mntmAddremoveServers.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                TabManager.addTab(ApplicationInfo.TAB_SERVERS, "/com/google/code/cubeirc/resources/img_servers.gif", true, ServerForm.class.getName(), new Object[] { TabManager.getTabfolder().getParent(), SWT.NORMAL, ApplicationInfo.TAB_SERVERS }, new Class[] { Composite.class, int.class, String.class });
            }
        });
        mntmAddremoveServers.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_servers.gif"));
        mntmAddremoveServers.setText("Add/Remove servers");
        new MenuItem(mServers, SWT.SEPARATOR);
        this.mntmConfiguration = new MenuItem(menu_1, SWT.NONE);
        this.mntmConfiguration.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                TabManager.addTab(ApplicationInfo.TAB_CONFIG, "/com/google/code/cubeirc/resources/img_config.png", true, ConfigForm.class.getName(), new Object[] { TabManager.getTabfolder().getParent(), SWT.NORMAL, ApplicationInfo.TAB_CONFIG }, new Class[] { Composite.class, int.class, String.class });
            }
        });
        this.mntmConfiguration.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_config.png"));
        this.mntmConfiguration.setText("&Configuration");
        MenuItem menuItem = new MenuItem(menu_1, SWT.SEPARATOR);
        MenuItem mntmexit = new MenuItem(menu_1, SWT.NONE);
        mntmexit.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                getShell().dispose();
            }
        });
        mntmexit.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_exit.png"));
        mntmexit.setText("&Exit");
        MenuItem mntmCubeirc = new MenuItem(menu, SWT.CASCADE);
        mntmCubeirc.setText("&CubeIRC");
        Menu menu_2 = new Menu(mntmCubeirc);
        mntmCubeirc.setMenu(menu_2);
        this.mntmChannels = new MenuItem(menu_2, SWT.CASCADE);
        this.mntmChannels.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_channel.png"));
        this.mntmChannels.setText("Channels");
        this.menu_7 = new Menu(this.mntmChannels);
        this.mntmChannels.setMenu(this.menu_7);
        MenuItem mntmFetchChannelsList = new MenuItem(menu_7, SWT.NONE);
        mntmFetchChannelsList.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                TabManager.addTab(ApplicationInfo.TAB_CHANNELSLIST, "/com/google/code/cubeirc/resources/img_refresh.png", true, ListChannelsForm.class.getName(), new Object[] { TabManager.getTabfolder().getParent(), SWT.NORMAL, ApplicationInfo.TAB_CHANNELSLIST }, new Class[] { Composite.class, int.class, String.class });
            }
        });
        mntmFetchChannelsList.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_refresh.png"));
        mntmFetchChannelsList.setText("Fetch channels list");
        new MenuItem(menu_7, SWT.SEPARATOR);
        this.mntmJoinInC = new MenuItem(this.menu_7, SWT.NONE);
        this.mntmJoinInC.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                InputjoinChannel();
            }
        });
        this.mntmJoinInC.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_channel.png"));
        this.mntmJoinInC.setText("Join in channel");
        this.mntmJoinInC.setAccelerator(SWT.CTRL + 'j');
        MenuItem mntmMessages = new MenuItem(menu_2, SWT.CASCADE);
        mntmMessages.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_message.png"));
        mntmMessages.setText("Messages");
        Menu menu_6 = new Menu(mntmMessages);
        mntmMessages.setMenu(menu_6);
        MenuItem mntmSendPrivateMessage = new MenuItem(menu_6, SWT.NONE);
        mntmSendPrivateMessage.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                mSendPrivateMessage();
            }
        });
        mntmSendPrivateMessage.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_send.png"));
        mntmSendPrivateMessage.setText("Send private message");
        this.mntmDownloads = new MenuItem(menu_2, SWT.NONE);
        this.mntmDownloads.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                TabManager.addTab(ApplicationInfo.TAB_DOWNLOADS, "/com/google/code/cubeirc/resources/img_send.png", true, DownloadsForm.class.getName(), new Object[] { TabManager.getTabfolder().getParent(), SWT.NORMAL, ApplicationInfo.TAB_DOWNLOADS }, new Class[] { Composite.class, int.class, String.class });
            }
        });
        this.mntmDownloads.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_sendfile.png"));
        this.mntmDownloads.setText("Downloads");
        new MenuItem(menu_2, SWT.SEPARATOR);
        MenuItem mntmplugins = new MenuItem(menu_2, SWT.CASCADE);
        mntmplugins.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_plugin.png"));
        mntmplugins.setText("&Plugins");
        Menu menu_4 = new Menu(mntmplugins);
        mntmplugins.setMenu(menu_4);
        new MenuItem(menu_2, SWT.SEPARATOR);
        this.mntmDebug = new MenuItem(menu_2, SWT.CASCADE);
        this.mntmDebug.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_debugger.png"));
        this.mntmDebug.setText("Debugger");
        this.menu_5 = new Menu(this.mntmDebug);
        this.mntmDebug.setMenu(this.menu_5);
        this.mntmShowDebugger = new MenuItem(this.menu_5, SWT.NONE);
        this.mntmShowDebugger.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                TabManager.addTab(ApplicationInfo.TAB_DEBUGGER, "/com/google/code/cubeirc/resources/img_debugger.png", true, DebuggerForm.class.getName(), new Object[] { TabManager.getTabfolder().getParent(), SWT.NORMAL, ApplicationInfo.TAB_DEBUGGER }, new Class[] { Composite.class, int.class, String.class });
            }
        });
        this.mntmShowDebugger.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_debugger.png"));
        this.mntmShowDebugger.setText("Show debugger");
        MenuItem mntmHelp = new MenuItem(menu, SWT.CASCADE);
        mntmHelp.setText("&Help");
        Menu menu_3 = new Menu(mntmHelp);
        mntmHelp.setMenu(menu_3);
        this.mntmAbout = new MenuItem(menu_3, SWT.NONE);
        this.mntmAbout.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                new AboutForm(getShell(), SWT.BORDER).open();
            }
        });
        this.mntmAbout.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_info.png"));
        this.mntmAbout.setText("About");
        ToolBar tbQuickLunch = new ToolBar(shell, SWT.FLAT | SWT.WRAP | SWT.RIGHT | SWT.SHADOW_OUT);
        FormData fd_tbQuickLunch = new FormData();
        fd_tbQuickLunch.top = new FormAttachment(0);
        fd_tbQuickLunch.left = new FormAttachment(0);
        fd_tbQuickLunch.right = new FormAttachment(100);
        tbQuickLunch.setLayoutData(fd_tbQuickLunch);
        CTabFolder tbMain = new CTabFolder(shell, SWT.BORDER);
        tbMain.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        tbMain.setSimple(false);
        fd_tbQuickLunch.bottom = new FormAttachment(tbMain, -1);
        ToolItem tltmQuick = new ToolItem(tbQuickLunch, SWT.NONE);
        tltmQuick.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                new QuickConnectForm(getShell(), SWT.NORMAL, getConnection()).open();
            }
        });
        tltmQuick.setToolTipText("Quick connect");
        tltmQuick.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_quickconnect.png"));
        ToolItem toolItem = new ToolItem(tbQuickLunch, SWT.SEPARATOR);
        tltmDisconnect = new ToolItem(tbQuickLunch, SWT.NONE);
        this.tltmDisconnect.setToolTipText("Disconnect");
        tltmDisconnect.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                getConnection().Disconnect();
            }
        });
        tltmDisconnect.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_disconnect.png"));
        tltmDisconnect.setEnabled(false);
        final ToolItem toolItem_1 = new ToolItem(tbQuickLunch, SWT.SEPARATOR);
        final ToolItem tltmConfiguration = new ToolItem(tbQuickLunch, SWT.NONE);
        tltmConfiguration.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                TabManager.addTab(ApplicationInfo.TAB_CONFIG, "/com/google/code/cubeirc/resources/img_config.png", true, ConfigForm.class.getName(), new Object[] { TabManager.getTabfolder().getParent(), SWT.NORMAL, ApplicationInfo.TAB_CONFIG }, new Class[] { Composite.class, int.class, String.class });
            }
        });
        tltmConfiguration.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_config.png"));
        tltmConfiguration.setToolTipText("Configuration");
        final ToolItem toolItem_2 = new ToolItem(tbQuickLunch, SWT.SEPARATOR);
        ToolItem tltmSendprivatemessage = new ToolItem(tbQuickLunch, SWT.NONE);
        tltmSendprivatemessage.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                mSendPrivateMessage();
            }
        });
        tltmSendprivatemessage.setToolTipText("Send private message");
        tltmSendprivatemessage.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_send.png"));
        this.tltmJoinInChannel = new ToolItem(tbQuickLunch, SWT.NONE);
        this.tltmJoinInChannel.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                InputjoinChannel();
            }
        });
        this.tltmJoinInChannel.setToolTipText("Join in channel");
        this.tltmJoinInChannel.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_channel.png"));
        ToolItem toolItem_3 = new ToolItem(tbQuickLunch, SWT.SEPARATOR);
        final ToolItem tltmDebugger = new ToolItem(tbQuickLunch, SWT.NONE);
        tltmDebugger.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                TabManager.addTab(ApplicationInfo.TAB_DEBUGGER, "/com/google/code/cubeirc/resources/img_debugger.png", true, DebuggerForm.class.getName(), new Object[] { TabManager.getTabfolder().getParent(), SWT.NORMAL, ApplicationInfo.TAB_DEBUGGER }, new Class[] { Composite.class, int.class, String.class });
            }
        });
        tltmDebugger.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_debugger.png"));
        tltmDebugger.setToolTipText("Debugger");
        FormData fd_tbMain = new FormData();
        fd_tbMain.right = new FormAttachment(100);
        fd_tbMain.bottom = new FormAttachment(100);
        fd_tbMain.top = new FormAttachment(0, 26);
        fd_tbMain.left = new FormAttachment(0);
        tbMain.setLayoutData(fd_tbMain);
        tbMain.setBorderVisible(false);
        tbMain.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
        CTabItem tbtmconsole = new CTabItem(tbMain, SWT.NONE);
        tbtmconsole.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_console.png"));
        tbtmconsole.setText("Console");
        Composite c_console = new Composite(tbMain, SWT.NONE);
        tbtmconsole.setControl(c_console);
        c_console.setLayout(new FormLayout());
        StyledText stConsole = new StyledText(c_console, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI | SWT.READ_ONLY);
        stConsole.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseDown(MouseEvent arg0) {
            }
        });
        FormData fd_stConsole = new FormData();
        fd_stConsole.top = new FormAttachment(0);
        fd_stConsole.right = new FormAttachment(100);
        stConsole.setLayoutData(fd_stConsole);
        final StyledText stInput = new StyledText(c_console, SWT.BORDER | SWT.SINGLE);
        stInput.addKeyListener(new HistoryAdapter());
        stInput.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent arg0) {
                if (arg0.keyCode == 13) {
                    String cmd = stInput.getText();
                    if (cmd.startsWith("/")) {
                        ScriptManager.ParseCmd(stInput.getText());
                    }
                    stInput.setText("");
                }
                super.keyPressed(arg0);
            }
        });
        fd_stConsole.bottom = new FormAttachment(stInput, -2);
        fd_stConsole.left = new FormAttachment(stInput, 0, SWT.LEFT);
        FormData fd_stInput = new FormData();
        fd_stInput.left = new FormAttachment(0);
        fd_stInput.right = new FormAttachment(100);
        fd_stInput.bottom = new FormAttachment(100);
        stInput.setLayoutData(fd_stInput);
        tbMain.setSelection(tbtmconsole);
        setEditor(new EditorManager(ApplicationInfo.EDT_CONSOLE_NAME, stConsole));
        setInputEditor(new EditorManager(ApplicationInfo.EDT_INPUTCONSOLE_NAME, stInput));
        getInputEditor().setEditable(true);
        TabManager.setTabfolder(tbMain);
        initServermenu();
        DownloadMOTD();
        initConnection();
        initRoomManager();
    }

    private void initConnection() {
        setConnection(new Connection("Connection"));
    }

    private void initRoomManager() {
        setRoommanager(new RoomManager("RoomManager", getConnection()));
    }

    private void initServermenu() {
        asyncExec(new Runnable() {

            @Override
            public void run() {
                addDebug(Level.TRACE, "Adding quick server menu");
                ArrayList<ConnectionSettings> l_cs = Configuration.getCurrentCSSettings();
                if (l_cs != null) {
                    int i = 1;
                    for (Iterator<ConnectionSettings> it = l_cs.iterator(); it.hasNext(); ) {
                        final ConnectionSettings cs = it.next();
                        MenuItem mi = new MenuItem(mServers, SWT.NORMAL);
                        mi.setImage(SWTResourceManager.getImage(MainForm.class, "/com/google/code/cubeirc/resources/img_server.png"));
                        mi.setText(String.format("%s ALT+%s", cs.getConnectionName(), i));
                        mi.setAccelerator(SWT.ALT + Integer.toString(i).toCharArray()[0]);
                        mi.addSelectionListener(new SelectionAdapter() {

                            @Override
                            public void widgetSelected(SelectionEvent arg0) {
                                getConnection().Connect(Configuration.getCurrentUSConfig(), cs);
                                super.widgetSelected(arg0);
                            }
                        });
                        i++;
                    }
                }
            }
        });
    }

    private void InputjoinChannel() {
        String res = new InputDialog(getShell(), SWT.NORMAL, "Join in channel(s)", "Enter channel(s) you want join (comma separated)", "").open().toString();
        if (res != "") {
            MessageQueue.addQueue(MessageQueueEnum.CHANNEL_USR_JOIN, res);
        }
    }

    private void DownloadMOTD() {
        asyncExec(new Runnable() {

            @Override
            public void run() {
                try {
                    MessageQueue.addQueue(MessageQueueEnum.GLOBAL, "Downloading [APP_NAME] motd...");
                    String[] app_motd = HTTPClient.sendGetRequest(ApplicationInfo.APP_MOTD_URL, "").split("\n");
                    for (int i = 0; i < app_motd.length; i++) {
                        MessageQueue.addQueue(MessageQueueEnum.GLOBAL, app_motd[i]);
                    }
                } catch (Exception ex) {
                    addDebug(Level.WARN, "Error during download motd! error: %s", ex.getMessage());
                    MessageQueue.addQueue(MessageQueueEnum.GLOBAL, "Error during download motd!");
                    MessageQueue.addQueue(MessageQueueEnum.GLOBAL, String.format("Error: %s", ex.getMessage()));
                }
            }
        });
    }

    @Override
    public void actionPerformed(MessageQueueEvent e) {
        final MessageQueueEvent ev = e;
        if (e.getMsgtype() == MessageQueueEnum.GLOBAL || e.getMsgtype() == MessageQueueEnum.CONSOLE) {
            getEditor().addText(ApplicationInfo.CLS_COLOR_GLOBAL, String.format(MessagesFormat.MSG_GLOBAL, e.getData().toString()));
        }
        if (e.getMsgtype() == MessageQueueEnum.IRC_NOTICE) {
            GenericUserResponse gr = (GenericUserResponse) e.getData();
            getEditor().addText(ApplicationInfo.CLS_COLOR_NOTICE, String.format(MessagesFormat.MSG_NOTICE, gr.getSender().getNick(), gr.getText()));
        }
        if (e.getMsgtype() == MessageQueueEnum.IRC_MOTD) {
            getEditor().addText(ApplicationInfo.CLS_COLOR_GLOBAL, String.format(MessagesFormat.MSG_MOTD, e.getData().toString()));
        }
        if (e.getMsgtype() == MessageQueueEnum.IRC_MODE) {
            GenericChannelResponse gcr = (GenericChannelResponse) e.getData();
            getEditor().addText(ApplicationInfo.CLS_COLOR_MODE, String.format(MessagesFormat.MSG_MODE, gcr.getUser().getNick(), gcr.getMessage()));
        }
        if (e.getMsgtype() == MessageQueueEnum.IRC_USERMODE) {
            GenericTargetResponse gtr = (GenericTargetResponse) e.getData();
            getEditor().addText(ApplicationInfo.CLS_COLOR_MODE, String.format(MessagesFormat.MSG_USERMODE, gtr.getSender().getNick(), gtr.getTarget().getNick(), gtr.getMessage()));
        }
        if (e.getMsgtype() == MessageQueueEnum.IRC_CONNECTED) {
            getEditor().addText(ApplicationInfo.CLS_COLOR_GLOBAL, "Connected!");
            asyncExec(new Runnable() {

                @Override
                public void run() {
                    tltmDisconnect.setEnabled(true);
                }
            });
        }
        if (e.getMsgtype() == MessageQueueEnum.IRC_DISCONNECTED) {
            getEditor().addText(ApplicationInfo.CLS_COLOR_GLOBAL, "Disconnected!");
            asyncExec(new Runnable() {

                @Override
                public void run() {
                    tltmDisconnect.setEnabled(false);
                }
            });
        }
        if (e.getMsgtype() == MessageQueueEnum.IRC_CONNECTING) {
            getEditor().addText(ApplicationInfo.CLS_COLOR_GLOBAL, "Connecting to %s:%s", ((ConnectionSettings) e.getData()).getServer(), ((ConnectionSettings) e.getData()).getPort());
        }
        if (e.getMsgtype() == MessageQueueEnum.IRC_ERROR_CHANNEL) {
            ChannelErrorResponse cer = (ChannelErrorResponse) e.getData();
            getEditor().addText(ApplicationInfo.CLS_COLOR_ERROR, String.format(MessagesFormat.MSG_CHANNEL_ERROR, cer.getChannel().getName(), cer.getMessage()));
        }
        if (e.getMsgtype() == MessageQueueEnum.DCC_FILE_INCOMING || e.getMsgtype() == MessageQueueEnum.DCC_FILE_OUTCOMING) {
            asyncExec(new Runnable() {

                @Override
                public void run() {
                    if (!TabManager.checkTabExists(ApplicationInfo.TAB_DOWNLOADS)) {
                        TabManager.addTab(ApplicationInfo.TAB_DOWNLOADS, "/com/google/code/cubeirc/resources/img_send.png", true, DownloadsForm.class.getName(), new Object[] { TabManager.getTabfolder().getParent(), SWT.NORMAL, ApplicationInfo.TAB_DOWNLOADS }, new Class[] { Composite.class, int.class, String.class });
                        MessageQueue.addQueue(ev.getMsgtype(), ev.getData());
                    }
                }
            });
        }
        super.actionPerformed(e);
    }

    private void mSendPrivateMessage() {
        asyncExec(new Runnable() {

            @Override
            public void run() {
                String nickname = new InputDialog(getShell(), SWT.NORMAL, "Send private message", "Enter nickname:", "").open().toString();
                if (nickname != "") {
                    String message = new InputDialog(getShell(), SWT.NORMAL, "Send private message", "Enter message:", "").open().toString();
                    if (message != "") {
                        MessageQueue.addQueue(MessageQueueEnum.MSG_PRIVATE_OUT, new PrivateMessageResponse(Connection.getUserInfo(), Connection.getIrcclient().getUser(nickname), message));
                    }
                }
            }
        });
    }
}
