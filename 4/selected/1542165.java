package org.iosgi.outpost;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.iosgi.outpost.operations.Exec;
import org.iosgi.outpost.operations.MkDir;
import org.iosgi.outpost.operations.Put;
import org.iosgi.util.io.Streams;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

/**
 * @author Sven Schulz
 */
public class Client {

    private final ClientBootstrap bootstrap;

    private final String host;

    private final int port;

    private Channel channel;

    private final Exchanger<Object> ex;

    public Client(String host) {
        this(host, Constants.DEFAULT_PORT, null);
    }

    public Client(String host, ClassLoader cl) {
        this(host, Constants.DEFAULT_PORT, cl);
    }

    public Client(String host, int port, final ClassLoader cl) {
        this.host = host;
        this.port = port;
        ex = new Exchanger<Object>();
        bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new ObjectEncoder(), cl != null ? new ObjectDecoder(Integer.MAX_VALUE, cl) : new ObjectDecoder(Integer.MAX_VALUE), new ClientHandler(Client.this.ex));
            }
        });
    }

    public void connect(long timeout, TimeUnit unit) throws Exception {
        bootstrap.setOption("connectTimeoutMillis", TimeUnit.MILLISECONDS.convert(timeout, unit));
        ChannelFuture cf = bootstrap.connect(new InetSocketAddress(host, port));
        cf.await();
        if (!cf.isSuccess()) {
            throw (Exception) cf.getCause();
        }
        channel = cf.getChannel();
    }

    public void close() {
        channel.close();
    }

    @SuppressWarnings("unchecked")
    public <T> T perform(final Operation<T> op) throws InterruptedException, OperationExecutionException {
        channel.write(op);
        Object result = ex.exchange(null);
        if (result.equals(Null.NULL)) {
            return null;
        } else if (result instanceof OperationExecutionException) {
            throw (OperationExecutionException) result;
        } else {
            return (T) result;
        }
    }

    public void put(InputStream is, File to) throws InterruptedException, OperationExecutionException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Streams.drain(is, baos);
        Put p = new Put(to, baos.toByteArray());
        perform(p);
    }

    public void put(byte[] data, File to) throws InterruptedException, OperationExecutionException {
        Put p = new Put(to, data);
        perform(p);
    }

    public void put(File from, File to) throws IOException, InterruptedException, OperationExecutionException {
        put(from, to, null);
    }

    public void put(File from, File to, FileFilter filter) throws IOException, InterruptedException, OperationExecutionException {
        if (filter != null && !filter.accept(from)) {
            return;
        }
        if (from.isFile()) {
            Put p = new Put(from, to);
            perform(p);
        } else if (from.isDirectory()) {
            mkdir(to);
            for (File f : from.listFiles()) {
                put(f, new File(to, f.getName()), filter);
            }
        }
    }

    public void mkdir(File dir) throws InterruptedException, OperationExecutionException {
        MkDir mkdir = new MkDir(dir);
        perform(mkdir);
    }

    public int execute(List<String> command, File workDir, boolean block, File out, File err) throws InterruptedException, OperationExecutionException {
        Exec exec = new Exec(workDir, command, block, out, err);
        return perform(exec);
    }
}
