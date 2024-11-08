package be.lassi.ui.main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import be.lassi.context.ShowContext;
import be.lassi.domain.Channel;
import be.lassi.domain.Channels;
import be.lassi.ui.base.BasicPanel;

/**
 * 
 * 
 * 
 */
public class DimmersPanel extends BasicPanel {

    private JTable table;

    private DimmerTableModel tableModel;

    public DimmersPanel(final ShowContext context) {
        add(createButtonPanel(), BorderLayout.NORTH);
        add(createTable(context), BorderLayout.CENTER);
    }

    private void actionClearPatch() {
        int answer = JOptionPane.showConfirmDialog(this, "Are you sure you want to clear the entire patch?", "Warning", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            tableModel.clearPatch();
        }
    }

    private void actionDefaultPatch() {
        int answer = JOptionPane.showConfirmDialog(this, "Are you sure you want to reset the entire patch?", "Warning", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            tableModel.defaultPatch();
        }
    }

    private JComponent createButtonClearPatch() {
        JButton button = new JButton("ClearPatch");
        button.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionClearPatch();
            }
        });
        return button;
    }

    private JComponent createButtonDefaultPatch() {
        JButton button = new JButton("DefaultPatch");
        button.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionDefaultPatch();
            }
        });
        return button;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(createButtonClearPatch());
        panel.add(createButtonDefaultPatch());
        return panel;
    }

    private JComponent createTable(final ShowContext context) {
        tableModel = new DimmerTableModel(context);
        table = new JTable(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(20);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        JComboBox combo = new JComboBox();
        combo.setRenderer(new ChannelNameListCellRenderer());
        combo.addItem(new Channel(0, "not patched"));
        Channels channels = context.getShow().getChannels();
        for (int i = 0; i < channels.size(); i++) {
            combo.addItem(channels.get(i));
        }
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(combo));
        table.setDefaultRenderer(Channel.class, new ChannelNameTableCellRenderer());
        return new JScrollPane(table);
    }

    public Dimension getPreferredSize() {
        return new Dimension(300, 250);
    }
}
