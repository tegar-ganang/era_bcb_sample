package com.mindbright.ssh;

import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Vector;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.applet.AppletContext;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import javax.swing.JFrame;
import com.mindbright.ssh2.*;
import com.mindbright.terminal.*;
import com.mindbright.net.*;
import com.mindbright.gui.Logo;
import com.mindbright.application.MindTermApp;
import com.mindbright.application.ModuleTerminalImpl;
import com.mindbright.sshcommon.SSHConsoleRemote;
import com.mindbright.util.RandomSeed;

public final class SSHInteractiveClient extends SSHClient implements Runnable, SSHInteractor, SSH2Interactor, MindTermApp, TerminalInputListener {

    static Logger logger = Logger.getLogger(SSHInteractiveClient.class.getName());

    private Frame terminalFrame;

    private boolean sshToUnicoreJobDirectory = false;

    private String unicoreJobDirectory = "";

    public boolean isSSH2 = false;

    public SSH2Transport transport;

    SSH2Connection connection;

    SSH2TerminalAdapter termAdapter;

    public static boolean wantHelpInfo = true;

    public static String customStartMessage = null;

    SSHMenuHandler menus;

    SSHStdIO sshStdIO;

    SSHPropertyHandler propsHandler;

    public boolean quiet;

    public boolean exitOnLogout;

    boolean initQuiet;

    public SSHInteractiveClient(boolean quiet, boolean exitOnLogout, SSHPropertyHandler propsHandler) {
        super(propsHandler, propsHandler);
        this.propsHandler = propsHandler;
        this.interactor = this;
        propsHandler.setInteractor(this);
        propsHandler.setClient(this);
        this.quiet = quiet;
        this.exitOnLogout = exitOnLogout;
        this.initQuiet = quiet;
        setConsole(new SSHStdIO());
        sshStdIO = (SSHStdIO) console;
        sshStdIO.setClient(this);
    }

    public SSHInteractiveClient(SSHInteractiveClient clone) {
        this(true, true, new SSHPropertyHandler(clone.propsHandler));
        this.activateTunnels = false;
        this.wantHelpInfo = clone.wantHelpInfo;
        this.customStartMessage = clone.customStartMessage;
    }

    /**
	 * Indicates that ssh should provide access to Unicore Job
	 * Directory.
	 * @author Morris Riedel
	 */
    public void setSshToUnicoreJobDirectory(boolean sshToUnicoreJobDirectory) {
        this.sshToUnicoreJobDirectory = sshToUnicoreJobDirectory;
    }

    /**
	 * Indicates the directory to access with ssh.
	 * @author Morris Riedel
	 */
    public void setUnicoreJobDirectory(String unicoreJobDirectory) {
        this.unicoreJobDirectory = unicoreJobDirectory;
    }

    /**
	 * Getter for terminal frame.
	 * @author Morris Riedel 22.06.2005
	 */
    public Frame getTerminalFrame() {
        return terminalFrame;
    }

    /**
	 * Setter for terminal frame.
	 * @author Morris Riedel 22.06.2005
	 */
    public void setTerminalFrame(Frame frame) {
        terminalFrame = frame;
    }

    public void setMenus(SSHMenuHandler menus) {
        this.menus = menus;
    }

    public SSHPropertyHandler getPropertyHandler() {
        return propsHandler;
    }

    public void updateMenus() {
        if (menus != null) menus.update();
    }

    public void splashScreen() {
        TerminalWin t = getTerminalWin();
        if (t != null) {
            t.clearScreen();
            t.cursorSetPos(0, 0, false);
        }
        showLogo();
        if ((menus != null) && menus.havePopupMenu) {
            if (t != null) {
                t.cursorSetPos(t.rows() - 3, 0, false);
            }
            console.println("\r\33[2Kpress <ctrl> + <mouse-" + menus.getPopupButton() + "> for Menu");
        }
        if (propsHandler.getSSHHomeDir() != null) {
            if (t != null) {
                t.cursorSetPos(t.rows() - 2, 0, false);
            }
            console.println("\r\33[2KMindTerm home: " + propsHandler.getSSHHomeDir());
        }
        if (t != null) {
            t.cursorSetPos(t.rows() - 1, 0, false);
        }
    }

    public boolean installLogo() {
        boolean isPresent = false;
        TerminalWin t = getTerminalWin();
        if (t != null) {
            ByteArrayOutputStream baos = readResource("/defaults/logo.gif");
            if (baos != null) {
                byte[] img = baos.toByteArray();
                Image logo = Toolkit.getDefaultToolkit().createImage(img);
                int width = -1;
                int height = -1;
                boolean ready = false;
                while (!ready) {
                    width = logo.getWidth(null);
                    height = logo.getHeight(null);
                    if (width != -1 && height != -1) {
                        ready = true;
                    }
                    Thread.yield();
                }
                t.setLogo(logo, -1, -1, width, height);
                isPresent = true;
            }
        }
        return isPresent;
    }

    public ByteArrayOutputStream readResource(String name) {
        InputStream in = getClass().getResourceAsStream(name);
        ByteArrayOutputStream baos = null;
        if (in != null) {
            baos = new ByteArrayOutputStream();
            try {
                int c;
                while ((c = in.read()) >= 0) baos.write(c);
            } catch (IOException e) {
                System.err.println("ERROR reading resource " + name + " : " + e);
            }
        }
        return baos;
    }

    void initRandomSeed() {
        if (!SSH.haveSecureRandom()) {
            console.print("Initializing random generator, please wait...");
            SSH.initSeedGenerator();
            console.print("done");
        }
    }

    public void doSingleCommand(String commandLine) throws Exception {
        this.commandLine = commandLine;
        if (com.mindbright.util.Util.isNetscapeJava()) {
            try {
                netscape.security.PrivilegeManager.enablePrivilege("TerminalEmulator");
            } catch (netscape.security.ForbiddenTargetException e) {
                console.println("Unsigned applet, can only connect to www host, tunneling can't be used");
                console.println("");
            }
        }
        installLogo();
        splashScreen();
        initRandomSeed();
        startSSHClient(false);
    }

    public void run() {
        boolean gotExtMsg;
        boolean keepRunning = true;
        while (keepRunning) {
            gotExtMsg = false;
            try {
                splashScreen();
                initRandomSeed();
                startSSHClient(true);
                if (sshStdIO.isConnected()) {
                    sshStdIO.serverDisconnect("\n\r\n\rServer died or connection lost");
                    disconnect(false);
                    propsHandler.clearServerSetting();
                }
                Thread.sleep(1000);
                try {
                    propsHandler.checkSave();
                } catch (IOException e) {
                    alert("Error saving settings!");
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "[SSHPlugin] Exception in run: " + e.getMessage());
                alert("Error in: " + e.getMessage());
                if (terminalFrame != null) terminalFrame.dispose();
                logger.log(Level.FINE, "[SSHPlugin] Exiting terminal...");
            }
            propsHandler.passivateProperties();
            activateTunnels = true;
            if (!gotExtMsg) {
                if (!propsHandler.savePasswords || usedOTP) {
                    propsHandler.clearPasswords();
                }
                propsHandler.currentPropsFile = null;
                if (!propsHandler.autoLoadProps) {
                    propsHandler.clearPasswords();
                    initQuiet = false;
                }
                quiet = false;
            }
            controller = null;
            TerminalWin t = getTerminalWin();
            if (t != null) t.setTitle(null);
            keepRunning = !exitOnLogout;
        }
    }

    private void startSSHClient(boolean shell) throws Exception {
        bootSSH(shell, true);
        int major = 2;
        String proto = propsHandler.getProperty("protocol");
        if ("auto".equals(proto)) {
            sshSocket = new SSHVersionSpySocket(sshSocket);
            major = ((SSHVersionSpySocket) sshSocket).getMajorVersion();
        } else if ("ssh1".equals(proto)) {
            major = 1;
        }
        if (major == 1) {
            console.println("Warning connecting using ssh1, consider upgrading server!");
            console.println("");
            boot(shell, sshSocket);
            if (isDumb()) System.out.println("No console...");
            controller.waitForExit();
        } else {
            runSSH2Client();
        }
    }

    public boolean isDumb() {
        return (console.getTerminal() == null);
    }

    public TerminalWin getTerminalWin() {
        Terminal term = console.getTerminal();
        if (term != null && term instanceof TerminalWin) return (TerminalWin) term;
        return null;
    }

    public void showLogo() {
        TerminalWin t = getTerminalWin();
        if (t != null) {
            t.showLogo();
        }
    }

    public void hideLogo() {
        TerminalWin t = getTerminalWin();
        if (t != null) {
            t.hideLogo();
        }
    }

    public Logo getLogo() {
        Logo logo = null;
        TerminalWin t = getTerminalWin();
        if (t != null) {
            Image img = t.getLogo();
            logo = new Logo(img);
        }
        return logo;
    }

    public void updateTitle() {
        sshStdIO.updateTitle();
    }

    public String promptLine(String prompt, boolean echo) throws SSH2UserCancelException {
        try {
            if (echo) {
                return promptLine(prompt, "");
            } else {
                return promptPassword(prompt);
            }
        } catch (IOException e) {
            throw new SSH2UserCancelException(e.getMessage());
        }
    }

    public String[] promptMulti(String[] prompts, boolean[] echos) throws SSH2UserCancelException {
        return promptMultiFull(null, null, prompts, echos);
    }

    public String[] promptMultiFull(String name, String instruction, String[] prompts, boolean[] echos) throws SSH2UserCancelException {
        try {
            console.println(name);
            console.println(instruction);
            String[] resp = new String[prompts.length];
            for (int i = 0; i < prompts.length; i++) {
                if (echos[i]) {
                    resp[i] = promptLine(prompts[i], "");
                } else {
                    resp[i] = promptPassword(prompts[i]);
                }
            }
            return resp;
        } catch (IOException e) {
            throw new SSH2UserCancelException(e.getMessage());
        }
    }

    public int promptList(String name, String instruction, String[] choices) throws SSH2UserCancelException {
        try {
            console.println(name);
            console.println(instruction);
            for (int i = 0; i < choices.length; i++) {
                console.println(i + ") " + choices[i]);
            }
            String choice = promptLine("Choice", "0");
            return Integer.parseInt(choice);
        } catch (Exception e) {
            throw new SSH2UserCancelException(e.getMessage());
        }
    }

    public void propsStateChanged(SSHPropertyHandler props) {
        updateMenus();
    }

    public void startNewSession(SSHClient client) {
    }

    public void sessionStarted(SSHClient client) {
        quiet = initQuiet;
    }

    public boolean quietPrompts() {
        return (commandLine != null || quiet);
    }

    public boolean isVerbose() {
        return wantHelpInfo;
    }

    public String promptLine(String prompt, String defaultVal) throws IOException {
        return sshStdIO.promptLine(prompt, defaultVal, false);
    }

    public String promptPassword(String prompt) throws IOException {
        return sshStdIO.promptLine(prompt, "", true);
    }

    public boolean askConfirmation(String message, boolean defAnswer) {
        boolean confirm = false;
        try {
            confirm = askConfirmation(message, true, defAnswer);
        } catch (IOException e) {
        }
        return confirm;
    }

    public boolean askConfirmation(String message, boolean preferDialog, boolean defAnswer) throws IOException {
        boolean confirm = false;
        if (menus != null && preferDialog) {
            confirm = menus.confirmDialog(message, defAnswer);
        } else {
            String answer = promptLine(message + (defAnswer ? " ([yes]/no) " : "(yes/[no]) "), "");
            if (answer.equalsIgnoreCase("yes") || answer.equals("y")) {
                confirm = true;
            } else if (answer.equals("")) {
                confirm = defAnswer;
            }
        }
        return confirm;
    }

    public boolean licenseDialog(String license) {
        if (license != null && menus instanceof SSHMenuHandlerFull) {
            return SSHMiscDialogs.confirm("MindTerm - License agreeement", license, 24, 100, "Accept", "Decline", false, ((SSHMenuHandlerFull) menus).parent, true);
        }
        return false;
    }

    public void connected(SSHClient client) {
        updateMenus();
        console.println("Connected to server running " + srvVersionStr);
    }

    public void open(SSHClient client) {
        updateMenus();
        updateTitle();
    }

    public void disconnected(SSHClient client, boolean graceful) {
        sshStdIO.breakPromptLine("Login aborted by user");
        updateMenus();
        updateTitle();
    }

    public void report(String msg) {
        if (msg != null && msg.length() > 0) {
            console.println(msg);
        }
        console.println("");
    }

    public SSH2Interactor getInteractor() {
        return this;
    }

    public void alert(String msg) {
        if (menus != null) {
            if (msg.length() < 50) menus.alertDialog(msg); else menus.textDialog("MindTerm - Alert", msg, 4, 38, true);
        } else {
            report(msg);
        }
    }

    public void forcedDisconnect() {
        if (isSSH2) {
            transport.normalDisconnect("Closed by user");
        } else {
            super.forcedDisconnect();
        }
    }

    public void requestLocalPortForward(String localHost, int localPort, String remoteHost, int remotePort, String plugin) throws IOException {
        if (isSSH2) {
            SSH2StreamFilterFactory filter = null;
            if ("ftp".equals(plugin)) {
                String serverLocalAddr = propsHandler.getProperty("real-server");
                if (serverLocalAddr == null) {
                    serverLocalAddr = propsHandler.getProperty("server");
                }
                filter = new SSH2FTPProxyFilter(localHost, serverLocalAddr);
            } else if ("sniff".equals(plugin)) {
                filter = SSH2StreamSniffer.getFilterFactory();
            }
            connection.newLocalForward(localHost, localPort, remoteHost, remotePort, filter);
        } else {
            super.requestLocalPortForward(localHost, localPort, remoteHost, remotePort, plugin);
        }
    }

    public void addRemotePortForward(String remoteHost, int remotePort, String localHost, int localPort, String plugin) {
        super.addRemotePortForward(remotePort, localHost, localPort, plugin);
        if (isSSH2) {
            delRemotePortForward(remoteHost, remotePort);
            connection.newRemoteForward(remoteHost, remotePort, localHost, localPort);
        }
    }

    public void delLocalPortForward(String localHost, int port) {
        boolean isop = isOpened;
        if (isSSH2) {
            connection.deleteLocalForward(localHost, port);
            isOpened = false;
        }
        super.delLocalPortForward(localHost, port);
        isOpened = isop;
    }

    public void delRemotePortForward(String remoteHost, int port) {
        if (isSSH2) {
            connection.deleteRemoteForward(remoteHost, port);
        }
    }

    void setAliveInterval(int i) {
        if (isSSH2) {
            transport.enableKeepAlive(i);
        } else {
            super.setAliveInterval(i);
        }
    }

    void runSSH2Client() throws IOException {
        try {
            SSH2Preferences prefs;
            isSSH2 = true;
            prefs = new SSH2Preferences(propsHandler.getProperties());
            if (SSH.DEBUGMORE) {
                prefs.setPreference(SSH2Preferences.LOG_LEVEL, "7");
            }
            transport = new SSH2Transport(sshSocket, prefs, null, secureRandom());
            transport.setEventHandler(new SSH2TransportEventAdapter() {

                public boolean kexAuthenticateHost(SSH2Transport tp, SSH2Signature serverHostKey) {
                    try {
                        propsHandler.showFingerprint(serverHostKey.getPublicKeyBlob(), serverHostKey.getAlgorithmName());
                        if (fingerprintMatch(serverHostKey)) {
                            return true;
                        }
                        return propsHandler.verifyKnownSSH2Hosts(SSHInteractiveClient.this, serverHostKey);
                    } catch (SSH2Exception e) {
                        transport.getLog().error("SSHInteractiveClient", "verifyKnownSSH2Hosts", "Error " + e.getMessage());
                    } catch (IOException e) {
                        transport.getLog().error("SSHInteractiveClient", "verifyKnownSSH2Hosts", "Error " + e.getMessage());
                    }
                    return false;
                }

                public void gotConnectInfoText(SSH2Transport tp, String text) {
                    alert(text);
                }
            });
            transport.boot();
            srvVersionStr = transport.getServerVersion();
            connected(null);
            if (!transport.waitForKEXComplete()) {
                throw new IOException("Key exchange failed: " + transport.getDisconnectMessage());
            }
            isConnected = true;
            SSH2Authenticator authenticator = new SSH2Authenticator() {

                public void peerMethods(String methods) {
                    addAuthModules(this, methods);
                }

                public void displayBanner(String banner) {
                    alert(banner);
                }
            };
            authenticator.setUsername(propsHandler.getUsername(null));
            SSH2UserAuth userAuth = new SSH2UserAuth(transport, authenticator);
            if (!userAuth.authenticateUser("ssh-connection")) {
                throw new AuthFailException("permission denied");
            }
            connection = new SSH2Connection(userAuth, transport, null);
            connection.setEventHandler(new SSH2ConnectionEventAdapter() {

                public void localSessionConnect(SSH2Connection connection, SSH2Channel channel) {
                }

                public void localDirectConnect(SSH2Connection connection, SSH2Listener listener, SSH2Channel channel) {
                    tunnels.addElement(channel);
                }

                public void remoteForwardConnect(SSH2Connection connection, String remoteAddr, int remotePort, SSH2Channel channel) {
                    tunnels.addElement(channel);
                }

                public void channelClosed(SSH2Connection connection, SSH2Channel channel) {
                    tunnels.removeElement(channel);
                }
            });
            transport.setConnection(connection);
            authenticator.clearSensitiveData();
            if (console != null) console.serverConnect(null, null);
            isOpened = true;
            open(null);
            if (menus != null) {
                ((SSHMenuHandlerFull) menus).modulesConnect();
            }
            propsHandler.passivateProperties();
            propsHandler.activateProperties();
            TerminalWin terminal = getTerminalWin();
            SSH2SessionChannel session;
            if (terminal != null) {
                terminal.addInputListener(this);
                termAdapter = new SSH2TerminalAdapterImpl(terminal);
                session = connection.newTerminal(termAdapter);
                logger.log(Level.FINE, "[SSHPlugin] session: " + session.toString());
                if (propsHandler.hasKeyTimingNoise()) {
                    termAdapter.startChaff();
                }
                if (session.openStatus() != SSH2Channel.STATUS_OPEN) {
                    throw new IOException("Failed to open ssh2 session channel");
                }
                session.requestX11Forward(false, 0);
                if (user.wantPTY()) {
                    session.requestPTY(terminal.terminalType(), terminal.rows(), terminal.cols(), null);
                }
                if (commandLine != null) {
                    session.doSingleCommand(commandLine);
                } else {
                    session.doShell();
                }
            } else {
                session = connection.newSession();
            }
            logger.log(Level.WARNING, "[SSHPlugin]Before waitforExit");
            if (sshToUnicoreJobDirectory) {
                executeCommand(terminal, "cd " + unicoreJobDirectory);
            }
            int status = session.waitForExit(0);
            logger.log(Level.WARNING, "[SSHPlugin]After waitforExit");
            if (terminal != null) {
                terminal.removeInputListener(this);
            }
            termAdapter.detach();
            transport.normalDisconnect("Disconnect by user");
            console.serverDisconnect(getServerAddr().getHostName() + " disconnected: " + status);
            disconnect(true);
            if (propsHandler.getCompressionLevel() != 0) {
                SSH2Compressor comp;
                for (int i = 0; i < 2; i++) {
                    comp = (i == 0 ? transport.getTxCompressor() : transport.getRxCompressor());
                    if (comp != null) {
                        String msg;
                        long compressed, uncompressed;
                        compressed = comp.numOfCompressedBytes();
                        uncompressed = (comp.numOfUncompressedBytes() > 0 ? comp.numOfUncompressedBytes() : 1);
                        msg = " raw data (bytes) = " + uncompressed + ", compressed = " + compressed + " (" + ((compressed * 100) / uncompressed) + "%)";
                        console.println((i == 0 ? "outgoing" : "incoming") + msg);
                    }
                }
            }
            sshStdIO.setTerminal(terminal);
        } catch (IOException e) {
            disconnect(false);
            throw e;
        } catch (Exception e) {
            System.err.println("** Error in ssh2: ");
            e.printStackTrace();
            disconnect(false);
            throw new IOException("Error in ssh2: " + e.getMessage());
        } finally {
            ((SSHMenuHandlerFull) menus).modulesDisconnect();
            connection = null;
            transport = null;
            isSSH2 = false;
        }
    }

    public boolean fingerprintMatch(SSH2Signature serverHostKey) {
        String fp = propsHandler.getProperty("fingerprint");
        if (fp == null) {
            fp = propsHandler.getProperty("fingerprint." + propsHandler.getProperty("server") + "." + propsHandler.getProperty("port"));
        }
        if (fp != null) {
            if (SSH2HostKeyVerifier.compareFingerprints(fp, serverHostKey)) {
                return true;
            }
            if (propsHandler.askChangeKeyConfirmation()) {
                byte[] blob = null;
                try {
                    blob = serverHostKey.getPublicKeyBlob();
                } catch (SSH2SignatureException e) {
                    return false;
                }
                String fpMD5Hex = SSH2KeyFingerprint.md5Hex(blob);
                propsHandler.setProperty("fingerprint", fpMD5Hex);
            }
        }
        return false;
    }

    /**
	 * Starts a command on the shell.
	 * @param command
	 * author Morris Riedel
	 */
    private void executeCommand(TerminalWin terminal, String command) {
        command += (char) 13;
        for (int i = 0; i < command.length(); i++) {
            logger.log(Level.WARNING, "Get char: " + command.charAt(i));
            terminal.typedChar(command.charAt(i));
        }
    }

    public void typedChar(char c) {
    }

    public void sendBytes(byte[] b) {
    }

    public void signalWindowChanged(int rows, int cols, int vpixels, int hpixels) {
        updateTitle();
    }

    public void addAuthModules(SSH2Authenticator authenticator, String methods) {
        try {
            int[] authTypes = propsHandler.getAuthTypes(null);
            for (int i = 0; i < authTypes.length; i++) {
                int type = authTypes[i];
                if (!SSH2ListUtil.isInList(methods, SSH.getAuthName(type)) && !SSH2ListUtil.isInList(methods, SSH.getAltAuthName(type)) && !((type == AUTH_SDI) && SSH2ListUtil.isInList(methods, "securid-1@ssh.com"))) {
                    report("Authentication method '" + SSH.getAuthName(type) + "' not supported by server.");
                    continue;
                }
                switch(type) {
                    case AUTH_PUBLICKEY:
                        String keyFile = propsHandler.getProperty("idfile");
                        if (keyFile.indexOf(File.separator) == -1) {
                            keyFile = propsHandler.getSSHHomeDir() + keyFile;
                        }
                        if (com.mindbright.util.Util.isNetscapeJava()) {
                            try {
                                netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
                            } catch (netscape.security.ForbiddenTargetException e) {
                            }
                        }
                        SSH2KeyPairFile kpf = new SSH2KeyPairFile();
                        try {
                            kpf.load(keyFile, "");
                        } catch (SSH2FatalException e) {
                            throw new IOException(e.getMessage());
                        } catch (SSH2AccessDeniedException e) {
                            String comment = kpf.getComment();
                            if (comment == null || comment.trim().length() == 0) {
                                comment = keyFile;
                            }
                            String prompt = "Key '" + comment + "' password: ";
                            String passwd = propsHandler.getIdentityPassword(prompt);
                            kpf.load(keyFile, passwd);
                        }
                        String alg = kpf.getAlgorithmName();
                        SSH2Signature sign = SSH2Signature.getInstance(alg);
                        sign.initSign(kpf.getKeyPair().getPrivate());
                        sign.setPublicKey(kpf.getKeyPair().getPublic());
                        authenticator.addModule(new SSH2AuthPublicKey(sign));
                        break;
                    case AUTH_PASSWORD:
                        authenticator.addModule(new SSH2AuthPassword(propsHandler.getPassword(null)));
                        break;
                    case AUTH_SDI:
                    case AUTH_TIS:
                    case AUTH_CRYPTOCARD:
                    case AUTH_KBDINTERACT:
                        authenticator.addModule(new SSH2AuthKbdInteract(this));
                        authenticator.addModule(new SSH2AuthSSHComSecurID(this, "Enter Passcode: ", "Wait for token to change and enter Passcode: ", "New PIN:", "Confirm new PIN: ", "Do you want to create your own new PIN (yes/no)? ", "Accept the server assigned PIN: "));
                        break;
                    default:
                        throw new IOException("Authentication type " + authTypeDesc[authTypes[i]] + " is not supported in SSH2");
                }
            }
        } catch (Exception e) {
            if (SSH.DEBUGMORE) {
                System.out.println("Error when setting up authentication: ");
                int[] t = propsHandler.getAuthTypes(null);
                for (int i = 0; i < t.length; i++) {
                    System.out.print(t[i] + ", ");
                }
                System.out.println("");
                e.printStackTrace();
            }
            alert("Error when setting up authentication: " + e.getMessage());
        }
    }

    public void newShell() {
        ModuleTerminalImpl terminal = new ModuleTerminalImpl();
        terminal.init(this);
        terminal.run();
    }

    public String getVersionId(boolean client) {
        String idStr = "SSH-" + SSH_VER_MAJOR + "." + SSH_VER_MINOR + "-";
        idStr += propsHandler.getProperty("package-version");
        return idStr;
    }

    public void closeTunnelFromList(int listIdx) {
        if (isSSH2) {
            SSH2Channel c = (SSH2Channel) tunnels.elementAt(listIdx);
            c.close();
        } else {
            controller.closeTunnelFromList(listIdx);
        }
    }

    private Vector tunnels = new Vector();

    public String[] listTunnels() {
        if (isSSH2) {
            String[] list = new String[tunnels.size()];
            Enumeration e = tunnels.elements();
            int cnt = 0;
            while (e.hasMoreElements()) {
                SSH2TCPChannel c = (SSH2TCPChannel) e.nextElement();
                list[cnt++] = c.toString();
            }
            return list;
        } else {
            return controller.listTunnels();
        }
    }

    public String getHost() {
        return getServerAddr().getHostName();
    }

    public int getPort() {
        return propsHandler.getSrvPort();
    }

    public Properties getProperties() {
        Properties props = new Properties(propsHandler.getProperties());
        TerminalWin term = getTerminalWin();
        Properties termProps = (term != null ? term.getProperties() : null);
        if (termProps != null) {
            Enumeration e = termProps.keys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                String val = termProps.getProperty(key);
                props.put(key, val);
            }
        }
        return props;
    }

    public String getProperty(String name) {
        String value = propsHandler.getProperty(name);
        if (value == null) {
            TerminalWin term = getTerminalWin();
            if (term != null) {
                value = term.getProperty(name);
            }
        }
        return value;
    }

    public void setProperty(String name, String value) {
        propsHandler.setProperty(name, value);
    }

    public String getUserName() {
        return propsHandler.getProperty("username");
    }

    public Frame getParentFrame() {
        return ((SSHMenuHandlerFull) menus).parent;
    }

    public String getAppName() {
        return "MindTerm";
    }

    public RandomSeed getRandomSeed() {
        return SSH.randomSeed();
    }

    public boolean isApplet() {
        return ((SSHMenuHandlerFull) menus).mindterm.weAreAnApplet;
    }

    public AppletContext getAppletContext() {
        return ((SSHMenuHandlerFull) menus).mindterm.getAppletContext();
    }

    public SSH2Transport getTransport() {
        return transport;
    }

    public SSH2Connection getConnection() {
        return connection;
    }

    public SSHConsoleRemote getConsoleRemote() {
        SSHConsoleRemote remote = null;
        if (isSSH2) {
            remote = new SSH2ConsoleRemote(getConnection());
        } else {
            quiet = true;
            try {
                remote = new SSHConsoleClient(propsHandler.getSrvHost(), propsHandler.getSrvPort(), propsHandler, null);
                ((SSHConsoleClient) remote).setClientUser(propsHandler);
            } catch (IOException e) {
                alert("Error creating remote console: " + e.getMessage());
            }
        }
        return remote;
    }
}
