package goldengate.commandexec.client.test;

import goldengate.commandexec.client.LocalExecClientHandler;
import goldengate.commandexec.client.LocalExecClientPipelineFactory;
import goldengate.commandexec.utils.LocalExecResult;
import goldengate.common.logging.GgSlf4JLoggerFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.logging.InternalLoggerFactory;
import ch.qos.logback.classic.Level;

/**
 * LocalExec client.
 *
 * This class is an example of client.
 *
 * On a bi-core Centrino2 vPro: 18/s in 50 sequential, 30/s in 10 threads with 50 sequential
 */
public class LocalExecClientTest extends Thread {

    static int nit = 1;

    static int nth = 1;

    static String command = "d:\\GG\\testexec.bat";

    static int port = 9999;

    static InetSocketAddress address;

    static LocalExecResult result;

    static int ok = 0;

    static int ko = 0;

    static ExecutorService threadPool;

    static ExecutorService threadPool2;

    static ClientBootstrap bootstrap;

    static LocalExecClientPipelineFactory localExecClientPipelineFactory;

    /**
     * Test & example main
     * @param args ignored
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        InternalLoggerFactory.setDefaultFactory(new GgSlf4JLoggerFactory(Level.WARN));
        InetAddress addr;
        byte[] loop = { 127, 0, 0, 1 };
        try {
            addr = InetAddress.getByAddress(loop);
        } catch (UnknownHostException e) {
            return;
        }
        address = new InetSocketAddress(addr, port);
        threadPool = Executors.newCachedThreadPool();
        threadPool2 = Executors.newCachedThreadPool();
        bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(threadPool, threadPool2));
        localExecClientPipelineFactory = new LocalExecClientPipelineFactory();
        bootstrap.setPipelineFactory(localExecClientPipelineFactory);
        try {
            LocalExecClientTest client = new LocalExecClientTest();
            long first = System.currentTimeMillis();
            client.connect();
            client.runOnce();
            client.disconnect();
            long second = System.currentTimeMillis();
            System.err.println("1=Total time in ms: " + (second - first) + " or " + (1 * 1000 / (second - first)) + " exec/s");
            System.err.println("Result: " + ok + ":" + ko);
            ok = 0;
            ko = 0;
            first = System.currentTimeMillis();
            for (int i = 0; i < nit; i++) {
                client.connect();
                client.runOnce();
                client.disconnect();
            }
            second = System.currentTimeMillis();
            System.err.println(nit + "=Total time in ms: " + (second - first) + " or " + (nit * 1000 / (second - first)) + " exec/s");
            System.err.println("Result: " + ok + ":" + ko);
            ok = 0;
            ko = 0;
            ExecutorService executorService = Executors.newFixedThreadPool(nth);
            first = System.currentTimeMillis();
            for (int i = 0; i < nth; i++) {
                executorService.submit(new LocalExecClientTest());
            }
            Thread.sleep(500);
            executorService.shutdown();
            while (!executorService.awaitTermination(200, TimeUnit.MILLISECONDS)) {
                Thread.sleep(50);
            }
            second = System.currentTimeMillis();
            System.err.println((nit * nth) + "=Total time in ms: " + (second - first) + " or " + (nit * nth * 1000 / (second - first)) + " exec/s");
            System.err.println("Result: " + ok + ":" + ko);
            ok = 0;
            ko = 0;
            first = System.currentTimeMillis();
            client.connect();
            client.runFinal();
            client.disconnect();
            second = System.currentTimeMillis();
            System.err.println("1=Total time in ms: " + (second - first) + " or " + (1 * 1000 / (second - first)) + " exec/s");
            System.err.println("Result: " + ok + ":" + ko);
            ok = 0;
            ko = 0;
        } finally {
            bootstrap.releaseExternalResources();
            localExecClientPipelineFactory.releaseResources();
        }
    }

    /**
     * Simple constructor
     */
    public LocalExecClientTest() {
    }

    private Channel channel;

    /**
     * Run method for thread
     */
    public void run() {
        connect();
        for (int i = 0; i < nit; i++) {
            this.runOnce();
        }
        disconnect();
    }

    /**
     * Connect to the Server
     */
    private void connect() {
        ChannelFuture future = bootstrap.connect(address);
        channel = future.awaitUninterruptibly().getChannel();
        if (!future.isSuccess()) {
            System.err.println("Client Not Connected");
            future.getCause().printStackTrace();
            return;
        }
    }

    /**
     * Disconnect from the server
     */
    private void disconnect() {
        channel.close().awaitUninterruptibly();
    }

    /**
     * Run method both for not threaded execution and threaded execution
     */
    public void runOnce() {
        LocalExecClientHandler clientHandler = (LocalExecClientHandler) channel.getPipeline().getLast();
        clientHandler.initExecClient();
        ChannelFuture lastWriteFuture = null;
        String line = command + "\n";
        if (line != null) {
            lastWriteFuture = channel.write(line);
            if (lastWriteFuture != null) {
                lastWriteFuture.awaitUninterruptibly();
            }
            LocalExecResult localExecResult = clientHandler.waitFor(10000);
            int status = localExecResult.status;
            if (status < 0) {
                System.err.println("Status: " + status + "\nResult: " + localExecResult.result);
                ko++;
            } else {
                ok++;
                result = localExecResult;
            }
        }
    }

    /**
     * Run method for closing Server
     */
    private void runFinal() {
        LocalExecClientHandler clientHandler = (LocalExecClientHandler) channel.getPipeline().getLast();
        clientHandler.initExecClient();
        ChannelFuture lastWriteFuture = null;
        String line = "-1000 stop\n";
        if (line != null) {
            lastWriteFuture = channel.write(line);
            if (lastWriteFuture != null) {
                lastWriteFuture.awaitUninterruptibly();
            }
            LocalExecResult localExecResult = clientHandler.waitFor(10000);
            int status = localExecResult.status;
            if (status < 0) {
                System.err.println("Status: " + status + "\nResult: " + localExecResult.result);
                ko++;
            } else {
                ok++;
                result = localExecResult;
            }
        }
    }
}
