package tico.imageGallery.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import tico.configuration.TLanguage;
import tico.editor.TEditor;
import tico.imageGallery.dataBase.TIGDataBase;

public class TIGImportTask {

    private int lengthOfTask;

    private int current = 0;

    private String statMessage;

    private TEditor myEditor;

    private TIGDataBase myDataBase;

    private String myDirectoryPath;

    private String myImagesBehaviour;

    private String errorImages = "";

    private boolean stop = false;

    private boolean cancel = false;

    private boolean running = false;

    public TIGImportTask() {
        lengthOfTask = 1000;
    }

    public void go(TEditor editor, TIGDataBase dataBase, String directoryPath, String imagesBehaviour) {
        current = 0;
        running = true;
        this.myEditor = editor;
        this.myDataBase = dataBase;
        this.myDirectoryPath = directoryPath;
        this.myImagesBehaviour = imagesBehaviour;
        final SwingWorker worker = new SwingWorker() {

            public Object construct() {
                return new ActualTask(myEditor, myDataBase, myDirectoryPath, myImagesBehaviour);
            }
        };
    }

    public int getLengthOfTask() {
        return lengthOfTask;
    }

    public void setLengthOfTask(int num) {
        lengthOfTask = num;
    }

    public int getCurrent() {
        return current;
    }

    public String getErrorImages() {
        return errorImages;
    }

    public void stop() {
        stop = true;
        running = false;
    }

    public void cancel() {
        cancel = true;
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean done() {
        if (current >= lengthOfTask) return true; else return false;
    }

    public String getMessage() {
        return statMessage;
    }

    private boolean exists(String list[], String name) {
        boolean exists = false;
        int i = 0;
        while ((i < list.length) && !exists) {
            exists = name.equals(list[i]);
            i++;
        }
        return exists;
    }

    public class ActualTask {

        public ActualTask(TEditor editor, TIGDataBase dataBase, String directoryPath, String myImagesBehaviour) {
            File myDirectory = new File(directoryPath);
            String[] list = myDirectory.list();
            File fileXML = new File(directoryPath + "images.xml");
            SAXBuilder builder = new SAXBuilder(false);
            try {
                Document docXML = builder.build(fileXML);
                Element root = docXML.getRootElement();
                List images = root.getChildren("image");
                Iterator j = images.iterator();
                int i = 0;
                TIGDataBase.activateTransactions();
                while (j.hasNext() && !stop && !cancel) {
                    current = i;
                    i++;
                    Element image = (Element) j.next();
                    String name = image.getAttributeValue("name");
                    List categories = image.getChildren("category");
                    Iterator k = categories.iterator();
                    if (exists(list, name)) {
                        String pathSrc = directoryPath.concat(name);
                        String pathDst = System.getProperty("user.dir") + File.separator + "images" + File.separator + name.substring(0, 1).toUpperCase() + File.separator;
                        String folder = System.getProperty("user.dir") + File.separator + "images" + File.separator + name.substring(0, 1).toUpperCase();
                        if (myImagesBehaviour.equals(TLanguage.getString("TIGImportDBDialog.REPLACE_IMAGES"))) {
                            Vector<Vector<String>> aux = TIGDataBase.imageSearchByName(name.substring(0, name.lastIndexOf('.')));
                            if (aux.size() != 0) {
                                int idImage = TIGDataBase.imageKeySearchName(name.substring(0, name.lastIndexOf('.')));
                                TIGDataBase.deleteAsociatedOfImage(idImage);
                            }
                            pathDst = pathDst.concat(name);
                        }
                        if (myImagesBehaviour.equals(TLanguage.getString("TIGImportDBDialog.ADD_IMAGES"))) {
                            Vector aux = new Vector();
                            aux = TIGDataBase.imageSearchByName(name.substring(0, name.lastIndexOf('.')));
                            int fileCount = 0;
                            if (aux.size() != 0) {
                                while (aux.size() != 0) {
                                    fileCount++;
                                    aux = TIGDataBase.imageSearchByName(name.substring(0, name.lastIndexOf('.')) + "_" + fileCount);
                                }
                                pathDst = pathDst + name.substring(0, name.lastIndexOf('.')) + '_' + fileCount + name.substring(name.lastIndexOf('.'), name.length());
                                name = name.substring(0, name.lastIndexOf('.')) + '_' + fileCount + name.substring(name.lastIndexOf('.'), name.length());
                            } else {
                                pathDst = pathDst.concat(name);
                            }
                        }
                        String pathThumbnail = (pathDst.substring(0, pathDst.lastIndexOf("."))).concat("_th.jpg");
                        File newDirectoryFolder = new File(folder);
                        if (!newDirectoryFolder.exists()) {
                            newDirectoryFolder.mkdirs();
                        }
                        try {
                            FileChannel srcChannel = new FileInputStream(pathSrc).getChannel();
                            FileChannel dstChannel = new FileOutputStream(pathDst).getChannel();
                            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                            srcChannel.close();
                            dstChannel.close();
                        } catch (IOException exc) {
                            System.out.println(exc.getMessage());
                            System.out.println(exc.toString());
                        }
                        TIGDataBase.insertImageDB(name.substring(0, name.lastIndexOf('.')), name);
                        int idImage = TIGDataBase.imageKeySearchName(name.substring(0, name.lastIndexOf('.')));
                        while (k.hasNext()) {
                            Element category = (Element) k.next();
                            int idCategory = TIGDataBase.insertConceptDB(category.getValue());
                            TIGDataBase.insertAsociatedDB(idCategory, idImage);
                        }
                    } else {
                        errorImages = errorImages + System.getProperty("line.separator") + name;
                    }
                }
                TIGDataBase.executeQueries();
                current = lengthOfTask;
            } catch (JDOMException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
