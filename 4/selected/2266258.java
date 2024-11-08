package gov.sns.xal.tools.widgets;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelRecord;
import gov.sns.ca.IEventSinkValue;
import gov.sns.ca.Monitor;
import gov.sns.xal.smf.impl.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Vector;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * @author sako
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class XALMagnetControlPanel extends XALAbstractControlPanel implements IEventSinkValue {

    Electromagnet magnet;

    Channel ch;

    boolean statx, staty;

    public XALMagnetControlPanel(Electromagnet magnett) {
        super(magnett.getId());
        magnet = magnett;
        stat = false;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        nrow = 1;
        ncolumn = 3;
        String[] hstr = { "channel", "read", "write" };
        String tag[] = { "BField" };
        Vector<Vector<String>> data = new Vector<Vector<String>>();
        for (int ir = 0; ir < nrow; ir++) {
            Vector<String> v = new Vector<String>();
            stat = true;
            for (int ic = 0; ic < ncolumn; ic++) {
                if (ic == 0) {
                    v.addElement(tag[ir]);
                } else {
                    v.addElement("");
                }
            }
            data.addElement(v);
        }
        Vector<String> header = new Vector<String>(Arrays.asList(hstr));
        table = new JTable(data, header);
        DefaultTableCellRenderer dtcr1 = new DefaultTableCellRenderer();
        dtcr1.setHorizontalAlignment(JLabel.RIGHT);
        DefaultTableCellRenderer dtcr2 = new DefaultTableCellRenderer();
        dtcr2.setBackground(Color.lightGray);
        dtcr2.setForeground(Color.blue);
        dtcr2.setHorizontalAlignment(JLabel.CENTER);
        JTextField tf = new JTextField("");
        tf.setEditable(false);
        DefaultCellEditor dce = new DefaultCellEditor(tf);
        table.setSize(20 + 30 + 30, 10);
        for (int ic = 0; ic < ncolumn; ic++) {
            if (ic == 0) {
                table.getColumnModel().getColumn(ic).setCellRenderer(dtcr2);
                table.getColumnModel().getColumn(ic).setCellEditor(dce);
                table.getColumnModel().getColumn(ic).setMinWidth(20);
            } else if (ic == 1) {
                table.getColumnModel().getColumn(ic).setCellRenderer(dtcr1);
                table.getColumnModel().getColumn(ic).setCellEditor(dce);
                table.getColumnModel().getColumn(ic).setMinWidth(30);
            } else {
                table.getColumnModel().getColumn(ic).setCellRenderer(dtcr1);
                table.getColumnModel().getColumn(ic).setMinWidth(30);
            }
        }
        JScrollPane sp = new JScrollPane();
        sp.setSize(new Dimension(150, 40));
        sp.getViewport().setView(table);
        sp.getViewport().setSize(new Dimension(150, 40));
        JPanel p = new JPanel();
        p.add(sp);
        getContentPane().add(p, BorderLayout.CENTER);
        connectChannel();
        readChannel();
        pack();
        setVisible(true);
        table.getModel().addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                if (e.getColumn() == 2) {
                    System.out.println("column =" + e.getColumn());
                    System.out.println("firstr, lastr =" + e.getFirstRow() + " " + e.getLastRow());
                    writeChannel();
                }
            }
        });
    }

    @Override
    public void readChannel() {
        stat = true;
        try {
            System.out.println("read B start");
            double b = magnet.getField();
            System.out.println("read B end, b =" + b);
            table.getModel().setValueAt(String.valueOf(b), 0, 1);
            System.out.println("fill B end");
        } catch (Exception e) {
            System.out.println("failed in reading B");
        }
    }

    @Override
    public void writeChannel() {
        double newB = Double.parseDouble((String) table.getModel().getValueAt(0, 2));
        System.out.println("newB = " + newB);
        try {
            magnet.setField(newB);
            System.out.println("readback, b = " + magnet.getField());
        } catch (Exception e) {
            System.out.println("failed in writing B");
        }
        readChannel();
    }

    @Override
    public void writeChannel(double v) {
    }

    @Override
    public void connectChannel() {
        try {
            ch = magnet.getAndConnectChannel(Electromagnet.FIELD_RB_HANDLE);
        } catch (Exception e) {
            System.out.println("failed in connecting " + ch.channelName());
        }
        try {
            ch.addMonitorValue(this, Monitor.VALUE);
        } catch (Exception e) {
            System.out.println("caught magnet monitor exception");
        }
    }

    public void eventValue(ChannelRecord record, Channel chan) {
        String chanId = chan.channelName();
        readChannel();
    }
}
