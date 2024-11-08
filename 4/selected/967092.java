package joom.gui;

import joom.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * This internal frame lists all the threads currently in the system.
 * @author Christopher Brind
 */
public class ThreadsFrame extends BaseFrame implements Runnable {

    private static Logger l = Logger.getLogger();

    private Thread t;

    private boolean bRunning;

    private JList list;

    public ThreadsFrame() {
        super("Threads");
        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        list = new JList();
        c.add(new JScrollPane(list), BorderLayout.CENTER);
    }

    public void start() {
        t = new Thread(this);
        t.setName(Globals.JOOM + " " + getClass().getName());
        bRunning = true;
        t.start();
    }

    private ThreadGroup rootThreadGroup() {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        while (null != tg.getParent()) {
            tg = tg.getParent();
        }
        return tg;
    }

    private String pad(int i) {
        String s = "";
        for (int iLoop = 0; iLoop < i; iLoop++) {
            s = s + " ";
        }
        return s;
    }

    private void writeThreadInfo(int iIndent, Thread at[], ThreadGroup tg, DefaultListModel dlm) {
        String sPad = pad(iIndent);
        for (int iLoop = 0; (iLoop < at.length) && (null != at[iLoop]); iLoop++) {
            dlm.addElement(sPad + "thread: " + at[iLoop].getName());
        }
    }

    private void writeThreadGroupInfo(int iIndent, ThreadGroup tg, DefaultListModel dlm) {
        dlm.addElement(pad(iIndent) + "group: " + tg.getName());
        int iThreadCount = tg.activeCount();
        int iGroupCount = tg.activeGroupCount();
        Thread threads[] = new Thread[iThreadCount];
        tg.enumerate(threads, false);
        writeThreadInfo(iIndent + 3, threads, tg, dlm);
        ThreadGroup groups[] = new ThreadGroup[iGroupCount];
        tg.enumerate(groups, false);
        for (int iLoop = 0; (iLoop < groups.length) && (null != groups[iLoop]); iLoop++) {
            writeThreadGroupInfo(iIndent + 3, groups[iLoop], dlm);
        }
    }

    private ListModel threadsAsListModel() {
        DefaultListModel dlm = new DefaultListModel();
        ThreadGroup tg = rootThreadGroup();
        writeThreadGroupInfo(0, tg, dlm);
        return dlm;
    }

    public void run() {
        while (bRunning) {
            list.setModel(threadsAsListModel());
            try {
                Thread.sleep(5000);
            } catch (Exception ex) {
            }
        }
    }

    public void stop() {
        bRunning = false;
        t.interrupt();
    }

    public void internalFrameOpened(InternalFrameEvent e) {
        start();
    }

    public void internalFrameClosing(InternalFrameEvent e) {
        stop();
    }

    public void internalFrameDeiconified(InternalFrameEvent e) {
        start();
    }

    public void internalFrameIconified(InternalFrameEvent e) {
        stop();
    }
}
