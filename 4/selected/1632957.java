package net.hanjava.test;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;

public class LockSample extends JPanel {

    private static final String PATH = "C:/Temp/memo.xls";

    private AbstractAction lockUnlockAction;

    private FileLock lock;

    public LockSample() {
        setLayout(new BorderLayout());
        lockUnlockAction = new AbstractAction("Lock") {

            public void actionPerformed(ActionEvent e) {
                if (lock != null) {
                    try {
                        lock.release();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    RandomAccessFile raf = null;
                    try {
                        raf = new RandomAccessFile(PATH, "rw");
                        lock = raf.getChannel().lock();
                        System.err.println("[LockSample] " + lock);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                boolean canLock = updateLockState();
            }
        };
        updateLockState();
        JButton btLock = new JButton(lockUnlockAction);
        add(BorderLayout.CENTER, btLock);
    }

    private boolean updateLockState() {
        boolean canLock = canLock(PATH);
        String text = canLock ? "Lock" : "Unlock";
        lockUnlockAction.putValue(Action.NAME, text);
        return canLock;
    }

    static boolean canLock(String path) {
        FileChannel channel;
        RandomAccessFile raf = null;
        boolean result = true;
        try {
            raf = new RandomAccessFile(PATH, "rw");
            channel = raf.getChannel();
            FileLock lock = channel.tryLock();
            if (lock == null) {
                result = false;
            } else {
                lock.release();
            }
        } catch (IOException e) {
            result = false;
            e.printStackTrace();
        } catch (OverlappingFileLockException ofle) {
            result = false;
            ofle.printStackTrace();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        Util.test(new LockSample());
    }
}
