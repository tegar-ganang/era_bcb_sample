package de.sistemich.mafrasi.stopmotion.gui.modules.filelist;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import de.sistemich.mafrasi.stopmotion.gui.ConstantKeys;
import de.sistemich.mafrasi.stopmotion.gui.Frame;
import de.sistemich.mafrasi.stopmotion.gui.FrameManager;
import de.sistemich.mafrasi.stopmotion.gui.FrameUpdateEvent;
import de.sistemich.mafrasi.stopmotion.gui.FrameUpdateListener;
import de.sistemich.mafrasi.stopmotion.gui.Messages;
import de.sistemich.mafrasi.stopmotion.gui.SMCUtilities;
import de.sistemich.mafrasi.stopmotion.gui.Settings;

/**
 * The class  {@code FileListTable} organizes a table with the actual frames.
 * You can get the table by using {@code getTable()}
 * @author Max Sistemich
 *
 */
public class FileListTable implements FrameUpdateListener {

    public static final int COLUMN_NO_WIDTH = 50;

    public static final int COLUMN_FILENAMNE_WIDTH = 100;

    public static final int COLUMN_SIZE_WIDTH = 70;

    private FileListTableModel model_;

    private JTable table_;

    private Frame[] copiedFiles_;

    /**
	 * Construct the table and registers listeners
	 */
    public FileListTable() {
        model_ = new FileListTableModel();
        table_ = new JTable(model_);
        table_.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table_.getColumnModel().getColumn(0).setWidth(COLUMN_NO_WIDTH);
        table_.getColumnModel().getColumn(1).setMinWidth(COLUMN_FILENAMNE_WIDTH);
        table_.getColumnModel().getColumn(2).setWidth(COLUMN_SIZE_WIDTH);
        table_.getTableHeader().setReorderingAllowed(false);
        table_.getTableHeader().setResizingAllowed(true);
        table_.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table_.setPreferredScrollableViewportSize(table_.getPreferredSize());
        JPopupMenu popup = new JPopupMenu();
        final JMenuItem copy = new JMenuItem(Messages.getString("Copy"));
        final JMenuItem paste = new JMenuItem(Messages.getString("Paste"));
        final JMenuItem delete = new JMenuItem(Messages.getString("Delete"));
        final JMenuItem invert = new JMenuItem(Messages.getString("Invert"));
        copy.setAccelerator(KeyStroke.getKeyStroke(Character.valueOf('C'), InputEvent.CTRL_DOWN_MASK));
        paste.setAccelerator(KeyStroke.getKeyStroke(Character.valueOf('V'), InputEvent.CTRL_DOWN_MASK));
        delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        table_.addKeyListener(new KeyListener() {

            private char pressedChar_ = 0;

            @Override
            public void keyTyped(KeyEvent e) {
                if (e.isControlDown()) {
                    System.out.println(e.isControlDown() + " + " + pressedChar_);
                    System.out.println(pressedChar_);
                    switch(pressedChar_) {
                        case 'c':
                            System.out.println("c is pressed");
                            copy.doClick();
                            break;
                        case 'v':
                            paste.doClick();
                            break;
                    }
                } else {
                    if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                        delete.doClick();
                    }
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                char c = Character.toLowerCase(e.getKeyChar());
                if (c >= 'a' && c <= 'z') {
                    pressedChar_ = c;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                char c = Character.toLowerCase(e.getKeyChar());
                if (c == pressedChar_) {
                    pressedChar_ = 0;
                }
            }
        });
        copy.addActionListener(new CopyListener());
        paste.addActionListener(new PasteListener());
        delete.addActionListener(new DeleteListener());
        invert.addActionListener(new InvertListener());
        popup.add(copy);
        popup.add(paste);
        popup.addSeparator();
        popup.add(delete);
        popup.addSeparator();
        popup.add(invert);
        table_.setComponentPopupMenu(popup);
    }

    /**
	 * Returns the table.
	 * @return the frame table
	 */
    public JTable getTable() {
        return table_;
    }

    @Override
    public void frameInserted(FrameUpdateEvent e) {
    }

    @Override
    public void frameRemoved(FrameUpdateEvent e) {
    }

    @Override
    public void frameUpdateFinished(FrameUpdateEvent e) {
        int newSelectionInterval = FrameManager.getInstance().getIndexOf(e.getFrames()[Math.max(0, e.getFrames().length - 1)]);
        getTable().getSelectionModel().setSelectionInterval(newSelectionInterval, newSelectionInterval);
    }

    public class DeleteListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] selectedRows = getTable().getSelectedRows();
            Arrays.sort(selectedRows);
            FrameManager.getInstance().deleteFrames(selectedRows);
        }
    }

    public class CopyListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("copy");
            int[] selectedRows = getTable().getSelectedRows();
            Frame[] copiedFiles = new Frame[selectedRows.length];
            for (int i = 0; i < selectedRows.length; i++) {
                copiedFiles[i] = FrameManager.getInstance().getFrameAtIndex(selectedRows[i]);
            }
            copiedFiles_ = copiedFiles;
        }
    }

    public class PasteListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (copiedFiles_ != null) {
                File[] tmpFiles = new File[copiedFiles_.length];
                File tmpDir = new File(Settings.getPropertyString(ConstantKeys.project_dir), "tmp/");
                tmpDir.mkdirs();
                for (int i = copiedFiles_.length - 1; i >= 0; i--) {
                    Frame f = FrameManager.getInstance().getFrameAtIndex(i);
                    try {
                        File in = f.getFile();
                        File out = new File(tmpDir, f.getFile().getName());
                        FileChannel inChannel = new FileInputStream(in).getChannel();
                        FileChannel outChannel = new FileOutputStream(out).getChannel();
                        inChannel.transferTo(0, inChannel.size(), outChannel);
                        if (inChannel != null) inChannel.close();
                        if (outChannel != null) outChannel.close();
                        tmpFiles[i] = out;
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                try {
                    FrameManager.getInstance().insertFrames(getTable().getSelectedRow(), FrameManager.INSERT_TYPE.MOVE, tmpFiles);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    protected class InvertListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] selectedRows = getTable().getSelectedRows();
            Frame[] frames = new Frame[selectedRows.length];
            for (int i = 0; i < selectedRows.length; i++) {
                frames[i] = FrameManager.getInstance().getFrameAtIndex(selectedRows[i]);
            }
            File projectDir = new File(Settings.getPropertyString(ConstantKeys.project_dir));
            File tmpDir = new File(projectDir, "tmp");
            tmpDir.mkdirs();
            Frame[] tmpFrames = new Frame[frames.length];
            for (int i = 0; i < frames.length; i++) {
                File newFile = new File(tmpDir, frames[frames.length - 1 - i].getFile().getName());
                tmpFrames[i] = new Frame(newFile);
                frames[i].moveTo(tmpFrames[i]);
            }
            for (int i = 0; i < tmpFrames.length; i++) {
                tmpFrames[i].moveTo(frames[frames.length - 1 - i]);
            }
            SMCUtilities.deleteDirectory(tmpDir);
        }
    }
}
