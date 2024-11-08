package phex.gui.tabs;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import phex.MainFrame;
import phex.ServiceManager;
import phex.query.Search;
import phex.query.ResearchSetting;
import phex.dialogues.DlgInput1Str;
import phex.dialogues.DlgLog;
import phex.download.DownloadFile;
import phex.download.DownloadManager;
import phex.download.RemoteFile;
import phex.event.DownloadFilesChangeListener;
import phex.gui.common.GUIUtils;
import phex.gui.models.DownloadTableModel;
import phex.gui.models.DownloadCandidateTableModel;
import phex.gui.dialogs.DownloadConfigDialog;
import phex.utils.TableSorter;
import phex.utils.StrUtil;
import phex.utils.Localizer;
import phex.gui.renderer.*;

/**
 * The DownloadTab Panel.
 */
public class DownloadTab extends JPanel {

    private DownloadManager mDownloadMgr = ServiceManager.getDownloadManager();

    private JTable mDownloadTable;

    private AbstractTableModel mDownloadModel;

    private TableSorter mDownloadSorter;

    private JTable mDownloadCandidate;

    private DownloadCandidateTableModel mDownloadCandidateModel;

    private JLabel mCandidateLabelStatus;

    private MainFrame mainFrame;

    public DownloadTab(MainFrame frame) {
        mainFrame = frame;
    }

    public void initComponent() {
        setLayout(new BorderLayout());
        JPanel downloadTablePanel = new JPanel(new BorderLayout());
        mDownloadModel = new DownloadTableModel();
        mDownloadSorter = new TableSorter(mDownloadModel);
        mDownloadTable = new JTable(mDownloadSorter);
        mDownloadTable.getSelectionModel().addListSelectionListener(new DownloadSelectionHandler());
        mDownloadSorter.addMouseListenerToHeaderInTable(mDownloadTable);
        JPanel downloadButtonsPanel = new JPanel(new FlowLayout());
        JButton stopDownloadButton = new JButton(Localizer.getString("Stop"));
        JButton resumeButton = new JButton(Localizer.getString("Resume"));
        JButton removeDownloadButton = new JButton(Localizer.getString("Remove"));
        JButton configDownloadButton = new JButton(Localizer.getString("Configure"));
        configDownloadButton.setActionCommand("ConfigureDownload");
        JButton showLogButton = new JButton(Localizer.getString("ShowLog"));
        mainFrame.addRefreshComponent("ActionDownloadStop", stopDownloadButton);
        mainFrame.addRefreshComponent("ActionDownloadResume", resumeButton);
        mainFrame.addRefreshComponent("ActionDownloadRemove", removeDownloadButton);
        mainFrame.addRefreshComponent("ActionDownloadShowLog", showLogButton);
        downloadButtonsPanel.add(stopDownloadButton);
        downloadButtonsPanel.add(resumeButton);
        downloadButtonsPanel.add(configDownloadButton);
        downloadButtonsPanel.add(removeDownloadButton);
        downloadButtonsPanel.add(showLogButton);
        stopDownloadButton.addActionListener(new StopDownloadHandler());
        resumeButton.addActionListener(new ResumeDownloadHandler());
        configDownloadButton.addActionListener(new ButtonActionListener());
        removeDownloadButton.addActionListener(new RemoveDownloadHandler());
        showLogButton.addActionListener(new ShowDownloadLogHandler());
        downloadTablePanel.add(BorderLayout.NORTH, new JLabel("Downloads:"));
        downloadTablePanel.add(BorderLayout.CENTER, new JScrollPane(mDownloadTable));
        downloadTablePanel.add(BorderLayout.SOUTH, downloadButtonsPanel);
        downloadTablePanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8), BorderFactory.createEtchedBorder()), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        mDownloadCandidateModel = new DownloadCandidateTableModel();
        mDownloadCandidate = new JTable(mDownloadCandidateModel);
        mDownloadCandidate.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mDownloadCandidate.getSelectionModel().addListSelectionListener(new SelectionHandler());
        JPanel downloadCandidateButtonsPanel = new JPanel(new FlowLayout());
        JButton currCandidateButton = new JButton(Localizer.getString("SetCurrent"));
        JButton removeCandidateButton = new JButton(Localizer.getString("Remove"));
        JButton removeAllCandidatesButton = new JButton(Localizer.getString("RemoveAll"));
        removeAllCandidatesButton.setActionCommand("RemoveAll");
        JButton searchCandidateButton = new JButton(Localizer.getString("Search"));
        JButton stopSearchCandidateButton = new JButton(Localizer.getString("StopSearch"));
        mainFrame.addRefreshComponent("ActionDownloadSetCandidateCurrent", currCandidateButton);
        mainFrame.addRefreshComponent("ActionDownloadRemoveCandidate", removeCandidateButton);
        mainFrame.addRefreshComponent("ActionDownloadSearchCandidate", searchCandidateButton);
        mainFrame.addRefreshComponent("ActionDownloadStopSearchCandidate", stopSearchCandidateButton);
        downloadCandidateButtonsPanel.add(currCandidateButton);
        downloadCandidateButtonsPanel.add(removeCandidateButton);
        downloadCandidateButtonsPanel.add(removeAllCandidatesButton);
        downloadCandidateButtonsPanel.add(searchCandidateButton);
        downloadCandidateButtonsPanel.add(stopSearchCandidateButton);
        removeAllCandidatesButton.addActionListener(new CandidateButtonHandler());
        currCandidateButton.addActionListener(new SetCandidateCurrentHandler());
        removeCandidateButton.addActionListener(new RemoveCandidateHandler());
        searchCandidateButton.addActionListener(new SearchCandidateHandler());
        stopSearchCandidateButton.addActionListener(new StopSearchCandidateHandler());
        JPanel downloadCandidatePanel = new JPanel(new BorderLayout());
        mCandidateLabelStatus = new JLabel("Download Candidates:  ");
        downloadCandidatePanel.add(BorderLayout.NORTH, mCandidateLabelStatus);
        downloadCandidatePanel.add(BorderLayout.CENTER, new JScrollPane(mDownloadCandidate));
        downloadCandidatePanel.add(BorderLayout.SOUTH, downloadCandidateButtonsPanel);
        downloadCandidatePanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8), BorderFactory.createEtchedBorder()), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        downloadTablePanel.setPreferredSize(new Dimension(780, 300));
        downloadCandidatePanel.setPreferredSize(new Dimension(780, 200));
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerSize(6);
        splitPane.setOneTouchExpandable(true);
        splitPane.setTopComponent(downloadTablePanel);
        splitPane.setBottomComponent(downloadCandidatePanel);
        JPanel mDownload = new JPanel(new BorderLayout());
        mDownload.add(BorderLayout.CENTER, splitPane);
        add(BorderLayout.CENTER, mDownload);
        CellRenderer cellRenderer = new CellRenderer();
        mDownloadTable.getColumn(mDownloadModel.getColumnName(0)).setCellRenderer(cellRenderer);
        mDownloadTable.getColumn(mDownloadModel.getColumnName(1)).setCellRenderer(cellRenderer);
        mDownloadTable.getColumn(mDownloadModel.getColumnName(2)).setCellRenderer(cellRenderer);
        mDownloadTable.getColumn(mDownloadModel.getColumnName(3)).setCellRenderer(cellRenderer);
        mDownloadTable.getColumn(mDownloadModel.getColumnName(7)).setCellRenderer(cellRenderer);
        DefaultPhexCellRenderers.setDefaultPhexCellRenderers(mDownloadTable);
        DefaultPhexCellRenderers.setDefaultPhexCellRenderers(mDownloadCandidate);
        GUIUtils.adjustTableProgresssBarHeight(mDownloadTable);
        mDownloadMgr.addDownloadFilesChangeListener(new DownloadFilesChangeHandler());
    }

    /**
     * This is overloaded to update the table size for the progress bar on
     * every UI update. Like font size change!
     */
    public void updateUI() {
        super.updateUI();
        if (mDownloadTable != null) {
            GUIUtils.adjustTableProgresssBarHeight(mDownloadTable);
        }
    }

    public void stopDownload() {
        int rowCount = mDownloadTable.getSelectedRowCount();
        if (rowCount <= 0) return;
        int[] rows = mDownloadTable.getSelectedRows();
        for (int i = 0; i < rowCount; i++) {
            int row = rows[i];
            if (row >= mDownloadMgr.getDownloadCount()) return;
            row = mDownloadSorter.indexes[row];
            mDownloadMgr.stopDownload(row);
        }
        mainFrame.refreshAllActions();
    }

    public void resumeDownload() {
        int rowCount = mDownloadTable.getSelectedRowCount();
        if (rowCount <= 0) return;
        int[] rows = mDownloadTable.getSelectedRows();
        for (int i = 0; i < rowCount; i++) {
            int row = rows[i];
            if (row >= mDownloadMgr.getDownloadCount()) return;
            row = mDownloadSorter.indexes[row];
            mDownloadMgr.resumeDownload(row);
        }
        mainFrame.refreshAllActions();
    }

    public boolean isDownloadSelected() {
        return (mainFrame.isDownloadTabSelected() && mDownloadTable.getSelectedRowCount() > 0 && mDownloadMgr.getDownloadCount() > 0);
    }

    public void removeDownload() {
        synchronized (mDownloadMgr) {
            int rowCount = mDownloadTable.getSelectedRowCount();
            if (rowCount <= 0) return;
            int[] rows = mDownloadTable.getSelectedRows();
            int[] orderedRows = new int[rows.length];
            Vector downloadFilesToRemove = new Vector();
            for (int i = 0; i < rowCount; i++) {
                int row = rows[i];
                if (row >= mDownloadMgr.getDownloadCount()) return;
                orderedRows[i] = mDownloadSorter.indexes[row];
            }
            mDownloadTable.removeRowSelectionInterval(0, mDownloadTable.getRowCount() - 1);
            for (int i = orderedRows.length - 1; i > 0; i--) {
                for (int j = 0; j < i; j++) {
                    if (orderedRows[j] > orderedRows[j + 1]) {
                        int tmp = orderedRows[j];
                        orderedRows[j] = orderedRows[j + 1];
                        orderedRows[j + 1] = tmp;
                    }
                }
            }
            for (int i = orderedRows.length - 1; i >= 0; i--) {
                mDownloadMgr.removeDownload(orderedRows[i]);
            }
            mainFrame.refreshAllActions();
        }
    }

    public void removeCompletedDownloads() {
        if (ServiceManager.sCfg.mDownloadAutoRemoveCompleted) {
            synchronized (mDownloadMgr) {
                mDownloadMgr.removeCompleted();
            }
            mainFrame.refreshAllActions();
        }
    }

    public void showDownloadLog() {
        DownloadFile download = getSelectedDownloadFile();
        if (download == null) {
            return;
        }
        DlgLog dlg = new DlgLog(mainFrame, "Download Log", "Log for " + download.getDestinationFileName(), download.getLog());
        dlg.setVisible(true);
    }

    public boolean isCandidateSelected() {
        if (!mainFrame.isDownloadTabSelected()) {
            return false;
        }
        int rowCount = mDownloadTable.getSelectedRowCount();
        if (rowCount <= 0) return false;
        int row = mDownloadCandidate.getSelectedRow();
        return (row != -1);
    }

    public void setCandidateCurrent() {
        DownloadFile download = getSelectedDownloadFile();
        if (download == null) {
            return;
        }
        if (download.getStatus() == DownloadFile.sConnecting || download.getStatus() == DownloadFile.sDownloading || download.getStatus() == DownloadFile.sRequestPushTransfer) {
            return;
        }
        int row = mDownloadCandidate.getSelectedRow();
        download.setCandidateCurrent(row);
    }

    public void removeCandidate() {
        DownloadFile download = getSelectedDownloadFile();
        if (download == null) {
            return;
        }
        int row = mDownloadCandidate.getSelectedRow();
        download.removeRemoteCandidate(row);
    }

    public void stopSearchCandidate() {
        DownloadFile download = getSelectedDownloadFile();
        if (download != null) {
            download.stopSearchForCandidates();
        }
    }

    private void updateCandidateLabel() {
        DownloadFile download = getSelectedDownloadFile();
        if (download == null) {
            return;
        }
        StringBuffer buffer = new StringBuffer("Download Candidates  ");
        ResearchSetting researchSetting = download.getResearchSetting();
        if (researchSetting.isSearchRunning()) {
            buffer.append("-  searching... ( " + researchSetting.getSearchHitCount() + " Hits )");
        } else {
            buffer.append("-  search stopped!");
        }
        mCandidateLabelStatus.setText(buffer.toString());
        mainFrame.refreshAllActions();
    }

    /**
     * Returns the selected download file with awarnes of the sorter.
     */
    public DownloadFile getSelectedDownloadFile() {
        int selIndex = mDownloadTable.getSelectedRow();
        if (selIndex < 0) {
            return null;
        }
        int index = mDownloadSorter.indexes[selIndex];
        if (index < 0) {
            return null;
        }
        return mDownloadMgr.getDownloadFileAt(index);
    }

    private void configureDownload() {
        DownloadFile dfile = getSelectedDownloadFile();
        if (dfile != null) {
            if (dfile.isDownloadInProgress()) {
                JOptionPane.showMessageDialog(this, Localizer.getString("NoConfigDownloadInProgress"), Localizer.getString("DownloadInProgress"), JOptionPane.WARNING_MESSAGE);
                return;
            }
            int oldStatus = dfile.getStatus();
            mDownloadMgr.stopDownload(dfile);
            DownloadConfigDialog dialog = new DownloadConfigDialog(dfile);
            dialog.show();
            dfile.setStatus(oldStatus);
        }
    }

    private class ButtonActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("ConfigureDownload")) {
                configureDownload();
            }
        }
    }

    private class DownloadFilesChangeHandler implements DownloadFilesChangeListener {

        /**
         * Called if a download file changed.
         */
        public void downloadFileChanged(int position) {
            if (mDownloadTable.getSelectedRow() == position) {
                updateCandidateLabel();
            }
        }

        /**
         * Called if a download file was added.
         */
        public void downloadFileAdded(int position) {
        }

        /**
         * Called if a download file was removed.
         */
        public void downloadFileRemoved(int position) {
        }
    }

    private class DownloadSelectionHandler implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                updateCandidateLabel();
                DownloadFile file = getSelectedDownloadFile();
                mDownloadCandidateModel.setCurrentDownloadFile(file);
                mainFrame.refreshAllActions();
            }
        }
    }

    private class CandidateButtonHandler implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd.equals("RemoveAll")) {
                DownloadFile file = getSelectedDownloadFile();
                file.removeAllRemoteCandidates();
            }
        }
    }

    private class StopDownloadHandler implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            stopDownload();
        }
    }

    private class ResumeDownloadHandler implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            resumeDownload();
        }
    }

    private class RemoveDownloadHandler implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            removeDownload();
        }
    }

    private class ShowDownloadLogHandler implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            showDownloadLog();
        }
    }

    private class SetCandidateCurrentHandler implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            setCandidateCurrent();
        }
    }

    private class RemoveCandidateHandler implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            removeCandidate();
        }
    }

    private class SearchCandidateHandler implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            DownloadFile download = getSelectedDownloadFile();
            if (download == null) {
                return;
            }
            download.startSearchForCandidates();
        }
    }

    private class StopSearchCandidateHandler implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            stopSearchCandidate();
        }
    }

    private class SelectionHandler implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            mainFrame.refreshAllActions();
        }
    }

    private class CellRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            this.setForeground(Color.black);
            if (table == mDownloadTable) {
                row = mDownloadSorter.indexes[row];
                DownloadFile download = mDownloadMgr.getDownloadFileAt(row);
                if (download == null) {
                    return this;
                }
                switch(download.getStatus()) {
                    case DownloadFile.sError:
                        this.setForeground(Color.darkGray);
                        break;
                    case DownloadFile.sDownloading:
                        this.setForeground(Color.red);
                        break;
                    case DownloadFile.sCompleted:
                        this.setForeground(Color.blue);
                        break;
                }
            }
            return this;
        }
    }
}
