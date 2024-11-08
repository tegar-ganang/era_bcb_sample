package ch.unifr.nio.framework.examples;

import ch.unifr.nio.framework.ChannelHandler;
import ch.unifr.nio.framework.Dispatcher;
import ch.unifr.nio.framework.FrameworkTools;
import ch.unifr.nio.framework.HandlerAdapter;
import ch.unifr.nio.framework.transform.AbstractForwarder;
import ch.unifr.nio.framework.transform.BenchmarkForwarder;
import ch.unifr.nio.framework.transform.ChannelReader;
import ch.unifr.nio.framework.transform.ChannelWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class BenchmarkClient implements ChannelHandler {

    private static final Logger LOGGER = Logger.getLogger(BenchmarkClient.class.getName());

    private static final int TIMEOUT = 20;

    private static int counter;

    private final long start;

    private static InetAddress serverAddress;

    private static int port;

    private static Dispatcher dispatcher;

    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private HandlerAdapter handlerAdapter;

    private static final DateFormat DATE_FORMAT = DateFormat.getTimeInstance(DateFormat.MEDIUM);

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();

    private final ChannelReader reader;

    private final ChannelWriter writer;

    private final BenchmarkForwarder benchmarkTransformer;

    /** Creates a new instance of BenchmarkClient
     * @param serverAddress the address of the server to connect to
     * @param port the port to connect to
     * @throws java.io.IOException if an I/O exception occurs
     */
    public BenchmarkClient(InetAddress serverAddress, int port) throws IOException {
        System.out.println("connecting to " + serverAddress + ":" + port);
        InetSocketAddress socketAddress = new InetSocketAddress(serverAddress, port);
        SocketChannel channel = SocketChannel.open(socketAddress);
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        int receiveBufferSize = socket.getReceiveBufferSize();
        receiveBufferSize = 3000;
        final int SEND_BUFFER_SIZE = 3000;
        reader = new ChannelReader(true, receiveBufferSize, receiveBufferSize * 10);
        reader.setNextForwarder(new BenchmarkClientTransformer());
        writer = new ChannelWriter(true);
        benchmarkTransformer = new BenchmarkForwarder(SEND_BUFFER_SIZE, true);
        benchmarkTransformer.setNextForwarder(writer);
        benchmarkTransformer.setChannelWriter(writer);
        dispatcher.registerChannel(channel, this);
        start = System.currentTimeMillis();
        System.out.println(DATE_FORMAT.format(start) + " scheduling test " + (++counter) + " to run for " + TIMEOUT + " seconds...");
        executor.schedule(new Report(), TIMEOUT, TimeUnit.SECONDS);
        benchmarkTransformer.forward(null);
    }

    /**
     * starts the BenchmarkClient
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: BenchmarkClient <server host> <server port>");
            System.exit(1);
        }
        Logger frameworkLogger = Logger.getLogger("ch.unifr.nio.framework");
        frameworkLogger.setLevel(Level.OFF);
        try {
            serverAddress = InetAddress.getByName(args[0]);
            dispatcher = new Dispatcher();
            dispatcher.start();
            port = Integer.parseInt(args[1]);
            new BenchmarkClient(serverAddress, port);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
    }

    @Override
    public void inputClosed() {
        System.out.println("BenchmarkServer closed the connection");
        System.exit(1);
    }

    @Override
    public void channelException(Exception exception) {
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
                int outputRemaining = writer.remaining();
                System.out.println(" -> " + NUMBER_FORMAT.format(outputRemaining) + " byte in writer");
                long sentBytes = writer.getWriteCounter();
                int sendRate = (int) (sentBytes / time);
                System.out.println("  -> data sent: " + NUMBER_FORMAT.format(sentBytes) + " byte (" + FrameworkTools.getBandwidthString(sendRate, 1) + ")");
                long receivedBytes = reader.getReadCounter();
                int receiveRate = (int) (receivedBytes / time);
                System.out.println("  <- data received: " + NUMBER_FORMAT.format(receivedBytes) + " byte (" + FrameworkTools.getBandwidthString(receiveRate, 1) + ")");
                int inputRemaining = reader.getBuffer().remaining();
                System.out.println(" <- " + NUMBER_FORMAT.format(inputRemaining) + " byte in reader");
                long diff = sentBytes - receivedBytes;
                System.out.println(" = " + NUMBER_FORMAT.format(diff) + " byte not received");
                long gap = TIMEOUT * 500;
                System.out.println("sleeping for " + (gap / 1000) + " seconds...\n");
                Thread.sleep(gap);
                new BenchmarkClient(serverAddress, port);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, null, ex);
                System.exit(-1);
            }
        }
    }

    @Override
    public ChannelReader getChannelReader() {
        return reader;
    }

    @Override
    public ChannelWriter getChannelWriter() {
        return writer;
    }

    private class BenchmarkClientTransformer extends AbstractForwarder<ByteBuffer, Void> {

        @Override
        public void forward(ByteBuffer input) throws IOException {
            input.position(input.limit());
        }
    }
}
