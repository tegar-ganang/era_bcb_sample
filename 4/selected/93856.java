package proper.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
* This class just copies a files from one location to another one.<br>
* Originally taken from <a href="http://www.experts-exchange.com/Programming/Programming_Languages/Java/Q_10245809.html">here</a>.
*
* @author      FracPete
* @version $Revision: 1.2 $
*/
public class CopyFile {

    /**
   * tries to copy the given file to the new location
   */
    public static boolean copy(String from, String to) {
        boolean result;
        String newLine;
        FileInputStream input;
        FileOutputStream output;
        File source;
        int fileLength;
        byte byteBuff[];
        result = false;
        input = null;
        output = null;
        source = null;
        try {
            input = new FileInputStream(from);
            output = new FileOutputStream(to);
            source = new File(from);
            fileLength = (int) source.length();
            byteBuff = new byte[fileLength];
            while (input.read(byteBuff, 0, fileLength) != -1) output.write(byteBuff, 0, fileLength);
            result = true;
        } catch (FileNotFoundException e) {
            System.out.println(from + " does not exist!");
        } catch (IOException e) {
            System.out.println("Error reading/writing files!");
        } finally {
            try {
                if (input != null) input.close();
                if (output != null) output.close();
            } catch (IOException e) {
            }
        }
        return result;
    }

    /**
   * for testing only
   */
    public static void main(String[] args) {
        boolean result;
        if (args.length != 2) {
            System.out.println("Command Syntax: " + CopyFile.class.getName() + " <source> <dest>");
            System.out.println();
            System.out.println("<source>\tThe source file name (path optional)");
            System.out.println("<dest>\t\tThe destination file name (path optional)");
            System.out.println();
            System.exit(1);
        } else {
            result = copy(args[0], args[1]);
            System.out.println("Copying " + args[0] + " to " + args[1] + " = " + result);
        }
    }
}
