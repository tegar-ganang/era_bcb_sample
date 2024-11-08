package tools.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class FileToolsContainer {

    /**
	 * The openFile method will open a file and if the opening is successful will return a Scanner object.
	 * @param filenameP The name of the file to be opened
	 * @return
	 */
    public static Scanner openFile(String filenameP) {
        Scanner myScanner = null;
        try {
            myScanner = new Scanner(new File(filenameP));
        } catch (FileNotFoundException e) {
            System.out.println();
            System.out.println("**********FILE NOT FOUND ERROR**********");
        }
        return myScanner;
    }

    /**
	 * The countLinesInFile method opens a file, counts the number of lines in the file and returns that number
	 * @param fileName the name of the file to open.
	 * @return Returns an integer that denotes the number of lines in the file.
	 */
    public static int countLinesInFile(String fileName) {
        Scanner myFile = openFile(fileName);
        int countlines = 0;
        while (myFile.hasNextLine()) {
            countlines++;
        }
        return countlines;
    }

    /**
	 * The method createFile, creates a file (opens one) for writing. It returns a BufferedWriter object.
	 * @param filenameP the desired filename for the file that is created.
	 * @return Returns a BufferedWriter object.
	 */
    public static BufferedWriter createFile(String filenameP) {
        FileWriter myFileWriter = null;
        try {
            myFileWriter = new FileWriter(filenameP);
        } catch (IOException e) {
            System.out.println();
            System.out.println("**********I/O ERROR while trying to create the file: " + filenameP + "**********");
        }
        BufferedWriter myFile = new BufferedWriter(myFileWriter);
        return myFile;
    }

    /**
	 * The duplicateFile method opens a file, and duplicates it while at the same time keeps track of the number of lines it duplicated. It returns the number of lines.
	 * @param writeFilenameP A string. It is the name of the file we create to duplicate the contents of the file we read.
	 * @param readFilenameP A string. It is the name of the file we open to read, and duplicate.
	 * @return Returns an integer that denotes the number of lines in the file.
	 */
    public static int duplicateFile(String writeFilenameP, String readFilenameP) {
        BufferedWriter writeFile = createFile(writeFilenameP);
        Scanner readFile = openFile(readFilenameP);
        int countlines = 0;
        while (readFile.hasNextLine()) {
            String textFileLine = readFile.nextLine() + '\n';
            try {
                writeFile.write(textFileLine);
            } catch (IOException e) {
                System.out.println();
                System.out.println("**********I/O ERROR while trying to write a line in the file: " + writeFilenameP + "**********");
            }
            countlines++;
        }
        readFile.close();
        try {
            writeFile.close();
        } catch (IOException e) {
            System.out.println();
            System.out.println("**********I/O ERROR while trying to close the file: " + writeFilenameP + "**********");
        }
        return countlines;
    }
}
