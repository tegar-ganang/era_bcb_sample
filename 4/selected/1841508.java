package chat.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import chat.client.gui.Main;
import chat.client.messages.ChatMessage;

class SendThread extends Thread {

    protected final InetAddress ip;

    protected final int port;

    protected boolean weiter = true;

    protected final LinkedList<ChatMessage> messages;

    protected File aktFile = null;

    private FileSendThread fst;

    public SendThread(final InetAddress ip, final int port) {
        this.ip = ip;
        this.port = port;
        this.messages = new LinkedList<ChatMessage>();
        this.setPriority(Thread.MIN_PRIORITY);
    }

    @Override
    public void run() {
        DatagramChannel channel = null;
        try {
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.connect(new InetSocketAddress(this.ip, this.port));
        } catch (final IOException e) {
            e.printStackTrace();
            this.weiter = false;
        }
        ByteBuffer bb;
        if (channel == null) {
            return;
        }
        while (this.weiter) {
            if (!this.messages.isEmpty()) {
                try {
                    ChatMessage msg;
                    synchronized (this.messages) {
                        msg = this.messages.poll();
                    }
                    bb = ByteBuffer.wrap(msg.toString().getBytes());
                    channel.write(bb);
                } catch (final IOException e) {
                    e.printStackTrace();
                    this.weiter = false;
                }
            }
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e1) {
                e1.printStackTrace();
            }
        }
        try {
            channel.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * @param msg
	 */
    public void sendMsg(final ChatMessage msg) {
        synchronized (this.messages) {
            this.messages.add(msg);
        }
    }

    /**
	 * 
	 */
    public void kill() {
        this.weiter = false;
        if (this.fst != null) {
            this.fst = null;
        }
    }

    public void sendFile() {
        System.out.println("Starting FileSendThread");
        this.fst = new FileSendThread();
        this.fst.setPriority(Thread.MIN_PRIORITY);
        this.fst.start();
    }

    public boolean isSendingFile() {
        if (this.fst != null && this.fst.isAlive()) {
            return true;
        }
        if (this.aktFile != null && this.fst != null) {
            return true;
        }
        return false;
    }

    public void setSendFile(final File f) {
        if (!this.isSendingFile()) {
            this.aktFile = f;
        }
    }

    public void abortSend() {
        this.fst = null;
        this.setSendFile(null);
    }

    public File getSendFile() {
        return this.aktFile;
    }

    class FileSendThread extends Thread {

        @Override
        public void run() {
            final long start = System.currentTimeMillis();
            try {
                System.out.println("Starting sending:" + start);
                final FileInputStream fis = new FileInputStream(SendThread.this.aktFile);
                final FileChannel fc = fis.getChannel();
                final ByteBuffer bb = ByteBuffer.allocate(4096);
                final SocketChannel sc = SocketChannel.open(new InetSocketAddress(SendThread.this.ip, Main.fileReceivePort));
                final long middle = System.currentTimeMillis();
                int i = 0;
                while (fc.read(bb) != -1) {
                    i++;
                    bb.flip();
                    sc.write(bb);
                    bb.compact();
                }
                System.out.println("Send: " + i);
                fc.close();
                bb.clear();
                sc.close();
                final long end = System.currentTimeMillis();
                System.out.println("Preparation: " + (middle - start));
                System.out.println("Reaing/Sending: " + (end - middle));
                Connection.getConnection(false).getChatSession(SendThread.this.ip, false).sendFileFinished(true);
            } catch (final IOException e) {
                e.printStackTrace();
                Connection.getConnection(false).getChatSession(SendThread.this.ip, false).sendFileFinished(false);
            } finally {
                SendThread.this.aktFile = null;
                System.gc();
            }
        }
    }
}
