package com.elibera.m.fileio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;
import com.elibera.m.app.MLE;
import com.elibera.m.display.ProgressCanvas;
import com.elibera.m.display.ScrollingImageCanvas;
import com.elibera.m.events.Closeable;
import com.elibera.m.events.RootThread;
import com.elibera.m.rms.HelperRMSStoreMLibera;
import com.elibera.m.utils.HelperApp;
import com.elibera.m.utils.HelperStd;
import com.elibera.m.utils.HelperConverter;
import com.elibera.m.utils.MyByte;
import com.elibera.m.xml.PageSuite;
import com.elibera.m.xml.XMLTagImage;
import com.elibera.m.io.HelperHTTP;
import com.elibera.m.media.HelperPlayer;
import com.elibera.m.media.ev.EventMediaPlayer;

/**
 * @author meisi
 *
 */
public class HelperFileIO {

    private static String[] CT_IMAGES = { "jpg", "png", "jpeg", "tif", "gif", "tga" };

    private static String[] CT_AUDIO = { "mp3", "wav", "amr", "amr-wb", "x-wav" };

    private static String[] CT_VIDEO = { "3gp", "3gpp", "mpeg", "mpg", "avi", "mp4" };

    private static String[] CT_TEXT = { "html", "txt", "xml" };

    public static void checkCurrentRoot(FileSelector fs) throws Exception {
        if (fs.currentRoot != null && !fs.currentRoot.isOpen()) {
            if (fs.lastDir != null) fs.currentRoot = (FileConnection) Connector.open(fs.lastDir, Connector.READ); else fs.currentRoot = null;
        }
    }

    /**
	 * hängt das aktuelle Datum an den Filename an
	 * @param name
	 * @return
	 */
    public static String prepareFileNameWithDate(String name) {
        if (name == null) name = "";
        int p = name.lastIndexOf('.');
        String t = HelperStd.formateTime(-1, false).replace('.', '_').replace(':', '_').replace(' ', '-');
        if (p >= 0) return name.substring(0, p) + '_' + t + name.substring(p);
        return name + '_' + t;
    }

    /**
	 * this method is called if the user clicks on an element in the list
	 * @param fs
	 */
    public static void openSelected(FileSelector fs) throws Exception {
        fs.el.fileName = null;
        checkCurrentRoot(fs);
        int selectedIndex = fs.getSelectedIndex();
        if (selectedIndex >= 0) {
            String selectedFile = fs.getString(selectedIndex);
            String url = null;
            if (fs.currentRoot != null) {
                url = fs.currentRoot.getURL() + selectedFile;
                if (selectedIndex == 0) {
                    url = fs.currentRoot.getURL();
                    url = url.substring(0, url.lastIndexOf(fs.FILE_SEPARATOR.charAt(0), url.length() - 2) + 1);
                    if (url.length() <= 8) {
                        fs.displayAllRoots();
                        return;
                    }
                }
            } else url = "file://" + (selectedFile.indexOf("/") == 0 ? "" : "/") + selectedFile;
            FileConnection fileConn = null;
            try {
                fileConn = (FileConnection) Connector.open(url, Connector.READ);
                fs.fileConn = fileConn;
                if (fileConn.isDirectory()) {
                    fs.currentRoot = fileConn;
                    fs.lastDir = fileConn.getURL();
                    displayCurrentRoot(fs);
                } else {
                    if (fs.el.selectOnlyDirectory) {
                        fs.el.tmpFileName = selectedFile;
                        fs.commandAction(fs.selectDir, fs.el.dc);
                    } else {
                        fs.el.contentType = getContentType(fileConn.getName());
                        String ct = checkContentType(fs.el.contentType);
                        if (ct != null) fs.el.contentType = ct;
                        fs.el.fileSize = fileConn.fileSize();
                        fs.el.lastModified = fileConn.lastModified();
                        if (fs.el.useAllFiles || ct != null) showFileSelectedForm(fs, fileConn);
                    }
                }
            } finally {
                if (fileConn != null && fileConn.isOpen()) fileConn.close();
            }
        }
        HelperApp.runJavaGarbageCollector();
    }

    /**
	 * zeigt das Form mit den Infos an
	 * @param fs
	 * @param fileConn
	 * @throws Exception
	 */
    public static void showFileSelectedForm(FileSelector fs, FileConnection fileConn) throws IOException {
        if (fs.formInfo == null) {
            Form f = new Form(fs.titleSelFile);
            fs.infoName = new StringItem(null, " ");
            fs.infoSize = new StringItem(null, " ");
            fs.infoCT = new StringItem(null, " ");
            StringItem butPreview = new StringItem(null, HelperApp.translateCoreWord(HelperApp.TEXT_MEDIA_FILEIO_BUT_PREVIEW), Item.BUTTON);
            StringItem butBack = new StringItem(null, HelperApp.translateCoreWord(HelperApp.WORD_BACK), Item.BUTTON);
            StringItem butUse = new StringItem(null, HelperApp.translateCoreWord(HelperApp.TEXT_MEDIA_FILEIO_BUT_USE_FILE), Item.BUTTON);
            butPreview.setDefaultCommand(MLE.midlet.ok);
            butBack.setDefaultCommand(MLE.midlet.ok);
            butUse.setDefaultCommand(MLE.midlet.ok);
            butPreview.setItemCommandListener(fs);
            butBack.setItemCommandListener(fs);
            butUse.setItemCommandListener(fs);
            f.addCommand(fs.cmdUse);
            f.addCommand(fs.cmdBack);
            f.addCommand(fs.cmdPreview);
            f.setCommandListener(fs);
            StringItem tName = new StringItem(null, HelperApp.translateCoreWord(HelperApp.TEXT_MEDIA_FILEIO_FILENAME) + ": ");
            StringItem tCT = new StringItem(null, HelperApp.translateCoreWord(HelperApp.TEXT_MEDIA_FILEIO_CONTENT_TYPE) + ": ");
            StringItem tSize = new StringItem(null, HelperApp.translateCoreWord(HelperApp.WORD_SIZE) + ": ");
            tName.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.getDefaultFont().getSize()));
            tSize.setFont(tName.getFont());
            tCT.setFont(tName.getFont());
            fs.butPreview = butPreview;
            fs.butUse = butUse;
            fs.butBack = butBack;
            f.append(tName);
            f.append(fs.infoName);
            f.append(" \n");
            f.append(tSize);
            f.append(fs.infoSize);
            f.append(" \n");
            f.append(tCT);
            f.append(fs.infoCT);
            f.append(" \n");
            f.append(butPreview);
            f.append(" \n");
            f.append(butUse);
            f.append(" \n");
            f.append(butBack);
            fs.formInfo = f;
        }
        fs.infoName.setText(fileConn.getPath() + fileConn.getName());
        fs.infoSize.setText(HelperStd.formatBytes(fileConn.fileSize()));
        fs.infoCT.setText("" + fs.el.contentType);
        fs.el.fileName = fileConn.getURL();
        fs.midlet.setCurrent(fs.formInfo);
    }

    /**
	 * loads the root list
	 * @param fs
	 */
    public static void loadRoots(FileSelector fs) throws Exception {
        if (fs.rootsList != null) fs.rootsList = null;
        Enumeration roots = FileSystemRegistry.listRoots();
        String[] others = new String[0];
        while (roots.hasMoreElements()) {
            String el = (String) roots.nextElement();
            if (el.indexOf(':') > 0) fs.rootsList = HelperStd.incArray(fs.rootsList, fs.FILE_SEPARATOR + el); else others = HelperStd.incArray(others, el);
        }
        if (fs.rootsList == null || fs.rootsList.length <= 0) {
            for (int i = 0; i < others.length; i++) {
                fs.rootsList = HelperStd.incArray(fs.rootsList, (others[i].indexOf(fs.FILE_SEPARATOR) == 0 ? "" : fs.FILE_SEPARATOR) + others[i]);
            }
        }
    }

    /**
	   * A method used to display the current root.
	   */
    public static void displayCurrentRoot(FileSelector fs) throws Exception {
        HelperApp.runJavaGarbageCollector();
        fs.setTitle(fs.title + fs.lastDir.substring(7));
        fs.deleteAll();
        checkCurrentRoot(fs);
        Enumeration listOfFiles = fs.currentRoot.list();
        fs.append("..", null);
        while (listOfFiles.hasMoreElements()) {
            String currentFile = (String) listOfFiles.nextElement();
            if (currentFile != null) {
                boolean isFolder = currentFile.charAt(currentFile.length() - 1) == fs.FILE_SEPARATOR.charAt(0);
                fs.append(currentFile, !isFolder ? fs.imgFile : fs.imgFolder);
            }
        }
        fs.setSelectedIndex(0, true);
    }

    /**
	   * gibt den Content-Type zurück für diesen Dateinamen
	   * @param filename
	   * @return
	   */
    public static String getContentType(String filename) {
        String endung = filename.substring(filename.indexOf('.') + 1).toLowerCase();
        for (int i = 0; i < CT_IMAGES.length; i++) {
            if (endung.indexOf(CT_IMAGES[i]) == 0) return "image/" + CT_IMAGES[i];
        }
        for (int i = 0; i < CT_VIDEO.length; i++) {
            if (endung.indexOf(CT_VIDEO[i]) == 0) return "video/" + CT_VIDEO[i];
        }
        for (int i = 0; i < CT_AUDIO.length; i++) {
            if (endung.indexOf(CT_AUDIO[i]) == 0) return "audio/" + CT_AUDIO[i];
        }
        for (int i = 0; i < CT_TEXT.length; i++) {
            if (endung.indexOf(CT_TEXT[i]) == 0) return "text/" + CT_TEXT[i];
        }
        return "application/" + endung;
    }

    /**
	 * checkt ob wir den Content-Type darstellen können
	 * @param ct
	 * @return
	 */
    public static String checkContentType(String ct) {
        if (ct.indexOf("imag") == 0) {
            return ct;
        } else if (ct.indexOf("audi") == 0 || ct.indexOf("vid") == 0) {
            String ct2 = checkMediaContentType(ct);
            if (ct2 != null) return ct2;
            return ct;
        }
        return null;
    }

    /**
	 * überprüft ob das Audio oder Video file abgespielt werden können
	 * @param ct
	 * @return
	 */
    public static String checkMediaContentType(String ct) {
        if (ct == null) return null;
        if (ct.indexOf("imag") == 0) return ct;
        if (ct.indexOf("vid") == 0) {
            for (int i = 0; i < CT_VIDEO.length; i++) {
                if (ct.indexOf(CT_VIDEO[i]) > 0) return HelperPlayer.isThisContentTypeSupported("video/" + CT_VIDEO[i]);
            }
        } else if (ct.indexOf("audi") == 0) {
            for (int i = 0; i < CT_AUDIO.length; i++) {
                if (ct.indexOf(CT_AUDIO[i]) > 0) return HelperPlayer.isThisContentTypeSupported("audio/" + CT_AUDIO[i]);
            }
        }
        return null;
    }

    /**
	 * zeigt das Bild an, oder spielt das Audio / Video ab
	 * @param fs
	 */
    public static void doPreview(FileSelector fs) {
        MyByte b = new MyByte();
        try {
            System.out.println(fs.el.contentType);
            HelperApp.runJavaGarbageCollector();
            b.fileConn = (FileConnection) Connector.open(fs.el.fileName, Connector.READ);
            b.fileName = fs.el.fileName;
            b.delete = false;
            b.isTmpFile = false;
            b.in = b.fileConn.openInputStream();
            if (fs.el.contentType.indexOf("imag") == 0) {
                Image img = XMLTagImage.createImage(b.in, 2);
                b.close(false);
                MLE.midlet.setImageCanvas(new ScrollingImageCanvas(img, fs.formInfo));
            } else if (fs.el.contentType.indexOf("audi") == 0 || fs.el.contentType.indexOf("vide") == 0) {
                EventMediaPlayer ev = new EventMediaPlayer(b, false, fs.el.contentType, fs.formInfo, null);
                if (fs.ev != null) fs.ev.closeEvent(null);
                fs.ev = ev;
                ev.doEvent(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            b.close(false);
        }
    }

    public static void deleteFileConn(FileConnection con, boolean newThread) throws Exception {
        con.delete();
        System.out.println("deleteFileConn:" + fconnTemp + "," + con);
        if (fconnTemp != null && con.equals(fconnTemp)) removeTempFile(newThread);
    }

    public static FileConnection closeConnection(FileConnection in) {
        return closeConnectionReopen(in, null);
    }

    public static FileConnection closeConnectionReopen(FileConnection in, String filename) {
        try {
            if (in != null) {
                in.close();
                if (fconnTemp != null && in.equals(fconnTemp)) {
                    if (filename != null) {
                        fconnTemp = openFileConnection(filename);
                        return fconnTemp;
                    }
                    fconnTemp = null;
                    fconnInOpened = false;
                }
                if (filename != null) return openFileConnection(filename);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * reads the binary data of this file
	 * @param fileURL
	 * @return null if an error occured
	 */
    public static byte[] getFileData(String fileURL, RootThread thread, ProgressCanvas pb) {
        byte[] b = null;
        Closeable c = null;
        try {
            HelperApp.runJavaGarbageCollector();
            System.out.println("getFileData:" + fileURL);
            FileConnection fileConn = (FileConnection) Connector.open(fileURL, Connector.READ);
            InputStream in = fileConn.openInputStream();
            if (thread != null) c = Closeable.create(in, thread);
            if (pb != null) pb.setMaxValue((int) fileConn.fileSize());
            b = HelperStd.readBytesFromStream(in, pb);
            fileConn = HelperFileIO.closeConnection(fileConn);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (c != null) c.remove();
        return b;
    }

    public static long getFileSize(String fileURL) {
        FileConnection fileConn = null;
        try {
            HelperApp.runJavaGarbageCollector();
            System.out.println("getFileSize:" + fileURL);
            fileConn = (FileConnection) Connector.open(fileURL, Connector.READ);
            return fileConn.fileSize();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            HelperFileIO.closeConnection(fileConn);
        }
        return -1;
    }

    /**
	 * writes the filedata to this stream<br>
	 * doesn't flush out!!!
	 * @param fileURL
	 * @param out
	 */
    public static void writeFileData(String fileURL, OutputStream out, ProgressCanvas bc) throws IOException {
        System.out.println("writeFileData:" + fileURL);
        FileConnection fileConn = null;
        Closeable c = null;
        InputStream is = null;
        try {
            HelperApp.runJavaGarbageCollector();
            fileConn = (FileConnection) Connector.open(fileURL, Connector.READ);
            int blockSize = HelperHTTP.BUFFER_SIZE, read = 0, ergeb = 0, size = (int) fileConn.fileSize();
            if (bc != null) bc.setMaxValue(size);
            is = fileConn.openInputStream();
            if (bc != null) c = Closeable.create(is, bc.curThread);
            byte[] b = new byte[blockSize <= size ? blockSize : size];
            String status = "start";
            try {
                while ((ergeb = is.read(b)) != -1) {
                    status = "writing";
                    out.write(b, 0, ergeb);
                    read += ergeb;
                    if (read >= size) return;
                    if (size - read < b.length) b = new byte[size - read];
                    status = "rep";
                    if (bc != null) bc.setValueRePaint(read);
                    status = "reading";
                }
            } catch (Exception ee) {
                throw new IOException("read-write:" + ee.getMessage() + "::" + status);
            }
        } finally {
            if (c != null) c.remove();
            HelperStd.closeStream(is);
            HelperFileIO.closeConnection(fileConn);
        }
    }

    /**
	 * erstellt ein directory wenn data == null && delete=false<br>
	 * erstellt ein file wenn data != null && delete=false<br>
	 * überschreibt ein file wenn data != null && delete=false<br>
	 * löscht ein file oder directory wenn data == null && delete=true<br>
	 * @param fileURL
	 * @param data
	 * @param delete
	 */
    public static void setFileData(String fileURL, byte[] data, boolean delete, RootThread thread) throws IOException {
        HelperApp.runJavaGarbageCollector();
        System.out.println("setFileData:" + fileURL);
        if (delete) {
            deleteFile(fileURL);
            return;
        }
        FileConnection fileConn = (FileConnection) Connector.open(fileURL, Connector.READ_WRITE);
        Closeable c = null;
        if (!fileConn.exists()) {
            if (data == null) fileConn.mkdir(); else fileConn.create();
        }
        if (data != null) {
            OutputStream os = fileConn.openOutputStream();
            if (thread != null) c = Closeable.create(os, thread);
            os.write(data);
            HelperStd.closeStream(os);
        }
        if (c != null) c.remove();
        fileConn = HelperFileIO.closeConnection(fileConn);
    }

    /**
	 * the temp file
	 */
    private static FileConnection fconnTemp;

    private static volatile boolean fconnInOpened = false, fconnLocked = false;

    public static String tempFileUrl = null;

    public static String FILE_IO_ROOT_KEY = "_fiork";

    /**
	 * returns the default root file system,e nds with "/"
	 * @return
	 */
    public static String getDefaultRoot() {
        String root = HelperRMSStoreMLibera.getDataValue(FILE_IO_ROOT_KEY, null);
        String useragent = "mle";
        if (HelperStd.isEmpty(root)) {
            String root2 = null;
            FileConnection fconn = null;
            Enumeration e = FileSystemRegistry.listRoots();
            long fd = 0;
            while (e.hasMoreElements()) {
                try {
                    String temp = (String) e.nextElement();
                    fconn = (FileConnection) Connector.open("file://" + (temp.indexOf("/") == 0 ? "" : "/") + temp, Connector.READ);
                    if (fconn.availableSize() >= fd) {
                        root2 = temp;
                        if (temp.indexOf(':') >= 0) {
                            FileConnection ft = null;
                            try {
                                ft = (FileConnection) Connector.open("file://" + (temp.indexOf("/") == 0 ? "" : "/") + temp + useragent + "/", Connector.READ);
                                ft.mkdir();
                                ft.delete();
                                root = temp;
                            } catch (Exception h) {
                            } finally {
                                HelperFileIO.closeConnection(ft);
                            }
                        }
                        fd = fconn.availableSize();
                    }
                } catch (Exception es) {
                } finally {
                    HelperFileIO.closeConnection(fconn);
                }
            }
            try {
                if (HelperStd.isEmpty(root)) root = root2;
                if (root == null) root = "/";
                root += useragent + "/";
                root2 = "file://" + (root.indexOf("/") == 0 ? "" : "/") + root;
                System.out.println(root2);
                fconn = (FileConnection) Connector.open(root2, Connector.READ_WRITE);
                if (!fconn.exists()) fconn.mkdir();
                HelperRMSStoreMLibera.setDataValue(FILE_IO_ROOT_KEY, root2);
                root = HelperRMSStoreMLibera.getDataValue(FILE_IO_ROOT_KEY, null);
            } catch (Exception se) {
            } finally {
                HelperFileIO.closeConnection(fconn);
            }
        }
        return root;
    }

    /**
	 * generates the tmp-file-url
	 * @return
	 */
    public static String getTmpFile() {
        if (tempFileUrl != null) return tempFileUrl;
        tempFileUrl = getDefaultRoot() + "_____tmp.tmp";
        System.out.println(tempFileUrl);
        return tempFileUrl;
    }

    /**
	 * writes the content to this file
	 * @param file
	 * @param data
	 * @param in if data is null, this must be set
	 * @return
	 */
    public static void writeFile(String file, byte[] data, MyByte bin, InputStream in, long len, ProgressCanvas pb) throws Exception {
        FileConnection f = (FileConnection) Connector.open(file, Connector.READ_WRITE);
        OutputStream out = null;
        Closeable c = null;
        try {
            if (!f.exists()) f.create();
            out = f.openOutputStream();
            int blockSize = HelperHTTP.BUFFER_SIZE, read = 0;
            if (pb != null) c = Closeable.create(out, pb.curThread);
            if (data != null) {
                if (pb != null) {
                    pb.setMaxValue(data.length);
                    pb.setText("Byte Size:" + HelperStd.formatBytes(data.length));
                }
                while (read < data.length) {
                    if (read + blockSize > data.length) blockSize = data.length - read;
                    out.write(data, read, blockSize);
                    read += blockSize;
                    if (pb != null) pb.setValueRePaint(read);
                }
            } else {
                if (len <= 0) len = -1;
                if (len > 0) {
                    if (bin != null) {
                        bin.reopenInputStream();
                        in = bin.in;
                    } else in = openInputStreamTmpFile(in);
                }
                if (pb != null) {
                    if (len > 0) pb.setText("Stream Size:" + HelperStd.formatBytes(len)); else pb.setText("Writing Stream ...");
                    if (len > 0) pb.setMaxValue((int) len); else if (len < 0 && in.available() > 0) pb.setMaxValue(in.available()); else pb.setInfinite(true);
                }
                System.out.println(in.available() + ";" + len);
                while ((in.available() > 0 && len == -1) || read < len) {
                    byte[] b = new byte[blockSize];
                    int ergeb = in.read(b);
                    if (ergeb == -1) break;
                    out.write(b, 0, ergeb);
                    read += ergeb;
                    if (pb != null) pb.setValueRePaint(read);
                }
                System.out.println("finished:" + read);
            }
        } finally {
            if (c != null) c.remove();
            HelperStd.closeStream(out);
            HelperFileIO.closeConnection(f);
        }
    }

    private static int tmpCount = 0;

    /**
	 * opens the tmp outputstream to write to
	 * @return
	 */
    public static FileConnection writeTmpFile(PageSuite ps, StringBuffer fileName) {
        if (!fconnInOpened) {
            String tmp = getTmpFile();
            System.out.println("Using standard tmp file:" + tmp);
            if (fconnTemp == null) fconnTemp = openFileConnection(tmp);
            removeTempFile(false);
            try {
                fconnTemp = openFileConnection(tmp);
                fileName.append(tmp);
                fconnInOpened = true;
                return fconnTemp;
            } catch (Exception e) {
            }
        } else {
            String root = HelperRMSStoreMLibera.getDataValue(FILE_IO_ROOT_KEY, null);
            tempFileUrl = root + tmpCount + "_____tmp.tmp";
            tmpCount++;
            System.out.println("Using new tmp file:" + tempFileUrl);
            if (ps != null) ps.tmpFileURLs = HelperStd.incArray(ps.tmpFileURLs, tempFileUrl);
            try {
                fileName.append(tempFileUrl);
                return openFileConnection(tempFileUrl);
            } catch (Exception ee) {
            }
        }
        return null;
    }

    /**
	 * creates the target if it doesn't exist
	 * @param filename
	 * @return
	 */
    public static FileConnection openFileConnection(String filename) {
        try {
            FileConnection fconnTemp = (FileConnection) Connector.open(filename, Connector.READ_WRITE);
            if (!fconnTemp.exists()) fconnTemp.create();
            return fconnTemp;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * schließt die Datei und übergibt die Größe der Datei
	 * @param fconn
	 * @return
	 */
    public static long closeFile(FileConnection fconn) {
        long size = -1;
        try {
            size = fconn.fileSize();
            fconn.close();
        } catch (Exception e) {
        }
        return size;
    }

    /**
	 * opens an input stream to the tmp file (closes the old one, if set)
	 * @param url
	 * @return
	 * @throws Exception
	 */
    public static InputStream openInputStreamTmpFile(InputStream in) throws Exception {
        System.out.println(fconnTemp);
        HelperStd.closeStream(in);
        if (fconnInOpened) closeOpenTempFile();
        int c = 0;
        while (fconnLocked) {
            Thread.sleep(50);
            c++;
            if (c > 100) break;
        }
        if (fconnTemp == null) fconnTemp = (FileConnection) Connector.open(getTmpFile(), Connector.READ);
        System.out.println(fconnTemp.fileSize() + "," + fconnTemp);
        fconnInOpened = true;
        return fconnTemp.openInputStream();
    }

    /**
	 * closes the tmp file
	 *
	 */
    public static void closeOpenTempFile() {
        fconnTemp = closeConnection(fconnTemp);
        fconnInOpened = false;
        fconnLocked = false;
    }

    /**
	 * deletes the tmp file
	 *
	 */
    public static void removeTempFile(boolean newThread) {
        if (!fconnInOpened) return;
        System.out.println("removing tmp file");
        closeOpenTempFile();
        if (newThread) {
            new Thread() {

                public void run() {
                    removeTmpFileReal();
                }
            }.start();
            return;
        }
        removeTmpFileReal();
    }

    private static void removeTmpFileReal() {
        try {
            if (fconnTemp != null || fconnLocked) return;
            fconnLocked = true;
            fconnTemp = (FileConnection) Connector.open(getTmpFile(), Connector.READ_WRITE);
            if (fconnTemp.exists()) fconnTemp.delete();
            fconnTemp.close();
            fconnTemp = null;
            fconnLocked = false;
        } catch (Exception e) {
        }
    }

    /**
	 * open a file wrapped in a MyByte struct
	 * @param url
	 * @return
	 */
    public static MyByte openFile(String url) {
        MyByte b = new MyByte();
        try {
            b.delete = false;
            b.isTmpFile = false;
            b.fileConn = openFileConnection(url);
            b.fileName = url;
            b.len = b.fileConn.fileSize();
            b.in = b.fileConn.openInputStream();
        } catch (Exception e) {
            if (b.fileConn != null) closeConnection(b.fileConn);
        }
        if (b.fileConn == null) return null;
        return b;
    }

    /**
	 * benennt eine Datei um
	 * @param url
	 * @param newFileName nur der datei-name nicht die URL
	 */
    public static void renameFile(String url, String newFileName) {
        FileConnection f = null;
        try {
            f = openFileConnection(url);
            f.rename(newFileName);
        } catch (Exception e) {
        } finally {
            HelperFileIO.closeConnection(f);
        }
    }

    /**
	 * deletes a file or directory
	 * @param file
	 */
    public static void deleteFile(String file) {
        FileConnection f = openFileConnection(file);
        try {
            if (f != null && f.exists()) f.delete();
            System.out.println("deltetedFile:" + file);
        } catch (Exception e) {
            try {
                String[] del = HelperConverter.getStringArrayFromByte(HelperRMSStoreMLibera.getDataObjectValue(RMS_KEY_FILE_DEL, null));
                del = HelperStd.incArray(del, file);
                HelperRMSStoreMLibera.setAppData(14, 1);
                HelperRMSStoreMLibera.setDataObjectValue(RMS_KEY_FILE_DEL, HelperConverter.getByte(del));
            } catch (Exception ee) {
            }
        } finally {
            HelperFileIO.closeConnection(f);
        }
    }

    private static String RMS_KEY_FILE_DEL = "_F_DEL";

    /**
	 * löscht alle files, die seit dem letzten start nicht gelöscht werden konnten
	 *
	 */
    public static void cleanOldNotDeletedFiles() {
        if (HelperRMSStoreMLibera.getAppData(14) != 1) return;
        try {
            byte[] b = HelperRMSStoreMLibera.getDataObjectValue(RMS_KEY_FILE_DEL, null);
            if (b == null) return;
            String[] del = HelperConverter.getStringArrayFromByte(b);
            if (del == null) return;
            for (int i = 0; i < del.length; i++) {
                deleteFile(del[i]);
            }
            HelperRMSStoreMLibera.setDataObjectValue(RMS_KEY_FILE_DEL, null);
            HelperRMSStoreMLibera.setAppData(14, 0);
        } catch (Exception ee) {
        }
    }
}
