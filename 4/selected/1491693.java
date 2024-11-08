package pl.olek.clojure;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import pl.olek.jruce.Daemon;
import pl.olek.jruce.GroupSender;
import pl.olek.textmash.Support;

/**
 * @author anaszko
 */
public class Remote {

    static final int PUT_INPUT = 2;

    static final int RUN_PROCESS = 3;

    static final int DESTROY_PROCESS = 4;

    Socket server;

    DataOutputStream dataOutput;

    public static String getPath(String path) {
        path = path.replaceAll("\\\\", "/");
        Configuration settings = new Configuration(Support.getInstance().getId());
        String projectPath = settings.get("project-classpath")[0];
        for (String customPath : projectPath.split(System.getProperty("path.separator"))) {
            if (path.startsWith(customPath)) {
                path = path.substring(customPath.length());
            }
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    public Remote(String host, int port) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            server = new Socket(addr, port);
            dataOutput = new DataOutputStream(server.getOutputStream());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void run(String workingDir, String[] cmd) {
        try {
            dataOutput.writeInt(RUN_PROCESS);
            dataOutput.writeUTF(workingDir);
            dataOutput.writeInt(cmd.length);
            for (String s : cmd) {
                dataOutput.writeUTF(s);
            }
            dataOutput.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void input(String cmd) {
        try {
            dataOutput.writeInt(PUT_INPUT);
            dataOutput.writeUTF(cmd);
            dataOutput.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void destroy() {
        try {
            dataOutput.writeInt(DESTROY_PROCESS);
            dataOutput.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            dataOutput.close();
            server.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Process repl = null;

    public static void spawn(int port, final String name) {
        try {
            final ServerSocket server = new ServerSocket(port);
            GroupSender gp = new GroupSender("textmash", 5000) {

                @Override
                public void send(OutputStream output) throws Exception {
                    DataOutputStream dat = new DataOutputStream(output);
                    dat.writeUTF(name);
                    dat.writeInt(server.getLocalPort());
                    dat.flush();
                }
            };
            gp.fire();
            Socket cl;
            boolean running = true;
            while ((running) && (cl = server.accept()) != null) {
                final Socket client = cl;
                new Daemon() {

                    @Override
                    public void run() {
                        try {
                            DataInputStream reader = new DataInputStream(client.getInputStream());
                            int msg;
                            try {
                                for (; ; ) {
                                    msg = reader.readInt();
                                    if (msg == RUN_PROCESS) {
                                        String workingDir = reader.readUTF();
                                        String[] cmds = new String[reader.readInt()];
                                        for (int i = 0; i < cmds.length; ++i) {
                                            cmds[i] = reader.readUTF();
                                        }
                                        if (repl != null) {
                                            repl.destroy();
                                        }
                                        repl = new Process(new File(workingDir), cmds);
                                        Stream.redirect(repl.getInput(), System.out);
                                    } else if (repl != null) {
                                        if (msg == PUT_INPUT) {
                                            repl.getOutput().write(reader.readUTF().getBytes());
                                            repl.getOutput().flush();
                                        } else if (msg == DESTROY_PROCESS) {
                                            repl.destroy();
                                        }
                                    }
                                }
                            } catch (EOFException e) {
                            }
                        } catch (IOException e) {
                        }
                    }
                };
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(final String[] args) {
        Remote.spawn(Integer.parseInt(args[0]), args[1]);
    }
}
