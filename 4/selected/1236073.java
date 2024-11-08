package net.assimilator.utilities.console.dnd;

import net.assimilator.tools.webster.Webster;
import net.assimilator.tools.webster.WebsterAdmin;
import net.assimilator.utility.discovery.ServiceDiscoverer;
import net.assimilator.utility.discovery.filter.WebsterServiceFilter;
import net.jini.core.lookup.ServiceItem;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Defines the behavior of the a drop target for files. Dragging a file object
 * to and dropping on the defined target causes the listener methods to be executed.
 *
 * @author Kevin Hartig
 * @version $Id: FileDropTarget.java 365 2007-11-08 15:32:57Z khartig $
 */
public class FileDropTarget implements DropTargetListener {

    Component target;

    DropTarget dropTarget;

    ServiceDiscoverer serviceDiscoverer;

    static DataFlavor fileDataFlavor;

    static {
        try {
            fileDataFlavor = new DataFlavor("application/x-java-file-list; class=java.util.List");
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
    }

    private static final Logger logger = Logger.getLogger("net.assimilator.utilities.console");

    /**
     * Constructor to set up a file drop target.
     *
     * @param target            The UI component to define as a drop target.
     * @param serviceDiscoverer Discovery object used to find code server instances.
     */
    public FileDropTarget(Component target, ServiceDiscoverer serviceDiscoverer) {
        this.target = target;
        dropTarget = new DropTarget(target, this);
        this.serviceDiscoverer = serviceDiscoverer;
    }

    public void dragEnter(DropTargetDragEvent dtde) {
        logger.finest("dragEnter event on target");
    }

    public void dragExit(DropTargetEvent dte) {
        logger.finest("dragExit event on target");
    }

    public void dragOver(DropTargetDragEvent dtde) {
        logger.finest("dragOver event on target");
    }

    public void drop(DropTargetDropEvent dtde) {
        boolean gotData = false;
        logger.finer("drop event on target");
        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        Transferable transferable = dtde.getTransferable();
        dumpDataFlavors(transferable);
        DataFlavor[] flavors = transferable.getTransferDataFlavors();
        for (DataFlavor flavor : flavors) {
            if (transferable.isDataFlavorSupported(fileDataFlavor) && flavor.equals(fileDataFlavor)) {
                logger.finer("Got a drop for a file");
                gotData = deployService(transferable);
            } else {
                logger.warning("Drop item not recognized or not supported. " + flavor);
            }
        }
        dtde.dropComplete(gotData);
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
        logger.finest("dropActionChanged event on target");
    }

    /**
     * Copies the dropped files into the service deployment directory causing the
     * services to be deployed.
     *
     * @param transferable Contains the data for the trafer operation.
     * @return true if the drop was successful, false otherwise.
     */
    private boolean deployService(Transferable transferable) {
        java.util.List files;
        try {
            ServiceItem serviceItem = serviceDiscoverer.cacheLookup(new WebsterServiceFilter(null, Webster.class));
            Webster webster = (Webster) serviceItem.service;
            WebsterAdmin websterAdmin = (WebsterAdmin) webster.getAdmin();
            String deploymentDirectory = websterAdmin.getDeploymentDirectory();
            logger.finer("Webster deployment directory = " + deploymentDirectory);
            files = (java.util.List) transferable.getTransferData(fileDataFlavor);
            for (Object obj : files) {
                File file = (File) obj;
                if (!file.isDirectory()) {
                    logger.info(file.getAbsolutePath());
                    copyFile(new File(file.getAbsolutePath()), new File(deploymentDirectory + "/" + file.getName()));
                } else {
                    logger.warning("File is a directory. Not handling directories.");
                }
            }
            return true;
        } catch (UnsupportedFlavorException e) {
            logger.log(Level.WARNING, e.getMessage(), e.getCause());
            e.printStackTrace();
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e.getCause());
            e.printStackTrace();
        }
        return false;
    }

    private void dumpDataFlavors(Transferable transferable) {
        logger.finest("Data Flavors:");
        DataFlavor[] flavors = transferable.getTransferDataFlavors();
        for (int i = 0; i < flavors.length; i++) {
            logger.finest("*** " + i + ": " + flavors[i]);
        }
    }

    /**
     * Copy file from defined input location to output location.
     *
     * @param in Input file
     * @param out Output file
     * @throws IOException if file fails to copy successfully.
     */
    private static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }
}
