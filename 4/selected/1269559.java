package br.com.visualmidia.ui.widgets;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.channels.FileChannel;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.ImageIcon;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import br.com.visualmidia.core.Constants;
import br.com.visualmidia.core.server.Communicate;
import br.com.visualmidia.system.GDSystem;

public class PersonPhoto extends Composite {

    private Canvas photoCanvas;

    private Composite parent;

    private int height;

    private int width;

    public PersonPhoto(Composite parent) {
        super(parent, SWT.NONE);
        this.parent = parent;
        this.height = 160;
        this.width = 120;
        configure();
        createFields();
    }

    public PersonPhoto(Composite parent, int height, int width) {
        super(parent, SWT.NONE);
        this.parent = parent;
        this.height = height;
        this.width = width;
        configure();
        createFields();
    }

    private void configure() {
        setLayout(new FillLayout());
        FormData data = new FormData();
        data.height = height;
        data.width = width;
        data.top = new FormAttachment(0, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        setLayoutData(data);
    }

    private void createFields() {
        createPhotoCanvas();
    }

    private void createPhotoCanvas() {
        photoCanvas = new Canvas(this, SWT.NONE);
    }

    public void loadImage(String filename) {
        Image image = new Image(parent.getDisplay(), filename);
        loadImage(image);
        image.dispose();
    }

    public void loadImage(Image imageToLoad) {
        Image image = new Image(parent.getDisplay(), imageToLoad.getImageData());
        photoCanvas.redraw();
        drawPhoto(image);
        photoCanvas.redraw();
    }

    private void drawPhoto(Image imageToLoad) {
        final Image image = new Image(parent.getDisplay(), imageToLoad.getImageData());
        GC gc = new GC(photoCanvas);
        gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, 0, 0, width, height);
        gc.drawRectangle(0, 0, photoCanvas.getBounds().width - 1, photoCanvas.getBounds().height - 1);
        photoCanvas.addListener(SWT.Paint, new Listener() {

            public void handleEvent(Event arg0) {
                GC gc = new GC(photoCanvas);
                gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, 0, 0, width, height);
                gc.drawRectangle(0, 0, photoCanvas.getBounds().width - 1, photoCanvas.getBounds().height - 1);
                gc.dispose();
            }
        });
        gc.dispose();
    }

    public void fillPhotoFromPerson(final String personId, final boolean isGetDefaultImage) {
        Thread conectThread = new Thread() {

            public void run() {
                try {
                    if (!GDSystem.isStandAloneMode()) {
                        SSLSocketFactory sslComunicationSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                        SSLSocket sslComunicationSocket = (SSLSocket) sslComunicationSocketFactory.createSocket(GDSystem.getServerIp(), 9998);
                        Communicate communicate = new Communicate(sslComunicationSocket);
                        communicate.receiveAndSend("200, Thank you bastard");
                        if (communicate.receive().equals("201")) {
                            communicate.send("209, Troca de imagens");
                            if (communicate.receive().equals("700")) {
                                communicate.send("701, me manda a imagem");
                                communicate.send(personId);
                                if (communicate.receive().equals("704")) {
                                    communicate.send("706, me mande a imagem");
                                    communicate.receivePersonPhoto();
                                    getDisplay().syncExec(new Runnable() {

                                        public void run() {
                                            Image image = new Image(getDisplay(), Constants.TEMP_DIR + "personPhoto" + ".jpg");
                                            loadImage(image);
                                        }
                                    });
                                } else {
                                    getDisplay().syncExec(new Runnable() {

                                        public void run() {
                                            if (isGetDefaultImage) getDefaultImage(); else parent.getChildren()[2].dispose();
                                        }
                                    });
                                }
                            }
                        }
                        sslComunicationSocket.close();
                        return;
                    } else {
                        File personPhotoFile = new File(Constants.PHOTO_DIR + personId + ".jpg");
                        boolean isPossibleOpenThisFile = true;
                        try {
                            new Image(null, Constants.PHOTO_DIR + personId + ".jpg");
                        } catch (Exception e) {
                            isPossibleOpenThisFile = false;
                        }
                        if (personPhotoFile.exists() && isPossibleOpenThisFile) {
                            getDisplay().syncExec(new Runnable() {

                                public void run() {
                                    loadImage(new Image(getDisplay(), Constants.PHOTO_DIR + personId + ".jpg"));
                                }
                            });
                        } else {
                            getDisplay().syncExec(new Runnable() {

                                public void run() {
                                    if (isGetDefaultImage) getDefaultImage(); else parent.getChildren()[2].dispose();
                                }
                            });
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        };
        conectThread.start();
    }

    private void getDefaultImage() {
        loadImage(new Image(getDisplay(), Constants.CURRENT_DIR + "img" + Constants.FILE_SEPARATOR + "userNoPhotoIco.png"));
    }

    public void savePhoto(final String personId, final String photoPath) {
        Thread conectThread = new Thread() {

            public void run() {
                try {
                    if (!(photoPath == null)) {
                        int width = 200;
                        int height = 200;
                        int quality = 80;
                        java.awt.Image imageicon = new ImageIcon(photoPath).getImage();
                        resizeImage(imageicon, width, height, quality, personId + ".jpg");
                        File file = new File(Constants.TEMP_DIR + personId + ".jpg");
                        if (!GDSystem.isStandAloneMode()) {
                            SSLSocketFactory sslComunicationSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                            SSLSocket sslComunicationSocket = (SSLSocket) sslComunicationSocketFactory.createSocket(GDSystem.getServerIp(), 9998);
                            Communicate communicate = new Communicate(sslComunicationSocket);
                            communicate.receiveAndSend("200, Thank you bastard");
                            communicate.receiveAndSend("209, troca de imagens");
                            communicate.send("702, vou te enviar uma imagem");
                            communicate.send(personId);
                            communicate.sendFile(communicate.getFileTransferConectionConnectMode(GDSystem.getServerIp()), file);
                        } else {
                            copyFileToPhotoFolder(file, personId);
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        };
        conectThread.start();
    }

    private void copyFileToPhotoFolder(File photo, String personId) {
        try {
            FileChannel in = new FileInputStream(photo).getChannel();
            File dirServer = new File(Constants.PHOTO_DIR);
            if (!dirServer.exists()) {
                dirServer.mkdirs();
            }
            File fileServer = new File(Constants.PHOTO_DIR + personId + ".jpg");
            if (!fileServer.exists()) {
                fileServer.createNewFile();
            }
            in.transferTo(0, in.size(), new FileOutputStream(fileServer).getChannel());
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage convertToBufferedImage(ImageData data) {
        ColorModel colorModel = null;
        PaletteData palette = data.palette;
        if (palette.isDirect) {
            colorModel = new DirectColorModel(data.depth, palette.redMask, palette.greenMask, palette.blueMask);
            BufferedImage bufferedImage = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = new int[3];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    int pixel = data.getPixel(x, y);
                    RGB rgb = palette.getRGB(pixel);
                    pixelArray[0] = rgb.red;
                    pixelArray[1] = rgb.green;
                    pixelArray[2] = rgb.blue;
                    raster.setPixels(x, y, 1, 1, pixelArray);
                }
            }
            return bufferedImage;
        } else {
            RGB[] rgbs = palette.getRGBs();
            byte[] red = new byte[rgbs.length];
            byte[] green = new byte[rgbs.length];
            byte[] blue = new byte[rgbs.length];
            for (int i = 0; i < rgbs.length; i++) {
                RGB rgb = rgbs[i];
                red[i] = (byte) rgb.red;
                green[i] = (byte) rgb.green;
                blue[i] = (byte) rgb.blue;
            }
            if (data.transparentPixel != -1) {
                colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue, data.transparentPixel);
            } else {
                colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue);
            }
            BufferedImage bufferedImage = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = new int[1];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    int pixel = data.getPixel(x, y);
                    pixelArray[0] = pixel;
                    raster.setPixel(x, y, pixelArray);
                }
            }
            return bufferedImage;
        }
    }

    private void resizeImage(java.awt.Image imageicon, int width, int height, int quality, String imageName) {
        double thumbRatio = (double) width / (double) height;
        int imageWidth = imageicon.getWidth(null);
        int imageHeight = imageicon.getHeight(null);
        double imageRatio = (double) imageWidth / (double) imageHeight;
        if (thumbRatio < imageRatio) {
            height = (int) (width / imageRatio);
        } else {
            width = (int) (height * imageRatio);
        }
        BufferedImage thumbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = thumbImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(imageicon, 0, 0, width, height, null);
        BufferedOutputStream out;
        try {
            File tempdir = new File(Constants.TEMP_DIR);
            if (!tempdir.exists()) {
                tempdir.mkdirs();
            }
            out = new BufferedOutputStream(new FileOutputStream(Constants.TEMP_DIR + imageName));
            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
            JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(thumbImage);
            quality = Math.max(0, Math.min(quality, 100));
            param.setQuality((float) quality / 100.0f, false);
            encoder.setJPEGEncodeParam(param);
            encoder.encode(thumbImage);
        } catch (FileNotFoundException e) {
            System.out.println("FileNotFoundException " + e.getMessage());
        } catch (ImageFormatException e) {
            System.out.println("ImageFormatException " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException " + e.getMessage());
        }
    }
}
