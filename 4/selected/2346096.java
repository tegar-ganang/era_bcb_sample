package be.lassi.ui.patch;

import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import be.lassi.ui.util.ComponentUtil;
import be.lassi.ui.util.table.JCheckBoxTableCellEditor;
import be.lassi.ui.util.table.Table;
import be.lassi.util.Help;
import be.lassi.util.NLS;

/**
 * User interface that allows the user to assign channels
 * to dimmers.
 */
public class PatchView {

    private final PatchPresentationModel model;

    private Table channelTable;

    private Table detailTable;

    private JButton buttonPatch;

    private JButton buttonUnPatch;

    /**
     * Constructs a new instance.
     *
     * @param model
     */
    public PatchView(final PatchPresentationModel model) {
        this.model = model;
    }

    public JComponent build() {
        buildDetailTable();
        buildChannelTable();
        buttonPatch = ComponentUtil.buildIconButton(model.getActions().getActionPatch(), "left.gif");
        buttonUnPatch = ComponentUtil.buildIconButton(model.getActions().getActionUnPatch(), "right.gif");
        Help.enable(channelTable, "patch.tableChannels");
        Help.enable(detailTable, "patch.tablePatchDetails");
        JPanel panel = new JPanel();
        FormLayout layout = new FormLayout("pref:grow, 4dlu, pref, 4dlu, pref:grow", "pref, 4dlu, 25dlu, 4dlu, pref, 4dlu, pref, 4dlu, 25dlu:grow");
        PanelBuilder builder = new PanelBuilder(layout, panel);
        builder.setDefaultDialogBorder();
        CellConstraints cc = new CellConstraints();
        builder.addTitle(NLS.get("patch.dimmers.title"), cc.xy(1, 1));
        builder.add(createTopRightPanel(), cc.xy(5, 1));
        builder.add(new JScrollPane(detailTable), cc.xywh(1, 3, 1, 7));
        builder.add(buttonPatch, cc.xy(3, 5));
        builder.add(buttonUnPatch, cc.xy(3, 7));
        builder.add(new JScrollPane(channelTable), cc.xywh(5, 3, 1, 7));
        panel.setTransferHandler(new PatchFrameTransferHandler(model));
        Dimension d1 = new JButton("A").getPreferredSize();
        Dimension d2 = new Dimension(d1.height, d1.height);
        buttonPatch.setPreferredSize(d2);
        buttonUnPatch.setPreferredSize(d2);
        return panel;
    }

    private JComponent createTopRightPanel() {
        JPanel panel = new JPanel();
        FormLayout layout = new FormLayout("pref:grow, 4dlu, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout, panel);
        CellConstraints cc = new CellConstraints();
        builder.addTitle(NLS.get("patch.channels.title"), cc.xy(1, 1));
        builder.add(Help.createHelpButton(), cc.xy(3, 1));
        return panel;
    }

    private void buildDetailTable() {
        detailTable = new Table(model.getDetailTableModel());
        detailTable.setName("detailTable");
        detailTable.setSelectionModel(model.getDetailSelectionModel());
        detailTable.setColumnWidth(0, 5);
        detailTable.setColumnWidth(1, 5);
        detailTable.setColumnPreferredWidth(2, 15);
        detailTable.setColumnWidth(3, 10);
        detailTable.setColumnWidth(4, 5);
        detailTable.setColumnPreferredWidth(5, 15);
        detailTable.setPreferredRowCount(20);
        detailTable.setShowGrid(false);
        detailTable.setCellEditor(PatchDetail.ON, new JCheckBoxTableCellEditor());
        detailTable.setDragEnabled(true);
        detailTable.setTransferHandler(new PatchDetailTransferHandler(model));
    }

    private void buildChannelTable() {
        channelTable = new Table(model.getChannelTableModel());
        channelTable.setName("channelTable");
        channelTable.setSelectionModel(model.getChannelSelectionModel());
        channelTable.setColumnWidth(0, 5);
        channelTable.setColumnPreferredWidth(1, 15);
        channelTable.setColumnWidth(2, 10);
        channelTable.setPreferredRowCount(20);
        channelTable.setShowGrid(false);
        channelTable.setDragEnabled(true);
        channelTable.setTransferHandler(new PatchChannelTransferHandler(channelTable, model));
        channelTable.setToolTipText(NLS.get("patch.channels.tooltip"));
        channelTable.setColumnToolTipText(0, NLS.get("patch.channels.column.number.tooltip"));
        channelTable.setColumnToolTipText(1, NLS.get("patch.channels.column.name.tooltip"));
    }
}
