package rs.realestate.service;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: petar.popovic
 * Date: 16.11.11.
 * Time: 13.49
 * To change this template use File | Settings | File Templates.
 */
public class ImageService {

    public static String folderPath = "C:/JavaProjects/idea/images";

    public static void defaultUploadImage(UploadedFile uploadedFile) {
        if (uploadedFile != null) imgSave(uploadedFile.getContents(), uploadedFile.getFileName());
    }

    public static void uploadImage(UploadedFile uploadedFile, String fileName) {
        if (uploadedFile != null) imgSave(uploadedFile.getContents(), fileName);
    }

    public static StreamedContent loadImage(String fileName) {
        String imgType = fileName.substring(fileName.lastIndexOf(".") + 1);
        try {
            ImageIcon icon = new ImageIcon(folderPath + "/" + fileName);
            Image image = icon.getImage();
            BufferedImage buffImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = buffImage.createGraphics();
            g.drawImage(image, null, null);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            System.out.print(imgType);
            ImageIO.write(buffImage, imgType, os);
            return new DefaultStreamedContent(new ByteArrayInputStream(os.toByteArray()), "image/" + imgType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void imgSave(byte[] readData, String fileName) {
        try {
            FileOutputStream fos = new FileOutputStream(folderPath + fileName);
            fos.write(readData);
            fos.flush();
            fos.close();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getCause());
        }
    }

    public static void main(String[] v) {
        loadImage("test.gif");
    }
}
