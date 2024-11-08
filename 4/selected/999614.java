package com.gamalocus.sgs.profile.viewer.data_loader;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import com.gamalocus.sgs.profile.listener.report.RawProfileReport;
import com.gamalocus.sgs.profile.listener.report.RawPropertyChangeEvent;
import com.gamalocus.sgs.profile.listener.report.RawTransactionId;
import com.gamalocus.sgs.profile.viewer.ProfileViewer;
import com.gamalocus.sgs.profile.viewer.ProfileViewer.ProfileReportKey;
import com.gamalocus.sgs.profile.viewer.util.DateUtil;
import com.gamalocus.sgs.profile.viewer.util.TypedTableModel;

public class DataLoader extends JDialog {

    private static final long serialVersionUID = -6869192554411119160L;

    private ConcurrentSkipListSet<ProfileReportKey> reports = new ConcurrentSkipListSet<ProfileReportKey>();

    private Hashtable<RawTransactionId, RawProfileReport> backlog = new Hashtable<RawTransactionId, RawProfileReport>();

    private long beginning_of_time = Long.MAX_VALUE;

    private long end_of_time = 0;

    Properties sgsProperties = new Properties();

    private ProfileViewer viewer;

    private JTable fileTable;

    public DataLoader(ProfileViewer viewer, final File outputDirectory) {
        super(viewer, "Load Profiling Data (select files you want to load)", true);
        setResizable(false);
        this.viewer = viewer;
        setLayout(new BorderLayout(2, 2));
        add(new JScrollPane(fileTable = new JTable()), BorderLayout.NORTH);
        fileTable.setPreferredScrollableViewportSize(new Dimension(700, 500));
        fileTable.setAutoCreateRowSorter(true);
        JPanel bottomPanel;
        add(bottomPanel = new JPanel(), BorderLayout.SOUTH);
        bottomPanel.setLayout(new GridLayout(1, 2));
        final File[] outFiles = getOutputFiles(outputDirectory).toArray(new File[0]);
        Object[][] tableData = new Object[outFiles.length][5];
        int i = 0;
        for (File f : outFiles) {
            String parts[] = f.getName().split("[_\\.]");
            long from = Long.parseLong(parts[1]);
            long to = Long.parseLong(parts[2]);
            tableData[i][0] = parts[4];
            tableData[i][1] = DateUtil.getTime(from);
            tableData[i][2] = DateUtil.getTime(to);
            tableData[i][3] = new FileHolder(f);
            tableData[i][4] = f;
            i++;
        }
        fileTable.setModel(new TypedTableModel(tableData, new String[] { "Thread", "From", "To", "Size" }, new Class<?>[] { String.class, String.class, String.class, FileHolder.class }));
        fileTable.selectAll();
        bottomPanel.add(new JButton("Load") {

            @Override
            protected void fireActionPerformed(ActionEvent event) {
                Collection<File> outputFiles = new ArrayList<File>();
                for (int i : fileTable.getSelectedRows()) {
                    outputFiles.add(((FileHolder) fileTable.getValueAt(i, 3)).file);
                }
                loadProfileData(outputFiles, new File(outputDirectory, "sgs.properties"));
                DataLoader.this.setVisible(false);
            }
        });
        bottomPanel.add(new JButton("Cancel") {

            @Override
            protected void fireActionPerformed(ActionEvent event) {
                DataLoader.this.setVisible(false);
            }
        });
        pack();
    }

    private void loadProfileData(Collection<File> outputFiles, File sgsProps) {
        try {
            sgsProperties.load(new FileInputStream(sgsProps));
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        int files_read = 0;
        Cursor oldCursor = getCursor();
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        for (File file : outputFiles) {
            files_read++;
            loadProfileDataFile(file);
        }
        setCursor(oldCursor);
        viewer.setNewData(reports, backlog, beginning_of_time, end_of_time, sgsProperties, outputFiles.size());
    }

    /**
	 * Get the output files (*.output) sorted by name.
	 * 
	 * @param outputDirectory
	 * @return
	 */
    private Collection<File> getOutputFiles(File outputDirectory) {
        Map<String, File> retVal = new TreeMap<String, File>();
        for (File file : outputDirectory.listFiles()) {
            if (file.isFile()) {
                if (file.getName().endsWith(".output")) {
                    retVal.put(file.getName(), file);
                }
            }
        }
        return retVal.values();
    }

    private void loadProfileDataFile(File file) {
        FileInputStream fileIn;
        try {
            ObjectInputStream in = new ObjectInputStream(fileIn = new FileInputStream(file));
            FileChannel fileChannel = fileIn.getChannel();
            while (fileChannel.position() < fileChannel.size()) {
                Object obj = null;
                try {
                    obj = in.readObject();
                    if (obj instanceof RawProfileReport) {
                        RawProfileReport r = (RawProfileReport) obj;
                        reports.add(new ProfileReportKey(r.getStartTime(), true, r));
                        reports.add(new ProfileReportKey(r.getEndTime(), false, r));
                        if (r.wasTransactional) {
                            backlog.put(r.txnId, r);
                        }
                        beginning_of_time = Math.min(beginning_of_time, r.getStartTime());
                        end_of_time = Math.max(end_of_time, r.getEndTime());
                    } else if (obj instanceof RawPropertyChangeEvent) {
                        RawPropertyChangeEvent event = (RawPropertyChangeEvent) obj;
                        System.out.println("event: " + event);
                    } else {
                        System.out.println("Unknown obj: " + obj + "[" + obj.getClass() + "]");
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class FileHolder implements Comparable<FileHolder> {

    final File file;

    final String asString;

    FileHolder(File f) {
        file = f;
        if (f.length() > 1024 * 1024) {
            asString = (f.length() / (1024 * 1024)) + "M";
        } else if (f.length() > 1024) {
            asString = (f.length() / (1024)) + "K";
        } else {
            asString = f.length() + "bytes";
        }
    }

    @Override
    public int compareTo(FileHolder o) {
        return (int) (o.file.length() - file.length());
    }

    @Override
    public String toString() {
        return asString;
    }
}
