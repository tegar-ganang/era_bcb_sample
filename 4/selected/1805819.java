package com.kitten.utilities;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;
import com.kitten.constants.KittenConstants;
import com.kitten.dao.KittenExportResultDao;
import com.kitten.gui.IKittenLayoutIssues;
import com.kitten.gui.IKittenView;

public class KittenUtilities implements IKittenLayoutIssues {

    private static Logger log = Logger.getLogger(KittenUtilities.class.getName());

    private KittenProperties kittenProperties;

    public KittenUtilities(KittenProperties kittenProperties) {
        try {
            log.addHandler(kittenProperties.getLogFileHandler());
        } catch (Exception e) {
        }
    }

    public static void playAudioFile(InputStream is, KittenProperties kittenProperties) {
        if (kittenProperties.getUserProperty("soundOn") != null && kittenProperties.getUserProperty("soundOn").equals("true")) {
            try {
                AudioStream as = new AudioStream(is);
                AudioPlayer.player.start(as);
            } catch (Exception e) {
                log.warning("Problem playing sound clip..." + e);
                e.printStackTrace();
            }
        }
    }

    public static final void openFile(IKittenView kittenView, KittenProperties kittenProperties) {
        JFrame frame = (JFrame) kittenView.getParentFrame();
        JFileChooser fc = new JFileChooser(kittenProperties.getInstallDirectory() + System.getProperty("file.separator") + "kitten" + System.getProperty("file.separator") + "scripts");
        fc.setFont(fileChooserFont);
        int returnVal = fc.showOpenDialog(frame);
        File selFile = fc.getSelectedFile();
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (readFile(selFile) != null) {
                kittenView.writeOpenFile2Screen(readFile(selFile));
                kittenView.setOpenedFile(selFile.getPath());
            }
        }
    }

    public static final synchronized void saveAsFile(IKittenView kittenView, KittenProperties kittenProperties, String title) {
        JFrame frame = (JFrame) kittenView.getParentFrame();
        String saveString = kittenView.getScreenText2Save2File();
        JFileChooser fc = new JFileChooser(kittenProperties.getInstallDirectory() + System.getProperty("file.separator") + "kitten" + System.getProperty("file.separator") + "scripts");
        File selFile = fc.getSelectedFile();
        fc.setDialogTitle(title);
        fc.setApproveButtonText(title);
        int returnVal = fc.showSaveDialog(frame);
        selFile = fc.getSelectedFile();
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            write2File(selFile, saveString);
        }
        if (selFile != null) {
            kittenView.setOpenedFile(selFile.getPath());
            log.info("Saving file: " + kittenView.getOpenedFile());
        }
        fc.setFont(fileChooserFont);
    }

    public static final synchronized void saveFile(IKittenView kittenView, KittenProperties kittenProperties) {
        JFrame frame = (JFrame) kittenView.getParentFrame();
        String saveString = kittenView.getScreenText2Save2File();
        if (kittenView.getOpenedFile() != null && !kittenView.getOpenedFile().equals("")) {
            File selFile = new File(kittenView.getOpenedFile());
            if (selFile.exists()) {
                write2File(selFile, saveString);
                kittenView.setOpenedFile(selFile.getPath());
                log.info("Saving file: " + kittenView.getOpenedFile());
            } else {
                log.warning("File '" + kittenView.getOpenedFile() + "' does not exist!");
            }
        } else {
            saveAsFile(kittenView, kittenProperties, kittenProperties.getApplicationProperty("mSave"));
        }
    }

    public static final synchronized void revertFile(IKittenView kittenView, KittenProperties kittenProperties) {
        if (kittenView.getOpenedFile() != null && !kittenView.getOpenedFile().equals("")) {
            File selFile = new File(kittenView.getOpenedFile());
            if (selFile.exists()) {
                if (readFile(selFile) != null) {
                    kittenView.writeOpenFile2Screen(readFile(selFile));
                    kittenView.setOpenedFile(selFile.getPath());
                }
            } else {
                log.warning("Could not revert file '" + kittenView.getOpenedFile() + "' because it does not exist!");
            }
        }
    }

    private static synchronized void write2File(File file, String saveString) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.write(saveString);
            out.close();
        } catch (IOException e) {
            log.warning("Error saving file '" + file.getName() + "'!" + e.toString());
        }
    }

    private static synchronized String readFile(File selFile) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(selFile));
            String text = "";
            String str;
            while ((str = in.readLine()) != null) {
                text = text + str + "\n";
            }
            return text;
        } catch (IOException io) {
            log.warning("Could not open file " + selFile.getName() + " for reading! " + io);
            return null;
        }
    }

    public static synchronized void saveExportFile(KittenProperties kittenProperties, KittenExportResultDao kittenExportResultDao) {
        StringBuffer data2Write = new StringBuffer();
        String separationChar = kittenExportResultDao.getSeparator();
        if (separationChar == null || separationChar.equals("")) {
            separationChar = KittenConstants.DEFAULT_SEPARATION_CHARACTER_EXPORT;
        }
        for (int i = 0; i < kittenExportResultDao.getResults().size(); i++) {
            for (int j = 1; j < ((Vector) kittenExportResultDao.getResults().get(i)).size(); j++) {
                try {
                    if (i == 0 && j == 1) {
                        data2Write.insert(0, ((Vector) kittenExportResultDao.getResults().get(i)).get(j).toString() + separationChar);
                    } else if (j == ((Vector) kittenExportResultDao.getResults().get(i)).size() - 1) {
                        data2Write.append(((Vector) kittenExportResultDao.getResults().get(i)).get(j).toString());
                    } else {
                        data2Write.append(((Vector) kittenExportResultDao.getResults().get(i)).get(j).toString() + separationChar);
                    }
                } catch (NullPointerException n) {
                    if (i == 0 && j == 1) {
                        data2Write.insert(0, separationChar);
                    } else if (j == ((Vector) kittenExportResultDao.getResults().get(i)).size() - 1) {
                    } else {
                        data2Write.append(separationChar);
                    }
                }
            }
            if (i < kittenExportResultDao.getResults().size() - 1) {
                data2Write.append("\n");
            }
        }
        String path = kittenExportResultDao.getPath();
        if (kittenExportResultDao.getPath() == null || kittenExportResultDao.getPath().equals("")) {
            path = "KittenExport";
        }
        try {
            if (path.indexOf(System.getProperty("file.separator")) < 0) {
                if (kittenProperties.getUserProperty("defaultExportPath") != null && !"".equals(kittenProperties.getUserProperty("defaultExportPath"))) {
                    if (kittenProperties.getUserProperty("defaultExportPath").trim().lastIndexOf(System.getProperty("file.separator")) + 1 == kittenProperties.getUserProperty("defaultExportPath").trim().length()) {
                        path = kittenProperties.getUserProperty("defaultExportPath") + path;
                    } else {
                        path = kittenProperties.getUserProperty("defaultExportPath").trim() + System.getProperty("file.separator") + path;
                    }
                } else {
                    path = kittenProperties.getInstallDirectory() + System.getProperty("file.separator") + "kitten" + System.getProperty("file.separator") + "export" + System.getProperty("file.separator") + path;
                }
            }
            FileWriter writer = new FileWriter(path);
            BufferedWriter out = new BufferedWriter(writer);
            out.write(data2Write.toString());
            out.close();
            log.info("Writing export file: " + path + "...");
        } catch (IOException io) {
            log.warning("Could not export file..." + io);
        }
    }

    public static String retrieveNthWordInString(String string, int occurrence, String split) {
        String word = null;
        int n = 0;
        if (string != null) {
            for (int i = 0; i < string.split(split).length; i++) {
                if (string.split(split)[i].replaceAll(" ", null).length() > 0) {
                    n++;
                    if (occurrence == n) {
                        word = string.split(split)[i];
                        break;
                    }
                }
            }
        }
        return word;
    }

    public static synchronized List readXml2ObjectList(String xmlFile) {
        List objectList = new ArrayList();
        try {
            XMLDecoder d = new XMLDecoder(new BufferedInputStream(new FileInputStream(new File(xmlFile).getCanonicalPath())));
            try {
                while (true) {
                    Object object = d.readObject();
                    objectList.add(object);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
            }
            d.close();
        } catch (Exception e) {
            log.warning("Could not read XML (" + xmlFile + ") to object! " + e);
            return null;
        }
        return objectList;
    }

    public static synchronized void writeObjects2Xml(HashMap objects, String xmlFile, KittenProperties kittenProperties) {
        try {
            XMLEncoder ex = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(new File(kittenProperties.getInstallDirectory() + System.getProperty("file.separator") + "kitten" + System.getProperty("file.separator") + xmlFile))));
            Iterator i = objects.keySet().iterator();
            while (i.hasNext()) {
                String key = (String) i.next();
                if (objects.get(key) != null) {
                    ex.writeObject(objects.get(key));
                }
            }
            ex.close();
        } catch (Exception exc) {
            log.warning("Could not write objects to XML file (" + xmlFile + ")! " + exc);
        }
    }

    public static synchronized String[] splitStringOnCharacter(String string, char splitCharacter, char escapeCharacter) {
        String[] splitString = null;
        if (string != null && !"".equals(string)) {
            ArrayList tempList = new ArrayList();
            char[] stringCharArray = string.toCharArray();
            int countQuotes = 0;
            int startPosition = -1;
            int finalPosition = 0;
            for (int i = 0; i < stringCharArray.length; i++) {
                if (stringCharArray[i] == escapeCharacter) {
                    countQuotes++;
                }
                if (stringCharArray[i] == splitCharacter && countQuotes % 2 == 0) {
                    tempList.add(string.substring(startPosition + 1, i));
                    startPosition = i;
                    finalPosition = startPosition;
                }
            }
            if (string.substring(finalPosition + 1) != null && !string.substring(finalPosition + 1).equals("")) {
                tempList.add(string.substring(finalPosition + 1));
            }
            splitString = new String[tempList.size()];
            for (int i = 0; i < tempList.size(); i++) {
                splitString[i] = (String) tempList.get(i);
            }
        }
        return splitString;
    }

    public static Object convertObject(Object value, String clazz) {
        Object returnObject = null;
        try {
            Class c = (Class.forName(clazz));
            Constructor[] con = c.getConstructors();
            Object[] params = new Object[1];
            params[0] = value;
            for (int i = 0; i < con.length; i++) {
                try {
                    if (clazz.equals(KittenConstants.JAVA_SQL_DATE)) {
                        returnObject = java.sql.Date.valueOf(value.toString());
                    } else if (clazz.equals(KittenConstants.JAVA_SQL_CLOB)) {
                        returnObject = con[i].newInstance(params);
                    } else {
                        returnObject = con[i].newInstance(params);
                    }
                    break;
                } catch (IllegalArgumentException ia) {
                } catch (Exception et) {
                    log.warning("Error(1) during conversion of datatype for value:" + value + " and clazz: " + clazz + "..." + et);
                }
            }
        } catch (Exception e) {
            log.warning("Error(2) during conversion of datatype..." + e);
            if (returnObject == null) {
                returnObject = value;
            }
        }
        if (returnObject == null && value != null) {
            returnObject = value;
        }
        return returnObject;
    }

    public static boolean checkOnlySpaces(String str) {
        if (retrieveNthWordInString(str.trim(), 1, " ") == null) {
            return true;
        } else {
            return false;
        }
    }

    public static String getSchemaNameAsString(String treePath) {
        int firstComma = treePath.indexOf(",");
        int secondComma = treePath.indexOf(",", firstComma + 1);
        int thirdComma = treePath.indexOf(",", secondComma + 1);
        String schemaName = treePath.substring(secondComma + 1, thirdComma).trim();
        return schemaName;
    }
}
