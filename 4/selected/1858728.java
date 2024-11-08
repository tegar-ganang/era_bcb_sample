package de.kout.wlFxp;

import de.kout.wlFxp.interfaces.wlFrame;
import de.kout.wlFxp.interfaces.wlPanel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Vector;

/**
 * this class can copy files and dirs locally as of yet no status while
 * copying
 *
 * @author Alexander Kout
 *
 * 3. August 2003
 */
public class CopyFile extends Thread {

    Vector<Object> cmd;

    wlFrame frame;

    /**
	 * DOCUMENT ME!
	 */
    public volatile long rest;

    /**
         * Constructor for the CopyFile object
         * 
         * @param frame
         *            the MainFrame
         */
    public CopyFile(wlFrame frame) {
        super();
        this.frame = frame;
        cmd = new Vector<Object>();
        start();
    }

    /**
	 * Main processing method for the CopyFile object
	 */
    public void run() {
        String command;
        while (true) {
            synchronized (cmd) {
                if (cmd.isEmpty()) {
                    try {
                        cmd.wait();
                    } catch (InterruptedException e) {
                    }
                }
                command = (String) cmd.firstElement();
                cmd.removeElementAt(0);
            }
            if (command.equals("cftd")) {
                wlPanel panel = (wlPanel) cmd.firstElement();
                cmd.removeElementAt(0);
                try {
                    Transfer transfer = (Transfer) cmd.firstElement();
                    cmd.removeElementAt(0);
                    MyFile file = transfer.getSource();
                    MyFile to = transfer.getDest();
                    if (file.isDirectory()) {
                        new File(to.getAbsolutePath()).mkdir();
                        frame.getQueueList().removeFirst();
                        frame.getQueueList().updateView();
                        MyFile[] files = file.list();
                        for (int i = files.length - 1; i >= 0; i--) {
                            MyFile tmp = new MyFile(files[i].getName());
                            tmp.setFtpMode(false);
                            tmp.setAbsolutePath(to.getAbsolutePath() + File.separator + files[i].getName());
                            frame.getQueueList().addAtBegin(new Transfer(files[i], tmp, transfer.modeFrom, transfer.modeTo, transfer.from_to, null, null));
                        }
                    } else {
                        copyFileToDir(file, to, panel);
                        frame.getQueueList().removeFirst();
                        frame.getQueueList().updateView();
                    }
                } catch (IOException e) {
                    System.out.println(e.toString());
                }
                while (!wlFxp.getTm().waiting) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                    }
                }
                synchronized (wlFxp.getTm().done) {
                    wlFxp.getTm().done.notify();
                }
            }
            try {
                sleep(50);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
	 * this method notifies the thread
	 *
	 * @param panel the parent panel
	 * @param transfer the transfer object
	 */
    public void copy(wlPanel panel, Transfer transfer) {
        synchronized (cmd) {
            cmd.addElement("cftd");
            cmd.addElement(panel);
            cmd.addElement(transfer);
            cmd.notify();
        }
    }

    /**
	 * this method copies a single file to a dir
	 *
	 * @param file the file to copy
	 * @param to the destination file
	 * @param panel the destination Panel
	 *
	 * @exception IOException IOException
	 */
    private void copyFileToDir(MyFile file, MyFile to, wlPanel panel) throws IOException {
        Utilities.print("started copying " + file.getAbsolutePath() + "\n");
        FileOutputStream fos = new FileOutputStream(new File(to.getAbsolutePath()));
        FileChannel foc = fos.getChannel();
        FileInputStream fis = new FileInputStream(new File(file.getAbsolutePath()));
        FileChannel fic = fis.getChannel();
        Date d1 = new Date();
        long amount = foc.transferFrom(fic, rest, fic.size() - rest);
        fic.close();
        foc.force(false);
        foc.close();
        Date d2 = new Date();
        long time = d2.getTime() - d1.getTime();
        double secs = time / 1000.0;
        double rate = amount / secs;
        frame.getStatusArea().append(secs + "s " + "amount: " + Utilities.humanReadable(amount) + " rate: " + Utilities.humanReadable(rate) + "/s\n", "black");
        panel.updateView();
    }
}
