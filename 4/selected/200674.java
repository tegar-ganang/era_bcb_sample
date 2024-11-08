package com.prolix.editor.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import uk.ac.reload.straker.datamodel.learningdesign.LearningDesign;
import uk.ac.reload.straker.datamodel.learningdesign.types.ItemContainer;
import uk.ac.reload.straker.datamodel.learningdesign.types.ItemType;
import com.prolix.editor.LDT_Constrains;

public class FilesManager {

    /**
	 * Returns the absolute path where the files should be stored
	 * @param ld
	 * @return
	 */
    public static String getRootFilePath(LearningDesign ld) {
        return ld.getRootFolder() + "/";
    }

    /**
	 * refreshes the Resourchen of a Learning Design
	 * @param ld
	 */
    public static void refreshResourches(LearningDesign ld) {
        ld.getResources().parse();
    }

    /**
	 * Set a new Text into the File that is linked to the item
	 * @param item
	 * @param newText
	 * @return
	 */
    public static boolean modItemFile(ItemType item, String newText) {
        return modTextFile(item.getIdentifierRef(), newText, item.getLearningDesign());
    }

    /**
	 * creates a new File and attach it to the Item Container
	 * @param filename
	 * @param text
	 * @param itemContainer
	 * @return
	 */
    public static boolean attachNewFile(String filename, String text, ItemContainer itemContainer) {
        if (!genTextFile(filename, itemContainer.getLearningDesign())) return false;
        if (!modTextFile(filename, text, itemContainer.getLearningDesign())) return false;
        return attachFile(filename, itemContainer);
    }

    /**
	 * Attach a file to an Item Container
	 * @param filename
	 * @param itemContainer
	 * @return
	 */
    public static boolean attachFile(String filename, ItemContainer itemContainer) {
        if (getTextFile(filename, itemContainer.getLearningDesign()) == null) return false;
        ItemType it = new ItemType(itemContainer.getDataModel());
        it.setIdentifierRef(filename);
        itemContainer.addChild(it);
        return true;
    }

    /**
	 * Attach the WaitingActivityFile to a WaitingActivity
	 * @param itemContainer
	 * @return
	 */
    public static boolean attachWaitActivity(ItemContainer itemContainer) {
        genWaitActivityText(itemContainer.getLearningDesign());
        return attachFile(LDT_Constrains.WaitActivityPath, itemContainer);
    }

    /**
	 * generates the Textfile for a Waiting Activity
	 * @param ld
	 * @return
	 */
    private static boolean genWaitActivityText(LearningDesign ld) {
        if (genTextFile(LDT_Constrains.WaitActivityPath, ld)) return modTextFile(LDT_Constrains.WaitActivityPath, LDT_Constrains.WaitActivityContent, ld);
        return false;
    }

    /**
	 * generates a new Textfile
	 * @param name
	 * @param ld
	 * @return
	 */
    private static boolean genTextFile(String name, LearningDesign ld) {
        File newFile = new File(getRootFilePath(ld) + name);
        if (newFile.exists()) return false;
        try {
            return newFile.createNewFile();
        } catch (IOException e) {
            return false;
        }
    }

    /**
	 * Returns a existing Textfile
	 * @param name
	 * @param ld
	 * @return
	 */
    private static File getTextFile(String name, LearningDesign ld) {
        File newFile = new File(getRootFilePath(ld) + name);
        if (newFile.exists() && newFile.isFile()) return newFile;
        return null;
    }

    /**
	 * Modifies a Textfile
	 * @param name
	 * @param newText
	 * @param ld
	 * @return
	 */
    private static boolean modTextFile(String name, String newText, LearningDesign ld) {
        File file = getTextFile(name, ld);
        if (file == null) return false;
        try {
            FileWriter write = new FileWriter(file);
            write.write(newText);
            write.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static String getTextFromFile(String name) {
        try {
            String tmp;
            StringBuffer buffer = new StringBuffer();
            File file = new File(name);
            if (file.exists() && file.isFile()) {
                FileReader fileReader = new FileReader(name);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                while ((tmp = bufferedReader.readLine()) != null) buffer.append(tmp);
                bufferedReader.close();
                return buffer.toString();
            } else return "<no content contained or wrong filename specified>";
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * reads the content of a file stored in the content folder
	 * @param learningDesign
	 * @param filename
	 * @return file content (String)
	 */
    public static String getFileContent(LearningDesign learningDesign, String filename) {
        return getTextFromFile(getRootFilePath(learningDesign) + filename);
    }

    public static boolean saveItemModelDescription(LearningDesign ld, String text, String filename) {
        try {
            File file = new File(getRootFilePath(ld) + filename);
            if (file == null) return false;
            FileWriter writer = new FileWriter(file);
            writer.write(text);
            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
	 * copy a file from the source location to the target location
	 * @param source
	 * @param target
	 * @return
	 */
    public static boolean copyFileToContentFolder(String source, LearningDesign learningDesign) {
        File inputFile = new File(source);
        File outputFile = new File(getRootFilePath(learningDesign) + inputFile.getName());
        FileReader in;
        try {
            in = new FileReader(inputFile);
            FileWriter out = new FileWriter(outputFile);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
	 * mehtod to delete a file from the content folder (e.g edit a component)
	 * @param filename
	 * @param learningDesign
	 * @return
	 */
    public static boolean deleteFile(String filename, LearningDesign learningDesign) {
        File file = new File(getRootFilePath(learningDesign) + filename);
        return file.delete();
    }
}
