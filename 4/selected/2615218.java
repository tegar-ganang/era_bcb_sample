package br.com.visualmidia.ui.wizard.composite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import br.com.visualmidia.GD;
import br.com.visualmidia.business.Photo;
import br.com.visualmidia.core.Constants;
import br.com.visualmidia.core.server.Communicate;
import br.com.visualmidia.system.GDSystem;

/**
 * @author  Lucas
 */
public class LogoComposite extends Composite {

    private Canvas photoCanvas;

    private Image image;

    protected String currentDir;

    private String filename;

    public LogoComposite(Composite parent, int style) {
        super(parent, style);
        GridLayout layout = new GridLayout(1, true);
        this.setLayout(layout);
        createLogoGroup();
    }

    private void createLogoGroup() {
        Group logoGroup = new Group(this, SWT.NONE);
        FillLayout fillLayout = new FillLayout();
        fillLayout.marginHeight = 10;
        fillLayout.marginWidth = 10;
        logoGroup.setLayout(fillLayout);
        logoGroup.setText("Logotipo");
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        logoGroup.setLayoutData(data);
        Composite groupComposite = new Composite(logoGroup, SWT.NONE);
        FormLayout formLayout = new FormLayout();
        groupComposite.setLayout(formLayout);
        createPhotoCanvas(groupComposite);
        createLoadImageButtom(groupComposite);
        if (photoExists("corporateLogo")) {
            fillPhoto("corporateLogo", true);
        } else {
            getDefaultImage();
        }
    }

    private void createPhotoCanvas(Composite groupComposite) {
        photoCanvas = new Canvas(groupComposite, SWT.NONE);
        photoCanvas.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        FormData data = new FormData();
        data.top = new FormAttachment(0, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        data.height = 200;
        photoCanvas.setLayoutData(data);
        photoCanvas.addDisposeListener(new DisposeListener() {

            public void widgetDisposed(DisposeEvent arg0) {
                savePhoto("corporateLogo", filename);
            }
        });
    }

    private void createLoadImageButtom(Composite groupComposite) {
        Button loadImageButton = new Button(groupComposite, SWT.NONE);
        loadImageButton.setText("Abrir foto");
        loadImageButton.addListener(SWT.MouseUp, new Listener() {

            public void handleEvent(Event arg0) {
                FileDialog fileChooser = new FileDialog(getShell(), SWT.OPEN);
                fileChooser.setText("Abrir fotografia do Logotipo");
                fileChooser.setFilterPath(currentDir);
                fileChooser.setFilterExtensions(new String[] { "*.gif;*.png" });
                fileChooser.setFilterNames(new String[] { "Arquivos de imagem" + " (gif, png)" });
                filename = fileChooser.open();
                if (filename != null) {
                    File file = new File(filename);
                    if ((file.length() / 500) > 1000) {
                        MessageBox dialog = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
                        dialog.setText("Alerta GerenteDigital");
                        dialog.setMessage("A foto selecionada � muito grande, selecione outra ou reduza a qualidade da foto! \nA foto deve ser menor que 500 Kb.");
                        dialog.open();
                    } else {
                        loadImage(filename);
                        currentDir = fileChooser.getFilterPath();
                    }
                }
            }
        });
        FormData data = new FormData();
        data.top = new FormAttachment(photoCanvas, 5);
        data.right = new FormAttachment(100, 0);
        data.width = 80;
        loadImageButton.setLayoutData(data);
    }

    private void loadImage(String filename) {
        Image image = new Image(null, filename);
        loadImage(image);
        image.dispose();
    }

    private void loadImage(Image image) {
        this.image = new Image(null, image.getImageData());
        photoCanvas.redraw();
        drawPhoto(image);
        photoCanvas.redraw();
    }

    private void drawPhoto(Image imageToLoad) {
        final Image image = new Image(getDisplay(), imageToLoad.getImageData());
        GC gc = new GC(photoCanvas);
        gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, 0, 0, 362, 198);
        gc.drawRectangle(0, 0, photoCanvas.getBounds().width - 1, photoCanvas.getBounds().height - 1);
        photoCanvas.addListener(SWT.Paint, new Listener() {

            public void handleEvent(Event arg0) {
                GC gc = new GC(photoCanvas);
                gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, 0, 0, 362, 198);
                gc.drawRectangle(0, 0, photoCanvas.getBounds().width - 1, photoCanvas.getBounds().height - 1);
                gc.dispose();
            }
        });
        gc.dispose();
    }

    public void fillPhoto(Photo photo) {
        Image image = (photo != null) ? new Image(null, photo.getPhoto()) : new Image(null, "img/userNoPhotoIco.png");
        loadImage(image);
    }

    public Photo getPhoto() {
        if (image == null) return null;
        return new Photo(image);
    }

    public void savePhoto(final String logoName, final String photoPath) {
        Thread conectThread = new Thread() {

            public void run() {
                try {
                    if (!(photoPath == null)) {
                        File file = new File(photoPath);
                        if (!GDSystem.isStandAloneMode()) {
                            SSLSocketFactory sslComunicationSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                            SSLSocket sslComunicationSocket = (SSLSocket) sslComunicationSocketFactory.createSocket(GDSystem.getServerIp(), 9998);
                            Communicate communicate = new Communicate(sslComunicationSocket);
                            communicate.receiveAndSend("200, Thank you bastard");
                            communicate.receiveAndSend("209, troca de imagens");
                            communicate.send("702, vou te enviar uma imagem");
                            communicate.send(logoName);
                            communicate.sendFile(communicate.getFileTransferConectionConnectMode(GDSystem.getServerIp()), file);
                            copyFileToPhotoFolder(file, logoName);
                        } else {
                            copyFileToPhotoFolder(file, logoName);
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void fillPhoto(final String logoName, final boolean isGetDefaultImage) {
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
                                communicate.send(logoName);
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
                                            if (isGetDefaultImage) getDefaultImage(); else dispose();
                                        }
                                    });
                                }
                            }
                        }
                        sslComunicationSocket.close();
                        return;
                    } else {
                        File personPhotoFile = new File(Constants.PHOTO_DIR + logoName + ".jpg");
                        boolean isPossibleOpenThisFile = true;
                        try {
                            new Image(null, Constants.PHOTO_DIR + logoName + ".jpg");
                        } catch (Exception e) {
                            isPossibleOpenThisFile = false;
                        }
                        if (personPhotoFile.exists() && isPossibleOpenThisFile) {
                            getDisplay().syncExec(new Runnable() {

                                public void run() {
                                    loadImage(new Image(getDisplay(), Constants.PHOTO_DIR + logoName + ".jpg"));
                                }
                            });
                        } else {
                            getDisplay().syncExec(new Runnable() {

                                public void run() {
                                    if (isGetDefaultImage) getDefaultImage(); else dispose();
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

    private boolean photoExists(String logoName) {
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
                        communicate.send(logoName);
                        if (communicate.receive().equals("704")) {
                            communicate.send("705, nao precisa me mandar imagem");
                            sslComunicationSocket.close();
                            return true;
                        } else {
                            sslComunicationSocket.close();
                            return false;
                        }
                    }
                }
            } else {
                File personPhotoFile = new File(Constants.PHOTO_DIR + logoName + ".jpg");
                boolean isPossibleOpenThisFile = true;
                try {
                    Image image = new Image(null, Constants.PHOTO_DIR + logoName + ".jpg");
                    image.dispose();
                    image = null;
                } catch (Exception e) {
                    isPossibleOpenThisFile = false;
                }
                if (personPhotoFile.exists() && isPossibleOpenThisFile) {
                    return true;
                } else {
                    return false;
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
