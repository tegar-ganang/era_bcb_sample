package com.inepex.ineForm.server.upload;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import javax.imageio.ImageIO;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.inepex.ineForm.server.util.StringUtil;
import com.inepex.ineFrame.server.util.OnDemandProperties;
import com.inepex.ineom.shared.IFConsts;

public class UploadProcessor {

    private final String DOT = ".";

    private OnDemandProperties props;

    private static final Logger _logger = LoggerFactory.getLogger(UploadProcessor.class);

    public UploadProcessor(OnDemandProperties props) {
        this.props = props;
    }

    public String storeImage(InputStream inStream, String fileName, boolean resize) throws Exception {
        Calendar rightNow = Calendar.getInstance();
        String dayNamedFolderName = "" + rightNow.get(Calendar.YEAR) + StringUtil.getPaddedIntWithZeros(2, rightNow.get(Calendar.MONTH) + 1) + StringUtil.getPaddedIntWithZeros(2, rightNow.get(Calendar.DATE));
        String uploadDirRoot = props.getProperty("uploaded.files.root");
        File file = new File(uploadDirRoot + System.getProperty("file.separator") + dayNamedFolderName);
        if (!file.exists()) file.mkdirs();
        String extension = FilenameUtils.getExtension(fileName);
        String outFileName;
        if (Boolean.parseBoolean(props.getPropertiesInstance().getProperty(IFConsts.USEORIGINALFILENAME, "true"))) {
            outFileName = StringUtil.removeSpecChars(StringUtil.unaccent(FilenameUtils.getBaseName(fileName)));
        } else {
            outFileName = StringUtil.hash(fileName + Long.toString(System.currentTimeMillis()));
        }
        if (Boolean.parseBoolean(props.getPropertiesInstance().getProperty(IFConsts.USEEXTENSION, "true"))) {
            outFileName = outFileName + DOT + extension;
        }
        String outPathAndName = uploadDirRoot + System.getProperty("file.separator") + dayNamedFolderName + System.getProperty("file.separator") + props.getProperty("uploaded.files.prefix") + outFileName;
        File uploadedFile = new File(outPathAndName);
        _logger.info("uploadedFile.getAbsolutePath() = {}", uploadedFile.getAbsolutePath());
        uploadedFile.createNewFile();
        OutputStream outStream = new FileOutputStream(outPathAndName);
        IOUtils.copyLarge(inStream, outStream);
        IOUtils.closeQuietly(inStream);
        outStream.close();
        if (resize) {
            writeResizedImage(outPathAndName, extension, "imgSize_xs");
            writeResizedImage(outPathAndName, extension, "imgSize_s");
            writeResizedImage(outPathAndName, extension, "imgSize_m");
            writeResizedImage(outPathAndName, extension, "imgSize_l");
            writeResizedImage(outPathAndName, extension, "imgSize_xl");
        }
        String retVal = dayNamedFolderName + "/" + props.getProperty("uploaded.files.prefix") + outFileName;
        return retVal;
    }

    private void writeResizedImage(String sourceFileName, String sourceExtension, String properyName) throws IOException {
        FileInputStream inputStream = new FileInputStream(sourceFileName);
        if (Boolean.parseBoolean(props.getPropertiesInstance().getProperty(IFConsts.USEEXTENSION, "true"))) {
            sourceFileName = sourceFileName.substring(0, sourceFileName.length() - (DOT.length() + sourceExtension.length()));
        }
        Integer targetWidth = Integer.parseInt(props.getProperty(properyName + ".width"));
        Integer targetHeight = Integer.parseInt(props.getProperty(properyName + ".height"));
        String targetSuffix = props.getProperty(properyName + ".suffix");
        BufferedImage sourceImg = ImageIO.read(inputStream);
        BufferedImage resizedImage = null;
        float sourceAspRat = sourceImg.getWidth() / (float) sourceImg.getHeight();
        float targetAspRat = targetWidth / (float) targetHeight;
        if (props.getProperty(properyName + ".crop").equals("true")) {
            if (sourceAspRat == targetAspRat) {
                resizedImage = ComponentHouseResizer.resize(sourceImg, targetWidth);
            } else {
                Rectangle cropRect = new Rectangle();
                if (targetAspRat > sourceAspRat) {
                    cropRect.width = sourceImg.getWidth();
                    cropRect.height = Math.round(cropRect.width * (1 / targetAspRat));
                    cropRect.x = 0;
                    cropRect.y = Math.round(sourceImg.getHeight() / 2f - cropRect.height / 2f);
                } else {
                    cropRect.height = sourceImg.getHeight();
                    cropRect.width = Math.round(cropRect.height * targetAspRat);
                    cropRect.x = Math.round(sourceImg.getWidth() / 2f - cropRect.width / 2f);
                    cropRect.y = 0;
                }
                resizedImage = ComponentHouseResizer.cutAndResize(sourceImg, cropRect, targetWidth);
            }
        } else {
            if (targetAspRat > sourceAspRat) {
                targetWidth = Math.round(sourceAspRat * targetHeight);
            }
            resizedImage = ComponentHouseResizer.resize(sourceImg, targetWidth);
        }
        String endFileName = sourceFileName + targetSuffix;
        if (Boolean.parseBoolean(props.getPropertiesInstance().getProperty(IFConsts.USEEXTENSION, "true"))) {
            endFileName = endFileName + DOT + sourceExtension;
        }
        FileOutputStream outputStream = new FileOutputStream(endFileName);
        ImageIO.write(resizedImage, sourceExtension, outputStream);
    }
}
