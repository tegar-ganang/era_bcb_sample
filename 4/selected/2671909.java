package be.lassi.ui.patch;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;
import be.lassi.domain.Channel;
import be.lassi.domain.Dimmer;

/**
 * Wrapper arround a collection of <code>PatchDetail</code> objectss to
 * support tranfer operations.  Both the generic 'string flavor' and a 
 * special <code>PatchDetail</code> flavor are supported. 
 */
public class PatchDetailTransferable implements Transferable {

    /**
     * Specific flavor for patch details object.
     */
    public static final DataFlavor FLAVOR = new DataFlavor(PatchDetail.class, "Lassi.PatchDetails");

    /**
     * The supported flavors.
     */
    private static final DataFlavor[] FLAVORS = { DataFlavor.stringFlavor, FLAVOR };

    /**
     * The patch details to be transferred.
     */
    private List<PatchDetail> patchDetails;

    /**
     * Constructs a new instance.
     * 
     * @param patchDetails the patch details to be transferred
     */
    public PatchDetailTransferable(final List<PatchDetail> patchDetails) {
        this.patchDetails = patchDetails;
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
        }
        return data;
    }

    /**
     * Formats the patch details in a tab separated string.
     * 
     * @return the patch details string
     */
    private String string() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < patchDetails.size(); i++) {
            PatchDetail detail = patchDetails.get(i);
            Dimmer dimmer = detail.getDimmer();
            Channel channel = dimmer.getChannel();
            b.append(dimmer.getId() + 1);
            b.append('\t');
            b.append(dimmer.getName());
            b.append('\t');
            if (channel != null) {
                b.append(channel.getId() + 1);
            }
            b.append('\t');
            if (channel != null) {
                b.append(channel.getName());
            }
            b.append('\n');
        }
        return b.toString();
    }
}
