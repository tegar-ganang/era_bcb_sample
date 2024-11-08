package net.sf.cclearly.ui.widgets;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import net.sf.cclearly.entities.Attachment;
import net.sf.cclearly.entities.Task;
import net.sf.cclearly.logic.TaskDAO;
import net.sf.cclearly.logic.UserDAO;
import net.sf.cclearly.resources.Icons;
import net.sf.cclearly.ui.UIControl;
import sun.awt.shell.ShellFolder;
import za.dats.util.events.EventManager;
import za.dats.util.injection.Dependant;
import za.dats.util.injection.Inject;
import za.dats.util.injection.Injector;
import za.dats.util.observer.Listener;
import za.dats.util.swing.table.JSimpleTable;
import za.dats.util.swing.table.MappedColumnProvider;
import za.dats.util.swing.table.MappedTableModel;
import com.jgoodies.forms.factories.ButtonBarFactory;

@Dependant
public class AttachPanel extends JPanel {

    @Inject
    UIControl trayIcon;

    @Inject
    private UserDAO userDao;

    private JSimpleTable dataList;

    private MappedTableModel<AttachFile> model;

    private Task attachTask;

    private File attachFolder;

    private List<AttachFile> deletedFiles = new LinkedList<AttachFile>();

    private TableRowSorter<MappedTableModel<AttachFile>> sorter;

    Listener changeEvent = EventManager.newEvent(Listener.class);

    public void addChangeEventListener(Listener listener) {
        EventManager.addListener(changeEvent, listener);
    }

    public void removeChangeEventListener(Listener listener) {
        EventManager.removeListener(changeEvent, listener);
    }

    public AttachPanel() {
        Injector.inject(this);
        setLayout(new BorderLayout());
        model = new MappedTableModel<AttachFile>(AttachFile.class);
        model.bindCustomColumn("Filename", new MappedColumnProvider<AttachFile>() {

            public Class<?> getColumnClass(String title) {
                return AttachFile.class;
            }

            public Object getValue(String title, AttachFile item) {
                return item;
            }
        });
        model.bindColumn("Description", "description", false);
        model.bindCustomColumn("Added By", new MappedColumnProvider<AttachFile>() {

            public Class<?> getColumnClass(String title) {
                return String.class;
            }

            public Object getValue(String title, AttachFile item) {
                if (item.getMetadata().getAddedBy() == null) {
                    return userDao.getCurrentUser().toString();
                }
                return item.getMetadata().getAddedBy().toString();
            }
        });
        model.bindCustomColumn("Date Added", new MappedColumnProvider<AttachFile>() {

            public Class<?> getColumnClass(String title) {
                return Date.class;
            }

            public Object getValue(String title, AttachFile item) {
                return item.getMetadata().getDateAdded();
            }
        });
        model.bindCustomColumn("Version", new MappedColumnProvider<AttachFile>() {

            public Class<?> getColumnClass(String title) {
                return String.class;
            }

            public Object getValue(String title, AttachFile item) {
                return Long.toString(item.getMetadata().getVersion());
            }
        });
        model.bindCustomColumn("File Size", new MappedColumnProvider<AttachFile>() {

            public Class<?> getColumnClass(String title) {
                return Long.class;
            }

            public Object getValue(String title, AttachFile item) {
                return item.getSize();
            }
        });
        model.addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                changeEvent.update();
            }
        });
        dataList = new JSimpleTable(model);
        dataList.setFillsViewportHeight(true);
        sorter = new TableRowSorter<MappedTableModel<AttachFile>>(model);
        dataList.setRowSorter(sorter);
        dataList.getColumn("Filename").setCellRenderer(new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel result = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof AttachFile) {
                    AttachFile file = (AttachFile) value;
                    result.setIcon(new ImageIcon(file.getIcon()));
                    result.setText(file.getName());
                }
                return result;
            }
        });
        dataList.getColumn("File Size").setCellRenderer(new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel result = (JLabel) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
                if (value instanceof Long) {
                    Long size = (Long) value;
                    if (size == null) {
                        return result;
                    }
                    if (size < 1024) {
                        result.setText(String.format("%,.0fb", (double) size));
                    } else if (size < 1024 * 1024) {
                        result.setText(String.format("%,.2fkb", (double) size / 1024d));
                    } else {
                        result.setText(String.format("%,.2fmb", (double) size / 1024d));
                    }
                }
                return result;
            }
        });
        add(new JScrollPane(dataList));
        dataList.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if ((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() == 2)) {
                    Object sel = model.getItemAtRow(sorter.convertRowIndexToModel(dataList.getSelectedRow()));
                    if (sel == null) {
                        openAttachBrowsePanel();
                    }
                    if (sel instanceof AttachFile) {
                        AttachFile file = (AttachFile) sel;
                        try {
                            Desktop.getDesktop().open(file.getCurrentPath());
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                } else {
                    if (e.getButton() == MouseInfo.getNumberOfButtons()) {
                        JPopupMenu popup = createContextMenu();
                        popup.show(dataList, e.getX(), e.getY());
                    }
                }
            }
        });
        dataList.addKeyListener(new KeyAdapter() {

            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteSelectedFiles();
                }
            }
        });
        dataList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        dataList.setDropMode(DropMode.INSERT);
        dataList.setTransferHandler(new AttachFileTransferHandler());
        dataList.setDragEnabled(true);
        setPreferredSize(new Dimension(50, 50));
        JButton screenshotButton = new JButton("Screenshot");
        screenshotButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                attachScreenShot();
            }
        });
        JButton browseButton = new JButton("Browse ...");
        browseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                openAttachBrowsePanel();
            }
        });
        JPanel buttonPanel = ButtonBarFactory.buildRightAlignedBar(screenshotButton, browseButton);
        setPreferredSize(new Dimension(screenshotButton.getPreferredSize().width + browseButton.getPreferredSize().width + 15, (int) browseButton.getPreferredSize().getHeight() + 6));
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public TransferHandler getAttachTransferHandler() {
        return new AttachFileTransferHandler();
    }

    protected synchronized void attachScreenShot() {
        String[] formatNames = ImageIO.getReaderFormatNames();
        try {
            trayIcon.minimizeAll();
            GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            int scrnNum = 1;
            for (GraphicsDevice screen : screens) {
                Robot robot = new Robot(screen);
                BufferedImage shot = robot.createScreenCapture(new Rectangle(0, 0, screen.getDisplayMode().getWidth(), screen.getDisplayMode().getHeight()));
                File shotFile = File.createTempFile("shot_" + scrnNum + "_", ".jpg");
                shotFile.deleteOnExit();
                ImageIO.write(shot, "jpg", shotFile);
                addFile(new AttachFile(shotFile, null, true));
                scrnNum++;
            }
            trayIcon.showAll();
            revalidate();
            repaint();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AWTException e1) {
            e1.printStackTrace();
        }
    }

    protected synchronized void openAttachBrowsePanel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Attach file..");
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.setMultiSelectionEnabled(true);
        if (fileChooser.showDialog(this, "Attach") == JFileChooser.APPROVE_OPTION) {
            for (File file : fileChooser.getSelectedFiles()) {
                addFile(new AttachFile(file, null, true));
            }
        }
    }

    public synchronized void setAttachmentTask(Task task) {
        this.attachTask = task;
        List<AttachFile> deleteRows = new LinkedList<AttachFile>();
        for (AttachFile file : model.getItems()) {
            if (!file.isNewFile()) {
                deleteRows.add(file);
            }
        }
        for (AttachFile file : deleteRows) {
            model.deleteRow(file);
        }
        if (task != null) {
            attachFolder = task.getAttachFolder();
            if ((attachFolder != null) && (attachFolder.exists())) {
                for (File file : attachFolder.listFiles()) {
                    if (file.isFile()) {
                        addFile(new AttachFile(file, task.getAttachmentMetadata(file), false));
                    }
                }
            }
        } else {
            attachFolder = null;
        }
    }

    public synchronized void saveAttachments(String destName) {
        if (attachFolder == null) {
            return;
        }
        for (AttachFile file : deletedFiles) {
            attachTask.detachFile(file.getCurrentPath());
        }
        for (AttachFile file : model.getItems()) {
            attachTask.addAttachMetadata(file.getMetadata());
            if (!file.isNewFile()) {
                continue;
            }
            file.copyTo(attachFolder);
        }
    }

    public boolean hasAttachFolder() {
        return attachFolder != null;
    }

    protected void openSelectedFiles() {
        for (Integer idx : dataList.getSelectedRows()) {
            Object entry = model.getItemAtRow(sorter.convertRowIndexToModel(idx));
            if (entry instanceof AttachFile) {
                AttachFile file = (AttachFile) entry;
                try {
                    Desktop.getDesktop().open(file.getCurrentPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void editSelectedFiles() {
        for (Integer idx : dataList.getSelectedRows()) {
            Object entry = model.getItemAtRow(sorter.convertRowIndexToModel(idx));
            if (entry instanceof AttachFile) {
                AttachFile file = (AttachFile) entry;
                try {
                    Desktop.getDesktop().edit(file.getCurrentPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void printSelectedFiles() {
        for (Integer idx : dataList.getSelectedRows()) {
            Object entry = model.getItemAtRow(sorter.convertRowIndexToModel(idx));
            if (entry instanceof AttachFile) {
                AttachFile file = (AttachFile) entry;
                try {
                    Desktop.getDesktop().print(file.getCurrentPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void deleteSelectedFiles() {
        for (Integer idx : dataList.getSelectedRows()) {
            Object obj = model.getItemAtRow(sorter.convertRowIndexToModel(idx));
            if (obj instanceof AttachFile) {
                AttachFile file = (AttachFile) obj;
                if (!file.isNewFile()) {
                    deletedFiles.add(file);
                }
                model.deleteRow(file);
            }
        }
    }

    private JPopupMenu createContextMenu() {
        JPopupMenu popup = new JPopupMenu();
        if (dataList.getSelectedRowCount() > 0) {
            JMenuItem saveItem = new JMenuItem("Save As..");
            saveItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    saveSelectedFiles();
                }
            });
            popup.add(saveItem);
            JMenuItem openItem = new JMenuItem("Open");
            openItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    openSelectedFiles();
                }
            });
            popup.add(openItem);
            JMenuItem editItem = new JMenuItem("Edit");
            editItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    editSelectedFiles();
                }
            });
            popup.add(editItem);
            JMenuItem printItem = new JMenuItem("Print");
            printItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    printSelectedFiles();
                }
            });
            popup.add(printItem);
            JMenuItem deleteItem = new JMenuItem("Delete");
            deleteItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    deleteSelectedFiles();
                }
            });
            popup.add(deleteItem);
            popup.addSeparator();
        }
        JMenuItem attachItem = new JMenuItem("Attach New");
        attachItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                openAttachBrowsePanel();
            }
        });
        popup.add(attachItem);
        return popup;
    }

    protected void saveSelectedFiles() {
        if (dataList.getSelectedRowCount() == 0) {
            return;
        }
        if (dataList.getSelectedRowCount() == 1) {
            Object obj = model.getItemAtRow(sorter.convertRowIndexToModel(dataList.getSelectedRow()));
            AttachFile entry = (AttachFile) obj;
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File(fc.getCurrentDirectory(), entry.getCurrentPath().getName()));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File current = entry.getCurrentPath();
                File dest = fc.getSelectedFile();
                try {
                    FileInputStream in = new FileInputStream(current);
                    FileOutputStream out = new FileOutputStream(dest);
                    byte[] readBuf = new byte[1024 * 512];
                    int readLength;
                    while ((readLength = in.read(readBuf)) != -1) {
                        out.write(readBuf, 0, readLength);
                    }
                    in.close();
                    out.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return;
        } else {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                for (Integer idx : dataList.getSelectedRows()) {
                    Object obj = model.getItemAtRow(sorter.convertRowIndexToModel(idx));
                    AttachFile entry = (AttachFile) obj;
                    File current = entry.getCurrentPath();
                    File dest = new File(fc.getSelectedFile(), entry.getName());
                    try {
                        FileInputStream in = new FileInputStream(current);
                        FileOutputStream out = new FileOutputStream(dest);
                        byte[] readBuf = new byte[1024 * 512];
                        int readLength;
                        while ((readLength = in.read(readBuf)) != -1) {
                            out.write(readBuf, 0, readLength);
                        }
                        in.close();
                        out.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return;
        }
    }

    public void addFile(AttachFile file) {
        if (model.getItems().contains(file)) {
            AttachFile oldFile = model.getItems().get(model.getItems().indexOf(file));
            file.setMetadata(new Attachment());
            file.getMetadata().setVersion(oldFile.getMetadata().getVersion() + 1);
            file.getMetadata().setAddedBy(userDao.getCurrentUser());
            file.getMetadata().setDateAdded(new Date());
            file.getMetadata().setFilename(file.getName());
            if (!file.isNewFile()) {
                deletedFiles.add(file);
            }
            model.replaceRow(file);
        } else {
            if (file.isNewFile()) {
                file.getMetadata().setVersion(1);
                file.getMetadata().setAddedBy(userDao.getCurrentUser());
                file.getMetadata().setDateAdded(new Date());
                file.getMetadata().setFilename(file.getName());
            }
            model.addRow(file);
        }
    }

    public class AttachFile {

        private String name;

        private File currentPath;

        private boolean newFile;

        private Attachment metadata;

        public AttachFile(File currentPath, Attachment attachment, boolean newFile) {
            this.currentPath = currentPath;
            this.metadata = attachment == null ? new Attachment() : attachment;
            this.newFile = newFile;
            name = currentPath.getName();
        }

        public File getCurrentPath() {
            return currentPath;
        }

        public void setCurrentPath(File currentPath) {
            this.currentPath = currentPath;
        }

        public String getName() {
            return currentPath.getName();
        }

        public void setName(String name) {
            this.name = name;
        }

        public Image getIcon() {
            try {
                ShellFolder shellFolder = ShellFolder.getShellFolder(currentPath);
                return shellFolder.getIcon(false);
            } catch (FileNotFoundException e1) {
                return Icons.ATTACHMENT.getImage();
            }
        }

        /**
         * This copies the file to a destination and to the new name if the name
         * is different from the original name.
         * 
         * @param folder
         * @param listener
         */
        public void copyTo(File folder) {
            if (!isNewFile()) {
                return;
            }
            if (!folder.exists()) {
                folder.mkdir();
            }
            File dest = new File(folder, name);
            try {
                FileInputStream in = new FileInputStream(currentPath);
                FileOutputStream out = new FileOutputStream(dest);
                byte[] readBuf = new byte[1024 * 512];
                int readLength;
                long totalCopiedSize = 0;
                boolean canceled = false;
                while ((readLength = in.read(readBuf)) != -1) {
                    out.write(readBuf, 0, readLength);
                }
                in.close();
                out.close();
                if (canceled) {
                    dest.delete();
                } else {
                    currentPath = dest;
                    newFile = false;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public long getSize() {
            return currentPath.length();
        }

        public boolean isNewFile() {
            return newFile;
        }

        public void setNewFile(boolean newFile) {
            this.newFile = newFile;
        }

        public Attachment getMetadata() {
            return metadata;
        }

        public void setMetadata(Attachment metadata) {
            this.metadata = metadata;
        }

        public String getDescription() {
            return metadata.getDescription();
        }

        public void setDescription(String description) {
            Attachment meta = new Attachment();
            meta.setAddedBy(metadata.getAddedBy());
            meta.setDateAdded(metadata.getDateAdded());
            meta.setDescription(description);
            meta.setFilename(metadata.getFilename());
            meta.setVersion(metadata.getVersion());
            metadata = meta;
        }

        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final AttachFile other = (AttachFile) obj;
            if (name == null) {
                if (other.name != null) return false;
            } else if (!name.toLowerCase().equals(other.name.toLowerCase())) return false;
            return true;
        }
    }

    private class AttachCellRenderer extends JLabel implements ListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof AttachFile) {
                AttachFile file = (AttachFile) value;
                if (file.getMetadata() != null) {
                    setText(file.getName() + " (" + file.getMetadata().getAddedBy().toString() + ")");
                    setForeground(Color.BLACK);
                } else {
                    setText(file.getName());
                    setForeground(Color.BLUE);
                }
                setIcon(new ImageIcon(file.getIcon()));
                if (isSelected) {
                    setBackground(list.getSelectionBackground());
                    setForeground(list.getSelectionForeground());
                } else {
                    setBackground(list.getBackground());
                    setForeground(list.getForeground());
                }
                setEnabled(list.isEnabled());
                setFont(list.getFont());
                setOpaque(true);
            } else {
                setText("N/A");
                setEnabled(false);
            }
            return this;
        }
    }

    private class AttachFileTransferHandler extends TransferHandler {

        private DataFlavor fileFlavor;

        public AttachFileTransferHandler() {
            fileFlavor = DataFlavor.javaFileListFlavor;
        }

        @Override
        public boolean canImport(JComponent c, DataFlavor[] flavors) {
            if (hasFileFlavor(flavors)) {
                return true;
            }
            return false;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (hasFileFlavor(support.getDataFlavors())) {
                return true;
            }
            return false;
        }

        private boolean hasFileFlavor(DataFlavor[] flavors) {
            for (int i = 0; i < flavors.length; i++) {
                if (fileFlavor.equals(flavors[i])) {
                    return true;
                }
                if (DataFlavor.getTextPlainUnicodeFlavor().equals(flavors[i])) {
                    return true;
                }
            }
            return false;
        }

        public boolean importData(JComponent c, Transferable t) {
            if (!canImport(c, t.getTransferDataFlavors())) {
                return false;
            }
            try {
                if (t.isDataFlavorSupported(fileFlavor)) {
                    List files = (List) t.getTransferData(fileFlavor);
                    for (int i = 0; i < files.size(); i++) {
                        File file = (File) files.get(i);
                        AttachFile add = new AttachFile(file, null, true);
                        addFile(add);
                    }
                    return true;
                } else if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    Object transferData = t.getTransferData(DataFlavor.stringFlavor);
                    System.out.println(transferData);
                }
            } catch (UnsupportedFlavorException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return false;
        }

        @Override
        public boolean importData(TransferSupport support) {
            return importData(dataList, support.getTransferable());
        }

        @Override
        protected Transferable createTransferable(final JComponent c) {
            return new Transferable() {

                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[] { DataFlavor.javaFileListFlavor };
                }

                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    if (flavor.equals(DataFlavor.javaFileListFlavor)) {
                        return true;
                    }
                    return false;
                }

                public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                    List<File> result = new LinkedList<File>();
                    for (Integer idx : dataList.getSelectedRows()) {
                        AttachFile source = model.getItemAtRow(sorter.convertRowIndexToModel(idx));
                        result.add(source.getCurrentPath());
                    }
                    return result;
                }
            };
        }

        @Override
        public int getSourceActions(JComponent c) {
            System.out.println("sourceActions()");
            return COPY;
        }
    }

    interface AttachListener {

        boolean updateCheck(long copiedSize);
    }

    public int getAttachCount() {
        return model.getRowCount();
    }
}
