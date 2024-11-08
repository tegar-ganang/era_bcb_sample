package be.lassi.ui.patch;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import be.lassi.domain.Channel;

/**
 * Wrapper arround a collection of <code>Channel</code> objects to
 * support tranfer operations.  Both the generic 'string flavor' and a
 * special <code>Channel</code> flavor are supported.
 */
public class PatchChannelTransferable implements Transferable {

    /**
     * Specific flavor for channels.
     */
    public static final DataFlavor FLAVOR = new DataFlavor(Channel.class, "Lassi.Channels");

    /**
     * The supported flavors.
     */
    private static final DataFlavor[] FLAVORS = { DataFlavor.stringFlavor, FLAVOR };

    private final int[] selectedRows;

    private final PatchPresentationModel model;

    /**
     * Constructs a new instance.
     *
     * @param channels the channels to be transferred
     */
    public PatchChannelTransferable(final PatchPresentationModel model, final int[] selectedRows) {
        this.model = model;
        this.selectedRows = selectedRows;
    }

    /**
     * {@inheritDoc}
     */
    public DataFlavor[] getTransferDataFlavors() {
        return FLAVORS.clone();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDataFlavorSupported(final DataFlavor flavor) {
        boolean supported = false;
        for (int i = 0; !supported && i < FLAVORS.length; i++) {
            supported = flavor.equals(FLAVORS[i]);
        }
        return supported;
    }

    /**
     * {@inheritDoc}
     */
    public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        Object data = null;
        if (DataFlavor.stringFlavor.equals(flavor)) {
            data = string();
        } else if (FLAVOR.equals(flavor)) {
            data = this;
        }
        return data;
    }

    public int[] getSelectedRows() {
        return selectedRows;
    }

    /**
     * Formats the channels in a tab separated string.
     *
     * @return the channels string
     */
    private String string() {
        PatchChannelTableModel m = model.getChannelTableModel();
        StringBuilder b = new StringBuilder();
        for (int row : selectedRows) {
            Channel channel = m.getChannel(row);
            b.append(channel.getId() + 1);
            b.append('\t');
            b.append(channel.getName());
            b.append('\n');
        }
        return b.toString();
    }
}
