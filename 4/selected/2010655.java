package org.mmt.gui.processlist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import org.mmt.core.MysqlServer;
import org.mmt.core.ServerStatusListener;
import org.mmt.core.bean.ServerStatus;
import org.mmt.gui.Application;

public class ProcessListInstancePanel extends JPanel implements ServerStatusListener {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private ProcessListTableModel tableModel;

    private MysqlServer server;

    private JPopupMenu popup = new JPopupMenu();

    private JScrollPane jsp;

    public ProcessListInstancePanel(MysqlServer server) {
        super();
        this.server = server;
        this.server.addServerStatusListener(this);
        setLayout(new BorderLayout());
        tableModel = new ProcessListTableModel(server);
        final JTable table = new JTable(tableModel);
        table.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    Object pid = table.getValueAt(row, 0);
                    Object exec = table.getValueAt(row, 5);
                    Object o = table.getValueAt(row, 8);
                    if (pid == null) return;
                    String execTime = (exec != null) ? " execution time: " + exec.toString() : "";
                    String query = (o != null) ? o.toString() : "";
                    JDialog dialog = new JDialog();
                    dialog.setTitle("Query pid " + pid.toString() + execTime);
                    dialog.setAlwaysOnTop(true);
                    dialog.setLocation(e.getLocationOnScreen());
                    dialog.setSize(new Dimension(table.getWidth() / 2, 120));
                    JTextArea text = new JTextArea();
                    text.setText(query);
                    text.setEditable(false);
                    text.setLineWrap(true);
                    JScrollPane jsp = new JScrollPane(text);
                    dialog.getContentPane().add(jsp);
                    text.setCaretPosition(0);
                    dialog.setVisible(true);
                }
            }

            private void rightClick(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                table.setRowSelectionInterval(row, row);
                popup.show(table, e.getX(), e.getY());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) rightClick(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) rightClick(e);
            }
        });
        JMenuItem killItem = new JMenuItem("Kill this query");
        killItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                long pid = Long.parseLong(table.getValueAt(row, 0).toString());
                killQuery(pid);
            }
        });
        JMenuItem killUserItem = new JMenuItem("Kill this user");
        killUserItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                String user = table.getValueAt(row, 1).toString();
                int ris = JOptionPane.showConfirmDialog(null, "Are you sure you want to kill all the query of user " + user + "?", "Sure?", JOptionPane.YES_NO_OPTION);
                if (ris == JOptionPane.YES_OPTION) {
                    List<Long> pids = new ArrayList<Long>();
                    int rowNum = tableModel.getRowCount();
                    for (int i = 0; i < rowNum; i++) {
                        if (tableModel.getValueAt(i, 1).toString().equals(user)) pids.add(Long.parseLong(tableModel.getValueAt(i, 0).toString()));
                    }
                    killAllQueries(pids.toArray(new Long[0]));
                }
            }
        });
        JMenuItem exportProcesslistItem = new JMenuItem("Export processlist");
        exportProcesslistItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser();
                int ris = jfc.showSaveDialog(null);
                if (ris == JFileChooser.APPROVE_OPTION) {
                    File file = jfc.getSelectedFile();
                    if (file.exists()) {
                        int res = JOptionPane.showConfirmDialog(null, "The selected file already exists. Do you want to overwrite it?", "Sure?", JOptionPane.YES_NO_OPTION);
                        if (res != JOptionPane.YES_OPTION) {
                            return;
                        }
                    }
                    if (file != null) {
                        exportProcessList(file.getAbsolutePath());
                    }
                }
            }
        });
        popup.add(killItem);
        popup.add(killUserItem);
        popup.add(exportProcesslistItem);
        jsp = new JScrollPane(table);
        float[] dims = { 0.06f, 0.1f, 0.15f, 0.05f, 0.05f, 0.05f, 0.05f, 0.05f, 1.30f };
        int tot = table.getColumnModel().getTotalColumnWidth();
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth((int) (tot * dims[i]));
        }
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(table.getRowHeight() + 4);
        table.setGridColor(Color.white);
        List<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(7, SortOrder.DESCENDING));
        sortKeys.add(new RowSorter.SortKey(8, SortOrder.DESCENDING));
        table.getRowSorter().setSortKeys(sortKeys);
        add(jsp, BorderLayout.CENTER);
        onServerStatusChange(server);
    }

    public void popolateTable() {
        tableModel.refreshData();
    }

    public void killQuery(long pid) {
        try {
            server.killQuery(pid);
        } catch (Exception e) {
            Application.showError(e.getMessage());
        }
        popolateTable();
    }

    public void killAllQueries(Long[] pids) {
        if (pids == null) return;
        for (long pid : pids) {
            try {
                server.killQuery(pid);
            } catch (Exception e) {
                Application.showError(e.getMessage());
            }
        }
        popolateTable();
    }

    public void exportProcessList(String filename) {
        StringBuffer sb = new StringBuffer();
        int numCols = tableModel.getColumnCount();
        Object[][] data = tableModel.getData();
        int numRows = data.length;
        for (int i = 0; i < numCols; i++) {
            sb.append(tableModel.getColumnName(i) + "\t\t");
        }
        sb.deleteCharAt(sb.length() - 1);
        for (int i = 0; i < numRows; i++) {
            sb.append("\n");
            for (int j = 0; j < numCols; j++) {
                sb.append(data[i][j] + "\t\t");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        FileWriter fstream = null;
        BufferedWriter out = null;
        try {
            fstream = new FileWriter(filename);
            out = new BufferedWriter(fstream);
            out.write(sb.toString());
        } catch (Exception e) {
            Application.showError(e.getMessage());
        } finally {
            try {
                if (out != null) out.close();
                if (fstream != null) fstream.close();
            } catch (Exception e) {
                Application.showError(e.getMessage());
            }
        }
    }

    public void setAutoRefreshMode(boolean autoRefreshMode) {
        tableModel.setInAutoRefreshMode(autoRefreshMode);
    }

    @Override
    public void onServerStatusChange(MysqlServer server) {
        removeAll();
        if (server.getStatus() != ServerStatus.CONNECTED) {
            setLayout(new FlowLayout());
            add(new JLabel(Application.errorMessage));
        } else {
            setLayout(new BorderLayout());
            add(jsp, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }
}
