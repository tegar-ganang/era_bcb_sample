package udt.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.text.NumberFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import udt.UDTInputStream;
import udt.UDTOutputStream;
import udt.UDTReceiver;
import udt.UDTServerSocket;
import udt.UDTSocket;

/**
 * helper application for sending a single file via UDT
 * Intended to be compatible with the C++ version in 
 * the UDT reference implementation
 * 
 * main method USAGE: java -cp .. udt.util.SendFile <server_port>
 */
public class SendFile extends Application {

    private final int serverPort;

    private final ExecutorService threadPool = Executors.newFixedThreadPool(3);

    public SendFile(int serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public void configure() {
        super.configure();
    }

    public void run() {
        configure();
        try {
            UDTReceiver.connectionExpiryDisabled = true;
            InetAddress myHost = localIP != null ? InetAddress.getByName(localIP) : InetAddress.getLocalHost();
            UDTServerSocket server = new UDTServerSocket(myHost, serverPort);
            while (true) {
                UDTSocket socket = server.accept();
                Thread.sleep(1000);
                threadPool.execute(new RequestRunner(socket));
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
	 * main() method for invoking as a commandline application
	 * @param args
	 * @throws Exception
	 */
    public static void main(String[] fullArgs) throws Exception {
        String[] args = parseOptions(fullArgs);
        int serverPort = 65321;
        try {
            serverPort = Integer.parseInt(args[0]);
        } catch (Exception ex) {
            usage();
            System.exit(1);
        }
        SendFile sf = new SendFile(serverPort);
        sf.run();
    }

    public static void usage() {
        System.out.println("Usage: java -cp ... udt.util.SendFile <server_port> " + "[--verbose] [--localPort=<port>] [--localIP=<ip>]");
    }

    public static class RequestRunner implements Runnable {

        private static final Logger logger = Logger.getLogger(RequestRunner.class.getName());

        private final UDTSocket socket;

        private final NumberFormat format = NumberFormat.getNumberInstance();

        private final boolean memMapped;

        public RequestRunner(UDTSocket socket) {
            this.socket = socket;
            format.setMaximumFractionDigits(3);
            memMapped = false;
        }

        public void run() {
            try {
                logger.info("Handling request from " + socket.getSession().getDestination());
                UDTInputStream in = socket.getInputStream();
                UDTOutputStream out = socket.getOutputStream();
                byte[] readBuf = new byte[32768];
                ByteBuffer bb = ByteBuffer.wrap(readBuf);
                while (in.read(readBuf) == 0) Thread.sleep(100);
                byte[] len = new byte[4];
                bb.get(len);
                if (verbose) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < len.length; i++) {
                        sb.append(Integer.toString(len[i]));
                        sb.append(" ");
                    }
                    System.out.println("[SendFile] name length data: " + sb.toString());
                }
                long length = decode(len, 0);
                if (verbose) System.out.println("[SendFile] name length     : " + length);
                byte[] fileName = new byte[(int) length];
                bb.get(fileName);
                File file = new File(new String(fileName));
                System.out.println("[SendFile] File requested: '" + file.getPath() + "'");
                FileInputStream fis = null;
                try {
                    long size = file.length();
                    System.out.println("[SendFile] File size: " + size);
                    out.write(encode64(size));
                    out.flush();
                    long start = System.currentTimeMillis();
                    if (memMapped) {
                        copyFile(file, out);
                    } else {
                        fis = new FileInputStream(file);
                        Util.copy(fis, out, size, false);
                    }
                    System.out.println("[SendFile] Finished sending data.");
                    long end = System.currentTimeMillis();
                    System.out.println(socket.getSession().getStatistics().toString());
                    double rate = 1000.0 * size / 1024 / 1024 / (end - start);
                    System.out.println("[SendFile] Rate: " + format.format(rate) + " MBytes/sec. " + format.format(8 * rate) + " MBit/sec.");
                    if (Boolean.getBoolean("udt.sender.storeStatistics")) {
                        socket.getSession().getStatistics().writeParameterHistory(new File("udtstats-" + System.currentTimeMillis() + ".csv"));
                    }
                } finally {
                    socket.getSender().stop();
                    if (fis != null) fis.close();
                }
                logger.info("Finished request from " + socket.getSession().getDestination());
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }
    }

    private static void copyFile(File file, OutputStream os) throws Exception {
        FileChannel c = new RandomAccessFile(file, "r").getChannel();
        MappedByteBuffer b = c.map(MapMode.READ_ONLY, 0, file.length());
        b.load();
        byte[] buf = new byte[1024 * 1024];
        int len = 0;
        while (true) {
            len = Math.min(buf.length, b.remaining());
            b.get(buf, 0, len);
            os.write(buf, 0, len);
            if (b.remaining() == 0) break;
        }
        os.flush();
    }
}
