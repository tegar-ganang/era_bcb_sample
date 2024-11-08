package goldengate.common.tar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * TAR support
 * @author Frederic Bregier
 *
 */
public class TarUtility {

    /**
     * Create a new Tar from a root directory
     * @param directory the base directory
     * @param filename the output filename
     * @param absolute store absolute filepath (from directory) or only filename
     * @return True if OK
     */
    public static boolean createTarFromDirectory(String directory, String filename, boolean absolute) {
        File rootDir = new File(directory);
        File saveFile = new File(filename);
        TarArchiveOutputStream taos;
        try {
            taos = new TarArchiveOutputStream(new FileOutputStream(saveFile));
        } catch (FileNotFoundException e) {
            return false;
        }
        taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        try {
            recurseFiles(rootDir, rootDir, taos, absolute);
        } catch (IOException e2) {
            try {
                taos.close();
            } catch (IOException e) {
            }
            return false;
        }
        try {
            taos.finish();
        } catch (IOException e1) {
        }
        try {
            taos.flush();
        } catch (IOException e) {
        }
        try {
            taos.close();
        } catch (IOException e) {
        }
        return true;
    }

    /**
     * Recursive traversal to add files
     * @param root
     * @param file
     * @param taos
     * @param absolute
     * @throws IOException 
     */
    private static void recurseFiles(File root, File file, TarArchiveOutputStream taos, boolean absolute) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File file2 : files) {
                recurseFiles(root, file2, taos, absolute);
            }
        } else if ((!file.getName().endsWith(".tar")) && (!file.getName().endsWith(".TAR"))) {
            String filename = null;
            if (absolute) {
                filename = file.getAbsolutePath().substring(root.getAbsolutePath().length());
            } else {
                filename = file.getName();
            }
            TarArchiveEntry tae = new TarArchiveEntry(filename);
            tae.setSize(file.length());
            taos.putArchiveEntry(tae);
            FileInputStream fis = new FileInputStream(file);
            IOUtils.copy(fis, taos);
            taos.closeArchiveEntry();
        }
    }

    /**
     * Create a new Tar from a list of Files (only name of files will be used)
     * @param files list of files to add
     * @param filename the output filename
     * @return True if OK
     */
    public static boolean createTarFromFiles(List<File> files, String filename) {
        File saveFile = new File(filename);
        TarArchiveOutputStream taos;
        try {
            taos = new TarArchiveOutputStream(new FileOutputStream(saveFile));
        } catch (FileNotFoundException e) {
            return false;
        }
        taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        for (File file : files) {
            try {
                addFile(file, taos);
            } catch (IOException e) {
                try {
                    taos.close();
                } catch (IOException e1) {
                }
                return false;
            }
        }
        try {
            taos.finish();
        } catch (IOException e1) {
        }
        try {
            taos.flush();
        } catch (IOException e) {
        }
        try {
            taos.close();
        } catch (IOException e) {
        }
        return true;
    }

    /**
     * Create a new Tar from an array of Files (only name of files will be used)
     * @param files array of files to add
     * @param filename the output filename
     * @return True if OK
     */
    public static boolean createTarFromFiles(File[] files, String filename) {
        File saveFile = new File(filename);
        TarArchiveOutputStream taos;
        try {
            taos = new TarArchiveOutputStream(new FileOutputStream(saveFile));
        } catch (FileNotFoundException e) {
            return false;
        }
        taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        for (File file : files) {
            try {
                addFile(file, taos);
            } catch (IOException e) {
                try {
                    taos.close();
                } catch (IOException e1) {
                }
                return false;
            }
        }
        try {
            taos.finish();
        } catch (IOException e1) {
        }
        try {
            taos.flush();
        } catch (IOException e) {
        }
        try {
            taos.close();
        } catch (IOException e) {
        }
        return true;
    }

    /**
     * Recursive traversal to add files
     * @param file
     * @param taos
     * @throws IOException 
     */
    private static void addFile(File file, TarArchiveOutputStream taos) throws IOException {
        String filename = null;
        filename = file.getName();
        TarArchiveEntry tae = new TarArchiveEntry(filename);
        tae.setSize(file.length());
        taos.putArchiveEntry(tae);
        FileInputStream fis = new FileInputStream(file);
        IOUtils.copy(fis, taos);
        taos.closeArchiveEntry();
    }

    /**
     * Extract all files from Tar into the specified directory
     * @param tarFile
     * @param directory
     * @return the list of extracted filenames
     * @throws IOException
     */
    public static List<String> unTar(File tarFile, File directory) throws IOException {
        List<String> result = new ArrayList<String>();
        InputStream inputStream = new FileInputStream(tarFile);
        TarArchiveInputStream in = new TarArchiveInputStream(inputStream);
        TarArchiveEntry entry = in.getNextTarEntry();
        while (entry != null) {
            OutputStream out = new FileOutputStream(new File(directory, entry.getName()));
            IOUtils.copy(in, out);
            out.close();
            result.add(entry.getName());
            entry = in.getNextTarEntry();
        }
        in.close();
        return result;
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("You need to provide 3 arguments:\n" + "   option filedest.tar \"source\"\n" + "   where option=1 means untar and source is a directory\n" + "   option=2 means tar and source is a directory\n" + "   option=3 means tar and source is a list of files comma separated");
            System.exit(1);
        }
        int option = Integer.parseInt(args[0]);
        String tarfile = args[1];
        String tarsource = args[2];
        String[] tarfiles = null;
        if (option == 3) {
            tarfiles = args[2].split(",");
            File[] files = new File[tarfiles.length];
            for (int i = 0; i < tarfiles.length; i++) {
                files[i] = new File(tarfiles[i]);
            }
            if (createTarFromFiles(files, tarfile)) {
                System.out.println("TAR OK from multiple files");
            } else {
                System.err.println("TAR KO from multiple files");
            }
        } else if (option == 2) {
            if (createTarFromDirectory(tarsource, tarfile, false)) {
                System.out.println("TAR OK from directory");
            } else {
                System.err.println("TAR KO from directory");
            }
        } else if (option == 1) {
            File tarFile = new File(tarfile);
            File directory = new File(tarsource);
            List<String> result = null;
            try {
                result = unTar(tarFile, directory);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (result == null || result.isEmpty()) {
                System.err.println("UNTAR KO from directory");
            } else {
                for (String string : result) {
                    System.out.println("File: " + string);
                }
            }
        }
    }
}
