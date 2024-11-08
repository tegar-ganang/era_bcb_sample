package com.zhyi.sse.ui;

import com.zhyi.sse.common.Context;
import com.zhyi.sse.common.SrtToolkit;
import com.zhyi.sse.common.SubtitleFormatException;
import com.zhyi.sse.model.SrtTableModel;
import com.zhyi.sse.model.Subtitle;
import com.zhyi.sse.ui.AjustTimelineDialog.TimelineAjustment;
import com.zhyi.sse.ui.ExportSubtitlesDialog.SubtitleExporter;
import com.zhyi.sse.ui.ImportSubtitlesDialog.SubtitleImporter;
import com.zhyi.zylib.toolkit.SwingToolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumnModel;

/**
 * The main frame. It displays subtitles and provides components to edit them.
 */
public class SrtSubtitleEditorFrame extends JFrame implements ActionListener, KeyListener, ListSelectionListener, PropertyChangeListener {

    private static final Subtitle DEFAULT_SUBTITLE;

    static {
        DEFAULT_SUBTITLE = new Subtitle();
        try {
            DEFAULT_SUBTITLE.setBeginTime("00:00:00,000");
            DEFAULT_SUBTITLE.setEndTime("12:34:56,789");
            DEFAULT_SUBTITLE.setContent("");
        } catch (SubtitleFormatException ex) {
        }
    }

    private JMenuItem openMenuItem;

    private JMenuItem closeMenuItem;

    private JMenuItem saveMenuItem;

    private JMenuItem saveAsMenuItem;

    private JMenuItem optionsMenuItem;

    private JMenuItem exitMenuItem;

    private JMenuItem addBeforeMenuItem;

    private JMenuItem addAfterMenuItem;

    private JMenuItem removeMenuItem;

    private JMenuItem moveUpMenuItem;

    private JMenuItem moveDownMenuItem;

    private JMenuItem ajustTimelineMenuItem;

    private JMenuItem importMenuItem;

    private JMenuItem exportMenuItem;

    private JMenuItem aboutMenuItem;

    private JTextField beginTimeTextField;

    private JTextField endTimeTextField;

    private JTextArea contentTextArea;

    private JTextField indexTextField;

    private JTable srtTable;

    private JButton saveSubtitleButton;

    private JFileChooser srtFileChooser;

    private OptionsDialog optionsDialog;

    private AjustTimelineDialog ajustTimelineDialog;

    private ImportSubtitlesDialog importSubtitlesDialog;

    private ExportSubtitlesDialog exportSubtitlesDialog;

    private File srtFile;

    private boolean saved = true;

    private boolean editorEnabled;

    private SrtTableModel srtTableModel;

    public SrtSubtitleEditorFrame() {
        initComponents();
        setSize(800, 500);
        setResizable(false);
        setTitle("SRT Subtitle Editor - Untitled");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    private void initComponents() {
        initMenuBar();
        initContentPane();
        initDialogs();
        enableEditor(false);
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        openMenuItem = SwingToolkit.createMenuItem("Open...", KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK), this);
        saveMenuItem = SwingToolkit.createMenuItem("Save", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), this);
        saveAsMenuItem = SwingToolkit.createMenuItem("Save As...", KeyEvent.VK_A, null, this);
        closeMenuItem = SwingToolkit.createMenuItem("Close", KeyEvent.VK_C, null, this);
        optionsMenuItem = SwingToolkit.createMenuItem("Options...", KeyEvent.VK_O, null, this);
        exitMenuItem = SwingToolkit.createMenuItem("Exit", KeyEvent.VK_X, null, this);
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(openMenuItem);
        fileMenu.add(closeMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(optionsMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);
        addBeforeMenuItem = SwingToolkit.createMenuItem("Add Before", KeyEvent.VK_B, null, this);
        addAfterMenuItem = SwingToolkit.createMenuItem("Add After", KeyEvent.VK_A, null, this);
        removeMenuItem = SwingToolkit.createMenuItem("Remove", KeyEvent.VK_R, null, this);
        moveUpMenuItem = SwingToolkit.createMenuItem("Move Up", KeyEvent.VK_U, null, this);
        moveDownMenuItem = SwingToolkit.createMenuItem("Move Down", KeyEvent.VK_D, null, this);
        ajustTimelineMenuItem = SwingToolkit.createMenuItem("Ajust Timeline...", KeyEvent.VK_T, null, this);
        importMenuItem = SwingToolkit.createMenuItem("Import...", KeyEvent.VK_I, null, this);
        exportMenuItem = SwingToolkit.createMenuItem("Export...", KeyEvent.VK_E, null, this);
        JMenu subtitleMenu = new JMenu("Subtitle");
        subtitleMenu.setMnemonic(KeyEvent.VK_S);
        subtitleMenu.add(addBeforeMenuItem);
        subtitleMenu.add(addAfterMenuItem);
        subtitleMenu.add(removeMenuItem);
        subtitleMenu.addSeparator();
        subtitleMenu.add(moveUpMenuItem);
        subtitleMenu.add(moveDownMenuItem);
        subtitleMenu.addSeparator();
        subtitleMenu.add(ajustTimelineMenuItem);
        subtitleMenu.addSeparator();
        subtitleMenu.add(importMenuItem);
        subtitleMenu.add(exportMenuItem);
        menuBar.add(subtitleMenu);
        aboutMenuItem = SwingToolkit.createMenuItem("About", KeyEvent.VK_A, null, this);
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        helpMenu.add(aboutMenuItem);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }

    private void initContentPane() {
        srtTableModel = new SrtTableModel(new ArrayList<Subtitle>());
        srtTable = new JTable(srtTableModel);
        srtTable.setFillsViewportHeight(true);
        srtTable.getTableHeader().setReorderingAllowed(false);
        srtTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        srtTable.getSelectionModel().addListSelectionListener(this);
        int tableWidth = srtTable.getPreferredScrollableViewportSize().width;
        TableColumnModel columnModel = srtTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(12);
        columnModel.getColumn(1).setPreferredWidth(36);
        columnModel.getColumn(2).setPreferredWidth(36);
        columnModel.getColumn(3).setPreferredWidth(tableWidth - 84);
        JScrollPane srtTableScrollPane = new JScrollPane(srtTable);
        JLabel indexLabel = new JLabel("Index:");
        JLabel beginTimeLabel = new JLabel("Begin Time:");
        JLabel endTimeLabel = new JLabel("End Time:");
        indexTextField = new JTextField();
        indexTextField.setEditable(false);
        beginTimeTextField = new JTextField();
        beginTimeTextField.addKeyListener(this);
        endTimeTextField = new JTextField();
        endTimeTextField.addKeyListener(this);
        contentTextArea = new JTextArea();
        contentTextArea.setLineWrap(true);
        contentTextArea.setWrapStyleWord(true);
        contentTextArea.addKeyListener(this);
        JScrollPane contentScrollPane = new JScrollPane(contentTextArea);
        saveSubtitleButton = SwingToolkit.createButton("<html><center>Save Subtitle<br>(Ctrl + Enter)</center></html>", null, this);
        JPanel panel = new JPanel();
        GroupLayout pgl = SwingToolkit.createGroupLayout(panel, false, true);
        pgl.setHorizontalGroup(pgl.createSequentialGroup().addGroup(pgl.createParallelGroup().addComponent(indexLabel).addComponent(beginTimeLabel).addComponent(endTimeLabel)).addGroup(pgl.createParallelGroup().addComponent(indexTextField, 96, 96, 96).addComponent(beginTimeTextField, 96, 96, 96).addComponent(endTimeTextField, 96, 96, 96)));
        pgl.setVerticalGroup(pgl.createSequentialGroup().addGroup(pgl.createParallelGroup(Alignment.BASELINE).addComponent(indexLabel).addComponent(indexTextField)).addGroup(pgl.createParallelGroup(Alignment.BASELINE).addComponent(beginTimeLabel).addComponent(beginTimeTextField)).addGroup(pgl.createParallelGroup(Alignment.BASELINE).addComponent(endTimeLabel).addComponent(endTimeTextField)));
        GroupLayout gl = SwingToolkit.createGroupLayout(getContentPane(), true, true);
        gl.setHorizontalGroup(gl.createParallelGroup().addComponent(srtTableScrollPane).addGroup(gl.createSequentialGroup().addComponent(panel).addComponent(contentScrollPane).addComponent(saveSubtitleButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)));
        gl.setVerticalGroup(gl.createSequentialGroup().addComponent(srtTableScrollPane).addGroup(gl.createParallelGroup().addComponent(panel).addComponent(contentScrollPane).addComponent(saveSubtitleButton)));
        gl.linkSize(SwingConstants.VERTICAL, panel, contentScrollPane, saveSubtitleButton);
    }

    private void initDialogs() {
        srtFileChooser = new JFileChooser();
        srtFileChooser.setFileFilter(new FileNameExtensionFilter("SRT Subtitle File (*.srt)", "srt"));
        optionsDialog = new OptionsDialog(this);
        ajustTimelineDialog = new AjustTimelineDialog(this);
        ajustTimelineDialog.addPropertyChangeListener(this);
        importSubtitlesDialog = new ImportSubtitlesDialog(this);
        importSubtitlesDialog.addPropertyChangeListener(this);
        exportSubtitlesDialog = new ExportSubtitlesDialog(this);
        exportSubtitlesDialog.addPropertyChangeListener(this);
    }

    private void enableEditor(boolean enabled) {
        removeMenuItem.setEnabled(enabled);
        moveUpMenuItem.setEnabled(enabled);
        moveDownMenuItem.setEnabled(enabled);
        beginTimeTextField.setEnabled(enabled);
        endTimeTextField.setEnabled(enabled);
        contentTextArea.setEnabled(enabled);
        saveSubtitleButton.setEnabled(enabled);
        if (enabled) {
            SwingToolkit.addPopupMenuForTextComponent(indexTextField, false);
            SwingToolkit.addPopupMenuForTextComponent(beginTimeTextField, true);
            SwingToolkit.addPopupMenuForTextComponent(endTimeTextField, true);
            SwingToolkit.addPopupMenuForTextComponent(contentTextArea, true);
        } else {
            indexTextField.setComponentPopupMenu(null);
            beginTimeTextField.setComponentPopupMenu(null);
            endTimeTextField.setComponentPopupMenu(null);
            contentTextArea.setComponentPopupMenu(null);
        }
        editorEnabled = enabled;
    }

    private void updateSavingStatus(boolean saved) {
        String title = "SRT Subtitle Editor - ";
        if (srtFile != null) {
            title += srtFile.getName();
        } else {
            title += "Untitled";
        }
        setTitle(saved ? title : title + "*");
        this.saved = saved;
    }

    private void onExit() {
        promptToSave();
        if (!saved) {
            return;
        }
        try {
            Context.saveOptions();
        } catch (IOException ex) {
            SwingToolkit.showRootCause(ex, this);
        }
        System.exit(0);
    }

    private void openSrtFile() {
        promptToSave();
        if (!saved) {
            return;
        }
        if ((srtFileChooser.showOpenDialog(this)) == JFileChooser.APPROVE_OPTION) {
            File srt = SrtToolkit.getSelectedSrtFile(srtFileChooser);
            try {
                List<Subtitle> subtitles = SrtToolkit.parseSrtFile(srt, Context.getCharset());
                srtTableModel.setSubtitles(subtitles);
                srtTable.setRowSelectionInterval(0, 0);
                srtFile = srt;
                if (!editorEnabled) {
                    enableEditor(true);
                }
                updateSavingStatus(true);
            } catch (Exception ex) {
                SwingToolkit.showRootCause(ex, this);
            }
        }
    }

    private void closeSrtFile() {
        promptToSave();
        if (!saved) {
            return;
        }
        srtTableModel.setSubtitles(new ArrayList<Subtitle>());
        srtFile = null;
        enableEditor(false);
        updateSavingStatus(true);
    }

    private void promptToSave() {
        if (saved) {
            return;
        }
        int option = JOptionPane.showConfirmDialog(this, "File has been changed. Do you want to save it?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        switch(option) {
            case JOptionPane.YES_OPTION:
                saveSrtFile();
                return;
            case JOptionPane.NO_OPTION:
                updateSavingStatus(true);
                return;
            case JOptionPane.CANCEL_OPTION:
                return;
        }
    }

    private void saveSrtFile() {
        try {
            SrtToolkit.saveSrtFile(srtTableModel.getSubtitles(), srtFile, Context.getCharset());
            updateSavingStatus(true);
        } catch (IOException ex) {
            SwingToolkit.showRootCause(ex, this);
        }
    }

    private void saveSrtFileAs() {
        while ((srtFileChooser.showSaveDialog(this)) == JFileChooser.APPROVE_OPTION) {
            File srt = SrtToolkit.getSelectedSrtFile(srtFileChooser);
            if (srt.exists()) {
                int option = JOptionPane.showConfirmDialog(this, "File already exists. Do you want to overwrite it?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                switch(option) {
                    case JOptionPane.YES_OPTION:
                        break;
                    case JOptionPane.NO_OPTION:
                        continue;
                    case JOptionPane.CANCEL_OPTION:
                        return;
                }
            }
            try {
                SrtToolkit.saveSrtFile(srtTableModel.getSubtitles(), srt, Context.getCharset());
                break;
            } catch (IOException ex) {
                SwingToolkit.showRootCause(ex, this);
            }
        }
    }

    private void addSubtitle(boolean addBefore) {
        int index = srtTable.getSelectedRow();
        if (index == -1) {
            index = addBefore ? 0 : srtTable.getRowCount();
        } else if (!addBefore) {
            index++;
        }
        srtTableModel.addSubtitle(index, DEFAULT_SUBTITLE);
        updateSavingStatus(false);
        srtTable.setRowSelectionInterval(index, index);
    }

    private void removeSelectedSubtitles() {
        int[] rows = srtTable.getSelectedRows();
        int option = JOptionPane.showConfirmDialog(this, "Do you really want to remove the selected subtitle(s)?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (option == JOptionPane.YES_OPTION) {
            srtTableModel.removeSubtitles(rows[0], rows[rows.length - 1]);
            updateSavingStatus(false);
            if (srtTable.getRowCount() == 0) {
                return;
            }
            int row = srtTable.getRowCount() - 1;
            if (row > rows[0]) {
                row = rows[0];
            }
            srtTable.setRowSelectionInterval(row, row);
        }
    }

    private void moveUpSelectedSubtitles() {
        int[] rows = srtTable.getSelectedRows();
        int first = rows[0];
        int last = rows[rows.length - 1];
        if (first == 0) {
            return;
        }
        Subtitle subtitleAbove = srtTableModel.getSubtitle(first - 1);
        srtTableModel.addSubtitle(last + 1, subtitleAbove);
        srtTableModel.removeSubtitles(first - 1, first - 1);
        srtTable.setRowSelectionInterval(first - 1, last - 1);
        updateSavingStatus(false);
    }

    private void moveDownSelectedSubtitles() {
        int[] rows = srtTable.getSelectedRows();
        int first = rows[0];
        int last = rows[rows.length - 1];
        if (last == srtTable.getRowCount() - 1) {
            return;
        }
        Subtitle subtitleBelow = srtTableModel.getSubtitle(last + 1);
        srtTableModel.removeSubtitles(last + 1, last + 1);
        srtTableModel.addSubtitle(first, subtitleBelow);
        srtTable.setRowSelectionInterval(first + 1, last + 1);
        updateSavingStatus(false);
    }

    private void showAjustTimelineDialog() {
        int beginIndex = 1;
        int endIndex = srtTable.getRowCount();
        int[] rows = srtTable.getSelectedRows();
        if (rows.length != 0) {
            beginIndex = rows[0] + 1;
            endIndex = rows[rows.length - 1] + 1;
        }
        ajustTimelineDialog.setIndexes(beginIndex, endIndex);
        ajustTimelineDialog.setLocationRelativeTo(this);
        ajustTimelineDialog.setVisible(true);
    }

    private void ajustTimeline(TimelineAjustment ta) {
        int beginRow = ta.getBeginIndex() - 1;
        int endRow = ta.getEndIndex() - 1;
        int offset = ta.getOffset();
        if (endRow > srtTable.getRowCount() - 1) {
            endRow = srtTable.getRowCount() - 1;
        }
        try {
            for (int i = beginRow; i <= endRow; i++) {
                Subtitle subtitle = srtTableModel.getSubtitle(i);
                subtitle.setBeginTime(SrtToolkit.moveTime(subtitle.getBeginTime(), offset));
                subtitle.setEndTime(SrtToolkit.moveTime(subtitle.getEndTime(), offset));
                srtTableModel.updateSubtitle(i, subtitle);
            }
            updateSavingStatus(false);
        } catch (SubtitleFormatException ex) {
            SwingToolkit.showRootCause(ex, this);
        }
    }

    private void showImportSubtitlesDialog() {
        int index = srtTable.getRowCount();
        int row = srtTable.getSelectedRow();
        if (row != -1) {
            index = row + 1;
        }
        importSubtitlesDialog.setIndex(index);
        importSubtitlesDialog.setLocationRelativeTo(this);
        importSubtitlesDialog.setVisible(true);
    }

    private void importSubtitles(final SubtitleImporter si) {
        int index0 = si.getIndex();
        if (index0 > srtTable.getRowCount()) {
            index0 = srtTable.getRowCount();
        }
        final int index = index0;
        new SwingWorker<List<Subtitle>, Void>() {

            @Override
            protected List<Subtitle> doInBackground() throws Exception {
                return SrtToolkit.parseSrtFile(si.getSourceFile(), Context.getCharset());
            }

            @Override
            protected void done() {
                try {
                    List<Subtitle> subtitles = get();
                    srtTableModel.addSubtitles(index, subtitles);
                    srtTable.setRowSelectionInterval(index, index + subtitles.size() - 1);
                    updateSavingStatus(false);
                } catch (Exception ex) {
                    SwingToolkit.showRootCause(ex, SrtSubtitleEditorFrame.this);
                }
            }
        }.execute();
    }

    private void showExportSubtitlesDialog() {
        int beginIndex = 1;
        int endIndex = srtTable.getRowCount();
        int[] rows = srtTable.getSelectedRows();
        if (rows.length != 0) {
            beginIndex = rows[0] + 1;
            endIndex = rows[rows.length - 1] + 1;
        }
        exportSubtitlesDialog.setIndexes(beginIndex, endIndex);
        exportSubtitlesDialog.setLocationRelativeTo(this);
        exportSubtitlesDialog.setVisible(true);
    }

    private void exportSubtitles(SubtitleExporter se) {
        int beginRow = se.getBeginIndex() - 1;
        int endRow = se.getEndIndex();
        if (endRow > srtTable.getRowCount()) {
            endRow = srtTable.getRowCount();
        }
        try {
            SrtToolkit.saveSrtFile(srtTableModel.getSubtitles().subList(beginRow, endRow), se.getTargetFile(), Context.getCharset());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void saveSubtitle() {
        int index = srtTable.getSelectedRow();
        Subtitle subtitle = srtTableModel.getSubtitle(index);
        try {
            subtitle.setBeginTime(beginTimeTextField.getText());
            subtitle.setEndTime(beginTimeTextField.getText());
            subtitle.setContent(contentTextArea.getText());
            srtTableModel.updateSubtitle(index, subtitle);
            updateSavingStatus(false);
            if (Context.isEditContinuously()) {
                if (++index < srtTable.getRowCount()) {
                    SwingToolkit.viewTableCell(srtTable, index, index);
                }
            }
        } catch (SubtitleFormatException ex) {
            SwingToolkit.showRootCause(ex, this);
        }
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            onExit();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == openMenuItem) {
            openSrtFile();
        } else if (src == closeMenuItem) {
            closeSrtFile();
        } else if (src == saveMenuItem) {
            saveSrtFile();
        } else if (src == saveAsMenuItem) {
            saveSrtFileAs();
        } else if (src == optionsMenuItem) {
            optionsDialog.setLocationRelativeTo(this);
            optionsDialog.setVisible(true);
        } else if (src == exitMenuItem) {
            onExit();
        } else if (src == addBeforeMenuItem) {
            addSubtitle(true);
        } else if (src == addAfterMenuItem) {
            addSubtitle(false);
        } else if (src == removeMenuItem) {
            removeSelectedSubtitles();
        } else if (src == moveUpMenuItem) {
            moveUpSelectedSubtitles();
        } else if (src == moveDownMenuItem) {
            moveDownSelectedSubtitles();
        } else if (src == ajustTimelineMenuItem) {
            showAjustTimelineDialog();
        } else if (src == importMenuItem) {
            showImportSubtitlesDialog();
        } else if (src == exportMenuItem) {
            showExportSubtitlesDialog();
        } else if (src == aboutMenuItem) {
            JOptionPane.showMessageDialog(this, "<html><h2>SRT Subtitle Editor</h2>" + "Copyright &copy; 2009-2010 Zhao Yi (shinzey@msn.com)" + "<br>Licensed under GNU General Public License Version 3</html>", "About SRT Subtitle Editor", JOptionPane.INFORMATION_MESSAGE);
        } else if (src == saveSubtitleButton) {
            saveSubtitle();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
            saveSubtitle();
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            int row = srtTable.getSelectedRow();
            if (row != -1) {
                Subtitle subtitle = srtTableModel.getSubtitle(row);
                indexTextField.setText(Integer.toString(row + 1));
                beginTimeTextField.setText(subtitle.getBeginTime());
                endTimeTextField.setText(subtitle.getEndTime());
                contentTextArea.setText(subtitle.getContent());
                enableEditor(true);
            } else {
                enableEditor(false);
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String name = evt.getPropertyName();
        if (name.equals(AjustTimelineDialog.TIMELINE_PROPERTY)) {
            ajustTimeline((TimelineAjustment) evt.getNewValue());
        } else if (name.equals(ImportSubtitlesDialog.IMPORT_SUBTITLES_PROPERTY)) {
            importSubtitles((SubtitleImporter) evt.getNewValue());
        } else if (name.equals(ExportSubtitlesDialog.EXPORT_SUBTITLES_PROPERTY)) {
            exportSubtitles((SubtitleExporter) evt.getNewValue());
        }
    }

    public static void main(String[] args) {
        Context.loadOptions();
        SwingToolkit.initSystemlookAndFeel();
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                JFrame frame = new SrtSubtitleEditorFrame();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
