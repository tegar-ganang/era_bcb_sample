package pt.iscte.dcti.visual_tracer.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtils {

    public static String getExtension(File file) {
        String ext = "";
        int indexBeginExtension = file.getName().lastIndexOf(".");
        if (indexBeginExtension != -1) ext = file.getName().substring(indexBeginExtension + 1);
        return ext;
    }

    public static boolean copyFileToDir(File inputFile, File outputDir) {
        try {
            String outputFileName = inputFile.getName();
            int index = 1;
            while (existFileInDir(outputFileName, outputDir)) {
                outputFileName = index + inputFile.getName();
                index++;
            }
            String directory = getDirectoryWithSlash(outputDir.getAbsolutePath());
            File outputFile = new File(directory + outputFileName);
            FileReader in = new FileReader(inputFile);
            FileWriter out = new FileWriter(outputFile);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static boolean existFileInDir(String fileName, File dir) {
        String directory = getDirectoryWithSlash(dir.getAbsolutePath());
        return new File(directory + fileName).exists();
    }

    /**
	 * Create a file with name and extension based on file argument in a directory and with a content
	 * @param content - content that file may contain
	 * @param directory - directory to save the file in
	 * @param file - the name and extension of the file
	 * @return What?
	 */
    public static boolean createFile(String content, String directory, String file, boolean append) {
        boolean response = true;
        try {
            directory = getDirectoryWithSlash(directory);
            File file_directory = new File(directory);
            if (!file_directory.exists()) file_directory.mkdirs();
            File file_file = new File(directory + file);
            response = file_file.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(file_file, append));
            out.write(content);
            out.close();
        } catch (Exception ex) {
            response = false;
        }
        return response;
    }

    /**
	 * Create a file with name and extension based on file argument in a directory and with a content
	 * #param content - content that file may contain
	 * #param directory - directory to save the file in
	 * @param file - the name and extension of the file
	 * @return What?
	 * @throws IOException 
	 */
    public static String getTextFile(File file) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String stringReader = "";
        while (bufferedReader.ready()) stringReader += bufferedReader.readLine() + "\n";
        bufferedReader.close();
        return stringReader;
    }

    /**
	 * Create a file with name and extension based on file argument in a directory and with a content
	 * @param content - content that file may contain
	 * #param directory - directory to save the file in
	 * @param file - the name and extension of the file
	 * @return What?
	 * @throws IOException 
	 */
    public static boolean saveTextFile(String content, File file, boolean append) throws IOException {
        boolean response = true;
        BufferedWriter out = new BufferedWriter(new FileWriter(file, append));
        out.write(content);
        out.close();
        return response;
    }

    /**
	 * Validate if this string have final slash
	 * e.g. C:\folder\ or C:\folder/
	 * @param directory
	 * @return What?
	 */
    public static String getDirectoryWithSlash(String directory) {
        if (directory.lastIndexOf("\\") != (directory.length() - 1) && directory.lastIndexOf("/") != (directory.length() - 1)) directory += "\\";
        return directory;
    }
}
