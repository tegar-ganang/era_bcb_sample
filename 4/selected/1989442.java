package com.nokia.ats4.appmodel.perspective.modeldesign.controller;

import com.nokia.ats4.appmodel.event.KendoEvent;
import com.nokia.ats4.appmodel.event.KendoEventListener;
import com.nokia.ats4.appmodel.grapheditor.event.InsertImageFromToolEvent;
import com.nokia.ats4.appmodel.main.swing.UiUtil;
import com.nokia.ats4.appmodel.util.Settings;
import com.nokia.ats4.appmodel.util.KendoResources;
import com.nokia.ats4.appmodel.util.FileAddedListener;
import com.nokia.ats4.appmodel.util.DirectoryChangePoller;
import com.nokia.ats4.appmodel.util.image.ImageData;
import com.nokia.ats4.appmodel.util.image.ImageGallery;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * InsertImageFromUiToolCommand
 * 
 * @author Esa-Matti Miettinen
 * @version $Revision: 2 $
 * 
 * This command aims to make it easier to import images for System Events
 * from another ui design tool. The command freezes Kendo ui 
 * launches the ui tool configured in Settings dialog and tries to
 * read new images created with the tool from a given directory.
 *  
 */
public class InsertImageFromUiToolCommand implements KendoEventListener, FileAddedListener {

    private InsertImageFromToolEvent event;

    private UiUtil ui;

    private Timer timer;

    private DirectoryChangePoller poller;

    private boolean finished;

    public InsertImageFromUiToolCommand(UiUtil ui, Timer timer) {
        if (timer == null) {
            this.timer = new Timer();
        } else {
            this.timer = timer;
        }
        this.ui = ui;
        this.finished = false;
    }

    public InsertImageFromUiToolCommand(UiUtil ui) {
        this(ui, null);
    }

    /**
     * Launch ui tool and poll a directory for a new image to be inserted
     * for the selected System Event. Kendo ui will be blocked during 
     * the processing.
     * @param event
     */
    @Override
    public void processEvent(KendoEvent event) {
        if (event instanceof InsertImageFromToolEvent) {
            this.finished = false;
            this.event = (InsertImageFromToolEvent) event;
            final String imagePath = Settings.getProperty("uiTool.imageDirectory");
            final String title = KendoResources.getString(KendoResources.INSERT_FROM_TOOL_TITLE);
            final String message = KendoResources.getString(KendoResources.INSERT_FROM_TOOL_MESSAGE) + " " + imagePath;
            File imageDir = new File(imagePath);
            if (imageDir.exists() && imageDir.isDirectory() && imageDir.canRead()) {
                final String uiAppPath = Settings.getProperty("uiTool.executable");
                try {
                    if ("".equals(uiAppPath.trim())) {
                        ui.showMessage(KendoResources.getString(KendoResources.UI_TOOL_INVALID_APPLICATION_PATH) + "\n" + uiAppPath);
                    } else {
                        this.ui.setCommand(this);
                        this.ui.launchApplication(Settings.getProperty("uiTool.executable"));
                        this.poller = new DirectoryChangePoller(imageDir);
                        this.poller.addListener(this);
                        this.timer.schedule(poller, 0, 2000);
                        this.ui.showModalDialog(title, message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    this.ui.showMessage(KendoResources.getString(KendoResources.UI_TOOL_LAUNCH_FAILED) + "\n" + e.getMessage());
                }
            } else {
                this.ui.showMessage(KendoResources.getString(KendoResources.UI_TOOL_INVALID_IMAGE_DIR) + "\n" + imagePath);
            }
        }
    }

    /**
     * Callback method for the poller. 
     * @param imgFile New file saved in the polled directory.
     */
    @Override
    public void fileAdded(File imgFile) {
        handleImageInsert(imgFile);
    }

    @Override
    public void fileModified(File modifiedFile) {
        handleImageInsert(modifiedFile);
    }

    @Override
    public void fileDeleted(File deletedFile) {
    }

    /**
     * Stop the polling, and cancel the command.
     */
    public void cancel() {
        this.poller.cancel();
        this.finished = true;
    }

    /**
     * If the file is valid image, insert it to the selected System Event.
     * 
     * @param imgFile image inserted in Ui Tool image directory.
     */
    private void handleImageInsert(File imgFile) {
        if (!this.finished && new FileNameExtensionFilter("", "jpg", "png", "gif", "jpeg").accept(imgFile)) {
            final String imageAddedMessage = KendoResources.getString(KendoResources.UI_TOOL_IMAGE_INSERTED_MESSAGE);
            final String variant = Settings.getProperty("language.variant");
            ImageData imgData = this.event.getSystemState().getImageData();
            ImageGallery gallery = imgData.getImageGallery();
            try {
                FileChannel channel = new RandomAccessFile(imgFile, "r").getChannel();
                FileLock lock = channel.lock(0, 0, true);
                File imported = gallery.importImage(imgFile);
                imgData.setImageFilename(variant, imported.getName());
                lock.release();
                channel.close();
                this.ui.closeModalDialog();
                this.poller.cancel();
                this.finished = true;
                this.ui.showMessage(imageAddedMessage + " " + this.event.getSystemState());
                this.ui.updateUi(this.event.getSource());
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
