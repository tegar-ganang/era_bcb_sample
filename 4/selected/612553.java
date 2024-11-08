package net.sf.poormans.tool.image;

import java.io.File;
import net.sf.poormans.Constants;
import net.sf.poormans.configuration.InitializationManager;
import net.sf.poormans.exception.FatalException;
import net.sf.poormans.tool.ckeditor.CKImageResource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

/**
 * Tool which contains all image processing functions.<br>
 * Managed by spring!
 *
 * @version $Id: ImageRenderTool.java 1912 2010-01-16 18:11:58Z th-schwarz $
 * @author <a href="mailto:th-schwarz@users.sourceforge.net">Thilo Schwarz</a>
 */
public class ImageRenderTool implements IImageTool {

    private static Logger logger = Logger.getLogger(ImageRenderTool.class);

    private IImageInfo imageInfo;

    private IImageManipulation imageManipulation;

    public void setImageInfo(final IImageInfo imageInfo) {
        this.imageInfo = imageInfo;
    }

    public void setImageManipulation(final IImageManipulation imageManipulation) {
        this.imageManipulation = imageManipulation;
    }

    public void createCashedImage(final CKImageResource imageFile) {
        File cachedFile = imageFile.getCacheFile();
        File srcFile = imageFile.getFile();
        if (cachedFile.exists() && srcFile.lastModified() <= cachedFile.lastModified()) {
            logger.info("Image exists and isn't modified, don't need to render again: ".concat(cachedFile.getAbsolutePath()));
            return;
        }
        if (!cachedFile.getParentFile().exists()) cachedFile.getParentFile().mkdirs();
        try {
            if (!InitializationManager.isRenderingPossible()) {
                logger.warn("Properties for imagemagick aren't set, so I can't render!");
                if (!cachedFile.exists()) {
                    FileUtils.copyFile(srcFile, cachedFile);
                    logger.debug("File successfull copied.");
                }
            } else {
                imageManipulation.resizeImage(srcFile, cachedFile, imageFile.getDimension());
            }
        } catch (Exception e) {
            throw new FatalException("Error while resizing image!", e);
        }
    }

    public File getDialogPreview(final File srcImageFile, final Dimension previewDimension) {
        if (srcImageFile == null) return null;
        File destImageFile = new File(Constants.TEMP_DIR, "swt_dialog_preview".concat(File.separator).concat(FilenameUtils.getName(srcImageFile.getAbsolutePath())));
        if (!destImageFile.getParentFile().exists()) destImageFile.getParentFile().mkdirs();
        try {
            if (destImageFile.exists()) destImageFile.delete();
            imageManipulation.resizeImage(srcImageFile, destImageFile, previewDimension, false);
        } catch (Exception e) {
            throw new FatalException("Error while resizing image!", e);
        }
        return destImageFile;
    }

    public Dimension getDimension(final File imageFile) {
        return imageInfo.getDimension(imageFile);
    }

    public String getSupportedImageExtensions() {
        return imageManipulation.getSupportedImageExtensions();
    }

    public boolean allRequirementsFulfilled() {
        return imageManipulation.allRequirementsFulfilled();
    }
}
