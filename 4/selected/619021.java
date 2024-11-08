package de.radis.io;

import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import de.radis.layout.RadisPanel;
import de.radis.util.Helper;

public class DnDManager extends DropTarget {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private final RadisPanel rp;

    public DnDManager(RadisPanel radisPanel) {
        rp = radisPanel;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void drop(DropTargetDropEvent arg0) {
        Helper.log().debug("Dropped");
        Transferable t = arg0.getTransferable();
        try {
            arg0.acceptDrop(arg0.getDropAction());
            List<File> filelist = (List<File>) t.getTransferData(t.getTransferDataFlavors()[0]);
            for (File file : filelist) {
                Helper.log().debug(file.getAbsolutePath());
                if (file.getName().toLowerCase().contains(".lnk")) {
                    Helper.log().debug(file.getName() + " is a link");
                    File target = new File(rp.getRoot().getFullPath() + "/" + file.getName());
                    Helper.log().debug("I have opened the mayor at " + target.getAbsolutePath());
                    FileOutputStream fo = new FileOutputStream(target);
                    FileInputStream fi = new FileInputStream(file);
                    int i = 0;
                    while (fi.available() > 0) {
                        fo.write(fi.read());
                        System.out.print(".");
                        i++;
                    }
                    Helper.log().debug(i + " bytes have been written to " + target.getAbsolutePath());
                    fo.close();
                    fi.close();
                }
            }
            rp.redraw();
        } catch (Throwable tr) {
            tr.printStackTrace();
        }
        Helper.log().debug(arg0.getSource().toString());
    }
}
