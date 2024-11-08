package jcfs.core.serverside;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import jcfs.core.commons.Requests;
import jcfs.core.commons.Responses;
import jcfs.core.fs.RFile;
import jcfs.core.fs.WriteMode;

/**
 * servers clients
 * @author enrico
 */
public class CommandServerWorker implements Runnable {

    private Socket socket;

    private Logger logger;

    private JCFSServerSideFileManager fileManager;

    private JCFSFileServer server;

    public CommandServerWorker(Socket socket, JCFSFileServer server) {
        this.socket = socket;
        this.logger = Logger.getLogger("input_" + socket.getInetAddress());
        this.server = server;
        this.fileManager = server.getFileManager();
    }

    @Override
    public void run() {
        DataOutputStream out = null;
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            handleClient(in, out);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable t) {
                }
            }
            try {
                socket.close();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private void serveLocalRead(String pathname, DataOutputStream out, Socket s) throws IOException {
        File file = fileManager.getRawFile(pathname);
        if (!file.isFile()) {
            sendError("file not found on this server " + pathname, out);
            return;
        }
        out.write(Responses.OK);
        out.writeUTF(file.length() + "");
        out.flush();
        byte[] cached = server.getCache().getCached(pathname);
        if (cached == null) {
            FileInputStream infile = fileManager.openFileRead(pathname);
            try {
                ByteBuffer all = ByteBuffer.allocate((int) file.length());
                infile.getChannel().read(all);
                all.rewind();
                out.write(all.array());
                out.flush();
            } finally {
                infile.close();
            }
        } else {
            out.write(cached);
            out.flush();
        }
    }

    private void executeCommand(String command, DataInputStream in, DataOutputStream out) throws IOException {
        if (command.startsWith(Requests.OPENWRITE)) {
            String pathname = command.substring(Requests.OPENWRITE.length());
            serveDoWrite(pathname, in, out);
        } else if (command.startsWith(Requests.OPENREAD)) {
            String pathname = command.substring(Requests.OPENREAD.length());
            serveDoRead(pathname, in, out);
        } else if (Requests.DELETE.equals(command)) {
            String pathname = in.readUTF();
            serveDelete(pathname, out);
        } else if (Requests.GETINFO.equals(command)) {
            String pathname = in.readUTF();
            serveInfo(pathname, out);
        } else if (Requests.PING.equals(command)) {
            String pathname = in.readUTF();
            servePing(pathname, out);
        } else {
            sendError("bad command " + command, out);
        }
    }

    private void sendError(String message, DataOutputStream out) throws IOException {
        out.write(Responses.ERROR);
        out.writeUTF(message);
        out.flush();
    }

    private void serveDoWrite(String pathname, DataInputStream in, DataOutputStream out) throws IOException {
        RFile file = new RFile(pathname);
        RFile ancestor = file.getRootFile();
        DirectoryConfiguration conf = server.getDirectoryDefaultConfiguration(ancestor.getName());
        String command = in.readUTF();
        boolean transaction = conf.getDefaultWriteMode() == WriteMode.TRANSACTED;
        boolean append = false;
        boolean allowoverwrite = true;
        int minpeers = conf.getMinPeers();
        while (!command.equals(Requests.STARTSTREAM)) {
            if (command.equals(Requests.APPEND)) {
                append = true;
            }
            if (command.equals(Requests.TRANSACTION)) {
                transaction = true;
            }
            if (command.equals(Requests.NOOVERWRITE)) {
                allowoverwrite = false;
            }
            if (command.equals(Requests.REPLICATE)) {
                minpeers = Integer.parseInt(in.readUTF());
            }
            command = in.readUTF();
        }
        if (append && transaction) {
            sendError("transaction cannot be done in APPEND mode", out);
            return;
        }
        logger.log(Level.FINE, "pathname={0} minpeers={1}", new Object[] { pathname, minpeers });
        String origpathname = pathname;
        File originalFile = fileManager.getRawFile(pathname);
        boolean swallow = false;
        if (!allowoverwrite && originalFile.isFile()) {
            System.out.println("File already exists " + originalFile.getAbsolutePath());
            swallow = true;
        }
        if (conf.isCachable()) {
            server.getCache().removeFromCache(pathname);
            server.getLocalClient().fileChanged(pathname);
        }
        originalFile.getParentFile().mkdirs();
        File tmpFile = originalFile;
        if (transaction) {
            pathname = pathname + ".part";
            tmpFile = fileManager.getRawFile(pathname);
            tmpFile.getParentFile().mkdirs();
        }
        int count = 0;
        FileOutputStream outfile = null;
        WritableByteChannel outC = null;
        try {
            if (!swallow) {
                outfile = fileManager.openFileWriter(pathname, append);
                outC = outfile.getChannel();
            }
            while (true) {
                command = in.readUTF();
                logger.log(Level.FINEST, "received command {0}", new Object[] { command });
                if (command.equals(Requests.END_OF_FILE)) {
                    break;
                } else if (command.equals(Requests.WRITE_SINGLE_BYTE)) {
                    int b = in.read();
                    logger.log(Level.FINEST, "receiving one single byte");
                    ByteBuffer buffer = ByteBuffer.allocateDirect(1);
                    buffer.put((byte) b);
                    buffer.rewind();
                    if (!swallow) {
                        outC.write(buffer);
                    }
                    count++;
                } else if (command.startsWith(Requests.WRITE_BYTE_ARRAY)) {
                    int len = Integer.parseInt(command.substring(1));
                    logger.log(Level.FINEST, "receiving {0} bytes", len);
                    ByteBuffer buffer = ByteBuffer.allocate(len);
                    int actual = 1;
                    int single = in.read();
                    buffer.put((byte) single);
                    while (actual < len) {
                        if (single < 0) {
                            throw new IOException("unexpected EOF");
                        }
                        single = in.read();
                        buffer.put((byte) single);
                        actual++;
                    }
                    logger.log(Level.FINEST, "receiving {0} bytes got {1}", new Object[] { len, actual });
                    if (actual != len) {
                        throw new IOException("expected " + len + " bytes but got " + actual);
                    }
                    buffer.rewind();
                    if (!swallow) {
                        outC.write(buffer);
                    }
                    count += len;
                }
            }
            if (swallow) {
                sendError("file already exists", out);
                return;
            }
            outfile.close();
            outfile = null;
            logger.log(Level.FINE, "pathname={0} received total {1} bytes", new Object[] { pathname, count });
            if (transaction) {
                logger.log(Level.FINE, "committing transaction");
                if (originalFile.isFile()) {
                    originalFile.delete();
                }
                tmpFile.renameTo(originalFile);
                if (tmpFile.exists() || !originalFile.exists()) {
                    throw new IOException("commit failed of file " + origpathname + " (file is " + originalFile.getAbsolutePath() + " tmp is " + tmpFile.getAbsolutePath() + ")");
                }
            }
            if (minpeers > 1) {
                logger.log(Level.FINE, "min peers is {0} so replicate file on other peers", minpeers);
                try {
                    this.server.getReplicaManager().replicate(origpathname, minpeers, true);
                } catch (IOException err) {
                    sendError("replication error " + err, out);
                    return;
                }
            }
            out.write(Responses.OK);
            out.flush();
            if (conf.isCachable()) {
                try {
                    server.getCache().cache(pathname, originalFile);
                } catch (IOException err) {
                    logger.log(Level.SEVERE, "error while caching file " + originalFile + ":{0}", err);
                }
            }
        } catch (IOException err) {
            logger.log(Level.INFO, "pathname={0} received {1} bytes but got {2}", new Object[] { pathname, count, err });
            sendError("server error " + err, out);
            if (transaction) {
                if (outfile != null) {
                    outfile.close();
                }
                outfile = null;
                logger.log(Level.INFO, "transaction failed! deleting tmp file");
                fileManager.getRawFile(pathname).delete();
            }
        } finally {
            if (outfile != null) {
                outfile.close();
            }
        }
    }

    private void serveDoRead(String pathname, DataInputStream in, DataOutputStream out) throws IOException {
        serveLocalRead(pathname, out, socket);
    }

    private void serveDelete(String pathname, DataOutputStream out) throws IOException {
        boolean ok = fileManager.delete(pathname);
        if (ok) {
            out.write(Responses.OK);
            out.flush();
        } else {
            sendError("failed to delete file " + pathname, out);
        }
    }

    private void serveInfo(String pathname, DataOutputStream out) throws IOException {
        File file = fileManager.getRawFile(pathname);
        out.write(Responses.OK);
        out.writeUTF(file.length() + "");
        out.writeUTF(file.lastModified() + "");
        out.flush();
    }

    private void servePing(String pathname, DataOutputStream out) throws IOException {
        out.write(Responses.OK);
        out.flush();
    }

    private void handleClient(DataInputStream in, DataOutputStream out) throws IOException {
        String command = in.readUTF();
        while (command != null && !command.equals(Requests.CLOSE_CONNECTION)) {
            executeCommand(command, in, out);
            try {
                command = in.readUTF();
            } catch (IOException err) {
                break;
            }
        }
    }
}
