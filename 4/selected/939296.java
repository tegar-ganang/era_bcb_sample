package be.lassi.ui.main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import be.lassi.domain.Channel;
import be.lassi.domain.Channels;
import be.lassi.ui.base.BasicPanel;

/**
 * Panel with table of {@link be.lassi.domain.Channel Channel} objects.
 * The table has two columns, showing "channel id" and "channel name".
 *
 */
public class ChannelsPanel extends BasicPanel {

    private JTable table;

    private ChannelTableModel tableModel;

    private int rowSelectedAtMousePressed = -1;

    /**
     * Method ChannelsPanel.
     * @param channels
     */
    public ChannelsPanel(final Channels channels) {
        add(createTable(channels), BorderLayout.CENTER);
    }

    /**
     * Method createTable.
     * @param channels
     * @return JComponent
     */
    private JComponent createTable(final Channels channels) {
        tableModel = new ChannelTableModel(channels);
        table = new JTable(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(2);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(final ListSelectionEvent e) {
            }
        });
        table.addMouseListener(new MouseAdapter() {

            public void mousePressed(final MouseEvent e) {
                rowSelectedAtMousePressed = table.getSelectedRow();
            }

            public void mouseReleased(final MouseEvent e) {
                int index = table.getSelectedRow();
                if (index != -1) {
                    if (rowSelectedAtMousePressed != index) {
                        Channel channel1 = tableModel.getChannels().get(index);
                        Channel channel2 = tableModel.getChannels().get(rowSelectedAtMousePressed);
                        String name = channel1.getName();
                        channel1.setName(channel2.getName());
                        channel2.setName(name);
                    }
                }
                rowSelectedAtMousePressed = -1;
            }
        });
        return new JScrollPane(table);
    }

    /**
     * {@inheritDoc}
     */
    public Dimension getPreferredSize() {
        return new Dimension(200, 250);
    }
}
