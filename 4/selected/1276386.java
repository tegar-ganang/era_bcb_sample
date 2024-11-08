package chat.client;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import chat.client.gui.Main;
import chat.client.gui.Settings;
import chat.client.messages.ChatMessage;
import chat.client.messages.FileInfoMessage;
import chat.client.messages.FileInfoReturnMessage;

class RecieveThread extends Thread {

    private boolean weiter = true;

    private final int port;

    FileReceiveThread frt = null;

    public RecieveThread(final int port) {
        this.port = port;
        this.setPriority(Thread.MIN_PRIORITY);
    }

    @Override
    public void run() {
        DatagramChannel channel = null;
        try {
            channel = DatagramChannel.open();
            channel.socket().bind(new InetSocketAddress(this.port));
            channel.configureBlocking(false);
        } catch (final IOException e1) {
            e1.printStackTrace();
        }
        if (channel == null) {
            return;
        }
        final ByteBuffer readBuf = ByteBuffer.allocate(4096);
        SocketAddress cl;
        String s;
        ChatMessage msg;
        while (this.weiter) {
            try {
                cl = channel.receive(readBuf);
                if (cl != null) {
                    readBuf.flip();
                    if (readBuf.array().length > 0) {
                        s = new String(readBuf.array());
                        if (s.contains("sender")) {
                            msg = ChatMessage.fromString(s);
                            if (msg != null) {
                                if (msg instanceof FileInfoMessage) {
                                    this.recievedFileInfoMessage((FileInfoMessage) msg, cl);
                                } else if (msg instanceof FileInfoReturnMessage) {
                                    Connection.getConnection(false).getChatSession(((InetSocketAddress) cl).getAddress(), true).sendFileData(((FileInfoReturnMessage) msg).isDownload());
                                } else if (s.contains(ChatMessage.class.getName())) {
                                    Vector<ChatSession> ses;
                                    synchronized (Connection.getConnection(false).openSessions) {
                                        ses = Connection.getConnection(false).openSessions;
                                    }
                                    if (msg.getSender().equals(RoomSendThread.senderRoom)) {
                                        Connection.getConnection(false).getChatRoomSession(false).addMsg(msg);
                                    } else {
                                        boolean found = false;
                                        for (final ChatSession cs : ses) {
                                            if (((InetSocketAddress) cl).getAddress().equals(cs.getAddress())) {
                                                cs.addMsg(msg);
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (!found) {
                                            final ChatSession s1 = Connection.getConnection(false).getChatSession(((InetSocketAddress) cl).getAddress(), true);
                                            s1.addMsg(msg);
                                        }
                                    }
                                } else {
                                    System.out.println(msg);
                                }
                                msg = null;
                            }
                        }
                        s = null;
                    }
                }
                cl = null;
                readBuf.clear();
                Thread.sleep(100);
            } catch (final IOException e) {
                e.printStackTrace();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            channel.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * 
	 */
    public void kill() {
        this.weiter = false;
        this.frt.weiter = false;
    }

    void recievedFileInfoMessage(final FileInfoMessage msg, final SocketAddress cl) throws IOException {
        if (this.frt != null) {
            JOptionPane.showMessageDialog(null, "There is already an incoming file-transfer.", "Warning", JOptionPane.OK_CANCEL_OPTION);
            return;
        }
        Connection.getConnection(false).getChatSession(((InetSocketAddress) cl).getAddress(), true).receiveFile(this, msg, (InetSocketAddress) cl);
    }

    void declineReceivedFile(final ChatMessage msg, final SocketAddress cl) {
        if (msg.getSender().equals(RoomSendThread.senderRoom)) {
            Connection.getConnection(false).getChatRoomSession(false).send(ChatMessage.returnReceiveThreadInfo(null, null, false), false);
        } else {
            Connection.getConnection(false).getChatSession(((InetSocketAddress) cl).getAddress(), true).send(ChatMessage.returnReceiveThreadInfo(null, null, false), false);
        }
        Connection.getConnection(false).getChatSession(((InetSocketAddress) cl).getAddress(), true).receivedFile(false);
    }

    void startFileReceiveThread(final FileInfoMessage msg, final JFileChooser fc, final InetSocketAddress cl) {
        System.out.println("Starting FileReceiveThread");
        this.frt = new FileReceiveThread(msg.getFileLenght(), fc.getSelectedFile(), Connection.getConnection(false).getChatSession(cl.getAddress(), true));
        this.frt.start();
        this.frt.setPriority(Thread.MIN_PRIORITY);
        if (msg.getSender().equals(RoomSendThread.senderRoom)) {
            Connection.getConnection(false).getChatRoomSession(false).send(ChatMessage.returnReceiveThreadInfo(null, null, true), false);
        } else {
            Connection.getConnection(false).getChatSession(cl.getAddress(), true).send(ChatMessage.returnReceiveThreadInfo(null, null, true), false);
        }
    }

    class FileReceiveThread extends Thread {

        private final long size;

        private final File dest;

        private static final int buff = 4096;

        boolean weiter = true;

        private final ProgressDialog pd;

        protected ChatSession curSess;

        FileReceiveThread(final long filesize, final File output, final ChatSession curSes) {
            this.size = filesize;
            this.dest = output;
            this.pd = new ProgressDialog(filesize);
            this.curSess = curSes;
        }

        @Override
        public void run() {
            if (this.dest == null) {
                return;
            }
            try {
                final long start = System.currentTimeMillis();
                if (!this.dest.exists()) {
                    this.dest.createNewFile();
                }
                System.out.println("Start recieving: " + start);
                final FileOutputStream fos = new FileOutputStream(this.dest);
                final FileChannel fc = fos.getChannel();
                final ServerSocketChannel ssc = ServerSocketChannel.open();
                ssc.socket().bind(new InetSocketAddress(Main.fileReceivePort));
                final SocketChannel sc = ssc.accept();
                sc.configureBlocking(true);
                final ByteBuffer bb = ByteBuffer.allocate(FileReceiveThread.buff);
                final long middle = System.currentTimeMillis();
                long sizeremaining = this.size;
                int n = -1;
                while (sizeremaining > 0 && this.weiter) {
                    if ((n = sc.read(bb)) != -1) {
                        bb.flip();
                        fc.write(bb);
                        bb.compact();
                        sizeremaining -= n;
                        this.pd.setProgress(this.size - sizeremaining);
                    }
                }
                bb.clear();
                fc.close();
                fos.flush();
                fos.close();
                sc.close();
                ssc.close();
                final long end = System.currentTimeMillis();
                System.out.println("Preparation: " + (middle - start));
                System.out.println("Reading/Sending: " + (end - middle));
                System.out.println("Sum: " + (end - start));
                System.gc();
                this.curSess.receivedFile(true);
            } catch (final IOException e) {
                e.printStackTrace();
                this.pd.setVisible(false);
                JOptionPane.showMessageDialog(this.pd, "An error occured while receiving a file", "Error", JOptionPane.ERROR_MESSAGE);
                this.curSess.receivedFile(false);
            }
            this.pd.setVisible(false);
            RecieveThread.this.frt = null;
        }

        private class ProgressDialog extends JDialog {

            private static final long serialVersionUID = -7480149650155934354L;

            private final JProgressBar p;

            ProgressDialog(final long size) {
                this.setTitle("Downloading...");
                this.setSize(150, 70);
                this.p = new JProgressBar(0, (int) size);
                this.p.setIndeterminate(false);
                this.add(this.p, BorderLayout.NORTH);
                final JButton b = new JButton("Cancel");
                b.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        FileReceiveThread.this.weiter = false;
                        ProgressDialog.this.setVisible(false);
                    }
                });
                this.add(b, BorderLayout.SOUTH);
            }

            void setProgress(final long progress) {
                this.p.setValue((int) progress);
                if (Settings.showDownloadString) {
                    if (Settings.showDownloadPercentage) {
                        this.p.setString(this.toPercent(this.p.getMaximum(), progress));
                    } else {
                        this.p.setString(this.toPart(this.p.getMaximum(), progress));
                    }
                }
            }

            private String toPercent(final long base, final long part) {
                String s = null;
                final double teiler = part / (double) base;
                final long teil = Math.round(teiler);
                s = "" + teil;
                System.out.println(s);
                return s;
            }

            private String toPart(final long base, final long part) {
                return this.calcUnit(part) + "/" + this.calcUnit(base);
            }

            private String calcUnit(final long num) {
                String unit = "B";
                double sum = num;
                if (sum >= 1024) {
                    sum -= 1024;
                    unit = "KB";
                }
                if (sum >= 1024) {
                    sum -= 1024;
                    unit = "MB";
                }
                if (sum >= 1024) {
                    sum -= 1024;
                    unit = "GB";
                }
                if (sum >= 1024) {
                    sum -= 1024;
                    unit = "TB";
                }
                final long sum1 = Math.round(sum * 100);
                final double value = sum1 / 1000.0;
                return value + unit;
            }
        }

        @Deprecated
        class ProgressEvent extends AWTEvent {

            private static final long serialVersionUID = -5535910213508068717L;

            private final long progressedSize;

            public ProgressEvent(final Event event, final long progressedSize) {
                super(event);
                this.progressedSize = progressedSize;
            }

            long getProgressedSize() {
                return this.progressedSize;
            }
        }
    }
}
