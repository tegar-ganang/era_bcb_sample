package httpanalyzer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import org.apache.http.HttpEntity;

/**
 *
 * @author vlad
 */
public class HttpFileUtils {

    static final int MAX_BUFFER_SIZE = 8192;

    /**
     * Save Http entity to file
     * @param entity HttpEntity
     * @param fileName String
     */
    public long saveEntity(HttpEntity entity, String fileName) {
        long size = 0L;
        File file = new File(fileName);
        byte buffer[] = new byte[MAX_BUFFER_SIZE];
        InputStream streamEntity;
        RandomAccessFile outFile;
        try {
            streamEntity = entity.getContent();
            outFile = new RandomAccessFile(file, "rw");
            int read = 0;
            outFile.setLength(0);
            System.out.println("Buffer size =" + buffer.length);
            while ((read = streamEntity.read(buffer)) != -1) {
                size = size + read;
                outFile.write(buffer, 0, read);
            }
            outFile.close();
            streamEntity.close();
        } catch (IOException ex) {
            Logger.getLogger(HttpFileUtils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalStateException ex) {
            Logger.getLogger(HttpFileUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return size;
    }

    /**
     * Filter for FileChooser *.XML
     */
    private class MyXmlFilter extends javax.swing.filechooser.FileFilter {

        @Override
        public boolean accept(File file) {
            return file.isDirectory() || file.getAbsolutePath().endsWith(".xml");
        }

        @Override
        public String getDescription() {
            return "XML files (*.xml)";
        }
    }

    /**
     * Filter for FileChooser *.TXT
     */
    private class MyTxtFilter extends javax.swing.filechooser.FileFilter {

        @Override
        public boolean accept(File file) {
            return file.isDirectory() || file.getAbsolutePath().endsWith(".txt");
        }

        @Override
        public String getDescription() {
            return "Text documents (*.txt)";
        }
    }

    /**
     * Method saves your template to file
     * Creates JFileChooser and save into XML file
     * @param properties
     * @param parentFrame
     */
    public void savePreferenceToFile(Properties properties, JFrame parentFrame) {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save template");
        fileChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        fileChooser.addChoosableFileFilter(new MyTxtFilter());
        fileChooser.addChoosableFileFilter(new MyXmlFilter());
        int returnVal = fileChooser.showSaveDialog(parentFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                String fileSetUser = fileChooser.getSelectedFile().toString();
                File file;
                if (fileSetUser.contains(".")) {
                    file = new File(fileSetUser);
                } else {
                    file = new File(fileSetUser + ".xml");
                }
                FileOutputStream fos = new FileOutputStream(file);
                properties.storeToXML(fos, "HttpAnalyzer Template", "UTF-8");
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(HttpFileUtils.class.getName()).log(Level.SEVERE, null, ex);
                new SwingTools(parentFrame).showErrorDialog("IO error", ex.getLocalizedMessage());
            }
        } else {
            System.out.println("File wasn't choosen!");
        }
    }

    /**
     * Method load your template from file
     * Creates JFileChooser and  load from XML file
     * @param parentFrame
     */
    public Properties loadPreferenceFromFile(JFrame parentFrame) {
        Properties properties = new Properties();
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load template");
        fileChooser.setDialogType(javax.swing.JFileChooser.OPEN_DIALOG);
        fileChooser.addChoosableFileFilter(new MyTxtFilter());
        fileChooser.addChoosableFileFilter(new MyXmlFilter());
        int returnVal = fileChooser.showOpenDialog(parentFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                String fileSetUser = fileChooser.getSelectedFile().toString();
                File file;
                if (fileSetUser.contains(".")) {
                    file = new File(fileSetUser);
                } else {
                    file = new File(fileSetUser + ".xml");
                }
                FileInputStream fis = new FileInputStream(file);
                properties.loadFromXML(fis);
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(HttpFileUtils.class.getName()).log(Level.SEVERE, null, ex);
                new SwingTools(parentFrame).showErrorDialog("IO error", ex.getLocalizedMessage());
            }
        } else {
            System.out.println("File wasn't choosen!");
        }
        return properties;
    }

    /**
     * Save information from ReplayTab in file
     * @param mainView
     * @param parentFrame
     */
    public void saveSessionInfo(HttpAnalyzerView mainView, JFrame parentFrame) {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save your session");
        fileChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        Calendar calendar = Calendar.getInstance();
        System.out.print("Date: ");
        System.out.print(calendar.get(Calendar.MONTH));
        System.out.print(" " + calendar.get(Calendar.DATE) + " ");
        System.out.println(calendar.get(Calendar.YEAR));
        String preFileName = "session-" + Integer.toString(calendar.get(Calendar.DATE)) + "-" + Integer.toString(calendar.get(Calendar.MONTH)) + ".txt";
        fileChooser.setSelectedFile(new File(preFileName));
        int returnVal = fileChooser.showSaveDialog(parentFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String fileName = fileChooser.getSelectedFile().toString();
            File file = null;
            System.out.println("File choose =" + fileName);
            if (!fileName.toString().endsWith(".txt")) {
                file = new File(fileName + ".txt");
            } else {
                file = new File(fileName);
            }
            PrintWriter outWriter = null;
            try {
                outWriter = new PrintWriter(file);
                String str[] = mainView.replayDataPane.getText().split("\n");
                for (int i = 0; i < str.length; i++) {
                    outWriter.print(str[i] + "\r\n");
                }
            } catch (Exception ex) {
                new SwingTools(parentFrame).showErrorDialog("IO error", ex.getLocalizedMessage());
            } finally {
                if (outWriter != null) {
                    outWriter.close();
                }
            }
        } else {
            System.out.println("File wasn't choosen!");
        }
    }

    /**
     * Show JChooseFile dialog and
     * put your choice in toFileTextField
     * @param mainView
     * @param parentFrame
     */
    public void fillToFileField(HttpAnalyzerView mainView, JFrame parentFrame) {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save your content");
        fileChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        int returnVal = fileChooser.showSaveDialog(parentFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            mainView.toFileTextField.setText(fileChooser.getSelectedFile().toString());
            mainView.toFileCheckBox.setSelected(true);
        } else {
            System.out.println("File wasn't choosen!");
        }
    }
}
