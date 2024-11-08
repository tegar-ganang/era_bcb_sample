package ch.unifr.nio.framework.examples;

import ch.unifr.nio.framework.ChannelHandler;
import ch.unifr.nio.framework.Dispatcher;
import ch.unifr.nio.framework.FrameworkTools;
import ch.unifr.nio.framework.HandlerAdapter;
import ch.unifr.nio.framework.NIOFormatter;
import ch.unifr.nio.framework.ssl.SSLTools;
import ch.unifr.nio.framework.transform.AbstractForwarder;
import ch.unifr.nio.framework.transform.BenchmarkForwarder;
import ch.unifr.nio.framework.transform.ByteBufferToArrayTransformer;
import ch.unifr.nio.framework.transform.ChannelReader;
import ch.unifr.nio.framework.transform.ChannelWriter;
import ch.unifr.nio.framework.transform.SSLInputForwarder;
import ch.unifr.nio.framework.transform.SSLOutputForwarder;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

/**
 *
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class SSLBenchmarkClient implements ChannelHandler {

    private static final int TIMEOUT = 30;

    private static int counter;

    private final long start;

    private static InetAddress serverAddress;

    private static int port;

    private static SSLContext sslContext;

    private static final int PLAINTEXT_BUFFER_SIZE = 8000;

    private static Dispatcher dispatcher;

    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private final ChannelWriter channelWriter;

    private final ChannelReader channelReader;

    private final SSLInputForwarder sslInputTransformer;

    private final BenchmarkForwarder benchmarkTransformer;

    private final SSLOutputForwarder sslOutputTransformer;

    private HandlerAdapter handlerAdapter;

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss:SSS");

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();

    /** Creates a new instance of SSLBenchmarkClient
     * @param sslContext the SSL context fot the connection
     * @param serverAddress the address of the server
     * @param port the port of the server
     * @throws java.io.IOException if an I/O exception occurs
     */
    public SSLBenchmarkClient(SSLContext sslContext, InetAddress serverAddress, int port) throws IOException {
        Logger frameworkLogger = Logger.getLogger("ch.unifr.nio.framework");
        frameworkLogger.setLevel(Level.OFF);
        FileHandler fileHandler = new FileHandler("%h/ssl_benchmark_client.log", 52428800, 2, true);
        fileHandler.setFormatter(NIOFormatter.getInstance());
        frameworkLogger.addHandler(fileHandler);
        InetSocketAddress socketAddress = new InetSocketAddress(serverAddress, port);
        SocketChannel channel = SocketChannel.open(socketAddress);
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        String remoteAddress = socket.getInetAddress().getHostAddress();
        int remotePort = socket.getPort();
        SSLEngine sslEngine = sslContext.createSSLEngine(remoteAddress, remotePort);
        sslEngine.setUseClientMode(true);
        channelReader = new ChannelReader(false, 1024, 10240);
        channelReader.setChannel(channel);
        channelWriter = new ChannelWriter(false);
        channelWriter.setChannel(channel);
        sslOutputTransformer = new SSLOutputForwarder(sslEngine, PLAINTEXT_BUFFER_SIZE);
        sslOutputTransformer.setNextForwarder(channelWriter);
        ByteBufferToArrayTransformer byteBufferToArrayTransformer = new ByteBufferToArrayTransformer();
        byteBufferToArrayTransformer.setNextForwarder(sslOutputTransformer);
        benchmarkTransformer = new BenchmarkForwarder(3000, false);
        benchmarkTransformer.setNextForwarder(byteBufferToArrayTransformer);
        benchmarkTransformer.setChannelWriter(channelWriter);
        benchmarkTransformer.setSSLOutputForwarder(sslOutputTransformer);
        sslInputTransformer = new SSLInputForwarder(sslEngine);
        channelReader.setNextForwarder(sslInputTransformer);
        sslInputTransformer.setNextForwarder(new BenchmarkClientTransformer());
        sslInputTransformer.setSSLOutputForwarder(sslOutputTransformer);
        sslOutputTransformer.setSSLInputForwarder(sslInputTransformer);
        dispatcher.registerChannel(channel, this);
        start = System.currentTimeMillis();
        System.out.println(DATE_FORMAT.format(start) + " scheduling test " + (++counter) + " to run for " + TIMEOUT + " seconds...");
        executor.schedule(new Report(), TIMEOUT, TimeUnit.SECONDS);
        benchmarkTransformer.forward(null);
    }

    /**
     * starts the SSLBenchmarkClient
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Usage: SSLBenchmarkClient <truststore URL> " + "<truststore password> <server host> <server port>");
            System.exit(1);
        }
        try {
            URL trustStoreURL = new URL("file:/" + args[0]);
            sslContext = SSLTools.getClientSSLContext(trustStoreURL, args[1]);
            serverAddress = InetAddress.getByName(args[2]);
            port = Integer.parseInt(args[3]);
            if (port < 1 || port > 65535) {
                System.out.println("port " + port + " is out of range (1..65535)");
                System.exit(1);
            }
            dispatcher = new Dispatcher();
            dispatcher.start();
            new SSLBenchmarkClient(sslContext, serverAddress, port);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void inputClosed() {
        System.out.println("SSLBenchmarkServer closed the connection");
        System.exit(1);
    }

    @Override
    public void channelException(Exception exception) {
        System.out.println("Connection error " + exception);
        System.exit(1);
    }

    @Override
    public void channelRegistered(HandlerAdapter handlerAdapter) {
        this.handlerAdapter = handlerAdapter;
    }

    private class Report implements Runnable {

        @Override
        public void run() {
            benchmarkTransformer.stop();
            try {
                handlerAdapter.closeChannel();
                long stop = System.currentTimeMillis();
                int time = (int) (stop - start) / 1000;
                System.out.println("test was running " + time + " seconds");
                long outputPlainTextCounter = sslOutputTransformer.getPlainTextCounter();
                int outputPlainTextSendRate = (int) (outputPlainTextCounter / time);
                System.out.println(" -> " + NUMBER_FORMAT.format(outputPlainTextCounter) + " byte plaintext enqueued (" + FrameworkTools.getBandwidthString(outputPlainTextSendRate, 1) + ")");
                int outputRemainingPlainText = sslOutputTransformer.getRemainingPlaintext();
                System.out.println(" -> " + NUMBER_FORMAT.format(outputRemainingPlainText) + " byte remaining plaintext in sslOutputTransformer");
                int outputRemaining = channelWriter.remaining();
                System.out.println("  -> " + NUMBER_FORMAT.format(outputRemaining) + " byte remaining ciphertext in channelwriter");
                long sentBytes = channelWriter.getWriteCounter();
                int sendRate = (int) (sentBytes / time);
                System.out.println("  -> " + NUMBER_FORMAT.format(sentBytes) + " byte ciphertext sent (" + FrameworkTools.getBandwidthString(sendRate, 1) + ")");
                System.out.println("  -------------------------------------------");
                long receivedCipherText = channelReader.getReadCounter();
                int receiveRate = (int) (receivedCipherText / time);
                System.out.println("  <- " + NUMBER_FORMAT.format(receivedCipherText) + " byte ciphertext received (" + FrameworkTools.getBandwidthString(receiveRate, 1) + ")");
                int inputRemaining = channelReader.getBuffer().remaining();
                System.out.println("  <- " + NUMBER_FORMAT.format(inputRemaining) + " byte remaining cipherText in channelReader");
                long inputPlainTextCounter = sslInputTransformer.getPlainTextCounter();
                int inputPlainTextSendRate = (int) (inputPlainTextCounter / time);
                System.out.println(" <- " + NUMBER_FORMAT.format(inputPlainTextCounter) + " byte plaintext produced (" + FrameworkTools.getBandwidthString(inputPlainTextSendRate, 1) + ")");
                int inputRemainingPlainText = sslInputTransformer.getPlainText().remaining();
                System.out.println(" <- " + NUMBER_FORMAT.format(inputRemainingPlainText) + " byte remaining plaintext in sslInputTransformer");
                long diff = outputPlainTextCounter - outputRemainingPlainText - inputPlainTextCounter;
                System.out.println(" = " + NUMBER_FORMAT.format(diff) + " byte plaintext not received");
                long gap = TIMEOUT * 500;
                System.out.println("sleeping for " + (gap / 1000) + " seconds...\n");
                Thread.sleep(gap);
                new SSLBenchmarkClient(sslContext, serverAddress, port);
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            }
        }
    }

    @Override
    public ChannelReader getChannelReader() {
        return channelReader;
    }

    @Override
    public ChannelWriter getChannelWriter() {
        return channelWriter;
    }

    private class BenchmarkClientTransformer extends AbstractForwarder<ByteBuffer, Void> {

        @Override
        public void forward(ByteBuffer input) throws IOException {
            input.position(input.limit());
        }
    }
}
