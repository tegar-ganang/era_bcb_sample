package org.dancres.blitz.tools.dash;

import java.awt.Color;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import org.dancres.blitz.stats.InstanceCount;
import org.dancres.blitz.stats.MemoryStat;
import org.dancres.blitz.stats.OpStat;
import org.dancres.blitz.stats.Stat;
import org.dancres.blitz.stats.TxnStat;
import org.dancres.blitz.stats.BlockingOpsStat;
import org.dancres.blitz.stats.FieldsStat;

public class StatsFrame extends JDialog implements UpdateableView {

    private JLabel _status;

    private Thread _updater;

    private boolean _exitOnClose;

    private StatsTableModel _allStats = new StatsTableModel();

    private Map _lookup;

    private PieChart _piechart;

    private ChartPanel _chart = new ChartPanel();

    private int _mode;

    private boolean _closed;

    private TypeTreeView _treeTypeView = new TypeTreeView();

    private Object _adminProxy;

    private JFrame _frame;

    private JTextArea _textArea = new JTextArea();

    public static final int OPSTATS = 0;

    public static final int INSTANCES = 1;

    public static final int MEMORY = 2;

    public static final int TXNS = 3;

    public static final int BLOCKERS = 4;

    public static final int RAW = 5;

    public StatsFrame(JFrame parent, String title, int mode, Object adminProxy) {
        super(parent, title, false);
        _adminProxy = adminProxy;
        init(parent, title, mode);
    }

    public StatsFrame(JFrame parent, String title, int mode) {
        super(parent, title, false);
        init(parent, title, mode);
    }

    private void init(JFrame parent, String title, int mode) {
        _frame = parent;
        _exitOnClose = false;
        _mode = mode;
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent evt) {
                closeWin();
            }
        });
        getContentPane().add(createUI(), BorderLayout.CENTER);
        getContentPane().add(createStatusBar(), BorderLayout.SOUTH);
    }

    private void closeWin() {
        _closed = true;
        dispose();
        if (_exitOnClose) {
            System.exit(0);
        }
    }

    private JComponent createUI() {
        final JTable table = new JTable(_allStats);
        TableColumnModel tcm = table.getColumnModel();
        if (_mode == OPSTATS) {
            TableColumn tc = tcm.getColumn(1);
            tcm.removeColumn(tc);
        } else if (_mode == INSTANCES) {
            int nCols = table.getColumnCount();
            for (int i = 2; i < nCols; i++) {
                TableColumn tc = tcm.getColumn(2);
                tcm.removeColumn(tc);
            }
        }
        JTabbedPane tp = new JTabbedPane();
        if (_mode == OPSTATS) {
            _piechart = new PieChart();
            tp.add("Entry table", new JScrollPane(table));
            tp.add("Type tree", _treeTypeView);
            tp.add("Pie chart", _piechart);
            tp.add("History", _chart);
        } else if (_mode == INSTANCES) {
            tp.add("Entry table", new JScrollPane(table));
            tp.add("Type tree", _treeTypeView);
            tp.add("History", _chart);
            tp.add("Entry browser", new OutriggerViewer(this, (com.sun.jini.outrigger.JavaSpaceAdmin) _adminProxy));
        } else if (_mode == MEMORY || _mode == TXNS || _mode == BLOCKERS) {
            tp.add("History", _chart);
        } else if (_mode == RAW) {
            _textArea.setLineWrap(true);
            _textArea.setWrapStyleWord(true);
            tp.add("Raw", new JScrollPane(_textArea));
        }
        return tp;
    }

    private JComponent createStatusBar() {
        _status = new JLabel();
        JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.LEFT));
        p.add(_status);
        return p;
    }

    public boolean update(Stat[] stats) {
        if (_closed) {
            return false;
        }
        try {
            _lookup = new HashMap();
            long readCounter = 0;
            long writeCounter = 0;
            long takeCounter = 0;
            long instanceCounter = 0;
            double memoryUsed = 0;
            long txnCounter = 0;
            long blockingReads = 0;
            long blockingTakes = 0;
            StringBuffer myRaw = new StringBuffer();
            for (int i = 0; i < stats.length; i++) {
                myRaw.append(stats[i].toString() + "\n");
                if (stats[i] instanceof MemoryStat) {
                    MemoryStat ms = (MemoryStat) stats[i];
                    double max = ms.getMaxMemory();
                    double used = ms.getCurrentMemory();
                    memoryUsed = used;
                    double pc = used / max * 100;
                    _status.setText("Memory: " + (int) pc + "% used: " + (int) used + " max: " + (int) max);
                } else if (stats[i] instanceof OpStat) {
                    OpStat op = (OpStat) stats[i];
                    String type = op.getType();
                    int theOp = op.getOp();
                    Long count = new Long(op.getCount());
                    Object[] data = getData(type);
                    data[0] = type;
                    switch(theOp) {
                        case OpStat.READS:
                            data[2] = count;
                            readCounter += count.longValue();
                            break;
                        case OpStat.WRITES:
                            data[3] = count;
                            writeCounter += count.longValue();
                            break;
                        case OpStat.TAKES:
                            data[4] = count;
                            takeCounter += count.longValue();
                            break;
                    }
                } else if (stats[i] instanceof InstanceCount) {
                    InstanceCount myCount = (InstanceCount) stats[i];
                    String type = myCount.getType();
                    Object[] data = getData(type);
                    data[0] = type;
                    data[1] = new Integer(myCount.getCount());
                    instanceCounter += myCount.getCount();
                } else if (stats[i] instanceof TxnStat) {
                    TxnStat myTxns = (TxnStat) stats[i];
                    txnCounter = myTxns.getActiveTxnCount();
                } else if (stats[i] instanceof BlockingOpsStat) {
                    BlockingOpsStat myBlocks = (BlockingOpsStat) stats[i];
                    blockingReads = myBlocks.getReaders();
                    blockingTakes = myBlocks.getTakers();
                } else if (stats[i] instanceof FieldsStat) {
                    FieldsStat myFieldsStat = (FieldsStat) stats[i];
                    _treeTypeView.update(myFieldsStat.getType(), myFieldsStat.getFields());
                }
            }
            Collection col = _lookup.values();
            ArrayList list = new ArrayList();
            list.addAll(col);
            _allStats.update(list);
            if (_piechart != null) {
                _piechart.update((int) takeCounter, (int) writeCounter, (int) readCounter);
            }
            if (_mode == OPSTATS) {
                _chart.update(new String[] { "read", "write", "take" }, new long[] { readCounter, writeCounter, takeCounter });
            } else if (_mode == INSTANCES) {
                _chart.update(new String[] { "Instance count" }, new long[] { instanceCounter });
            } else if (_mode == MEMORY) {
                _chart.update(new String[] { "Memory usage KB" }, new long[] { (long) (memoryUsed / 1024) });
            } else if (_mode == TXNS) {
                _chart.update(new String[] { "Active Txns" }, new long[] { txnCounter });
            } else if (_mode == BLOCKERS) {
                _chart.update(new String[] { "read", "take" }, new long[] { blockingReads, blockingTakes });
            }
            _textArea.setText(myRaw.toString());
            _textArea.revalidate();
        } catch (Exception ex) {
            closeWin();
            DashBoardFrame.theLogger.log(Level.SEVERE, "Problem in update", ex);
        }
        return true;
    }

    private Object[] getData(String type) {
        Object[] data = (Object[]) _lookup.get(type);
        if (data == null) {
            data = new Object[] { "", new Long(0), new Long(0), new Long(0), new Long(0) };
            _lookup.put(type, data);
        }
        return data;
    }
}
