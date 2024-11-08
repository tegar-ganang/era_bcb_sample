package net.nsgenerator.generator;

import java.io.File;
import java.io.IOException;
import net.nsgenerator.gui.model.ImageObject;
import org.apache.commons.io.FileUtils;

public class NormalCopier extends AbstractCopier {

    private static final String IMAGEFOLDER = "images";

    @Override
    public void copyImage(ImageObject image, String outputDir) {
        File sourceFile = new File(image.getPath());
        File targetDir = new File(outputDir + "/" + IMAGEFOLDER);
        try {
            FileUtils.copyFileToDirectory(sourceFile, targetDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
