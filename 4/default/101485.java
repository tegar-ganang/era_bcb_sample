import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import fi.hip.gb.mobile.MobileAgent;
import fi.hip.gb.utils.FileUtils;

/**
 * List or transfer files to/from remove computer.
 * 
 * @author Juho Karppinen
 * @version $Id: FileFetcher.java 1081 2006-06-13 18:47:49Z jkarppin $
 */
@MobileAgent
public class FileFetcher {

    public static final String LIST = "list";

    public static final String DOWNLOAD = "download";

    public static final String UPLOAD = "upload";

    public FileFetcher() {
    }

    /**
	 * Download a file from the remote server.
     * @param sourceFile file to download
     * @return File object
     * @throws FileNotFoundException if file cannot be found
	 */
    public File downloadFile(String sourceFile) throws FileNotFoundException {
        File file = new File(sourceFile);
        if (file.exists() == false) try {
            throw new FileNotFoundException("File " + sourceFile + " not found from host " + InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
        }
        return file;
    }

    /**
     * Uploads a file to the remote server.
     * @param inputFile file to upload
     * @param targetFile target file or directory if ends with a slash 
     */
    public void uploadFile(File inputFile, String targetFile) throws IOException {
        System.out.println("Uploading " + inputFile.getName() + " to " + targetFile);
        File outputFile = new File(targetFile);
        if (targetFile.endsWith("/")) {
            outputFile = new File(outputFile, inputFile.getName());
        } else if (outputFile.getParentFile().exists() == false) {
            outputFile.getParentFile().mkdirs();
        }
        if (inputFile.renameTo(outputFile) == false) {
            InputStream in = new FileInputStream(inputFile);
            OutputStream out = new FileOutputStream(outputFile);
            byte[] line = new byte[16384];
            int bytes = -1;
            while ((bytes = in.read(line)) != -1) out.write(line, 0, bytes);
            in.close();
            out.close();
        }
    }

    /**
	 * Lists the content of directory.
	 * @param path path to be listed. If points to single file, parent directory is listed instead
     * @param recursive list files recursively
	 * @return listing with every line containing one file in format "ISDIRECTORY,FILENAME,FILESIZE"
	 */
    public String listDirectory(String path, boolean recursive) {
        StringBuffer listing = new StringBuffer();
        File directory = new File(path);
        File[] files = directory.isDirectory() ? directory.listFiles() : directory.getParentFile().listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                listing.append(files[i].isDirectory()).append(";");
                listing.append(files[i]).append(";");
                listing.append(files[i].length());
                listing.append("\n");
            }
        }
        return listing.toString();
    }

    public void finalize() throws Throwable {
        System.out.println("finalize() called");
        super.finalize();
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            FileFetcher ff = new FileFetcher();
            if ("up".equals(args[0])) {
                File local = new File(args[1]);
                String remote = local.getName();
                if (args.length > 2) {
                    remote = args[2];
                }
                ff.uploadFile(local, remote);
            } else if ("down".equals(args[0])) {
                File local = ff.downloadFile(args[1]);
                if (args.length > 2) {
                    File target = new File(args[2]);
                    local.renameTo(target);
                    local = target;
                }
                System.out.println("Downloaded to " + local.getPath());
            } else {
                System.out.println("Listing: " + ff.listDirectory(args[0], false));
            }
        } else {
            System.out.println("Usage: FileFether up    localfile         [remotefile/folder]");
            System.out.println("       FileFether down  remotefile/folder [localfile]");
            System.out.println("       FileFether remotefolder");
            System.out.println("Running a test case...");
            File upfile = new File("up.txt");
            FileUtils.writeFile(upfile.getAbsolutePath(), "mycontent");
            FileFetcher file = new FileFetcher();
            System.out.println("Uploadeding file with content '" + readFile(upfile) + "'");
            file.uploadFile(upfile, "/remotefile.txt");
            file = new FileFetcher();
            File downfile = file.downloadFile("/remotefile.txt");
            System.out.println("Downloaded file with content '" + readFile(downfile) + "'");
        }
    }

    /**
     * Reads the content of a file.
     * @param inputFile file to be read
     * @return content of the file
     * @throws IOException
     */
    private static String readFile(File inputFile) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = new FileInputStream(inputFile);
        byte[] line = new byte[16384];
        int bytes = -1;
        while ((bytes = in.read(line)) != -1) out.write(line, 0, bytes);
        in.close();
        out.close();
        return out.toString();
    }
}
