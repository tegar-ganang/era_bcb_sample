import java.io.*;

/**
* This class is a standalone program to copy a file, and also defines a 
* static copy() method that other programs can use to copy files.
**/
public class FileCopy {

    /**
* The static method that actually performs the file copy.
* Before copying the file, however, it performs a lot of tests to make
* sure everything is as it should be.
*/
    public static void copy(String from_name, String to_name, boolean overwriteOk) throws IOException {
        File from_file = new File(from_name);
        File to_file = new File(to_name);
        if (!from_file.exists()) abort("FileCopy: no such source file: " + from_name);
        if (!from_file.isFile()) abort("FileCopy: can't copy directory: " + from_name);
        if (!from_file.canRead()) abort("FileCopy: source file is unreadable: " + from_name);
        if (to_file.isDirectory()) to_file = new File(to_file, from_file.getName());
        if (to_file.exists()) {
            if (!to_file.canWrite()) abort("FileCopy: destination file is unwriteable: " + to_name);
            if (!overwriteOk) {
                System.out.print("Overwrite existing file " + to_name + "? (Y/N): ");
                System.out.flush();
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                String response = in.readLine();
                if (!response.equals("Y") && !response.equals("y")) abort("FileCopy: existing file was not overwritten.");
            }
        } else {
            String parent = to_file.getParent();
            if (parent == null) parent = System.getProperty("user.dir");
            File dir = new File(parent);
            if (!dir.exists()) abort("FileCopy: destination directory doesn't exist: " + parent);
            if (dir.isFile()) abort("FileCopy: destination is not a directory: " + parent);
            if (!dir.canWrite()) abort("FileCopy: destination directory is unwriteable: " + parent);
        }
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(from_file);
            to = new FileOutputStream(to_file);
            byte[] buffer = new byte[4096];
            int bytes_read;
            while ((bytes_read = from.read(buffer)) != -1) to.write(buffer, 0, bytes_read);
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
                ;
            }
            if (to != null) try {
                to.close();
            } catch (IOException e) {
                ;
            }
        }
    }

    /** A convenience method to throw an exception */
    private static void abort(String msg) throws IOException {
        throw new IOException(msg);
    }
}
