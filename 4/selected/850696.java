package de.mpiwg.vspace.images.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.eclipse.core.runtime.Path;
import de.mpiwg.vspace.common.project.ProjectManager;
import de.mpiwg.vspace.extension.ExceptionHandlingService;
import de.mpiwg.vspace.images.core.ImageImpl;
import de.mpiwg.vspace.images.db.DbEntryProvider;
import de.mpiwg.vspace.images.util.PropertyHandler;

public class DatabaseServiceImpl implements DatabaseService {

    protected DatabaseServiceImpl() {
    }

    public Image storeImage(String title, String pathToImage, Map<String, Object> additionalProperties) {
        File collectionFolder = ProjectManager.getInstance().getFolder(PropertyHandler.getInstance().getProperty("_default_collection_name"));
        File imageFile = new File(pathToImage);
        String filename = "";
        String format = "";
        File copiedImageFile;
        while (true) {
            filename = "image" + UUID.randomUUID().hashCode();
            if (!DbEntryProvider.INSTANCE.idExists(filename)) {
                Path path = new Path(pathToImage);
                format = path.getFileExtension();
                copiedImageFile = new File(collectionFolder.getAbsolutePath() + File.separator + filename + "." + format);
                if (!copiedImageFile.exists()) break;
            }
        }
        try {
            copiedImageFile.createNewFile();
        } catch (IOException e1) {
            ExceptionHandlingService.INSTANCE.handleException(e1);
            return null;
        }
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            in = new BufferedInputStream(new FileInputStream(imageFile), 4096);
            out = new BufferedOutputStream(new FileOutputStream(copiedImageFile), 4096);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            ExceptionHandlingService.INSTANCE.handleException(e);
            return null;
        } catch (IOException e) {
            ExceptionHandlingService.INSTANCE.handleException(e);
            return null;
        }
        Image image = new ImageImpl();
        image.setId(filename);
        image.setFormat(format);
        image.setEntryDate(new Date());
        image.setTitle(title);
        image.setAdditionalProperties(additionalProperties);
        boolean success = DbEntryProvider.INSTANCE.storeNewImage(image);
        if (success) return image;
        return null;
    }

    public String getRelativePath(Image image) {
        File collectionFolder = ProjectManager.getInstance().getFolder(PropertyHandler.getInstance().getProperty("_default_collection_name"));
        File storedImage = new File(collectionFolder.getAbsolutePath() + File.separator + image.getFilename());
        if (storedImage.exists()) return collectionFolder.getName() + File.separator + image.getFilename();
        return null;
    }

    public String getAbsolutePath(Image image) {
        File collectionFolder = ProjectManager.getInstance().getFolder(PropertyHandler.getInstance().getProperty("_default_collection_name"));
        File storedImage = new File(collectionFolder.getAbsolutePath() + File.separator + image.getFilename());
        if (storedImage.exists()) return collectionFolder.getAbsolutePath() + File.separator + image.getFilename();
        return null;
    }

    public String getRelativePath(String id) {
        Image image = DbEntryProvider.INSTANCE.getImageById(id);
        if (image != null) {
            File collectionFolder = ProjectManager.getInstance().getFolder(PropertyHandler.getInstance().getProperty("_default_collection_name"));
            File storedImage = new File(collectionFolder.getAbsolutePath() + File.separator + image.getFilename());
            if (storedImage.exists()) return collectionFolder.getName() + File.separator + image.getFilename();
        }
        return null;
    }
}
