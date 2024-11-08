package net.hawk.digiextractor.GUI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import net.hawk.digiextractor.digic.InfoBlockEntry;
import net.hawk.digiextractor.mpeg2.EventInformationTable;
import net.hawk.digiextractor.mpeg2.ServiceDescriptionTable;
import net.hawk.digiextractor.mpeg2.TransportStreamProgramMapSection;

/**
 * The Class Viewer.
 * Implements a simple application that can be used for debug purposes.
 * It displays the content of an information block saved to a file.
 */
public final class Viewer implements ListSelectionListener, ActionListener {

    /** The Marker for Stream ID blocks. */
    private static final int SID_TYPE = 0x0B;

    /** The number of bytes per int. */
    private static final int BYTES_PER_INT = 4;

    /** The marker for stream descriptor tables. */
    private static final int SDT_TYPE = 0x07;

    /** The marker for program map tables. */
    private static final int PMT_TYPE = 0x06;

    /** The marker for event information tables. */
    private static final int EIT_TYPE = 0x05;

    /** The Constant WINDOW_SIZE. */
    private static final Dimension WINDOW_SIZE = new Dimension(500, 500);

    /** The Number of clusters per MB (1024 KB / 64KB). */
    private static final int CLUSTERS_PER_MB = 16;

    /** The Constant STATS_SIZE. The number of streams we collect statistic
	 * data for. */
    private static final int STATS_SIZE = 8;

    /**
	 * The Class ViewerTableModel.
	 * The Table Model for the table displaying the entries in the information
	 * block.
	 */
    private class ViewerTableModel extends AbstractTableModel {

        /** The Constant TIMESTAMP_COLUMN. */
        private static final int TIMESTAMP_COLUMN = 5;

        /** The Constant PREV_ADDR_COLUMN. */
        private static final int PREV_ADDR_COLUMN = 4;

        /** The Constant LENGTH_COLUMN. */
        private static final int LENGTH_COLUMN = 3;

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 1L;

        /** The column names. */
        private String[] columnNames = { "Data", "Type", "Stream", "Length", "previous", "timestamp" };

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public int getRowCount() {
            return blocks.size();
        }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            InfoBlockEntry block = blocks.get(rowIndex);
            switch(columnIndex) {
                case 0:
                    return Integer.toHexString(block.getData());
                case 1:
                    return Integer.toHexString(block.getType());
                case 2:
                    return Integer.toHexString(block.getStreamID());
                case LENGTH_COLUMN:
                    return Integer.toHexString(block.getLength());
                case PREV_ADDR_COLUMN:
                    return Integer.toHexString(block.getPreviousAddress());
                case TIMESTAMP_COLUMN:
                    return Integer.toHexString(block.getTimestamp());
                default:
                    return 0;
            }
        }

        @Override
        public final String getColumnName(final int col) {
            return columnNames[col];
        }
    }

    /** The frame. */
    private JFrame frame;

    /** The main panel. */
    private JPanel mainPanel;

    /** The table displaying the entries. */
    private JTable table;

    /** A Byte buffer containing the information block. */
    private ByteBuffer buff;

    /** The info block entries. */
    private ArrayList<InfoBlockEntry> blocks;

    /** The model. */
    private ViewerTableModel model = new ViewerTableModel();

    /** A text area displaying additional information. */
    private JTextArea text;

    /** The stats. */
    private int[] stats = new int[STATS_SIZE];

    /**
	 * Instantiates a new viewer.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    private Viewer() throws IOException {
        for (int i = 0; i < stats.length; ++i) {
            stats[i] = 0;
        }
        frame = new JFrame("Viewer");
        final JFileChooser fc = new JFileChooser("F:\\DigicorderImages\\dump HDS2\\");
        int returnVal = fc.showOpenDialog(frame);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            System.exit(0);
        }
        FileInputStream fis = new FileInputStream(fc.getSelectedFile());
        FileChannel chan = fis.getChannel();
        buff = chan.map(MapMode.READ_ONLY, 0, fis.available());
        chan.close();
        fis.close();
        System.out.println("Size: " + buff.capacity());
        blocks = new ArrayList<InfoBlockEntry>();
        buff.getInt();
        buff.order(ByteOrder.BIG_ENDIAN);
        if (buff.getInt() != 1) {
            buff.order(ByteOrder.LITTLE_ENDIAN);
        }
        buff.rewind();
        InfoBlockEntry tmp;
        do {
            tmp = new InfoBlockEntry();
            tmp.parseData(buff);
            addToStats(tmp);
            blocks.add(tmp);
        } while (tmp.getType() != 0x02);
        System.out.println(blocks.size());
        System.out.println("STATISTICS:");
        for (int i = 0; i < stats.length; ++i) {
            System.out.println(i + ": " + stats[i] / CLUSTERS_PER_MB + " (" + stats[i] + ")");
        }
        try {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            mainPanel = new JPanel();
            mainPanel.setLayout(new BorderLayout());
            jbInit();
            frame.add(mainPanel);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        frame.pack();
        frame.validate();
        frame.setVisible(true);
    }

    /**
	 * Adds the to stats.
	 *
	 * @param tmp the tmp
	 */
    private void addToStats(final InfoBlockEntry tmp) {
        if (tmp.getType() == 0) {
            ++stats[tmp.getStreamID()];
        }
    }

    /**
	 * Jb init.
	 */
    private void jbInit() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 2));
        table = new JTable(model);
        table.setPreferredScrollableViewportSize(WINDOW_SIZE);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(this);
        JScrollPane scroll = new JScrollPane(table);
        panel.add(scroll);
        text = new JTextArea();
        text.setEditable(false);
        JScrollPane scroll2 = new JScrollPane(text);
        panel.add(scroll2);
        mainPanel.add(panel, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        JButton pPMT = new JButton("prev PMT");
        pPMT.addActionListener(this);
        pPMT.setActionCommand("pPMT");
        JButton nPMT = new JButton("next PMT");
        nPMT.addActionListener(this);
        nPMT.setActionCommand("nPMT");
        JButton pSDT = new JButton("prev SDT");
        pSDT.addActionListener(this);
        pSDT.setActionCommand("pSDT");
        JButton nSDT = new JButton("next SDT");
        nSDT.addActionListener(this);
        nSDT.setActionCommand("nSDT");
        JButton pEIT = new JButton("prev EIT");
        pEIT.addActionListener(this);
        pEIT.setActionCommand("pEIT");
        JButton nEIT = new JButton("next EIT");
        nEIT.addActionListener(this);
        nEIT.setActionCommand("nEIT");
        JButton save = new JButton("save EXT");
        save.addActionListener(this);
        save.setActionCommand("save");
        buttonPanel.add(pPMT);
        buttonPanel.add(nPMT);
        buttonPanel.add(pSDT);
        buttonPanel.add(nSDT);
        buttonPanel.add(pEIT);
        buttonPanel.add(nEIT);
        buttonPanel.add(save);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        table.setRowSelectionInterval(0, 0);
    }

    /**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public static void main(final String[] args) throws IOException {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                try {
                    new Viewer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void valueChanged(final ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        int index = ((ListSelectionModel) e.getSource()).getMinSelectionIndex();
        InfoBlockEntry entry = blocks.get(index);
        if (entry.getLength() == 0) {
            text.setText("no extended data");
        } else {
            switch(entry.getType()) {
                case EIT_TYPE:
                    EventInformationTable eit = new EventInformationTable(entry.getExtendedData());
                    text.setText(eit.toString());
                    break;
                case PMT_TYPE:
                    TransportStreamProgramMapSection pms = new TransportStreamProgramMapSection(entry.getExtendedData());
                    text.setText(pms.toString());
                    break;
                case SDT_TYPE:
                    ServiceDescriptionTable sdt = new ServiceDescriptionTable(entry.getExtendedData());
                    text.setText(sdt.toString());
                    break;
                case SID_TYPE:
                    StringBuffer sb = new StringBuffer();
                    while (entry.getExtendedData().remaining() >= BYTES_PER_INT) {
                        sb.append(Integer.toHexString(entry.getExtendedData().getInt()));
                        sb.append("\n");
                    }
                    text.setText(sb.toString());
                    break;
                default:
                    StringBuffer sbuf = new StringBuffer();
                    while (entry.getExtendedData().hasRemaining()) {
                        sbuf.append(String.format(" 0x%02X", entry.getExtendedData().get()));
                    }
                    text.setText(sbuf.toString());
            }
            entry.getExtendedData().rewind();
        }
        text.setCaretPosition(1);
    }

    /**
	 * Find next by type.
	 *
	 * @param type the type
	 * @return the int
	 */
    private int findNextByType(final int type) {
        int i = 0;
        for (i = table.getSelectedRow() + 1; i < blocks.size(); ++i) {
            if (blocks.get(i).getType() == type) {
                return i;
            }
        }
        return i - 1;
    }

    /**
	 * Find previous by type.
	 *
	 * @param type the type
	 * @return the int
	 */
    private int findPreviousByType(final int type) {
        int i = 0;
        for (i = table.getSelectedRow() - 1; i >= 0; --i) {
            if (blocks.get(i).getType() == type) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        int idx = 0;
        if (e.getActionCommand().equals("nEIT")) {
            idx = findNextByType(EIT_TYPE);
        } else if (e.getActionCommand().equals("nPMT")) {
            idx = findNextByType(PMT_TYPE);
        } else if (e.getActionCommand().equals("nSDT")) {
            idx = findNextByType(SDT_TYPE);
        } else if (e.getActionCommand().equals("pEIT")) {
            idx = findPreviousByType(EIT_TYPE);
        } else if (e.getActionCommand().equals("pPMT")) {
            idx = findPreviousByType(PMT_TYPE);
        } else if (e.getActionCommand().equals("pSDT")) {
            idx = findPreviousByType(SDT_TYPE);
        } else if (e.getActionCommand().equals("save")) {
            idx = table.getSelectedRow();
            final JFileChooser fc = new JFileChooser("F:\\DigicorderImages\\HD8S\\");
            int returnVal = fc.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                try {
                    FileOutputStream fos = new FileOutputStream(fc.getSelectedFile());
                    byte[] outp = new byte[blocks.get(idx).getExtendedData().capacity()];
                    blocks.get(idx).getExtendedData().get(outp);
                    fos.write(outp);
                    fos.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
        Rectangle rect = table.getCellRect(idx, 0, true);
        table.scrollRectToVisible(rect);
        table.setRowSelectionInterval(idx, idx);
    }
}
