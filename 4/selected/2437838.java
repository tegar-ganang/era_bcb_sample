package org.xsocket.server;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xsocket.INonBlockingConnection;
import org.xsocket.LogFormatter;
import org.xsocket.server.handler.chain.Chain;
import org.xsocket.util.TextUtils;

/**
*
* @author grro@xsocket.org
*/
public final class RunnableSimpleSmtpServer {

    public static void main(String... args) throws IOException {
        Logger logger = Logger.getLogger("org.xsocket");
        logger.setLevel(Level.WARNING);
        ConsoleHandler hdl = new ConsoleHandler();
        hdl.setLevel(Level.FINEST);
        logger.addHandler(hdl);
        if (args.length != 1) {
            System.err.println("usage java org.xsocket.server.RunnableSimpleSmtpServer <port>");
            return;
        }
        new RunnableSimpleSmtpServer().launch(Integer.parseInt(args[0]));
    }

    public void launch(int port) throws IOException {
        IMultithreadedServer server = new MultithreadedServer(port);
        server.setReceiveBufferPreallocationSize(524288);
        Chain tcpBasedSpamfilter = new Chain();
        JamFilter jamFilter = new JamFilter();
        jamFilter.addSuspiciousAddress("localhost");
        tcpBasedSpamfilter.addLast(jamFilter);
        Chain mainChain = new Chain();
        mainChain.addLast(tcpBasedSpamfilter);
        mainChain.addLast(new SmtpProtocolHandler(true));
        server.setHandler(mainChain);
        Thread t = new Thread(server);
        t.start();
        RmiAgent rmiAgent = new RmiAgent("TestSmtpSrv");
        rmiAgent.start();
        while (true) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignore) {
            }
        }
    }

    /**
	 * the smtp protocol handler is connection scoped. That means all varaibles 
	 * are session parameters 
	 */
    private static final class SmtpProtocolHandler implements IDataHandler, IConnectHandler, IConnectionScoped {

        private static final String LINE_DELIMITER = "\r\n";

        private static final int COMMAND_HANDLING = 0;

        private static final int MESSAGE_DATA_HANDLING = 1;

        private boolean writeFile = true;

        private int state = COMMAND_HANDLING;

        private String mailFrom = null;

        private List<String> rcptTos = new ArrayList<String>();

        private GatheringByteChannel msgSink = null;

        SmtpProtocolHandler(boolean writeFile) {
            this.writeFile = writeFile;
        }

        public boolean onConnect(INonBlockingConnection connection) throws IOException {
            connection.write("220 this is the example smtp server" + LINE_DELIMITER);
            return true;
        }

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException {
            switch(state) {
                case COMMAND_HANDLING:
                    handleCommand(connection);
                    break;
                case MESSAGE_DATA_HANDLING:
                    handleMessageData(connection);
                    break;
            }
            return true;
        }

        private void handleCommand(INonBlockingConnection connection) throws IOException, BufferUnderflowException {
            String cmd = connection.readStringByDelimiter(LINE_DELIMITER).toUpperCase();
            if (cmd.startsWith("HELO")) {
                connection.write("250 helo nice to meet you" + LINE_DELIMITER);
            } else if (cmd.startsWith("MAIL FROM:")) {
                mailFrom = cmd.substring("MAIL FROM:".length(), cmd.length());
                connection.write("250 " + mailFrom + " sender OK" + LINE_DELIMITER);
            } else if (cmd.startsWith("RCPT TO:")) {
                String rcptTo = cmd.substring("RCPT TO:".length(), cmd.length());
                rcptTos.add(rcptTo);
                connection.write("250 " + rcptTo + " Receiver OK" + LINE_DELIMITER);
            } else if (cmd.startsWith("DATA")) {
                connection.write("354 Start mail input; end with <CRLF>.<CRLF>" + LINE_DELIMITER);
                if (writeFile) {
                    msgSink = new FileOut();
                } else {
                    msgSink = new ConsoleOut();
                }
                String receiveEntry = "Received: from " + connection.getRemoteAddress().getCanonicalHostName() + " (" + connection.getRemoteAddress().getCanonicalHostName() + " [" + connection.getRemoteAddress().getHostAddress() + "])" + "\n by " + connection.getLocalAddress().getCanonicalHostName() + "\"";
                msgSink.write(TextUtils.toByteBuffer(receiveEntry, "US-ASCII"));
                state = MESSAGE_DATA_HANDLING;
            } else if (cmd.startsWith("QUIT")) {
                connection.write("221 closing" + LINE_DELIMITER);
                connection.close();
            } else {
                connection.write("502 Command unrecognized" + LINE_DELIMITER);
            }
        }

        private void handleMessageData(INonBlockingConnection connection) throws IOException, BufferUnderflowException {
            boolean delimiterFound = connection.readAvailableByDelimiter(LINE_DELIMITER + "." + LINE_DELIMITER, msgSink);
            if (delimiterFound) {
                msgSink.close();
                mailFrom = null;
                rcptTos.clear();
                msgSink = null;
                state = COMMAND_HANDLING;
                connection.write("250 message accept for delivery (con.id=" + connection.getId() + ")" + LINE_DELIMITER);
            }
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            SmtpProtocolHandler copy = (SmtpProtocolHandler) super.clone();
            copy.rcptTos = new ArrayList<String>();
            return copy;
        }
    }

    private static class ConsoleOut implements GatheringByteChannel {

        private boolean isOpen = true;

        public boolean isOpen() {
            return isOpen;
        }

        public void close() throws IOException {
            isOpen = false;
        }

        public int write(ByteBuffer buffer) throws IOException {
            int size = buffer.remaining();
            System.out.print(TextUtils.toString(buffer));
            return size;
        }

        public long write(ByteBuffer[] buffers) throws IOException {
            int size = 0;
            for (ByteBuffer buffer : buffers) {
                size = write(buffer);
            }
            return size;
        }

        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            throw new UnsupportedOperationException("write(ByteBuffer[, int , int) is not supported");
        }
    }

    private static class FileOut implements GatheringByteChannel {

        private FileChannel channel = null;

        private File file = null;

        FileOut() throws IOException {
            file = File.createTempFile("test", "test");
            channel = new RandomAccessFile(file, "rw").getChannel();
        }

        public boolean isOpen() {
            return channel.isOpen();
        }

        public void close() throws IOException {
            channel.close();
            file.delete();
            System.out.print(".");
        }

        public int write(ByteBuffer buffer) throws IOException {
            return channel.write(buffer);
        }

        public long write(ByteBuffer[] buffers) throws IOException {
            return channel.write(buffers);
        }

        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            return channel.write(srcs, offset, length);
        }
    }

    /**
	 * jam filter. if a unknown host opens the connection 
	 * for the first time the write rate of the response 
	 * will be reduce. If the host has already be conntact
	 * this server the full write rate will be provided
	 *
	 */
    private static final class JamFilter implements IConnectHandler {

        private final Set<String> suspiciousAddresses = new HashSet<String>();

        public void addSuspiciousAddress(String canonicalHostName) {
            suspiciousAddresses.add(canonicalHostName);
        }

        public boolean onConnect(INonBlockingConnection connection) throws IOException {
            if (isSuspiciousAddress(connection.getRemoteAddress())) {
                System.out.println("address " + connection.getRemoteAddress().getCanonicalHostName() + " is Suspicious. Slow down transfer rate");
                connection.setWriteTransferRate(5);
                suspiciousAddresses.remove(connection.getRemoteAddress().getCanonicalHostName());
            }
            return false;
        }

        private boolean isSuspiciousAddress(InetAddress remoteAddress) {
            return suspiciousAddresses.contains(remoteAddress.getCanonicalHostName());
        }
    }
}
