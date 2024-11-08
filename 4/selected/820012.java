package net.nsgenerator.generator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import net.nsgenerator.controller.Controller;
import net.nsgenerator.event.ProgressEvent;
import net.nsgenerator.gui.model.ImageObject;
import net.nsgenerator.gui.util.SwingUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.bushe.swing.event.EventBus;

public class Copier {

    private static final Logger logger = Logger.getLogger(Copier.class);

    private static final String IMAGEFOLDER = "images";

    private static final String CSSFOLDER = "templates/css";

    private static final String ICONFOLDER = "templates/icon";

    private static final String JSFOLDER = "templates/javascript";

    public void copyImages(String outputDir, List<ImageObject> imageList) {
        for (ImageObject imageObject : imageList) {
            copyFileToTargetFolder(outputDir, imageObject.getPath());
            EventBus.publish(new ProgressEvent());
        }
        copyFiles(CSSFOLDER, outputDir);
        copyFiles(ICONFOLDER, outputDir);
        copyFiles(JSFOLDER, outputDir);
    }

    private void copyFileToTargetFolder(String outputDir, String filename) {
        File sourceFile = new File(filename);
        File targetDir = new File(outputDir + "/" + IMAGEFOLDER);
        try {
            FileUtils.copyFileToDirectory(sourceFile, targetDir);
        } catch (IOException e) {
            logger.error("Could not copy file to dir", e);
            SwingUtils.showError(Controller.guiMessages.getString("error.copyFile"));
        }
    }

    private void copyFiles(String inputDir, String outputDir) {
        File targetDir = new File(outputDir);
        File sourceDir = new File(inputDir);
        try {
            FileUtils.copyDirectory(sourceDir, targetDir);
        } catch (IOException e) {
            logger.error("could not copy directory", e);
            SwingUtils.showError(Controller.guiMessages.getString("error.copyDir"));
        }
    }
}
