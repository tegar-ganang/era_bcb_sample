package no.uio.edd.utils.fileutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import no.uio.edd.utils.datautils.ExtendableStringList;

public class FileUtils {

    public static String readTextFile(String fullPathFilename) throws IOException {
        StringBuffer sb = new StringBuffer("");
        BufferedReader br = new BufferedReader(new FileReader(fullPathFilename));
        String line = null;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        String returnString = sb.toString();
        return returnString;
    }

    /**
	 * Each line in the text file becomes one record in the array.
	 * 
	 * @param fullPathFilename
	 * @return
	 * @throws IOException
	 */
    public static String[] readTextFileIntoArray(String fullPathFilename) throws IOException {
        ExtendableStringList sb = new ExtendableStringList();
        BufferedReader reader = new BufferedReader(new FileReader(fullPathFilename));
        String strLine;
        while ((strLine = reader.readLine()) != null) sb.addElem(strLine);
        reader.close();
        return sb.getListNoNulls();
    }

    /**
	 * Fast and simple file copy
	 * 
	 * @param source
	 *            Source file
	 * @param dest
	 *            Destination file
	 * @throws IOException
	 */
    public static void copy(File source, File dest) throws IOException {
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    public static void writeTextFile(String path, String contents) throws IOException {
        FileWriter fw = new FileWriter(path);
        PrintWriter pw = new PrintWriter(fw);
        pw.print(contents);
        pw.close();
    }
}
