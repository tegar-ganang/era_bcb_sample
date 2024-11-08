package net.afternoonsun.imaso.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Observable;
import org.apache.log4j.Logger;
import net.afternoonsun.imaso.ImasoLog;
import net.afternoonsun.imaso.common.Image;
import net.afternoonsun.imaso.common.Image.ImageState;
import net.afternoonsun.imaso.common.PEController;
import net.afternoonsun.imaso.common.PhotoEvent;
import net.afternoonsun.imaso.common.Profile;
import net.afternoonsun.imaso.core.database.ImageManager;
import net.afternoonsun.imaso.core.events.EventPlaceProgress;
import net.afternoonsun.imaso.core.events.EventTraverseProgress.TaskResult;

/**
 * Acts as the final step and places files where they belong. Tries to avoid
 * certain file-system problems if ROBUST mode is enabled. That, however, takes
 * some additional resources.
 *
 * @author Sergey Pisarenko aka drseergio (drseergio AT gmail DOT com)
 */
public class FileCopier extends Observable implements Worker {

    /** Log message for a failed copy. */
    private static final String WASNOTCOPIED = "' was not copied because destination folder '";

    /** Log message for a destination folder. */
    private static final String DESTINATIONFOLDER = "Destination folder '";

    /** Log message for an image file. */
    private static final String IMAGE = "Image file '";

    /** Get an instance of a logger. */
    private Logger logger = ImasoLog.getBatchLogger();

    /** Indicator whether the placing process should continue. */
    private boolean continuePlace;

    /**
     * Default public constructor.
     */
    public FileCopier() {
    }

    /**
     * Places images in the correct destination folders, right according to the
     * previously executed sorter.
     *
     * @param profile collection of necessary settings
     * @param event a photo event object with files that went through hell and
     * which are going to be placed
     * @param pe Photo Event Controller
     */
    public void placeImages(Profile profile, PhotoEvent event, PEController pe) {
        final ImageManager manager = getImageManager();
        continuePlace = true;
        Image image;
        final Iterator<Image> it = event.iterator();
        while (it.hasNext() && continuePlace) {
            image = it.next();
            final File finalDestinationFolder = getFile(image.getDestination());
            if (!finalDestinationFolder.exists()) {
                if (!finalDestinationFolder.mkdirs()) {
                    attemptRobustMode(profile, image, it, event, pe);
                } else {
                    logger.info(DESTINATIONFOLDER + finalDestinationFolder.getAbsolutePath() + "' successfully created.");
                }
            }
            final File destinationFile = findFilename(image);
            try {
                if (profile.isCopy()) {
                    copy(image.getFile(), destinationFile);
                } else {
                    move(image.getFile(), destinationFile);
                }
                if (profile.isManagement()) {
                    manager.putSorted(image);
                }
                image.setState(ImageState.PLACED);
                logger.info(IMAGE + image.getFilename() + "' is successfully copied to destination.");
                setChanged();
                notifyObservers(new EventPlaceProgress(image, TaskResult.OK));
            } catch (IOException e) {
                it.remove();
                pe.trashImage(image);
                logger.warn("File '" + image.getFilename() + "' copy has failed.");
            }
        }
    }

    /**
     * Notifies copier to stop.
     */
    @Override
    public void stop() {
        continuePlace = false;
    }

    /**
     * Abstracts File object creation for testing purposes.
     *
     * @param path desired File path
     * @return File object
     */
    @Override
    public File getFile(String path) {
        return new File(path);
    }

    /**
     * Determine the final destination filename. If a file exists with the same
     * name the filename shall be prepended with underscore (_) until it is
     * unique.
     *
     * @param image to be placed
     * @return File object of the final image position in the file-system
     */
    protected File findFilename(Image image) {
        final StringBuffer sb = new StringBuffer(image.getFilename());
        File destination = getFile(image.getDestination() + File.separator + sb);
        while (destination.exists()) {
            sb.insert(0, "_");
            logger.info("Chose filename '" + sb.toString() + "' because original '" + image.getFilename() + "' already existed.");
            destination = getFile(image.getDestination() + File.separator + sb);
        }
        return destination;
    }

    /**
     * Does what it says, copies files. Makes use of java NIO (new I/O) which is
     * included in JDK since 1.4. Mentioned somewhere that it will fail on 64Mb+
     * files under Windows. I guess that's more than enough for images as of
     * 2008.
     *
     * @param source file that needs to be copied
     * @param destination destination of the file
     * @throws java.io.IOException if any IO-related problems occur
     */
    protected void copy(File source, File destination) throws IOException {
        final FileChannel inChannel = new FileInputStream(source).getChannel();
        final FileChannel outChannel = new FileOutputStream(destination).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }
    }

    /**
     * Renames the specified source File object to the specified destination.
     *
     * @param source object to be renamed
     * @param destination new place of the object
     * @throws java.io.IOException if anything goes wrong with Java file IO
     */
    protected void move(File source, File destination) throws IOException {
        if (!source.renameTo(destination)) {
            throw new IOException();
        }
    }

    /**
     * A ROBUST mode-related method that tries to fix permissions in the
     * destination tree. For example, imagine you already happend to have
     * ~/images/2008/04 folder and the 04 folder happens to be non-accessible.
     * But the criteria dictates that 12 subfolder needs to be created. This
     * method will track all parents up to the destination to identify the
     * faulty member and try to fix it.
     *
     * @param destination final destination folder that needs to be created
     * @param path user-selected destination for all images
     * @return true if success, false if fail
     */
    protected boolean fixPermissions(File destination, File path) {
        final Deque<File> parents = new LinkedList<File>();
        File parent = destination.getParentFile();
        do {
            parents.addFirst(parent);
            parent = parent.getParentFile();
        } while (!parent.equals(path));
        while ((parent = parents.poll()) != null) {
            if (!parent.exists()) {
                break;
            }
            if (!parent.canRead() || !parent.canWrite() || !parent.canExecute()) {
                if (!parent.setWritable(true) || !parent.setReadable(true) || !parent.setExecutable(true)) {
                    return false;
                }
            }
        }
        return destination.mkdirs();
    }

    /**
     * A method that abstracts the image manager retrieval so that testing mock
     * ups would be possible.
     *
     * @return an instance of the image manager
     */
    protected ImageManager getImageManager() {
        return ImageManager.getInstance();
    }

    /**
     * Attempt to fix problems with the destination folder. If that fails the
     * supplied image shall be added to the garbage queue.
     *
     * @param profile collection of all settings
     * @param image an image object which is being subjected to destination
     * problems
     * @param it an iterator of the whole image collection
     * @param event collection of all photos
     * @param pe Photo Event Controller
     */
    protected void attemptRobustMode(Profile profile, Image image, Iterator it, PhotoEvent event, PEController pe) {
        final File finalDestinationFolder = getFile(image.getDestination());
        final File destinationFolder = profile.getDestination();
        if (profile.isRobust()) {
            if (fixPermissions(finalDestinationFolder, destinationFolder)) {
                logger.info(DESTINATIONFOLDER + finalDestinationFolder.getAbsolutePath() + "' originally could not be created but " + "permissions have been fixed.");
            } else {
                it.remove();
                pe.trashImage(image);
                logger.warn(IMAGE + image.getFilename() + WASNOTCOPIED + finalDestinationFolder.getAbsolutePath() + "' could not be created. Fix failed as well.");
            }
        } else {
            it.remove();
            pe.trashImage(image);
            logger.warn(IMAGE + image.getFilename() + WASNOTCOPIED + finalDestinationFolder.getAbsolutePath() + "' could not be created.");
        }
    }
}
