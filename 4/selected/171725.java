package rs.realestate.repository.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import rs.realestate.domain.Image;
import rs.realestate.domain.RealEstate;
import rs.realestate.domain.TempFolder;
import rs.realestate.repository.ImageRepository;
import rs.realestate.util.FileUtil;
import rs.realestate.util.ImageUtil;
import rs.realestate.util.Util;

@Repository("imageRepository")
public class JpaImageRepository extends JpaBaseRepository implements ImageRepository {

    public static final String DOCROOT_EXT = "C:/JavaProjects/Master/petar.popovic/docroot/uploaded/";

    private static final String DOCROOT_WEB = "C:/JavaProjects/Master/.metadata/.plugins/org.eclipse.wst.server.core/tmp1/wtpwebapps/petar.popovic/uploaded/";

    private static final String DELETE_TYPE = "DELETE";

    private static final String COPY_TYPE = "COPY";

    private static final String TEMP_KEY_FLAG = "tEmP_KeY";

    public static final String THUMB_PREFIX = "thumb_";

    public static final Integer THUMB_WIDHT = 200;

    @Override
    public String getUniqueKey() {
        String key = TEMP_KEY_FLAG + new Date().getTime();
        return key;
    }

    @Autowired
    protected EntityManagerFactory entityManagerFactory;

    @PostConstruct
    public void init() {
        super.setEntityManagerFactory(entityManagerFactory);
    }

    @Override
    public void createTempImgFolder(String folderName) {
        if (!validKey(folderName)) throw new RuntimeException();
        TempFolder t = new TempFolder();
        t.setFolderName(folderName);
        t.setCreatedAt(new Date());
        persist(t);
    }

    @Override
    public boolean validKey(String folderName) {
        return Util.isEmpty(findByNamedQueryAndNamedParams(TempFolder.QUERY_BY_FOLDER_NAME, params("folderName", folderName)));
    }

    @Override
    public void removeTempImgFolder(String folderName) {
        List<TempFolder> result = findByNamedQueryAndNamedParams(TempFolder.QUERY_BY_FOLDER_NAME, params("folderName", folderName));
        if (!Util.isEmpty(result)) remove(result.get(0));
    }

    @Transactional
    public void moveImageFromTempToRealestate(Integer realId, String tempKey) {
        RealEstate real = findById(RealEstate.class, realId);
        TempFolder temp = findById(TempFolder.class, tempKey);
        if (real == null || temp == null) return;
        if (Util.isEmpty(temp.getImages())) return;
        String tempRelativePath = "/images/" + tempKey + "/";
        String newRelativePath = "/images/" + realId + "/";
        copyImagesFromTo(tempRelativePath, newRelativePath);
        publishToWeb(path(DOCROOT_EXT, newRelativePath), COPY_TYPE);
        for (Image image : temp.getImages()) {
            String path = image.getPicturePath();
            path = newRelativePath + path.substring(tempRelativePath.length());
            image.setPicturePath(path);
            real.addImage(image);
        }
        remove(temp);
        removeImageFolder(tempRelativePath);
    }

    @Transactional
    public void removeImage(String tempKey, Integer id) {
        Image img = findById(Image.class, id);
        if (img == null) return;
        if (tempKey.contains(TEMP_KEY_FLAG)) {
            TempFolder t = findById(TempFolder.class, tempKey);
            if (t == null) return;
            t.getImages().remove(img);
            removeImageFromFolder(path(DOCROOT_EXT, t.getFolderName() + "/" + img.getPicturePath()));
            merge(TempFolder.class, t);
            remove(img);
        } else {
            RealEstate real = findById(RealEstate.class, id);
            if (real == null) return;
            real.getImages().remove(img);
            removeImageFromFolder(path(DOCROOT_EXT, real.getId() + "/" + img.getPicturePath()));
            merge(RealEstate.class, real);
            remove(img);
        }
    }

    private String saveImageToFile(InputStream inputStream, String imageRelativePath) {
        OutputStream outputStream;
        String destination = "";
        try {
            destination = path(DOCROOT_EXT, imageRelativePath);
            String destinationRoot = destination.substring(0, destination.lastIndexOf("/"));
            File df = new File(destinationRoot);
            if (!df.exists()) df.mkdir();
            outputStream = new FileOutputStream(destination);
            fromInputToOutputStream(inputStream, outputStream);
            publishToWeb(destination, COPY_TYPE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return destination;
    }

    private void removeImageFolder(String relativeFolderPath) {
        String folderPath = path(DOCROOT_EXT, relativeFolderPath);
        File file = new File(folderPath);
        if (file != null) file.delete();
        publishToWeb(folderPath, DELETE_TYPE);
    }

    private void removeImageFromFolder(String imagePath) {
        imagePath = path(DOCROOT_EXT, imagePath);
        File file = new File(imagePath);
        if (file != null) file.delete();
        publishToWeb(imagePath, DELETE_TYPE);
    }

    private void copyImagesFromTo(String relativeTempFolder, String relativeDestFolder) {
        String sourceFolder = path(DOCROOT_EXT, relativeTempFolder);
        String destinationFolder = path(DOCROOT_EXT, relativeDestFolder);
        File df = new File(destinationFolder);
        if (!df.exists()) df.mkdir();
        copyFiles(sourceFolder, destinationFolder);
    }

    private List<String> getImagesPath(String destinationFolder) {
        File destF = new File(destinationFolder);
        List<String> result = new ArrayList<String>();
        if (destF != null) {
            for (File f : destF.listFiles()) {
                result.add(f.getAbsolutePath().substring(DOCROOT_EXT.length()));
            }
        }
        return result;
    }

    private Image makeImageFrom(String filePath) {
        java.awt.Image img = ImageUtil.getImageFrom(path(DOCROOT_EXT, filePath));
        Image image = new Image();
        img.getHeight(null);
        image.setHeight(img.getHeight(null));
        image.setWidth(img.getWidth(null));
        image.setPicturePath(filePath);
        return image;
    }

    @Transactional
    public Image addImage(InputStream inputStream, String tempKey, String originalFilename) {
        if (Util.isEmpty(tempKey)) throw new RuntimeException();
        String path = "/images/" + tempKey + "/" + originalFilename;
        String destPath = saveImageToFile(inputStream, path);
        String thmbPath = makeThmbImageFor(destPath);
        publishToWeb(thmbPath, COPY_TYPE);
        Image img = null;
        if (tempKey.contains(TEMP_KEY_FLAG)) {
            TempFolder tImg = findById(TempFolder.class, tempKey);
            if (tImg == null) throw new RuntimeException();
            img = makeImageFrom(path);
            img.setPosition(tImg.getMaxPosition() + 1);
            if (img.getPosition() == 0) img.setMainPicture(true);
            tImg.addImage(img);
            persist(img);
            merge(TempFolder.class, tImg);
        } else {
            Integer realId = Integer.parseInt(tempKey);
            RealEstate real = findById(RealEstate.class, realId);
            if (real == null) throw new RuntimeException();
            img = makeImageFrom(path);
            img.setPosition(real.getMaxPosition() + 1);
            if (img.getPosition() == 0) img.setMainPicture(true);
            real.addImage(img);
            persist(img);
            merge(RealEstate.class, real);
        }
        return img;
    }

    private String makeThmbImageFor(String filePath) {
        java.awt.Image img = ImageUtil.getImageFrom(filePath);
        BufferedImage bi = ImageUtil.getBufferedImageFromImage(img);
        bi = ImageUtil.resizeImage(bi, THUMB_WIDHT, bi.getType());
        String path = FileUtil.addPrefixToFileName(filePath, THUMB_PREFIX);
        ImageUtil.saveBufferedImage(bi, path);
        return path;
    }

    public String getAdvertisementImagePath(Integer realestateId) {
        return DOCROOT_EXT + "/images/" + realestateId + "/";
    }

    private void publishToWeb(String sourcefile, String type) {
        String diff = sourcefile.substring(DOCROOT_EXT.length());
        String webFolder = DOCROOT_WEB + "/" + diff;
        if (type.equals("DELETE")) {
            File f = new File(webFolder);
            if (f != null) f.delete();
        } else if (type.equals("COPY")) {
            File sFile = new File(sourcefile);
            if (sFile.isDirectory()) {
                sFile.mkdir();
                for (File oneFile : sFile.listFiles()) {
                    String sourceDest = oneFile.getAbsolutePath();
                    String dest = DOCROOT_WEB + sourceDest.substring(DOCROOT_EXT.length());
                    File destF = new File(dest);
                    copyFile(oneFile, destF);
                }
            } else {
                File destF = new File(DOCROOT_WEB + sourcefile.substring(DOCROOT_EXT.length()));
                copyFile(sFile, destF);
            }
        }
    }

    private void copyFiles(String sourceFolder, String destinationFolder) {
        File f = new File(sourceFolder);
        File destinaionRoot = new File(destinationFolder);
        destinaionRoot.mkdir();
        if (f != null) {
            for (File sFile : f.listFiles()) {
                copyFile(sFile, destinaionRoot);
            }
        }
    }

    private void copyFile(File source, File destination) {
        try {
            InputStream input = new FileInputStream(source);
            if (destination.isDirectory()) {
                String imageName = source.getName();
                destination.mkdir();
                destination = new File(destination.getAbsolutePath(), imageName);
            } else {
                destination.getParentFile().mkdir();
            }
            OutputStream output = new FileOutputStream(destination);
            fromInputToOutputStream(input, output);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void fromInputToOutputStream(InputStream input, OutputStream output) {
        try {
            int readBytes = 0;
            byte[] buffer = new byte[10000];
            while ((readBytes = input.read(buffer, 0, 10000)) != -1) {
                output.write(buffer, 0, readBytes);
            }
            input.close();
            output.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String path(String root, String relative) {
        return (root + relative).replace("//", "/");
    }
}
