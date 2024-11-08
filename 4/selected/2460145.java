package be.lassi.ui.patch;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import be.lassi.ui.util.table.Table;

/**
 * Handles the transfer of channel data to and from the channel
 * table in the patch frame.
 */
public class PatchChannelTransferHandler extends TransferHandler {

    private final PatchPresentationModel model;

    private final Table channelTable;

    /**
     * Constructs a new transfer handler.
     *
     * @param model the patch model
     */
    public PatchChannelTransferHandler(final Table channelTable, final PatchPresentationModel model) {
        this.channelTable = channelTable;
        this.model = model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Transferable createTransferable(final JComponent c) {
        int[] rows = channelTable.getSelectedRows();
        return new PatchChannelTransferable(model, rows);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSourceActions(final JComponent c) {
        return COPY_OR_MOVE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean importData(final JComponent c, final Transferable t) {
        boolean dropOk = true;
        boolean patchDrop = t.isDataFlavorSupported(PatchDetailTransferable.FLAVOR);
        if (patchDrop) {
            model.unpatch();
        } else {
            boolean channelDrop = t.isDataFlavorSupported(PatchChannelTransferable.FLAVOR);
            if (channelDrop) {
                Table table = (Table) c;
                int[] rows = table.getSelectedRows();
                if (rows.length != 1) {
                    throw new AssertionError("Expected 1 selected row, found: " + rows.length);
                }
                int target = rows[0];
                try {
                    PatchChannelTransferable pct = (PatchChannelTransferable) t.getTransferData(PatchChannelTransferable.FLAVOR);
                    int[] sourceChannels = pct.getSelectedRows();
                    model.getChannelTableModel().shift(target, sourceChannels);
                } catch (UnsupportedFlavorException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return dropOk;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canImport(final JComponent c, final DataFlavor[] flavors) {
        boolean canImport = false;
        for (int i = 0; !canImport && i < flavors.length; i++) {
            canImport = DataFlavor.stringFlavor.equals(flavors[i]) || PatchDetailTransferable.FLAVOR.equals(flavors[i]);
        }
        return canImport;
    }
}
