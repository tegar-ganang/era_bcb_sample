package com.lovehorsepower.imagemailerapp.workers;

import com.lovehorsepower.imagemailerapp.ImageMailerView;
import com.lovehorsepower.imagemailerapp.panels.MainTabPanel;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.ws.BindingProvider;
import org.jdesktop.swingworker.SwingWorker;
import org.openide.util.Exceptions;

/**
 *
 * @author obernbergerj
 */
public class SendImagesWorker extends SwingWorker {

    MainTabPanel mf = null;

    String galleryName = "";

    ArrayList imageBeanList = null;

    String emailAddress = "";

    String processingFlag = "";

    String galleryType = "";

    public SendImagesWorker(MainTabPanel mf, String galleryName, String emailAddress, ArrayList imageBeanList, String galleryType) {
        this.mf = mf;
        this.galleryName = galleryName;
        this.imageBeanList = imageBeanList;
        this.emailAddress = emailAddress;
        this.galleryType = galleryType;
    }

    @Override
    protected Object doInBackground() {
        Iterator it = null;
        ImageBean ib = null;
        byte imageData[] = null;
        String resizedFilename = "";
        File resizedFile = null;
        int imageCounter = 0;
        int counter = 1;
        boolean flipFlop = false;
        String detail = "";
        File commentsFile = null;
        File albumFile = null;
        System.out.println("Have gallery to process: " + galleryName + " for email: " + emailAddress + " type is: " + galleryType + ".");
        if (galleryType.length() == 0) {
            System.out.println("Call cybrina - ask for galleryType.");
            try {
                com.lovehorsepower.emailimages.EIWSService service = new com.lovehorsepower.emailimages.EIWSService();
                com.lovehorsepower.emailimages.EIWS port = service.getEIWSPort();
                ((BindingProvider) port).getRequestContext().put("com.sun.xml.ws.request.timeout", new Integer(25000));
                java.util.List<java.lang.String> result = port.getGalleryDetails(galleryName, emailAddress);
                System.out.println("Got result: " + result);
                it = result.iterator();
                while (it != null && it.hasNext()) {
                    detail = (String) it.next();
                    if (detail.startsWith("$TYPE$:")) {
                        galleryType = new String(detail.substring(detail.indexOf(":") + 1));
                    }
                }
            } catch (Exception ex) {
                System.out.println("Exception calling web service: " + ex);
                JOptionPane.showMessageDialog(mf, "Error sending data to web service.\nPlease try again later.\n" + ex, "Web Service Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        it = imageBeanList.iterator();
        while (it != null && it.hasNext()) {
            ib = (ImageBean) it.next();
            System.out.println("Checking ib: " + ib.getCheckBox().getText());
            if (ib.getCheckBox().isSelected()) {
                imageCounter++;
            }
        }
        mf.progressBar.setMaximum(imageCounter);
        mf.progressBar.setValue(0);
        it = imageBeanList.iterator();
        while (it != null && it.hasNext()) {
            ib = (ImageBean) it.next();
            if (ib.getCheckBox().isSelected()) {
                try {
                    com.lovehorsepower.emailimages.EIWSService service = new com.lovehorsepower.emailimages.EIWSService();
                    com.lovehorsepower.emailimages.EIWS port = service.getEIWSPort();
                    ((BindingProvider) port).getRequestContext().put("com.sun.xml.ws.request.timeout", new Integer(60000));
                    if (ib.getImageFile().getAbsolutePath().endsWith(".AVI") || ib.getImageFile().getAbsolutePath().endsWith(".avi") || ib.getImageFile().getAbsolutePath().endsWith(".MOV") || ib.getImageFile().getAbsolutePath().endsWith(".mov")) {
                        SwingUtilities.invokeLater(new UpdateProgress(mf, counter, imageCounter, "Converting video file " + ib.getImageFile().getName() + " to flash format..."));
                        resizedFilename = convertVideoToFlash(ib.getImageFile());
                    } else {
                        resizedFilename = resizeImage(ib.getImageFile());
                    }
                    imageData = read(resizedFilename);
                    resizedFile = new File(resizedFilename);
                    java.lang.String result = port.sendImages(emailAddress, galleryName, resizedFile.getName(), imageData);
                    System.out.println("Result = " + result);
                    resizedFile.delete();
                    SwingUtilities.invokeLater(new UpdateProgress(mf, counter, imageCounter, "Resizing and sending image " + counter + " of " + imageCounter + "."));
                    counter++;
                    if (result.startsWith("ERROR")) {
                        JOptionPane.showMessageDialog(mf, "Error sending data to web service.\nPlease try again later.\n" + result, "Web Service Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(mf, "Error sending data to web service.\nPlease try again later.\n" + ex, "Web Service Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        }
        commentsFile = new File("comments.properties." + mf.currentDir.getName());
        if (commentsFile.exists()) {
            try {
                com.lovehorsepower.emailimages.EIWSService service = new com.lovehorsepower.emailimages.EIWSService();
                com.lovehorsepower.emailimages.EIWS port = service.getEIWSPort();
                imageData = read(commentsFile.getAbsolutePath());
                java.lang.String result = port.sendImages(emailAddress, galleryName, "comments.properties", imageData);
                System.out.println("Result from sending comments = " + result);
                if (result.startsWith("ERROR")) {
                    JOptionPane.showMessageDialog(mf, "Error sending comment data to web service.\nPlease try again later.\n" + result, "Web Service Error", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mf, "Error sending comment data to web service.\nPlease try again later.\n" + ex, "Web Service Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        albumFile = new File("albumfiles.txt" + mf.currentDir.getName());
        if (albumFile.exists()) {
            try {
                com.lovehorsepower.emailimages.EIWSService service = new com.lovehorsepower.emailimages.EIWSService();
                com.lovehorsepower.emailimages.EIWS port = service.getEIWSPort();
                imageData = read(albumFile.getAbsolutePath());
                java.lang.String result = port.sendImages(emailAddress, galleryName, "albumfiles.txt", imageData);
                System.out.println("Result from sending file order data = " + result);
                if (result.startsWith("ERROR")) {
                    JOptionPane.showMessageDialog(mf, "Error sending file order data to web service.\nPlease try again later.\n" + result, "Web Service Error", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mf, "Error sending file order data to web service.\nPlease try again later.\n" + ex, "Web Service Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        try {
            com.lovehorsepower.emailimages.EIWSService service = new com.lovehorsepower.emailimages.EIWSService();
            com.lovehorsepower.emailimages.EIWS port = service.getEIWSPort();
            ((BindingProvider) port).getRequestContext().put("com.sun.xml.ws.request.timeout", new Integer(25000));
            java.lang.String result = port.buildGallery(emailAddress, galleryName, galleryType);
            System.out.println("Result = " + result);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mf, "Error sending build command to web service.\nPlease try again later.\n" + ex, "Web Service Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
        while (true) {
            try {
                com.lovehorsepower.emailimages.EIWSService service = new com.lovehorsepower.emailimages.EIWSService();
                com.lovehorsepower.emailimages.EIWS port = service.getEIWSPort();
                ((BindingProvider) port).getRequestContext().put("com.sun.xml.ws.request.timeout", new Integer(25000));
                processingFlag = port.getBuildStatus(emailAddress, galleryName);
                if (processingFlag.equals("PROCESSING")) {
                    if (flipFlop) {
                        mf.setStatusMessage("Gallery " + galleryName + " is still processing...");
                        flipFlop = false;
                    } else {
                        mf.setStatusMessage("Gallery " + galleryName + " is currently processing...");
                        flipFlop = true;
                    }
                    Thread.sleep(10 * 1000);
                    continue;
                } else {
                    return null;
                }
            } catch (Exception ex) {
                System.out.println("Got exception calling webservice: " + ex);
                return null;
            }
        }
    }

    private String convertVideoToFlash(File imageFile) {
        String command = "";
        Process proc = null;
        Runtime rt = Runtime.getRuntime();
        String newFilename = "";
        String fileToConvert = imageFile.getAbsolutePath();
        System.out.println("Convert file: " + fileToConvert);
        newFilename = fileToConvert.substring(0, (fileToConvert.lastIndexOf("."))) + ".flv";
        command = "ffmpeg -i \"" + fileToConvert + "\" -s 320x240 -b 1000k -maxrate 4000k -bufsize 1024k \"" + newFilename + "\"";
        System.out.println("Convert command is: " + command);
        try {
            proc = rt.exec(command);
            StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ffmpeg ERROR ");
            StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "ffmpeg OUTPUT ");
            errorGobbler.start();
            outputGobbler.start();
            try {
                proc.waitFor();
            } catch (InterruptedException ex) {
                System.out.println("FFMpeg interrupted exeception: " + ex);
            }
        } catch (IOException ex1) {
            System.out.println("IOException converting video to flash: " + ex1);
        }
        return newFilename;
    }

    private byte[] read(String file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        FileChannel fc = fis.getChannel();
        byte[] data = new byte[(int) fc.size()];
        ByteBuffer bb = ByteBuffer.wrap(data);
        fc.read(bb);
        return data;
    }

    private String resizeImage(File filename) throws FileNotFoundException, IOException {
        Image image = Toolkit.getDefaultToolkit().getImage(filename.getAbsolutePath());
        MediaTracker mediaTracker = new MediaTracker(new Container());
        mediaTracker.addImage(image, 0);
        try {
            mediaTracker.waitForID(0);
        } catch (Exception ex) {
            System.out.println("Media tracker exception: " + ex);
        }
        int thumbWidth = 1280;
        int thumbHeight = 1024;
        double thumbRatio = (double) thumbWidth / (double) thumbHeight;
        int imageWidth = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        double imageRatio = (double) imageWidth / (double) imageHeight;
        if (thumbRatio < imageRatio) {
            thumbHeight = (int) (thumbWidth / imageRatio);
        } else {
            thumbWidth = (int) (thumbHeight * imageRatio);
        }
        BufferedImage thumbImage = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = thumbImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(image, 0, 0, thumbWidth, thumbHeight, null);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename.getName()));
        JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
        JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(thumbImage);
        int quality = 98;
        quality = Math.max(0, Math.min(quality, 100));
        param.setQuality((float) quality / 100.0f, false);
        encoder.setJPEGEncodeParam(param);
        encoder.encode(thumbImage);
        out.close();
        return filename.getName();
    }

    protected void done() {
        mf.processingDone(emailAddress, galleryName);
    }
}

class UpdateProgress implements Runnable {

    MainTabPanel mf = null;

    int value = 0;

    int totalCount = 0;

    String message = "";

    public UpdateProgress(MainTabPanel mf, int value, int totalCount, String message) {
        this.mf = mf;
        this.value = value;
        this.totalCount = totalCount;
        this.message = message;
    }

    public void run() {
        mf.progressBar.setValue(value);
        mf.statusMessageLabel.setText(message);
    }
}
