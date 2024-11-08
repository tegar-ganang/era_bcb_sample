package chat.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Vector;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import chat.client.gui.Main;
import chat.client.messages.ChatMessage;
import chat.client.messages.FileInfoMessage;
import chat.client.messages.FileInfoReturnMessage;

class ReceiveThread extends Thread {

    private boolean weiter = true;

    private int port;

    FileReceiveThread frt = null;

    public ReceiveThread(int port) {
        this.port = port;
        new LinkedList<ChatMessage>();
        setPriority(MIN_PRIORITY);
    }

    @Override
    public void run() {
        DatagramChannel channel = null;
        try {
            channel = DatagramChannel.open();
            channel.socket().bind(new InetSocketAddress(port));
            channel.configureBlocking(false);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        ByteBuffer readBuf = ByteBuffer.allocate(4096);
        while (weiter) try {
            SocketAddress cl = channel.receive(readBuf);
            if (cl != null) {
                readBuf.flip();
                if (readBuf.array().length > 0) {
                    String s = new String(readBuf.array());
                    if (s.contains("sender")) {
                        ChatMessage msg = ChatMessage.fromString(s);
                        if (msg != null) if (msg instanceof FileInfoMessage) {
                            if (frt != null) {
                                JOptionPane.showMessageDialog(null, "Es wird bereits ein eingehender Dateitransfer get�tigt.", "Hinweis", JOptionPane.OK_CANCEL_OPTION);
                                continue;
                            }
                            JFileChooser fc = new JFileChooser(msg.getContent());
                            int i = fc.showSaveDialog(null);
                            if (i == JFileChooser.APPROVE_OPTION) {
                                fc.getSelectedFile().createNewFile();
                                System.out.println("Starting FileReceiveThread");
                                frt = new FileReceiveThread(((FileInfoMessage) msg).getFileLenght(), fc.getSelectedFile());
                                frt.start();
                                frt.setPriority(MIN_PRIORITY);
                                Main.con.getChatSession(((InetSocketAddress) cl).getAddress(), false).send(ChatMessage.returnReceiveThreadInfo(null, null), false);
                            }
                        } else if (msg instanceof FileInfoReturnMessage) Main.con.getChatSession(((InetSocketAddress) cl).getAddress(), false).sendFileData(); else if (s.contains(ChatMessage.class.getName())) {
                            Vector<ChatSession> ses;
                            synchronized (Main.con.openSessions) {
                                ses = Main.con.openSessions;
                            }
                            boolean found = false;
                            for (ChatSession cs : ses) if (((InetSocketAddress) cl).getAddress().equals(cs.getAddress())) {
                                cs.addMsg(msg);
                                found = true;
                                break;
                            }
                            if (!found) {
                                ChatSession s1 = Main.con.getChatSession(((InetSocketAddress) cl).getAddress(), false);
                                s1.addMsg(msg);
                            }
                        } else System.out.println(msg);
                        msg = null;
                    }
                    s = null;
                }
            }
            cl = null;
            readBuf.clear();
            Thread.sleep(100);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * 
	 */
    public void kill() {
        weiter = false;
    }

    class FileReceiveThread extends Thread {

        private long size;

        private File dest;

        private int buff = 4096;

        FileReceiveThread(long filesize, File output) {
            size = filesize;
            dest = output;
        }

        @Override
        public void run() {
            try {
                long start = System.currentTimeMillis();
                System.out.println("Start receiving: " + start);
                FileOutputStream fos = new FileOutputStream(dest);
                FileChannel fc = fos.getChannel();
                ServerSocketChannel ssc = ServerSocketChannel.open();
                ssc.socket().bind(new InetSocketAddress(Main.fileReceivePort));
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(true);
                ByteBuffer bb = ByteBuffer.allocate(buff);
                long middle = System.currentTimeMillis();
                long sizeremaining = size;
                int n = -1;
                while (sizeremaining > 0) if ((n = sc.read(bb)) != -1) {
                    bb.flip();
                    fc.write(bb);
                    bb.compact();
                    sizeremaining -= n;
                }
                bb.clear();
                fc.close();
                fos.flush();
                fos.close();
                sc.close();
                ssc.close();
                long end = System.currentTimeMillis();
                System.out.println("Vorbereitung: " + (middle - start));
                System.out.println("Lesen/Senden: " + (end - middle));
                System.out.println("Gesamt: " + (end - start));
                System.gc();
            } catch (Exception e) {
                e.printStackTrace();
            }
            frt = null;
        }
    }
}
