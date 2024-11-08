package ch.unifr.nio.framework.examples;

import ch.unifr.nio.framework.AbstractAcceptor;
import ch.unifr.nio.framework.BufferSizeListener;
import ch.unifr.nio.framework.ChannelHandler;
import ch.unifr.nio.framework.Dispatcher;
import ch.unifr.nio.framework.FrameworkTools;
import ch.unifr.nio.framework.HandlerAdapter;
import ch.unifr.nio.framework.transform.ChannelReader;
import ch.unifr.nio.framework.transform.ChannelWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This Server just echos every data it receives
 */
public class BenchmarkServer extends AbstractAcceptor {

    private static int counter;

    private static final DateFormat DATE_FORMAT = DateFormat.getTimeInstance(DateFormat.MEDIUM);

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();

    /**
     * creates a new BenchmarkServer
     * @param dispatcher the central dispatcher
     * @param socketAddress the address to bind to
     * @throws java.io.IOException if an I/O exception occurs
     */
    public BenchmarkServer(Dispatcher dispatcher, SocketAddress socketAddress) throws IOException {
        super(dispatcher, socketAddress);
    }

    /**
     * starts the EchoServer
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        if (args.length != 1) {
            System.out.println("Usage: BenchmarkServer <port>");
            System.exit(1);
        }
        Logger logger = Logger.getLogger("ch.unifr.nio.framework");
        logger.setLevel(Level.OFF);
        try {
            int port = Integer.parseInt(args[0]);
            Dispatcher dispatcher = new Dispatcher();
            dispatcher.start();
            SocketAddress socketAddress = new InetSocketAddress(port);
            BenchmarkServer benchmarkServer = new BenchmarkServer(dispatcher, socketAddress);
            benchmarkServer.start();
            System.out.println("BenchmarkServer is running...");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected ChannelHandler getHandler(SocketChannel socketChannel) {
        return new BenchmarkChannelHandler(3000);
    }

    private class BenchmarkChannelHandler implements ChannelHandler, BufferSizeListener {

        private final ChannelReader reader;

        private final ChannelWriter writer;

        private HandlerAdapter handlerAdapter;

        private final long start;

        public BenchmarkChannelHandler(int bufferSize) {
            reader = new ChannelReader(true, bufferSize, bufferSize * 10);
            writer = new ChannelWriter(true);
            writer.addBufferSizeListener(this);
            reader.setNextForwarder(writer);
            start = System.currentTimeMillis();
            System.out.println(DATE_FORMAT.format(start) + " Starting test " + (++counter) + "...");
        }

        @Override
        public void inputClosed() {
            System.out.println("Client closed the connection");
        }

        @Override
        public void channelException(Exception exception) {
            printResult();
        }

        @Override
        public void channelRegistered(HandlerAdapter handlerAdapter) {
            this.handlerAdapter = handlerAdapter;
        }

        private void printResult() {
            long stop = System.currentTimeMillis();
            int time = (int) ((stop - start) / 1000);
            System.out.println("Test " + counter + " was running " + time + " seconds");
            long receivedBytes = reader.getReadCounter();
            int receiveRate = (int) (receivedBytes / time);
            System.out.println(" -> data received: " + NUMBER_FORMAT.format(receivedBytes) + " byte (" + FrameworkTools.getBandwidthString(receiveRate, 1) + ")");
            int inputRemaining = reader.getBuffer().remaining();
            System.out.println("  -> " + NUMBER_FORMAT.format(inputRemaining) + " byte in reader");
            int outputRemaining = writer.remaining();
            System.out.println("  <- " + NUMBER_FORMAT.format(outputRemaining) + " byte in writer");
            long sentBytes = writer.getWriteCounter();
            int sendRate = (int) (sentBytes / time);
            System.out.println(" <- data sent: " + NUMBER_FORMAT.format(sentBytes) + " byte (" + FrameworkTools.getBandwidthString(sendRate, 1) + ")");
            System.out.println();
        }

        @Override
        public void bufferSizeChanged(Object source, int newLevel) {
            if (newLevel == 0) {
                handlerAdapter.addInterestOps(SelectionKey.OP_READ);
            } else {
                handlerAdapter.removeInterestOps(SelectionKey.OP_READ);
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
    }
}
