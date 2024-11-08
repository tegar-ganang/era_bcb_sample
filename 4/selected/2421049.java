package org.vmasterdiff.gui.swing.dirdiff2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.vmasterdiff.lib.file.PercentDoneEvent;
import org.vmasterdiff.lib.file.PercentDoneListener;
import biz.xsoftware.impl.thread.ExceptionHandler;
import biz.xsoftware.impl.thread.Runner;

public class DefaultDirDiffModel implements DirDiffModel, Runnable {

    private static final Logger log = Logger.getLogger(DefaultDirDiffModel.class.getName());

    /**
	 *  The BASE_DIR is // because this way we know when the last node on the
	 *  JTree was added.  We know when to call root.notify() when we see this
	 *  directory because it is the last node
	 */
    private static final String BASE_DIR = "Base Directory";

    private EventListenerList listenerList = new EventListenerList();

    private DefaultTreeModel oldTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());

    private TreeExpansionListener listener = null;

    private DefaultBoundedRangeModel progressModel = new DefaultBoundedRangeModel();

    private PlainDocument textModel = new PlainDocument();

    private boolean doTimePreDiff = false;

    private boolean doSizePreDiff = false;

    public void setDoTimePreDiff(boolean b) {
        doTimePreDiff = b;
    }

    public boolean getDoTimePreDiff() {
        return doTimePreDiff;
    }

    public void setDoSizePreDiff(boolean b) {
        doSizePreDiff = b;
    }

    public boolean getDoSizePreDiff() {
        return doSizePreDiff;
    }

    public TreeModel getTreeModel() {
        return oldTreeModel;
    }

    public void setTreeExpansionListener(TreeExpansionListener l) {
        this.listener = l;
    }

    public BoundedRangeModel getProgressBarModel() {
        return progressModel;
    }

    public Document getTextFieldModel() {
        return textModel;
    }

    public Vector getTreePath() {
        return treePaths;
    }

    public final void diffTwoDirectories(File oldDir, File newDir) {
        root = new FileNode(BASE_DIR, true, oldDir.lastModified(), 0, newDir.lastModified(), 0, true);
        oldTreeModel.setRoot(root);
        oldBase = new File(oldDir.getAbsolutePath());
        newBase = new File(newDir.getAbsolutePath());
        totalFiles = 0;
        numProcessedFiles = 0;
        Thread t = new Thread(this);
        t.start();
    }

    public void addPercentDoneListener(PercentDoneListener l) {
        listenerList.add(PercentDoneListener.class, l);
    }

    public void removePercentDoneListener(PercentDoneListener l) {
        listenerList.remove(PercentDoneListener.class, l);
    }

    protected void firePercentDoneEvent(int percentDone) {
        PercentDoneEvent e = new PercentDoneEvent(this, percentDone);
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == PercentDoneListener.class) {
                ((PercentDoneListener) listeners[i + 1]).percentDoneChanged(e);
            }
        }
    }

    private FileNode root;

    protected File oldBase = null;

    protected File newBase = null;

    protected int totalFiles = 0;

    protected int numProcessedFiles = 0;

    protected Vector treePaths = new Vector();

    protected boolean notified = false;

    protected byte[] empty;

    public void run() {
        notified = false;
        log.fine("new thread, oldDir=" + oldBase + "  newDir=" + newBase);
        Date before = new Date();
        try {
            initFileTree(root, oldBase, newBase);
            synchronized (root) {
                if (!notified) root.wait();
            }
            Runner.runOnEventThreadLater(new Runnable() {

                public void run() {
                    progressModel.setMinimum(0);
                    progressModel.setValue(0);
                    progressModel.setMaximum(totalFiles);
                }
            });
            if (doTimePreDiff) diffByTime();
            if (doSizePreDiff) diffBySize();
            diffByContent();
            Runner.runOnEventThreadLater(new Runnable() {

                public void run() {
                    DefaultDirDiffModel.this.firePercentDoneEvent(100);
                }
            });
        } catch (Throwable e) {
            ExceptionHandler.handle(e);
        }
        Date after = new Date();
        double time = ((double) after.getTime() - before.getTime()) / 1000;
        System.out.println("time=" + time);
    }

    private int timesCalled = 0;

    protected void initFileTree(FileNode node, File oldD, File newD) {
        timesCalled++;
        Hashtable oldFiles = new Hashtable();
        log.finer("process dir=" + node.getFilePath());
        final Vector children = new Vector();
        final FileNode parent = node;
        if (oldD != null) {
            File[] files = oldD.listFiles();
            for (int i = 0; i < files.length; i++) {
                String relative = files[i].getAbsolutePath().substring(oldBase.getAbsolutePath().length() + 1);
                oldFiles.put(relative, files[i]);
            }
        }
        if (newD != null) {
            File[] files = newD.listFiles();
            for (int i = 0; i < files.length; i++) {
                File newF = files[i];
                String relative = newF.getAbsolutePath().substring(newBase.getAbsolutePath().length() + 1);
                File oldF = (File) oldFiles.get(relative);
                log.finest("     child=" + relative);
                boolean allowsChildren = false;
                long oldLastMod = -1;
                long newLastMod = newF.lastModified();
                if (newF.isDirectory() || oldF != null && oldF.isDirectory()) allowsChildren = true;
                if (oldF != null) oldLastMod = oldF.lastModified();
                FileNode child = new FileNode(relative, allowsChildren, oldLastMod, getFileLength(oldF), newLastMod, getFileLength(newF));
                oldFiles.remove(relative);
                children.add(child);
                if (allowsChildren) initFileTree(child, oldF, newF);
                totalFiles++;
            }
        }
        Enumeration iter = oldFiles.keys();
        while (iter.hasMoreElements()) {
            String key = (String) iter.nextElement();
            File f = (File) oldFiles.get(key);
            log.finest("    child(old only)=" + key);
            FileNode child = new FileNode(key, f.isDirectory(), f.lastModified(), getFileLength(f), -1, -1);
            children.add(child);
            if (f.isDirectory()) initFileTree(child, f, null);
            totalFiles++;
        }
        Runner.runOnEventThreadLater(new Runnable() {

            public void run() {
                int[] indices = new int[children.size()];
                for (int i = 0; i < children.size(); i++) {
                    FileNode n = (FileNode) children.get(i);
                    if (n.getAllowsChildren()) parent.insert(n, 0); else parent.add(n);
                    indices[i] = i;
                }
                oldTreeModel.nodesWereInserted(parent, indices);
                if (parent.isBaseDirectory()) synchronized (root) {
                    root.notify();
                    notified = true;
                }
            }
        });
    }

    private long getFileLength(File f) {
        if (f == null) return -1;
        if (f.isDirectory()) return 0; else return f.length();
    }

    /**
	 * This function analyzes files and removes files that have the same timestamp
	 * from before from the hashtable so they don't need to be analyzed by diffBySize
	 * or diffByContent.  This function adds no files to the ListModel.
	 */
    private void diffByTime() {
    }

    /**
	 * This function analyzes file sizes and adds files that have changed in size
	 * to the ListModel and deletes them from the hashtable so diffByContent does
	 * not need to reanalyze those files.  All other files will be left to diffByContent
	 * to analyze.
	 */
    private void diffBySize() {
    }

    private void diffByContent() {
        ByteBuffer buffer1 = ByteBuffer.allocateDirect(10000);
        ByteBuffer buffer2 = ByteBuffer.allocateDirect(10000);
        empty = new byte[10000];
        for (int i = 0; i < 10000; i++) empty[i] = 0;
        FileNode[] children = root.getUnprocessedChildren();
        for (int i = 0; i < children.length; i++) diffByContentRecursive(children[i], buffer1, buffer2);
    }

    private void diffByContentRecursive(FileNode node, ByteBuffer b1, ByteBuffer b2) {
        FileNode[] children = node.getUnprocessedChildren();
        for (int i = 0; i < children.length; i++) diffByContentRecursive(children[i], b1, b2);
        diffFile(node, b1, b2);
    }

    private void diffFile(FileNode n, ByteBuffer buffer1, ByteBuffer buffer2) {
        final FileNode node = n;
        final String path = node.getFilePath();
        int status = JDirDiff.FILE_NOT_CHANGED;
        Runner.runOnEventThreadLater(new Runnable() {

            public void run() {
                try {
                    textModel.remove(0, textModel.getLength());
                    textModel.insertString(0, path, null);
                } catch (BadLocationException e) {
                    ExceptionHandler.handle(e);
                }
            }
        });
        File oldFile = new File(oldBase.getAbsolutePath(), path);
        File newFile = new File(newBase.getAbsolutePath(), path);
        if (!oldFile.exists() && !newFile.exists()) status = FileNode.ERROR_DIFFING_FILES; else if (oldFile.exists() && !newFile.exists()) status = FileNode.DELETED; else if (!oldFile.exists() && newFile.exists()) status = FileNode.ADDED; else if (oldFile.isDirectory() || newFile.isDirectory()) {
            if (oldFile.isDirectory() && !newFile.isDirectory()) status = FileNode.DIR_CHANGED_TO_FILE; else if (!oldFile.isDirectory() && newFile.isDirectory()) status = FileNode.FILE_CHANGED_TO_DIR;
        } else {
            try {
                FileInputStream in1 = new FileInputStream(oldFile);
                FileInputStream in2 = new FileInputStream(newFile);
                FileChannel channel1 = in1.getChannel();
                FileChannel channel2 = in2.getChannel();
                int bytesRead1 = 0;
                int bytesRead2 = 0;
                boolean fileIsSame = true;
                while (bytesRead1 != -1 && bytesRead2 != -1) {
                    bytesRead1 = channel1.read(buffer1);
                    bytesRead2 = channel2.read(buffer2);
                    if (!buffer1.equals(buffer2)) {
                        status = FileNode.CHANGED;
                        buffer1.rewind();
                        buffer2.rewind();
                        buffer1.put(empty);
                        buffer2.put(empty);
                        buffer1.rewind();
                        buffer2.rewind();
                        break;
                    } else {
                        buffer1.rewind();
                        buffer2.rewind();
                    }
                }
                channel1.close();
                channel2.close();
                in1.close();
                in2.close();
            } catch (IOException e) {
                status = FileNode.ERROR_DIFFING_FILES;
            }
        }
        log.finest("diffFile=" + path + "  status=" + node.getStatus());
        node.removeFromUnprocessedTree();
        final int statusResult = status;
        if (statusResult != JDirDiff.FILE_NOT_CHANGED || numProcessedFiles % 10 == 0) Runner.runOnEventThreadLater(new Runnable() {

            public void run() {
                if (statusResult != JDirDiff.FILE_NOT_CHANGED) {
                    node.setStatus(statusResult);
                    FileNode parent = (FileNode) node.getParent();
                    TreeExpansionEvent evt = new TreeExpansionEvent(this, new TreePath(parent.getPath()));
                    listener.treeExpanded(evt);
                }
                progressModel.setValue(numProcessedFiles);
            }
        });
        numProcessedFiles++;
    }
}
