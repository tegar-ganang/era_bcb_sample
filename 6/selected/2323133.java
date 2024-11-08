package quietcoffee.client;

import quietcoffee.ssh.*;
import quietcoffee.util.GetOpt;

/**
 *  Main program for SSH
 * @author  Brett Porter
 * @version $Id: SSH.java,v 1.1.1.1 2002/05/13 03:49:12 brettporter Exp $
 */
public final class SSH {

    private static final int SSH_CMSG_REQUEST_COMPRESSION = 37;

    private static final int SSH_SMSG_SUCCESS = 14;

    private static final int SSH_SMSG_FAILURE = 15;

    /** The options given. */
    private SSHOptions cmdOptions = new SSHOptions();

    /** The configuration options. */
    private Options options = new Options();

    /** Creates a new instance of SSH client.
     *      @param args the command line arguments
     */
    public SSH(String[] args) {
        readOptions(args);
    }

    /**
     *  Read the options from a config file, command line, etc.
     *      @param args the command line arguments
     */
    private void readOptions(String[] args) {
        GetOpt getOpt = new GetOpt(args, "1246ab:c:e:fgi:kl:m:no:p:qstvxACD:F:I:L:NPR:TVX");
        char opt = '\0';
        int counter = 0;
        try {
            while ((opt = getOpt.getOpt()) != getOpt.END_OF_OPTIONS) {
                counter++;
                switch(opt) {
                    case '1':
                        options.setProtocol(Compatibility.SSH_PROTO_1);
                        break;
                    case '2':
                        options.setProtocol(Compatibility.SSH_PROTO_2);
                        break;
                    case 'i':
                        cmdOptions.setIdentityFilename(getOpt.getOptArg());
                        break;
                    case 'l':
                        cmdOptions.setUsername(getOpt.getOptArg());
                        break;
                    case 'p':
                        options.setPort(getOpt.getOptArg());
                        break;
                    case 'C':
                        options.setCompression(true);
                        break;
                    case 's':
                        cmdOptions.setInvokeAsSubsystem(true);
                        break;
                    case 'v':
                        if (!cmdOptions.isDebugFlag()) {
                            cmdOptions.setDebugFlag(true);
                            options.setLogLevel(Log.SYSLOG_LEVEL_DEBUG1);
                        } else if (options.getLogLevel() < Log.SYSLOG_LEVEL_MAX) {
                            options.setLogLevel(options.getLogLevel() + 1);
                            break;
                        } else {
                            Log.getLogInstance().fatal("Too high debugging level.");
                        }
                    case 'V':
                        System.out.println(SSHConnection.SSH_VERSION + ", SSH protocols " + SSHConnection.PROTOCOL_MAJOR_1 + "." + SSHConnection.PROTOCOL_MINOR_1 + "/" + SSHConnection.PROTOCOL_MAJOR_2 + "." + SSHConnection.PROTOCOL_MINOR_2);
                        break;
                    case 'q':
                        options.setLogLevel(Log.SYSLOG_LEVEL_QUIET);
                        break;
                    case 'c':
                        if (CipherFactory.valid(getOpt.getOptArg())) {
                            options.setCiphers(getOpt.getOptArg());
                            options.setCipher(CipherDetails.SSH_CIPHER_ILLEGAL);
                        } else {
                            options.setCipher(CipherFactory.getCipherNumber(getOpt.getOptArg()));
                            if (options.getCipher() == CipherDetails.SSH_CIPHER_NOT_SET) {
                                System.err.println("Unknown cipher type '" + getOpt.getOptArg() + "'");
                                System.exit(1);
                            }
                            if (options.getCipher() == CipherDetails.SSH_CIPHER_3DES) {
                                options.setCiphers("3des-cbc");
                            } else if (options.getCipher() == CipherDetails.SSH_CIPHER_BLOWFISH) {
                                options.setCiphers("blowfish-cbc");
                            } else {
                                options.setCiphers(null);
                            }
                        }
                        break;
                    case 'a':
                        options.setForwardAgent(false);
                        break;
                    case 'A':
                        options.setForwardAgent(true);
                        break;
                    case 'F':
                        cmdOptions.setConfig(getOpt.getOptArg());
                        break;
                    case 'o':
                        if (options.processConfigLine((cmdOptions.getHost() != null) ? cmdOptions.getHost() : "", getOpt.getOptArg(), "command-line", counter, false) == false) {
                            System.exit(1);
                        }
                        break;
                    default:
                        throw new Exception("Legal but unhandled option: " + opt);
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid format for option: " + opt + " = " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        int index = getOpt.getArgIndex();
        if (index < args.length) {
            String host = args[index++];
            int delim = host.indexOf('@');
            if (delim != -1) {
                cmdOptions.setUsername(host.substring(0, delim));
                cmdOptions.setHost(host.substring(delim + 1));
            } else {
                cmdOptions.setHost(host);
            }
        }
        if (index < args.length) {
            StringBuffer cmd = new StringBuffer();
            while (index < args.length) {
                cmd.append(args[index++]);
                cmd.append(" ");
            }
            cmdOptions.setCommand(cmd.toString());
        } else {
            cmdOptions.setTtyFlag(true);
            if (cmdOptions.isInvokeAsSubsystem()) {
                System.err.println("You must specify a subsystem to invoke.");
                usage();
                System.exit(0);
            }
        }
        if (cmdOptions.getHost() == null) {
            usage();
            System.exit(0);
        }
        if (cmdOptions.getCommand() == null) {
            cmdOptions.setTtyFlag(true);
        }
    }

    /**
     *  Show usage.
     */
    private void usage() {
        System.out.println("Usage: ssh [options] host [command]");
        System.out.println("Options:");
        System.out.println("  -l user     Log in using this user name.");
        System.out.println("  -n          Redirect input from /dev/null.");
        System.out.println("  -F config   Config file (default: ~/" + Options.PATH_SSH_USER_CONFFILE + ").");
        System.out.println("  -A          Enable authentication agent forwarding.");
        System.out.println("  -a          Disable authentication agent forwarding (default).");
        System.out.println("  -X          Enable X11 connection forwarding.");
        System.out.println("  -x          Disable X11 connection forwarding (default).");
        System.out.println("  -i file     Identity for public key authentication (default: ~/.ssh/identity)");
        System.out.println("  -t          Tty; allocate a tty even if command is given.");
        System.out.println("  -T          Do not allocate a tty.");
        System.out.println("  -v          Verbose; display verbose debugging messages.");
        System.out.println("              Multiple -v increases verbosity.");
        System.out.println("  -V          Display version number only.");
        System.out.println("  -P          Don't allocate a privileged port.");
        System.out.println("  -q          Quiet; don't display any warning messages.");
        System.out.println("  -f          Fork into background after authentication.");
        System.out.println("  -e char     Set escape character; ``none'' = disable (default: ~).");
        System.out.println("  -c cipher   Select encryption algorithm");
        System.out.println("  -m macs     Specify MAC algorithms for protocol version 2.");
        System.out.println("  -p port     Connect to this port.  Server must be on the same port.");
        System.out.println("  -L listen-port:host:port   Forward local port to remote address");
        System.out.println("  -R listen-port:host:port   Forward remote port to local address");
        System.out.println("              These cause ssh to listen for connections on a port, and");
        System.out.println("              forward them to the other side by connecting to host:port.");
        System.out.println("  -D port     Enable dynamic application-level port forwarding.");
        System.out.println("  -C          Enable compression.");
        System.out.println("  -N          Do not execute a shell or command.");
        System.out.println("  -g          Allow remote hosts to connect to forwarded ports.");
        System.out.println("  -1          Force protocol version 1.");
        System.out.println("  -2          Force protocol version 2.");
        System.out.println("  -4          Use IPv4 only.");
        System.out.println("  -6          Use IPv6 only.");
        System.out.println("  -o 'option' Process the option as if it was read from a configuration file.");
        System.out.println("  -s          Invoke command (mandatory) as SSH2 subsystem.");
        System.out.println("  -b addr     Local IP address.");
    }

    /**
     *  Run the program.
     *      @throws SSHException if it fails? TODO: fix entire exception structure
     */
    private void execute() throws SSHException, java.io.IOException {
        System.err.println("TODO: SSH.java:execute() SSL init");
        Log.init(options.getLogLevel(), true);
        if (cmdOptions.getConfig() != null) {
            options.readConfigFile(cmdOptions.getConfig(), cmdOptions.getHost());
        } else {
            options.readConfigFile(System.getProperty("home.dir") + "/" + Options.PATH_SSH_USER_CONFFILE, cmdOptions.getHost());
            options.readConfigFile(Options.PATH_HOST_CONFIG_FILE, cmdOptions.getHost());
        }
        options.fillDefaultOptions();
        Log.init(options.getLogLevel(), true);
        Log.getLogInstance().TODO("SSH.execute() seed RNG");
        if (options.getUser() == null) {
            options.setUser(System.getProperty("user.name"));
        }
        if (options.getHostname() != null) {
            cmdOptions.setHost(options.getHostname());
        }
        Log.getLogInstance().TODO("SSH.execute() options manipulation");
        SSHConnection connection = null;
        try {
            Log.getLogInstance().log("TODO: SSH.java:execute() init connection");
            connection = new SSHConnection(options);
            connection.connect(cmdOptions.getHost(), options.getPort(), options.getConnectionAttempts(), options.getProxyCommand());
        } catch (java.net.UnknownHostException e) {
            System.err.println("Couldn't find the host " + cmdOptions.getHost());
            return;
        } catch (java.net.SocketException e) {
            connection.close();
            System.err.println("Error on socket: " + e.getMessage());
            return;
        } catch (ConnectionAbortedException e) {
            System.err.println("Connection aborted");
            return;
        } catch (ConnectionRefusedException e) {
            System.err.println("Connection refused");
            return;
        }
        Log.getLogInstance().log("TODO: SSH.java:execute() load host key");
        Log.getLogInstance().log("TODO: SSH.java:execute() load identity");
        try {
            connection.login(cmdOptions.getHost(), cmdOptions.getUsername());
        } catch (java.io.IOException e) {
            System.err.println(e.getMessage());
            return;
        }
        Log.getLogInstance().log("TODO: SSH.java:execute() clear host keys");
        int exitStatus = 0;
        Log.getLogInstance().log("TODO: SSH.java:execute() initiate session");
        exitStatus = sshSession(connection);
        connection.close();
        System.exit(exitStatus);
    }

    /**
     *  Execute an SSH session.
     *      @param connection the SSH connection to use
     *      @returns the success of the session (TODO)
     *      @throws SSHException TODO
     */
    private int sshSession(SSHConnection connection) throws SSHException, java.io.IOException {
        if (options.isCompression()) {
            Log.getLogInstance().debug("Requesting compression at level " + options.getCompressionLevel() + ".");
            if (options.getCompressionLevel() < 1 || options.getCompressionLevel() > 9) {
                Log.getLogInstance().fatal("Compression level must be from 1 (fast) to 9 (slow, best).");
            }
            Packet packet = connection.getPacketHandler();
            packet.start(SSH_CMSG_REQUEST_COMPRESSION);
            packet.putInt(options.getCompressionLevel());
            packet.send();
            packet.writeWait();
            PacketReadInfo readInfo = packet.read();
            if (readInfo.getType() == SSH_SMSG_SUCCESS) {
                packet.startCompression(options.getCompressionLevel());
            } else if (readInfo.getType() == SSH_SMSG_FAILURE) {
                Log.getLogInstance().log("Warning: Remote host refused compression.");
            } else {
                packet.disconnect("Protocol error waiting for compression response.");
            }
        }
        if (cmdOptions.isTtyFlag()) {
            Log.getLogInstance().debug("Requesting pty.");
            Log.getLogInstance().log("TODO: SSH.java:sshSession() start packets");
        }
        Log.getLogInstance().log("TODO: SSH.java:sshSession() request X11 forwarding");
        Log.getLogInstance().log("TODO: SSH.java:sshSession() initiate interactive");
        return -1;
    }

    /**
     *  Main program.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SSH sshProgram = new SSH(args);
        try {
            sshProgram.execute();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
